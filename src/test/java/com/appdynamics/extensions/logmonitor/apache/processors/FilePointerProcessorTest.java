/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.Constants.FILEPOINTER_FILENAME;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class FilePointerProcessorTest {
	
	private FilePointerProcessor classUnderTest;
	
	@Test
	public void testUpdateFilePointerFileIsPersisted() {
		classUnderTest = new FilePointerProcessor();
		String logPath = "src/test/resources/test-logs/access.log";
		
		FilePointer origFilePointer =  classUnderTest.getFilePointer(logPath, logPath);
		assertEquals(0, origFilePointer.getLastReadPosition().get());
		
		// lets update the filePointer
		long newFilePointer = 1234;
		String newFilename = "src/test/resources/test-logs/access2.log";
		origFilePointer.setFilename(newFilename);
		origFilePointer.updateLastReadPosition(newFilePointer);
		classUnderTest.updateFilePointerFile();
		
		// re-initialise the filepointer 
		// it should pick up from the file
		classUnderTest = new FilePointerProcessor();
		FilePointer result = classUnderTest.getFilePointer(logPath, logPath);
		assertEquals(newFilePointer, result.getLastReadPosition().get());
		assertEquals(newFilename, result.getFilename());
	}
	
	@After
	public void cleanUp() throws Exception {
		deleteFilePointerFile();
	}
	
	@Before
	public void setUp() throws Exception {
		deleteFilePointerFile();
	}
	
	public void deleteFilePointerFile() throws Exception {
		File filePointerFile = new File("./target/classes/com/appdynamics/extensions/logmonitor/apache/" + 
					FILEPOINTER_FILENAME);
		
		if (filePointerFile.exists()) {
			filePointerFile.delete();
		}
	}

}
