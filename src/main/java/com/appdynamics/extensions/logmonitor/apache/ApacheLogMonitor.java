/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.Constants.*;
import static com.appdynamics.extensions.logmonitor.apache.config.DefaultValues.*;
import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.*;
import static com.appdynamics.extensions.yml.YmlReader.readFromFile;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.Configuration;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.GroupMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.Metrics;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

/**
 * @author Florencio Sarmiento
 *
 */
public class ApacheLogMonitor extends AManagedMonitor {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.logmonitor.apache.ApacheLogMonitor");
	
	private volatile FilePointerProcessor filePointerProcessor;
	
	public ApacheLogMonitor() {
		LOGGER.info(String.format("Using Apache Log Monitor Version [%s]", 
				getImplementationVersion()));
		filePointerProcessor = new FilePointerProcessor();
	}

	public TaskOutput execute(Map<String, String> args,
			TaskExecutionContext arg1) throws TaskExecutionException {
		
		LOGGER.info("Starting Apache Log Monitoring task");
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("Args received were: %s", args));
		}
		
		if (args != null) {
			ExecutorService threadPool = null;
			
			String confDir = resolveConfigDir(args.get(CONFIG_DIR));
			String configFilename = confDir + CONFIG_FILE;
			String grokFilePath = confDir + GROK_FILE;
			String userAgentFilePath = confDir + USER_AGENT_FILE;
			
			try {
				Configuration config = readFromFile(configFilename, Configuration.class);
				
				String lastReadPointerFileLocation = config.getLastReadPointerFileLocation();
				if(lastReadPointerFileLocation == null || lastReadPointerFileLocation.equals("")) {
					if(LOGGER.isDebugEnabled()) {
						LOGGER.debug("Can't find lastReadPointerFileLocation in the configuration file. Initializing default.");
					}
					filePointerProcessor.initialize();
				}else {
					if(LOGGER.isDebugEnabled()) {
						LOGGER.debug(String.format("lastReadPointerFileLocation in the configuration file is %s",lastReadPointerFileLocation));
					}
					filePointerProcessor.initialize(lastReadPointerFileLocation);
				}
				//System.out.println("Configuration: " + config);
				
				int noOfThreads = config.getNoOfThreads() > 0 ? config.getNoOfThreads() : DEFAULT_NO_OF_THREADS;
				threadPool = Executors.newFixedThreadPool(noOfThreads);
				
				CompletionService<ApacheLogMetrics> apacheLogMonitorTasks =
						createConcurrentTasks(threadPool, config, grokFilePath, userAgentFilePath);
				
				List<ApacheLogMetrics> apacheLogMetricsList = collectMetrics(apacheLogMonitorTasks,
						config.getApacheLogs().size());
				
				uploadMetrics(config, apacheLogMetricsList);
				
				filePointerProcessor.updateFilePointerFile();
				
				return new TaskOutput("Apache Log Monitoring task successfully completed");
				
			} catch (Exception ex) {
				LOGGER.error("Unfortunately an issue has occurred: ", ex);
				
			} finally {
				if (threadPool != null && !threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
		}
		
		throw new TaskExecutionException("Apache Log Monitoring task completed with failures.");
	}
	
	private CompletionService<ApacheLogMetrics> createConcurrentTasks(ExecutorService threadPool,
			Configuration config, 
			String grokFilePath, 
			String userAgentFilePath) {
		CompletionService<ApacheLogMetrics> apacheLogMonitorTasks = 
				new ExecutorCompletionService<ApacheLogMetrics>(threadPool);
		
		for (ApacheLog apacheLog : config.getApacheLogs()) {
			ApacheLogMonitorTask task = new ApacheLogMonitorTask(
					filePointerProcessor, 
					grokFilePath, 
					userAgentFilePath, 
					apacheLog);
			apacheLogMonitorTasks.submit(task);
		}
		
		return apacheLogMonitorTasks;
	}
	
	private List<ApacheLogMetrics> collectMetrics(CompletionService<ApacheLogMetrics> parallelTasks,
			int noOfApacheLogMonitorTasks) {
		
		List<ApacheLogMetrics> apacheLogMetricsList = new ArrayList<ApacheLogMetrics>();
		
		for (int i=0; i<noOfApacheLogMonitorTasks; i++) {
			
			try {
				ApacheLogMetrics collectedMetrics = 
						parallelTasks.take().get(THREAD_TIMEOUT, TimeUnit.SECONDS);
				apacheLogMetricsList.add(collectedMetrics);
				
			} catch (InterruptedException e) {
				LOGGER.error("Task interrupted. ", e);
				
			} catch (ExecutionException e) {
				LOGGER.error("Task execution failed. ", e);
				
			} catch (TimeoutException e) {
				LOGGER.error("Task timed out. ", e);
			}
		}
		
		return apacheLogMetricsList;
	}
    
    private void uploadMetrics(Configuration config, List<ApacheLogMetrics> apacheLogMetricsList) {
    	for (ApacheLogMetrics apacheLogMetrics : apacheLogMetricsList) {
    		String apacheLogPrefix = String.format("%s%s%s", getMetricPrefix(config),
    				apacheLogMetrics.getApacheLogName(), METRIC_PATH_SEPARATOR);
    		
    		uploadSummaryMetrics(config, apacheLogPrefix, apacheLogMetrics);
    		uploadAllMetrics(config, apacheLogPrefix, BROWSER, apacheLogMetrics.getBrowserMetrics());
    		uploadAllMetrics(config, apacheLogPrefix, OS, apacheLogMetrics.getOsMetrics());
    		uploadAllMetrics(config, apacheLogPrefix, SPIDER, apacheLogMetrics.getSpiderMetrics());
    		uploadAllMetrics(config, apacheLogPrefix, VISITOR, apacheLogMetrics.getVisitorMetrics());
    		uploadPageMetrics(config, apacheLogPrefix, apacheLogMetrics.getPageMetrics());
    		uploadRequestClassMetrics(config,apacheLogPrefix,apacheLogMetrics.getRequestClassificationMetrics());
    		uploadResponseCodeMetrics(config, apacheLogPrefix, apacheLogMetrics.getResponseCodeMetrics());
    	}
    }
    
    private void uploadSummaryMetrics(Configuration config, String apacheLogPrefix, ApacheLogMetrics apacheLogMetrics) {
    	printCollectiveObservedSum(apacheLogPrefix + TOTAL_HITS, 
    			apacheLogMetrics.getTotalHitCount());
    	printCollectiveObservedSum(apacheLogPrefix + TOTAL_BANDWIDTH, 
    			apacheLogMetrics.getTotalBandwidth());
    	printCollectiveObservedSum(apacheLogPrefix + TOTAL_PAGES, 
    			apacheLogMetrics.getTotalPageViewCount());
    	printCollectiveObservedSum(apacheLogPrefix + TOTAL_FAILURES, 
    			apacheLogMetrics.getTotalFailureCount());
    	printCollectiveObservedCurrent(apacheLogPrefix + ERROR_RATE_PERCENTAGE, 
    			apacheLogMetrics.getErrorRatePercentage());
    	printCollectiveObservedCurrent(apacheLogPrefix + AVERGAGE_RESPONSE_TIME, 
    			apacheLogMetrics.getAvgResponseTime());
    	
    	if(config.hasPercentiles()) {
    		Set<Integer> percentiles = config.getIncludeResponseTimePercentiles();
    		
    		for(Integer percentile : percentiles) {
    			printCollectiveObservedCurrent(apacheLogPrefix + String.format(RESPONSE_TIME_PERCENTILE, percentile), 
    	    			apacheLogMetrics.getResponseTimePercentile(percentile));
    		}
    	}
    }
    
    private void uploadAllMetrics(Configuration config, String apacheLogPrefix, String groupName, 
    		GroupMetrics groupMetrics) {
    	String groupPrefix = createGroupPrefix(apacheLogPrefix, groupName);	
    	uploadGroupMetrics(config, groupPrefix, groupMetrics, true);
    	uploadMemberMetrics(config, groupPrefix, groupMetrics, true);
    }
    
    private void uploadPageMetrics(Configuration config, String apacheLogPrefix, GroupMetrics groupMetrics) {
    	String groupPrefix = createGroupPrefix(apacheLogPrefix, PAGE);
    	uploadGroupMetrics(config, groupPrefix, groupMetrics, false);
    	uploadMemberMetrics(config, groupPrefix, groupMetrics, false);
    }
    
    private void uploadRequestClassMetrics(Configuration config, String apacheLogPrefix, GroupMetrics groupMetrics) {
    	String groupPrefix = createGroupPrefix(apacheLogPrefix, REQUEST_CLASSIFICATIONS);
    	uploadGroupMetrics(config, groupPrefix, groupMetrics, false);
    	uploadMemberMetrics(config, groupPrefix, groupMetrics, false);
    }
    
    private void uploadResponseCodeMetrics(Configuration config, String apacheLogPrefix, GroupMetrics groupMetrics) {
    	String groupPrefix = createGroupPrefix(apacheLogPrefix, RESPONSE_CODE);
    	uploadMemberMetrics(config, groupPrefix, groupMetrics, true);
    }
    
    private void uploadGroupMetrics(Configuration config, String groupPrefix, GroupMetrics groupMetrics, 
    		boolean includePageMetrics) {
    	printCollectiveObservedSum(groupPrefix + TOTAL_HITS, groupMetrics.getHitCount());
    	printCollectiveObservedSum(groupPrefix + TOTAL_BANDWIDTH, groupMetrics.getBandwidth());
    	printCollectiveObservedSum(groupPrefix + TOTAL_FAILURES, groupMetrics.getFailureCount());
    	printCollectiveObservedCurrent(groupPrefix + ERROR_RATE_PERCENTAGE, groupMetrics.getErrorRatePercentage());
    	printCollectiveObservedCurrent(groupPrefix + AVERGAGE_RESPONSE_TIME, groupMetrics.getAvgResponseTime());
    	
    	if (includePageMetrics) {
    		printCollectiveObservedSum(groupPrefix + TOTAL_PAGES, groupMetrics.getPageViewCount());
    	}
    	
    	if(config.hasPercentiles()) {
    		Set<Integer> percentiles = config.getIncludeResponseTimePercentiles();
    		
    		for(Integer percentile : percentiles) {
    			printCollectiveObservedCurrent(groupPrefix + String.format(RESPONSE_TIME_PERCENTILE, percentile), 
    					groupMetrics.getResponseTimePercentile(percentile));
    		}
    	}
    }
    
    private void uploadMemberMetrics(Configuration config, String groupPrefix, GroupMetrics groupMetrics, 
    		boolean includePageMetrics) {
    	for (Map.Entry<String, Metrics> member : groupMetrics.getMembers().entrySet()) {
    		String memberPrefix = String.format("%s%s%s", 
    				groupPrefix, member.getKey(), METRIC_PATH_SEPARATOR);
    		
    		Metrics metrics = member.getValue();
    		printCollectiveObservedSum(memberPrefix + HITS, metrics.getHitCount());
    		printCollectiveObservedSum(memberPrefix + BANDWIDTH, metrics.getBandwidth());
    		
    		if(groupPrefix != null && !groupPrefix.endsWith(String.format("%s%s%s",METRIC_PATH_SEPARATOR,RESPONSE_CODE,METRIC_PATH_SEPARATOR))) {
    			printCollectiveObservedSum(memberPrefix + FAILURES, metrics.getFailureCount());
            	printCollectiveObservedCurrent(memberPrefix + ERROR_RATE_PERCENTAGE, metrics.getErrorRatePercentage());
    		}
    		
        	printCollectiveObservedCurrent(memberPrefix + AVERGAGE_RESPONSE_TIME, metrics.getAvgResponseTime());
        	
        	if (includePageMetrics) {
        		printCollectiveObservedSum(memberPrefix + PAGES, metrics.getPageViewCount());
        	}
        	
        	if(config.hasPercentiles()) {
        		Set<Integer> percentiles = config.getIncludeResponseTimePercentiles();
        		
        		for(Integer percentile : percentiles) {
        			printCollectiveObservedCurrent(memberPrefix + String.format(RESPONSE_TIME_PERCENTILE, percentile), 
        					metrics.getResponseTimePercentile(percentile));
        		}
        	}
    	}
    }
    
    private void printCollectiveObservedCurrent(String metricName, BigInteger metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }
    
    private void printCollectiveObservedSum(String metricName, BigInteger metricValue) {
        printMetric(metricName, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_SUM,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }
    
    private void printMetric(String metricName, BigInteger metricValue, String aggregation, 
    		String timeRollup, String cluster) {
    	
		MetricWriter metricWriter = getMetricWriter(metricName, aggregation,
				timeRollup, cluster);
        
        BigInteger valueToReport = convertValueToZeroIfNullOrNegative(metricValue);
        
        if (LOGGER.isDebugEnabled()) {
        	LOGGER.debug(String.format("Sending [%s/%s/%s] metric = %s = %s => %s",
            		aggregation, timeRollup, cluster,
                    metricName, metricValue, valueToReport));
        }
        
        metricWriter.printMetric(valueToReport.toString());
    }
    
	private String getMetricPrefix(Configuration config) {
		String metricPrefix = config.getMetricPrefix();
		
		if (StringUtils.isBlank(metricPrefix)) {
			metricPrefix = DEFAULT_METRIC_PATH;
			
		} else {
			metricPrefix = metricPrefix.trim();
			
			if (!metricPrefix.endsWith(METRIC_PATH_SEPARATOR)) {
				metricPrefix = metricPrefix + METRIC_PATH_SEPARATOR;
			}
		}
		
		return metricPrefix;
	}
    
    private String createGroupPrefix(String apacheLogPrefix, String groupName) {
    	return String.format("%s%s%s", apacheLogPrefix, 
    			groupName, METRIC_PATH_SEPARATOR);
    }
	
	private String resolveConfigDir(String confDirPath) {
		String resolvedPath = resolvePath(confDirPath);
		
		if (!resolvedPath.endsWith(File.separator)) {
			resolvedPath = resolvedPath + File.separator;
		}
		
		return resolvedPath;
	}
	
	private static String getImplementationVersion() {
		return ApacheLogMonitor.class.getPackage().getImplementationTitle();
	}
}
