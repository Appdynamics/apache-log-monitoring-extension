/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.IndividualMetricsToDisplay;
import com.appdynamics.extensions.logmonitor.apache.config.MetricsFilterForCalculation;
import com.appdynamics.extensions.logmonitor.apache.config.RequestClassification;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;

public class ApacheLogMonitorTaskTest {
	
	private ApacheLogMonitorTask classUnderTest;
	
	@Test
	public void testProcessLogIsSuccessful() throws Exception {
		
		FilePointerProcessor filePointerProcessor = new FilePointerProcessor();
		
		ApacheLog apacheLog = new ApacheLog();
		apacheLog.setDisplayName("TestLog");
		apacheLog.setLogName("access.log");
		apacheLog.setLogDirectory("src/test/resources/test-logs");
		apacheLog.setLogPattern("%{COMMONAPACHELOG_WITH_RESP_TIME}");
		apacheLog.setHitResponseCodes(new HashSet<Integer>());
		apacheLog.setIndividualMetricsToDisplay(new IndividualMetricsToDisplay());
		apacheLog.setMetricsFilterForCalculation(new MetricsFilterForCalculation());
		apacheLog.setNonPageExtensions(new HashSet<String>());
		apacheLog.setRequestClassifications(new ArrayList<RequestClassification>());
		filePointerProcessor.initialize("");
		
		classUnderTest = new ApacheLogMonitorTask(filePointerProcessor, 
				new File("src/test/resources/conf/patterns/grok-patterns.grok").getAbsolutePath(), 
				new File("src/test/resources/conf/patterns/user-agent-regexes.yaml").getAbsolutePath(), 
				apacheLog);
		
		ApacheLogMetrics result = classUnderTest.call();
		filePointerProcessor.updateFilePointerFile();
		assertNotNull(result);
		
		File testFile = new File("src/test/resources/test-logs/access.log");
		long expectedLastReadPosition = testFile.length();
		FilePointer filePointer = filePointerProcessor.getFilePointer(
				testFile.getPath(), testFile.getPath());
		
		assertEquals(expectedLastReadPosition, filePointer.getLastReadPosition().get());
	}
}
