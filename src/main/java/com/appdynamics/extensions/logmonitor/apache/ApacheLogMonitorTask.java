/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.closeRandomAccessFile;
import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.resolvePath;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bitbucket.kienerj.OptimizedRandomAccessFile;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.RequestClassification;
import com.appdynamics.extensions.logmonitor.apache.exceptions.FileException;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.GroupMetrics;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;

/**
 * @author Florencio Sarmiento
 *
 */
public class ApacheLogMonitorTask implements Callable<ApacheLogMetrics> {
	
	private static final Logger LOGGER = 
			Logger.getLogger("com.singularity.extensions.logmonitor.apache.ApacheLogMonitorTask");
	
	private FilePointerProcessor filePointerProcessor;
	
	private ApacheLog apacheLogConfig;
	
	private String grokPatternFilePath;
	
	private String userAgentPatternFilePath;
	
	public ApacheLogMonitorTask(FilePointerProcessor filePointerProcessor,
			String grokPatternFilePath,
			String userAgentPatternFilePath,
			ApacheLog apacheLogConfig) {
		
		this.filePointerProcessor = filePointerProcessor;
		this.grokPatternFilePath = grokPatternFilePath;
		this.userAgentPatternFilePath = userAgentPatternFilePath;
		this.apacheLogConfig = apacheLogConfig;
	}

	public ApacheLogMetrics call() throws Exception {
		String dirPath = resolveDirPath(apacheLogConfig.getLogDirectory());
		LOGGER.info("Apache Log monitor task started...");
		
		ApacheLogMetrics logMetrics = new ApacheLogMetrics();
		logMetrics.setApacheLogName(getApacheLogName());
		
		//Initialize Request Classification Metrics
		List<RequestClassification> requestClassifications = apacheLogConfig.getRequestClassifications();
		GroupMetrics requestClassMetrics = logMetrics.getRequestClassificationMetrics();
		for(RequestClassification requestClass : requestClassifications) {
			requestClassMetrics.addMember(requestClass.getRequestName());
		}
		
		OptimizedRandomAccessFile randomAccessFile = null;
		long curFilePointer = 0;
		
		try {
			MetricsExtractor metricsExtractor = new MetricsExtractor(
					grokPatternFilePath,
					userAgentPatternFilePath,
					apacheLogConfig);
			
			File file = getLogFile(resolvePath(dirPath));
			randomAccessFile = new OptimizedRandomAccessFile(file, "r");
			long fileSize = randomAccessFile.length();
			String dynamicLogPath = dirPath + apacheLogConfig.getLogName();
			curFilePointer = getCurrentFilePointer(dynamicLogPath, file.getPath(), fileSize);
			
			LOGGER.info(String.format("Processing log file [%s], starting from [%s]", 
					file.getPath(), curFilePointer));
			
			randomAccessFile.seek(curFilePointer);
			
			String currentLine = null;
			
			while((currentLine = randomAccessFile.readLine()) != null) {
				metricsExtractor.extractMetrics(currentLine, logMetrics);
				curFilePointer = randomAccessFile.getFilePointer();
			}
			
			setNewFilePointer(dynamicLogPath, file.getPath(), curFilePointer);
			
			LOGGER.info(String.format("Sucessfully processed log file [%s]", 
					file.getPath()));
			
			
		} finally {
			closeRandomAccessFile(randomAccessFile);
		}
		
		return logMetrics;
	}
    
    private long getCurrentFilePointer(String dynamicLogPath, 
    		String actualLogPath, long fileSize) {
    	
    	FilePointer filePointer = 
    			filePointerProcessor.getFilePointer(dynamicLogPath, actualLogPath);
    	
    	long currentPosition = filePointer.getLastReadPosition().get();
    	
    	if (isFilenameChanged(filePointer.getFilename(), actualLogPath) || 
    		isLogRotated(fileSize, currentPosition)) {
    		
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug("Filename has either changed or rotated, resetting position to 0");
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
	
	private File getLogFile(String dirPath) throws FileNotFoundException {
		File directory = new File(dirPath);
		File logFile = null;
		
		if (directory.isDirectory()) {
			FileFilter fileFilter = new WildcardFileFilter(apacheLogConfig.getLogName());
			File[] files = directory.listFiles(fileFilter);
			
			if (files != null && files.length > 0) {
				logFile = getLatestFile(files);
				
				if (!logFile.canRead()) {
					throw new FileException(
							String.format("Unable to read file [%s]", logFile.getPath()));
				}
				
			} else {
				throw new FileNotFoundException(
						String.format("Unable to find any file with name [%s] in [%s]", 
								apacheLogConfig.getLogName(), dirPath));
			}
			
		} else {
			throw new FileNotFoundException(
					String.format("Directory [%s] not found. Ensure it is a directory.", 
							dirPath));
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
	
	private String getApacheLogName() {
		return StringUtils.isBlank(apacheLogConfig.getDisplayName()) ? 
				apacheLogConfig.getLogName() : apacheLogConfig.getDisplayName();
	}
	
}
