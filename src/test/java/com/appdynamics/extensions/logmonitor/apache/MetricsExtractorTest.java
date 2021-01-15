/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.config.IndividualMetricsToDisplay;
import com.appdynamics.extensions.logmonitor.apache.config.MetricsFilterForCalculation;
import com.appdynamics.extensions.logmonitor.apache.config.RequestClassification;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.Metrics;

import oi.thekraken.grok.api.exception.GrokException;

public class MetricsExtractorTest {
	
	private MetricsExtractor classUnderTest;
	
	@Test(expected = GrokException.class)
	public void testNullGrokPatternPathThrowsException() throws Exception {
		classUnderTest = new MetricsExtractor(null, 
				"src/test/resources/conf/patterns/user-agent-regexes.yaml", 
				new ApacheLog());
	}
	
	@Test(expected = GrokException.class)
	public void testInvalidGrokPatternPathThrowsException() throws Exception {
		classUnderTest = new MetricsExtractor("src/invalid/path/grok-pattern.grok", 
				"src/test/resources/conf/patterns/user-agent-regexes.yaml", 
				new ApacheLog());
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testNullUserAgentRegexThrowsException() throws Exception {
		classUnderTest = new MetricsExtractor("src/test/resources/conf/patterns/grok-patterns.grok", 
				null, 
				new ApacheLog());
	}
	
	@Test(expected = FileNotFoundException.class)
	public void testInvalidUserAgentRegexThrowsException() throws Exception {
		classUnderTest = new MetricsExtractor("src/test/resources/conf/patterns/grok-patterns.grok", 
				"src/invalid/path/user-agent-regexes.yaml", 
				new ApacheLog());
	}
	
	@Test
	public void testExtractMetricsFromSpider() throws Exception {
		classUnderTest = new MetricsExtractor("src/test/resources/conf/patterns/grok-patterns.grok", 
				"src/test/resources/conf/patterns/user-agent-regexes.yaml", 
				getTestApacheLog());
		
		String testLog = "10.10.1.2 - - [14/Apr/2015:04:54:21 -0400] \"GET /test.html?param1=value HTTP/1.1\" 200 5678 \"-\" "
				+ "\"Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)\" 500";
		
		ApacheLogMetrics result = new ApacheLogMetrics();
		classUnderTest.extractMetrics(testLog, result);
		
		// summary metrics
		assertEquals(BigInteger.ONE, result.getTotalHitCount());
		assertEquals(BigInteger.ONE, result.getTotalPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getTotalBandwidth());
		
		// visitor metrics
		assertEquals(BigInteger.ZERO, result.getVisitorMetrics().getHitCount());
		assertEquals(BigInteger.ZERO, result.getVisitorMetrics().getPageViewCount());
		assertEquals(BigInteger.ZERO, result.getVisitorMetrics().getBandwidth());
		
		// spider metrics
		assertEquals(BigInteger.ONE, result.getSpiderMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getSpiderMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getSpiderMetrics().getBandwidth());
		assertEquals(1, result.getSpiderMetrics().getMembers().size());
		
		assertTrue(result.getSpiderMetrics().getMembers().containsKey("Googlebot"));
		
		Metrics googleBotMetrics = result.getSpiderMetrics().getMembers().get("Googlebot");
		assertEquals(BigInteger.ONE, googleBotMetrics.getHitCount());
		assertEquals(BigInteger.ONE, googleBotMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), googleBotMetrics.getBandwidth());
		
		// browser metrics
		assertEquals(BigInteger.ZERO, result.getBrowserMetrics().getHitCount());
		assertEquals(BigInteger.ZERO, result.getBrowserMetrics().getPageViewCount());
		assertEquals(BigInteger.ZERO, result.getBrowserMetrics().getBandwidth());
		
		// os metrics
		assertEquals(BigInteger.ONE, result.getOsMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getOsMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getOsMetrics().getBandwidth());
		assertEquals(1, result.getOsMetrics().getMembers().size());
		
		assertTrue(result.getOsMetrics().getMembers().containsKey("Other"));
		Metrics osMetrics = result.getOsMetrics().getMembers().get("Other");
		assertEquals(BigInteger.ONE, osMetrics.getHitCount());
		assertEquals(BigInteger.ONE, osMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), osMetrics.getBandwidth());
		
		// page metrics
		assertEquals(BigInteger.ONE, result.getPageMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getPageMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getPageMetrics().getBandwidth());
		assertEquals(1, result.getPageMetrics().getMembers().size());
		
		assertTrue(result.getPageMetrics().getMembers().containsKey("/test.html"));
		Metrics pageTestMetrics = result.getPageMetrics().getMembers().get("/test.html");
		assertEquals(BigInteger.ONE, pageTestMetrics.getHitCount());
		assertEquals(BigInteger.ONE, pageTestMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), pageTestMetrics.getBandwidth());
		
		// response code metrics
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getHitCount());
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getPageViewCount());
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getBandwidth());
		assertEquals(1, result.getResponseCodeMetrics().getMembers().size());
		
		assertTrue(result.getResponseCodeMetrics().getMembers().containsKey("200"));
		Metrics resp200Metrics = result.getResponseCodeMetrics().getMembers().get("200");
		assertEquals(BigInteger.ONE, resp200Metrics.getHitCount());
		assertEquals(BigInteger.ONE, resp200Metrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), resp200Metrics.getBandwidth());
	}
	
	@Test
	public void testExtractMetricsFromVisitor() throws Exception {
		classUnderTest = new MetricsExtractor("src/test/resources/conf/patterns/grok-patterns.grok", 
				"src/test/resources/conf/patterns/user-agent-regexes.yaml", 
				getTestApacheLog());
		
		String testLog = "10.10.1.2 - - [14/Apr/2015:04:54:21 -0400] \"GET /test.html?param1=value HTTP/1.1\" 200 5678 \"-\" "
				+ "\"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_4; en-US) AppleWebKit/534.3 (KHTML, like Gecko) Chrome/6.0.472.25 Safari/534.3\" 500";
		
		ApacheLogMetrics result = new ApacheLogMetrics();
		classUnderTest.extractMetrics(testLog, result);
		
		// summary metrics
		assertEquals(BigInteger.ONE, result.getTotalHitCount());
		assertEquals(BigInteger.ONE, result.getTotalPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getTotalBandwidth());
		
		// visitor metrics
		assertEquals(BigInteger.ONE, result.getVisitorMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getVisitorMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getVisitorMetrics().getBandwidth());
		assertEquals(1, result.getVisitorMetrics().getMembers().size());
		
		assertTrue(result.getVisitorMetrics().getMembers().containsKey("10.10.1.2"));
		
		Metrics visitorMetrics = result.getVisitorMetrics().getMembers().get("10.10.1.2");
		assertEquals(BigInteger.ONE, visitorMetrics.getHitCount());
		assertEquals(BigInteger.ONE, visitorMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), visitorMetrics.getBandwidth());
		
		// spider metrics
		assertEquals(BigInteger.ZERO, result.getSpiderMetrics().getHitCount());
		assertEquals(BigInteger.ZERO, result.getSpiderMetrics().getPageViewCount());
		assertEquals(BigInteger.ZERO, result.getSpiderMetrics().getBandwidth());
		
		// browser metrics
		assertEquals(BigInteger.ONE, result.getBrowserMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getBrowserMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getBrowserMetrics().getBandwidth());
		assertEquals(1, result.getBrowserMetrics().getMembers().size());
		
		assertTrue(result.getBrowserMetrics().getMembers().containsKey("Chrome"));
		
		Metrics chromeMetrics = result.getBrowserMetrics().getMembers().get("Chrome");
		assertEquals(BigInteger.ONE, chromeMetrics.getHitCount());
		assertEquals(BigInteger.ONE, chromeMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), chromeMetrics.getBandwidth());
		
		// os metrics
		assertEquals(BigInteger.ONE, result.getOsMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getOsMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getOsMetrics().getBandwidth());
		assertEquals(1, result.getOsMetrics().getMembers().size());
		
		assertTrue(result.getOsMetrics().getMembers().containsKey("Mac OS X"));
		Metrics osMetrics = result.getOsMetrics().getMembers().get("Mac OS X");
		assertEquals(BigInteger.ONE, osMetrics.getHitCount());
		assertEquals(BigInteger.ONE, osMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), osMetrics.getBandwidth());
		
		// page metrics
		assertEquals(BigInteger.ONE, result.getPageMetrics().getHitCount());
		assertEquals(BigInteger.ONE, result.getPageMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), result.getPageMetrics().getBandwidth());
		assertEquals(1, result.getPageMetrics().getMembers().size());
		
		assertTrue(result.getPageMetrics().getMembers().containsKey("/test.html"));
		Metrics pageTestMetrics = result.getPageMetrics().getMembers().get("/test.html");
		assertEquals(BigInteger.ONE, pageTestMetrics.getHitCount());
		assertEquals(BigInteger.ONE, pageTestMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), pageTestMetrics.getBandwidth());
		
		// response code metrics
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getHitCount());
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getPageViewCount());
		assertEquals(BigInteger.ZERO, result.getResponseCodeMetrics().getBandwidth());
		assertEquals(1, result.getResponseCodeMetrics().getMembers().size());
		
		assertTrue(result.getResponseCodeMetrics().getMembers().containsKey("200"));
		Metrics resp200Metrics = result.getResponseCodeMetrics().getMembers().get("200");
		assertEquals(BigInteger.ONE, resp200Metrics.getHitCount());
		assertEquals(BigInteger.ONE, resp200Metrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(5678), resp200Metrics.getBandwidth());
	}
	
	private ApacheLog getTestApacheLog() {
		Set<String> displayAll = new HashSet<String>(Arrays.asList(".*"));
		IndividualMetricsToDisplay metricsToDisplay = new IndividualMetricsToDisplay();
		metricsToDisplay.setIncludeBrowsers(displayAll);
		metricsToDisplay.setIncludeOs(displayAll);
		metricsToDisplay.setIncludePages(displayAll);
		metricsToDisplay.setIncludeSpiders(displayAll);
		metricsToDisplay.setIncludeVisitors(displayAll);
		metricsToDisplay.setIncludeResponseCodes(new HashSet<Integer>(Arrays.asList(200, 404)));
		
		ApacheLog apacheLog = new ApacheLog();
		apacheLog.setLogPattern("%{COMBINEDAPACHELOG_WITH_RESP_TIME}");
		apacheLog.setIndividualMetricsToDisplay(metricsToDisplay);
		apacheLog.setMetricsFilterForCalculation(new MetricsFilterForCalculation());
		apacheLog.setRequestClassifications(new ArrayList<RequestClassification>());
		
		return apacheLog;
	}

}
