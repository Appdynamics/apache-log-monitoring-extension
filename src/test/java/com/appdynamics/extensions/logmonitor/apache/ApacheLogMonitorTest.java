/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import static com.appdynamics.extensions.logmonitor.apache.Constants.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.Configuration;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;
import com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil;
import com.appdynamics.extensions.yml.YmlReader;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ApacheLogMonitor.class, 
	ApacheLogMonitorUtil.class, YmlReader.class})
@PowerMockIgnore({"org.apache.*, javax.xml.*"})
public class ApacheLogMonitorTest {
	
	private ApacheLogMonitor classUnderTest;
	
	@Mock
	private MetricWriter mockMetricWriter;
	
	@Mock
	private ApacheLogMonitorTask mockApacheLogMonitorTask;
	
	@Mock
	private Configuration mockConfiguration;
	
	@Mock
	private ApacheLog mockApacheLog;
	
	@Mock
	private FilePointerProcessor mockFilePointerProcessor;
	
	@Before
	public void setUp() throws Exception {
		mockStatic(ApacheLogMonitorUtil.class);
		PowerMockito.when(ApacheLogMonitorUtil.resolvePath(anyString())).thenCallRealMethod();
		PowerMockito.when(ApacheLogMonitorUtil.convertValueToZeroIfNullOrNegative(any(BigInteger.class)))
			.thenCallRealMethod();
		
		List<ApacheLog> mockApacheLogs = Arrays.asList(mockApacheLog);
		when(mockConfiguration.getApacheLogs()).thenReturn(mockApacheLogs);
		
		whenNew(FilePointerProcessor.class).withNoArguments().thenReturn(mockFilePointerProcessor);
		
		whenNew(MetricWriter.class).withArguments(any(AManagedMonitor.class), anyString()).thenReturn(mockMetricWriter);
		setUpTestMetricsAndApacheLogMonitorTask();
		
		classUnderTest = spy(new ApacheLogMonitor());
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithNoArgs() throws TaskExecutionException {
		classUnderTest.execute(null, null);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithEmptyArgs() throws TaskExecutionException {
		classUnderTest.execute(new HashMap<String, String>(), null);
	}
	
	@Test(expected=TaskExecutionException.class)
	public void testWithNonExistentConfigDir() throws TaskExecutionException {
		String testConfDir = "src/test/resources/invalidConfDir";
		
		Map<String, String> args = Maps.newHashMap();
		args.put("config-dir", testConfDir);
		
		PowerMockito.when(ApacheLogMonitorUtil.resolvePath(anyString())).thenReturn(testConfDir);
		classUnderTest.execute(args, null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testMetricsAreReportedCorrectly() throws Exception {
		mockStatic(YmlReader.class);
		PowerMockito.when(YmlReader.readFromFile(anyString(), any(Class.class))).thenReturn(mockConfiguration);
		
		Map<String, String> args = Maps.newHashMap();
		args.put("config-dir", "src/test/resources/conf");
		
		classUnderTest.execute(args, null);
		
		verifySummaryMetrics();
		verifyBrowserMetrics();
		verifyOsMetrics();
		verifySpiderMetrics();
		verifyVisitorMetrics();
		verifyPageMetrics();
		verifyResponseCodeMetrics();
	}

	private void verifySummaryMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + TOTAL_HITS, 
				BigInteger.valueOf(5));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + TOTAL_PAGES, 
				BigInteger.valueOf(4));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(65));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + TOTAL_FAILURES, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
	}
	
	private void verifyBrowserMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + TOTAL_HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(3));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + TOTAL_FAILURES, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|" + TOTAL_PAGES, 
				BigInteger.ONE);
			
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + BANDWIDTH, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + FAILURES, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|Chrome|" + PAGES, 
				BigInteger.ONE);
			
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + BANDWIDTH, 
				BigInteger.valueOf(2));		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Browser|IE|" + PAGES, 
				BigInteger.ZERO);
		
	}
	
	private void verifyOsMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + TOTAL_HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(7));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + TOTAL_FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));	
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|" + TOTAL_PAGES, 
				BigInteger.valueOf(2));
		
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + BANDWIDTH, 
				BigInteger.valueOf(7));	
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|OS|Mac OS X|" + PAGES, 
				BigInteger.valueOf(2));
		
	}
	
	private void verifyPageMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|" + TOTAL_HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(11));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|" + TOTAL_FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));	
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test2.html|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test2.html|" + BANDWIDTH, 
				BigInteger.valueOf(6));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test2.html|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test2.html|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test2.html|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test1.html|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test1.html|" + BANDWIDTH, 
				BigInteger.valueOf(5));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test1.html|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test1.html|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Page|/test1.html|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
	}
	
	private void verifyResponseCodeMetrics() throws Exception {
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|404|" + HITS, 
				BigInteger.ZERO);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|404|" + BANDWIDTH, 
				BigInteger.valueOf(10));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|404|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|404|" + PAGES, 
				BigInteger.ONE);
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|200|" + HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|200|" + BANDWIDTH, 
				BigInteger.valueOf(15));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|200|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|200|" + PAGES, 
				BigInteger.valueOf(2));
		
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|304|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|304|" + BANDWIDTH, 
				BigInteger.valueOf(9));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|304|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Response Code|304|" + PAGES, 
				BigInteger.ONE);
		
		
		
		
	}
	
	private void verifySpiderMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + TOTAL_HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(23));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + TOTAL_FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));	
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|" + TOTAL_PAGES, 
				BigInteger.ONE);
		
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + HITS, 
				BigInteger.valueOf(2));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + BANDWIDTH, 
				BigInteger.valueOf(23));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Spider|GoogleBot|" + PAGES, 
				BigInteger.ONE);
		
	}
	
	private void verifyVisitorMetrics() throws Exception {
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + TOTAL_HITS, 
				BigInteger.valueOf(3));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + TOTAL_BANDWIDTH, 
				BigInteger.valueOf(42));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + TOTAL_FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));	
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|" + TOTAL_PAGES, 
				BigInteger.valueOf(3));
		
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + BANDWIDTH, 
				BigInteger.valueOf(13));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|localhost|" + PAGES, 
				BigInteger.ONE);
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + BANDWIDTH, 
				BigInteger.valueOf(15));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|10.10.5.5|" + PAGES, 
				BigInteger.ONE);
		
		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + HITS, 
				BigInteger.ONE);
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + BANDWIDTH, 
				BigInteger.valueOf(14));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + FAILURES, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + ERROR_RATE_PERCENTAGE, 
				BigInteger.ZERO);		
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + AVERGAGE_RESPONSE_TIME, 
				BigInteger.valueOf(100L));
		verifyMetric("Custom Metrics|Apache Log Monitor|TestApache|Visitor|127.0.0.1|" + PAGES, 
				BigInteger.ONE);
		
	}
	
	private void setUpTestMetricsAndApacheLogMonitorTask() throws Exception {
		ApacheLogMetrics logMetrics = new ApacheLogMetrics();
		logMetrics.setApacheLogName("TestApache");
		Long responseTime = 100L;
		
		logMetrics.getBrowserMetrics().incrementGroupAndMemberMetrics("Chrome", 1, true,true,responseTime);
		logMetrics.getBrowserMetrics().incrementGroupAndMemberMetrics("IE", 2, false,true,responseTime);
		
		logMetrics.getOsMetrics().incrementGroupAndMemberMetrics("Mac OS X", 3, true,true,responseTime);
		logMetrics.getOsMetrics().incrementGroupAndMemberMetrics("Mac OS X", 4, true,true,responseTime);
		
		logMetrics.getPageMetrics().incrementGroupAndMemberMetrics("/test1.html", 5, true,true,responseTime);
		logMetrics.getPageMetrics().incrementGroupAndMemberMetrics("/test2.html", 6, true,true,responseTime);
		
		logMetrics.getResponseCodeMetrics().incrementMemberMetrics("200", 7, true,true,responseTime);
		logMetrics.getResponseCodeMetrics().incrementMemberMetrics("200", 8, true,true,responseTime);
		logMetrics.getResponseCodeMetrics().incrementMemberMetrics("304", 9, true,true,responseTime);
		logMetrics.getResponseCodeMetrics().incrementMemberMetrics("404", 10, true,false,responseTime);
		
		logMetrics.getSpiderMetrics().incrementGroupAndMemberMetrics("GoogleBot", 11, false,true,responseTime);
		logMetrics.getSpiderMetrics().incrementGroupAndMemberMetrics("GoogleBot", 12, true,true,responseTime);
		
		logMetrics.getVisitorMetrics().incrementGroupAndMemberMetrics("localhost", 13, true,true,responseTime);
		logMetrics.getVisitorMetrics().incrementGroupAndMemberMetrics("127.0.0.1", 14, true,true,responseTime);
		logMetrics.getVisitorMetrics().incrementGroupAndMemberMetrics("10.10.5.5", 15, true,true,responseTime);
		
		whenNew(ApacheLogMonitorTask.class)
			.withArguments(any(AtomicLong.class),
							anyString(), 
							anyString(), 
							any(ApacheLog.class))
			.thenReturn(mockApacheLogMonitorTask);
		
		when(mockApacheLogMonitorTask.call()).thenReturn(logMetrics);
		
	}
	
	private void verifyMetric(String metricName, BigInteger value) throws Exception {
		if(metricName != null && (metricName.endsWith("Hits") 
				|| metricName.endsWith("Bandwidth (bytes)")
				 || metricName.endsWith("Pages")
				 || metricName.endsWith("Failures")
				 )) {
			verifyPrivate(classUnderTest).invoke("printCollectiveObservedSum", 
					metricName, value);
		}else {
			verifyPrivate(classUnderTest).invoke("printCollectiveObservedCurrent", 
					metricName, value);
		}
		
	}
}
