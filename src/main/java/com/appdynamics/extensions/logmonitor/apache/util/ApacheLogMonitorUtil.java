/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.IndividualMetricsToDisplay;
import com.appdynamics.extensions.logmonitor.apache.config.MetricsFilterForCalculation;
import com.appdynamics.extensions.util.PathResolver;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import org.slf4j.Logger;

import static com.appdynamics.extensions.logmonitor.apache.Constants.*;

/**
 * @author Florencio Sarmiento
 *
 */
public class ApacheLogMonitorUtil {

	private static final Logger logger = ExtensionsLoggerFactory.getLogger(ApacheLogMonitorUtil.class);
	
    public static String resolvePath(String filename) {
        if(StringUtils.isBlank(filename)){
            return "";
        }
        
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = String.format("%s%s%s", jarPath, File.separator, filename);
        return configFileName;
    }
    
	public static Pattern createPattern(Set<String> rawPatterns) {
		Pattern pattern = null;
		
		if (rawPatterns != null && !rawPatterns.isEmpty()) {
			StringBuilder rawPatternsStringBuilder = new StringBuilder();
			int index = 0;
			
			for (String rawPattern : rawPatterns) {
				if (index > 0) {
					rawPatternsStringBuilder.append("|");
				}
				
				rawPatternsStringBuilder.append(rawPattern);
				index++;
			}
			
			pattern = Pattern.compile(rawPatternsStringBuilder.toString());
		}
		
		return pattern;
	}
	
	public static final boolean isNotMatch(String name, Pattern pattern) {
		return !isMatch(name, pattern);
	}
	
	public static final boolean isMatch(String name, Pattern pattern) {
		if (name != null && pattern != null) {
			Matcher matcher = pattern.matcher(name);
			
			if (matcher.matches()) {
				return true;
			}
		}
		
		return false;
	}
	
	public static void closeRandomAccessFile(OptimizedRandomAccessFile randomAccessFile) {
		if (randomAccessFile != null) {
			try {
				randomAccessFile.close();
			} catch (IOException e) {}
		}
	}
	
    public static BigInteger convertValueToZeroIfNullOrNegative(BigInteger value) {
    	if (value == null || value.compareTo(BigInteger.ZERO) < 0) {
    		return BigInteger.ZERO;
    	}
    	
    	return value;
    }

    public static List<ApacheLog> getLogsFromConfig(List<Map<String,?>> apacheLogsFromConfig){
		List<ApacheLog> apacheLogs = new ArrayList<ApacheLog>();
    	for(Map<String,?> apacheLogFromConfig: apacheLogsFromConfig){
    		try{
				ApacheLog log = initializeApacheLogs(apacheLogFromConfig);
				validateLogConfigurations(log);
				apacheLogs.add(log);
			}catch(IllegalArgumentException e){
    			logger.error("Error while getting logs from config file for " + apacheLogFromConfig.get(DISPLAY_NAME),e);
			}
		}
		return apacheLogs;
	}

	public static ApacheLog initializeApacheLogs(Map<String,?> apacheLogFromConfig){
    	ApacheLog apacheLog = new ApacheLog();
    	apacheLog.setDisplayName((String) apacheLogFromConfig.get(DISPLAY_NAME));
    	apacheLog.setLogDirectory((String) apacheLogFromConfig.get(LOG_DIRECTORY));
    	apacheLog.setLogName((String)apacheLogFromConfig.get(LOG_NAME));
    	apacheLog.setLogPattern((String)apacheLogFromConfig.get(LOG_PATTERN));
    	apacheLog.setHitResponseCodes(new HashSet<>(((List<Integer>) apacheLogFromConfig.get(HIT_RESPONSE_CODES))));
    	apacheLog.setNonPageExtensions(new HashSet<>((List<String>) apacheLogFromConfig.get(NON_PAGE_EXTENSIONS)));
		apacheLog.setMetricsFilterForCalculation(initializeMetricFilterForCalculation((Map<String, ?>) apacheLogFromConfig.get(METRIC_FILTER_FOR_CALCULATION)));
		apacheLog.setIndividualMetricsToDisplay(initializeIndividualMetricToDisplay((Map<String, ?>) apacheLogFromConfig.get(INDIVIDUAL_METRIC_TO_DISPLAY)));
		return apacheLog;
    }

    public static MetricsFilterForCalculation initializeMetricFilterForCalculation(Map<String,?> metricFilterForCalc){
		MetricsFilterForCalculation metricsFilterForCalculation = new MetricsFilterForCalculation();
		metricsFilterForCalculation.setExcludeVisitors(new HashSet<>((List<String>)metricFilterForCalc.get(EXCLUDE_VISITORS)));
		metricsFilterForCalculation.setExcludeSpiders(new HashSet<>((List<String>)metricFilterForCalc.get(EXCLUDE_SPIDERS)));
		metricsFilterForCalculation.setExcludeUrls(new HashSet<>((List<String>)metricFilterForCalc.get(EXCLUDE_URLS)));
		metricsFilterForCalculation.setExcludeBrowsers(new HashSet<>((List<String>)metricFilterForCalc.get(EXCLUDE_BROWSERS)));
		metricsFilterForCalculation.setExcludeOs(new HashSet<>((List<String>)metricFilterForCalc.get(EXCLUDE_OS)));
    	return metricsFilterForCalculation;
	}

	public static IndividualMetricsToDisplay initializeIndividualMetricToDisplay(Map<String,?> metricFilterForCalc){
		IndividualMetricsToDisplay individualMetricsToDisplay = new IndividualMetricsToDisplay();
		individualMetricsToDisplay.setIncludeVisitors(new HashSet<>((List<String>)metricFilterForCalc.get(INCLUDE_VISITORS)));
		individualMetricsToDisplay.setIncludeSpiders(new HashSet<>((List<String>)metricFilterForCalc.get(INCLUDE_SPIDERS)));
		individualMetricsToDisplay.setIncludePages(new HashSet<>((List<String>)metricFilterForCalc.get(INCLUDE_PAGES)));
		individualMetricsToDisplay.setIncludeBrowsers(new HashSet<>((List<String>)metricFilterForCalc.get(INCLUDE_BROWSERS)));
		individualMetricsToDisplay.setIncludeOs(new HashSet<>((List<String>)metricFilterForCalc.get(INCLUDE_OS)));
		individualMetricsToDisplay.setIncludeResponseCodes(new HashSet<>((List<Integer>)metricFilterForCalc.get(INCLUDE_RESPONSE_CODES)));
		return individualMetricsToDisplay;
	}

    public static void validateLogConfigurations(ApacheLog log){
    	if(Strings.isNullOrEmpty(log.getDisplayName())){
    		throw new IllegalArgumentException("Display name cannot be null or empty");
		}
    	if(Strings.isNullOrEmpty(log.getLogDirectory())){
    		throw new IllegalArgumentException("Log directory cannot be null or empty");
		}
    	if(Strings.isNullOrEmpty(log.getLogName())){
    		throw new IllegalArgumentException("Log file cannot be null or empty");
		}
	}

}
