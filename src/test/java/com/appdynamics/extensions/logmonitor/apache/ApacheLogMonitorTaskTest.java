/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.Constants.FILEPOINTER_FILENAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.appdynamics.extensions.AMonitorJob;
import com.appdynamics.extensions.MetricWriteHelper;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Maps;
import org.junit.After;
import org.junit.Test;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.IndividualMetricsToDisplay;
import com.appdynamics.extensions.logmonitor.apache.config.MetricsFilterForCalculation;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointer;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ApacheLogMonitorTask.class)
public class ApacheLogMonitorTaskTest {

	private ApacheLogMonitorTask classUnderTest;

    @Mock
	MetricWriteHelper metricWriteHelper;

	@Test
	public void testProcessLogIsSuccessful() throws Exception {
		ArgumentCaptor<List> pathCaptor = ArgumentCaptor.forClass(List.class);

		MonitorContextConfiguration monitorContextConfiguration = new MonitorContextConfiguration("Apache Log Monitor",
				"Custom Metrics|Apache Log Monitor|", Mockito.mock(File.class),Mockito.mock(AMonitorJob.class));

		FilePointerProcessor filePointerProcessor = new FilePointerProcessor();

		IndividualMetricsToDisplay individualMetricsToDisplay = new IndividualMetricsToDisplay();
		individualMetricsToDisplay.setIncludeVisitors(new HashSet<String>());
		individualMetricsToDisplay.setIncludePages(new HashSet<String>());
		individualMetricsToDisplay.setIncludeOs(new HashSet<String>());
		individualMetricsToDisplay.setIncludeBrowsers(new HashSet<String>());
		individualMetricsToDisplay.setIncludeSpiders(new HashSet<String>());
		individualMetricsToDisplay.setIncludeResponseCodes(new HashSet<Integer>());

		MetricsFilterForCalculation metricsFilterForCalculation = new MetricsFilterForCalculation();
		metricsFilterForCalculation.setExcludeOs(new HashSet<>());
		metricsFilterForCalculation.setExcludeBrowsers(new HashSet<>());
		metricsFilterForCalculation.setExcludeSpiders(new HashSet<>());
		metricsFilterForCalculation.setExcludeUrls(new HashSet<>());
		metricsFilterForCalculation.setExcludeVisitors(new HashSet<>());


		ApacheLog apacheLog = new ApacheLog();
		apacheLog.setDisplayName("TestLog");
		apacheLog.setLogName("access.log");
		apacheLog.setLogDirectory("src/test/resources/test-logs");
		apacheLog.setLogPattern("%{COMMONAPACHELOG}");
		apacheLog.setHitResponseCodes(new HashSet<Integer>());
		apacheLog.setIndividualMetricsToDisplay(individualMetricsToDisplay);
		apacheLog.setMetricsFilterForCalculation(metricsFilterForCalculation);
		apacheLog.setNonPageExtensions(new HashSet<String>());

		classUnderTest = new ApacheLogMonitorTask(monitorContextConfiguration,metricWriteHelper,filePointerProcessor,
				new File("src/test/resources/conf/patterns/grok-patterns.grok").getAbsolutePath(),
				new File("src/test/resources/conf/patterns/user-agent-regexes.yaml").getAbsolutePath(),
				apacheLog);

		classUnderTest.run();

		Mockito.verify(metricWriteHelper).transformAndPrintMetrics(pathCaptor.capture());

		Map<String,String> resultMap = Maps.newHashMap();

		for(Metric metric: (List<Metric>)pathCaptor.getValue()){
			resultMap.put(metric.getMetricPath(),metric.getMetricValue());
		}

		assertNotNull(resultMap);

		File testFile = new File("src/test/resources/test-logs/access.log");
		long expectedLastReadPosition = testFile.length();
		FilePointer filePointer = filePointerProcessor.getFilePointer(
				testFile.getPath(), testFile.getPath());

		assertEquals(expectedLastReadPosition, filePointer.getLastReadPosition().get());
	}

	@After
	public void deleteFilePointerFile() throws Exception {
		File filePointerFile = new File("./target/classes/com/appdynamics/extensions/logmonitor/apache/" +
				FILEPOINTER_FILENAME);

		if (filePointerFile.exists()) {
			filePointerFile.delete();
		}
	}
}
