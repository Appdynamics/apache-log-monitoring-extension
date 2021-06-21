/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.Constants.*;
import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import com.appdynamics.extensions.AMonitorTaskRunnable;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.metrics.Metric;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;
import org.slf4j.Logger;

/**
 * @author Florencio Sarmiento
 *
 */
public class ApacheLogMonitorTask implements AMonitorTaskRunnable {

	private static final Logger logger = ExtensionsLoggerFactory.getLogger(ApacheLogMonitorTask.class);

	private  MonitorContextConfiguration contextConfiguration;
	private MetricWriteHelper metricWriteHelper;
	private  FilePointerProcessor filePointerProcessor;
	private String grokPatternFilePath;
	private String userAgentPatternFilePath;
	private ApacheLog apacheLogConfig;
	private String metricPrefix;

	public ApacheLogMonitorTask(MonitorContextConfiguration contextConfiguration, MetricWriteHelper metricWriteHelper,
								FilePointerProcessor filePointerProcessor, String grokPatternFilePath, String userAgentPatternFilePath, ApacheLog apacheLogConfig){

		this.contextConfiguration=contextConfiguration;
		this.metricWriteHelper=metricWriteHelper;
		this.filePointerProcessor=filePointerProcessor;
		this.grokPatternFilePath=grokPatternFilePath;
		this.userAgentPatternFilePath=userAgentPatternFilePath;
		this.apacheLogConfig=apacheLogConfig;
		this.metricPrefix=contextConfiguration.getMetricPrefix()+METRIC_PATH_SEPARATOR+apacheLogConfig.getDisplayName()+METRIC_PATH_SEPARATOR;
	}

	@Override
	public void run() {

		String dirPath = resolveDirPath(apacheLogConfig.getLogDirectory());

		ApacheLogMetrics logMetrics = new ApacheLogMetrics();
		logMetrics.setApacheLogName(apacheLogConfig.getDisplayName());

		OptimizedRandomAccessFile randomAccessFile = null;
		long curFilePointer = 0;

		try {
			MetricsExtractor metricsExtractor = new MetricsExtractor(grokPatternFilePath, userAgentPatternFilePath, apacheLogConfig);

			File file = getLogFile(resolvePath(dirPath));
			randomAccessFile = new OptimizedRandomAccessFile(file, "r");
			long fileSize = randomAccessFile.length();
			String dynamicLogPath = dirPath + apacheLogConfig.getLogName();
			curFilePointer = getCurrentFilePointer(dynamicLogPath, file.getPath(), fileSize);

			logger.info(String.format("Processing log file [%s], starting from [%s]", file.getPath(), curFilePointer));

			randomAccessFile.seek(curFilePointer);

			String currentLine = null;

			while((currentLine = randomAccessFile.readLine()) != null) {
				metricsExtractor.extractMetrics(currentLine, logMetrics);
				curFilePointer = randomAccessFile.getFilePointer();
			}

			setNewFilePointer(dynamicLogPath, file.getPath(), curFilePointer);

			logger.info(String.format("Sucessfully processed log file [%s]",
					file.getPath()));

			logger.info("Starting metric generation for log name "+apacheLogConfig.getDisplayName());
			MetricGenerator metricGenerator = new MetricGenerator(logMetrics,metricPrefix);
			List<Metric> metricListToUpload = metricGenerator.uploadMetrics();
			logger.info(String.format("Number of metrics to upload for %s are %d",apacheLogConfig.getDisplayName(),metricListToUpload.size()));
			if(metricListToUpload!=null && metricListToUpload.size()>0){
				metricWriteHelper.transformAndPrintMetrics(metricListToUpload);
			} else{
				logger.warn("There are no metrics to upload for log name "+apacheLogConfig.getDisplayName()+" as metric list is empty!!!");
			}

		}catch(Exception e){
			logger.error("Error occurred while running task for "+apacheLogConfig.getDisplayName(), e);

		} finally {
			closeRandomAccessFile(randomAccessFile);
			filePointerProcessor.updateFilePointerFile();
		}
	}
    
    private long getCurrentFilePointer(String dynamicLogPath, 
    		String actualLogPath, long fileSize) {
    	
    	FilePointer filePointer = filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
    	
    	long currentPosition = filePointer.getLastReadPosition().get();
    	
    	if (isFilenameChanged(filePointer.getFilename(), actualLogPath) || 
    		isLogRotated(fileSize, currentPosition)) {
    		
    		if (logger.isDebugEnabled()) {
    			logger.debug("Filename has either changed or rotated, resetting position to 0");
    		}

    		currentPosition = 0;
    	} 
    	
    	return currentPosition;
    }
	
	private boolean isLogRotated(long fileSize, long startPosition) {
		return fileSize < startPosition;
	}
	
	private boolean isFilenameChanged(String oldFilename, String newFilename) {
		return !oldFilename.equals(newFilename);
	}
	
	private File getLogFile(String dirPath) throws Exception {
		File directory = new File(dirPath);
		File logFile = null;
		
		if (directory.isDirectory()) {
			FileFilter fileFilter = new WildcardFileFilter(apacheLogConfig.getLogName());
			File[] files = directory.listFiles(fileFilter);
			
			if (files != null && files.length > 0) {
				logFile = getLatestFile(files);
				
				if (!logFile.canRead()) {
					throw new IOException(String.format("Unable to read file [%s]", logFile.getPath()));
				}
				
			} else {
				throw new FileNotFoundException(
						String.format("Unable to find any file with name [%s] in [%s]", apacheLogConfig.getLogName(), dirPath));
			}
			
		} else {
			throw new FileNotFoundException(
					String.format("Directory [%s] not found. Ensure it is a directory.", dirPath));
		}
		
		return logFile;
	}
	
	private String resolveDirPath(String confDirPath) {
		String resolvedPath = resolvePath(confDirPath);
		
		if (!resolvedPath.endsWith(File.separator)) {
			resolvedPath = resolvedPath + File.separator;
		}
		
		return resolvedPath;
	}
	
	private File getLatestFile(File[] files) {
		File latestFile = null;
		long lastModified = Long.MIN_VALUE;
		
		for (File file : files) {
			if (file.lastModified() > lastModified) {
				latestFile = file;
				lastModified = file.lastModified();
			}
		}
		
		return latestFile;
	}
	
	private void setNewFilePointer(String dynamicLogPath, 
    		String actualLogPath, long lastReadPosition) {
		filePointerProcessor.updateFilePointer(dynamicLogPath, actualLogPath, lastReadPosition);
	}

	@Override
	public void onTaskComplete() {
		logger.info("Task completed for Logs "+apacheLogConfig.getDisplayName());
	}
}
