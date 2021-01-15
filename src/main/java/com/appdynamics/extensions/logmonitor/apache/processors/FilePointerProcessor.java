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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.appdynamics.extensions.logmonitor.apache.ApacheLogMonitor;

/**
 * @author Florencio Sarmiento
 *
 */
public class FilePointerProcessor {
	
	public static final Logger LOGGER = Logger.getLogger("com.singularity.extensions.logmonitor.apache.FilePointerProcessor");
	
	private ConcurrentHashMap<String, FilePointer> filePointers = new ConcurrentHashMap<String, FilePointer>();
	
	private ObjectMapper mapper = new ObjectMapper();
	
	private boolean initialized = false;
	
	private String filePointerPath = null;
	
	public FilePointerProcessor() {
		//initialiseFilePointers();
	}
	
	public void updateFilePointer(String dynamicLogPath, 
			String actualLogPath, long lastReadPosition) {
		FilePointer filePointer = getFilePointer(dynamicLogPath, actualLogPath);
		filePointer.setFilename(actualLogPath);
		filePointer.updateLastReadPosition(lastReadPosition);
	}

	public FilePointer getFilePointer(String dynamicLogPath, String actualLogPath) {
		if(!initialized) {
    		initialize();
    	}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug(String.format("getFilePointer called. dynamicLogPath=%s, actualLogPath=%s",dynamicLogPath,actualLogPath));
		}
		dynamicLogPath = dynamicLogPath.replace("\\", "/");
		
		if (filePointers.containsKey(dynamicLogPath)) {
			return filePointers.get(dynamicLogPath);
		}
		
		FilePointer newFilePointer = new FilePointer();
		newFilePointer.setFilename(actualLogPath);
		
		FilePointer previousFilePointer = filePointers.putIfAbsent(dynamicLogPath, newFilePointer);
		return previousFilePointer != null ? previousFilePointer : newFilePointer;
	}
    
    public void updateFilePointerFile() {
    	if(!initialized) {
    		initialize();
    	}
    	
    	File file = new File(this.filePointerPath);
    	
		try {
			mapper.writerWithDefaultPrettyPrinter().writeValue(file, filePointers);
	 
		} catch (Exception ex) {
    		LOGGER.error(String.format(
					"Unfortunately an error occurred while saving filepointers to %s", 
					file.getPath()), ex);
		}
    }
	
    public void initialize() {
    	initialiseFilePointers();
    }
    
    public void initialize(String filePointerRootFolder) {
    	String filePointerPath = null;
    			
    	if(!initialized) {
    		
    		if(filePointerRootFolder == null || filePointerRootFolder.equals("")) {
    			filePointerPath = getFilePointerPath();
    		}else {
    			filePointerPath = filePointerRootFolder;
    			if (filePointerRootFolder.endsWith(File.separator)) {
    				filePointerPath = filePointerRootFolder + FILEPOINTER_FILENAME;
        					
        		} else {
        			filePointerPath = String.format("%s%s%s", filePointerRootFolder , 
                			File.separator, FILEPOINTER_FILENAME);
        		}
    		}
    		
    		initializeFilePointerWithFullPath(filePointerPath);
    	}else {
    		if(LOGGER.isDebugEnabled()) {
    			LOGGER.debug("File Pointers Already Initialized. filePointers = " + filePointers);
    		}
    	}
    }
    
    private void initializeFilePointerWithFullPath(String filePointerPath) {
    	if(!initialized) {
    		
    		this.filePointerPath = filePointerPath;
    		LOGGER.info("Initialising filepointers using path " + this.filePointerPath + "...");
        	
        	File file = new File(this.filePointerPath);
        	
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
    		
    		initialized = true;
    	}else {
    		if(LOGGER.isDebugEnabled()) {
    			LOGGER.debug("File Pointers Already Initialized. filePointers = " + filePointers);
    		}
    	}   	
    }
    
    private void initialiseFilePointers() {
    	initializeFilePointerWithFullPath(getFilePointerPath());
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
