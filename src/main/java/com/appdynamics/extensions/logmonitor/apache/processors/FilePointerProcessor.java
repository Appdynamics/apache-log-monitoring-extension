/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.Constants.FILEPOINTER_FILENAME;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;

import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import com.appdynamics.extensions.logmonitor.apache.ApacheLogMonitor;
import org.slf4j.Logger;

/**
 * @author Florencio Sarmiento
 *
 */
public class FilePointerProcessor {
	
	public static final Logger LOGGER = ExtensionsLoggerFactory.getLogger(FilePointerProcessor.class);
	
	private ConcurrentHashMap<String, FilePointer> filePointers = new ConcurrentHashMap<String, FilePointer>();
	
	private ObjectMapper mapper = new ObjectMapper();
	
	public FilePointerProcessor() {
		initialiseFilePointers();
	}
	
	public void updateFilePointer(String dynamicLogPath, 
			String actualLogPath, long lastReadPosition) {
		FilePointer filePointer = getFilePointer(dynamicLogPath, actualLogPath);
		filePointer.setFilename(actualLogPath);
		filePointer.updateLastReadPosition(lastReadPosition);
	}

	public FilePointer getFilePointer(String dynamicLogPath, String actualLogPath) {
		if (filePointers.containsKey(dynamicLogPath)) {
			return filePointers.get(dynamicLogPath);
		}
		
		FilePointer newFilePointer = new FilePointer();
		newFilePointer.setFilename(actualLogPath);
		
		FilePointer previousFilePointer = filePointers.putIfAbsent(dynamicLogPath, newFilePointer);
		return previousFilePointer != null ? previousFilePointer : newFilePointer;
	}
    
    public void updateFilePointerFile() {
    	File file = new File(getFilePointerPath());
    	
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(file, filePointers);
	 
		} catch (Exception ex) {
			LOGGER.error(String.format(
					"Unfortunately an error occurred while saving filepointers to %s", 
					file.getPath()), ex);
		}
    }
	
    private void initialiseFilePointers() {
		LOGGER.info("Initialising filepointers...");
    	
    	File file = new File(getFilePointerPath());
    	
		if (!file.exists()) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Unable to find: " + file.getPath());
			}
			
		} else {
			try {
				filePointers = mapper.readValue(file,
								new TypeReference<ConcurrentHashMap<String, FilePointer>>() {
								});

			} catch (Exception ex) {
				LOGGER.error(String.format(
								"Unfortunately an error occurred while reading filepointer %s",
								file.getPath()), ex);
			}
		}

		LOGGER.info("Filepointers initialised with: " + filePointers);
    }
	
    private String getFilePointerPath() {
    	String path = null;
    	
    	try {
    		URL classUrl = ApacheLogMonitor.class.getResource(
    				ApacheLogMonitor.class.getSimpleName() + ".class");
    		String jarPath = classUrl.toURI().toString();
    		
    		// workaround for jar file
    		jarPath = jarPath.replace("jar:", "").replace("file:", "");
    		
    		if (jarPath.contains("!")) {
    			jarPath = jarPath.substring(0, jarPath.indexOf("!"));
    		}
    		
    		File file = new File(jarPath);
    		String jarDir = file.getParentFile().toURI().getPath();
    		
    		if (jarDir.endsWith(File.separator)) {
    			path = jarDir + FILEPOINTER_FILENAME;
    					
    		} else {
    			path = String.format("%s%s%s", jarDir , 
            			File.separator, FILEPOINTER_FILENAME);
    		}
    		
    	} catch (Exception ex) {
			LOGGER.warn("Unable to resolve installation dir, finding an alternative.");
    	}
    	
    	if (StringUtils.isBlank(path)) {
    		path = String.format("%s%s%s", new File(".").getAbsolutePath(), 
        			File.separator, FILEPOINTER_FILENAME);
    	}
    	
    	try {
			path = URLDecoder.decode(path, "UTF-8");
			
		} catch (UnsupportedEncodingException e) {
			LOGGER.warn(String.format("Unable to decode file path [%s] using UTF-8", path));
		}
    	
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug("Filepointer path: " + path);
    	}
    	
    	return path;
    }
}
