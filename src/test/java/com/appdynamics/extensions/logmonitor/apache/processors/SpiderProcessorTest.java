/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.Metrics;

public class SpiderProcessorTest {
	
	private SpiderProcessor classUnderTest;
	
	@Test
	public void testIsSpiderReturnsTrue() {
		classUnderTest = new SpiderProcessor(null, null);
		
		assertTrue(classUnderTest.isSpider("spider", null));
		assertTrue(classUnderTest.isSpider(null, "/robots.txt"));
		assertTrue(classUnderTest.isSpider("spider", "/robots.txt"));
	}
	
	@Test
	public void testIsSpiderReturnsFalse() {
		classUnderTest = new SpiderProcessor(null, null);
		
		assertFalse(classUnderTest.isSpider("mac", null));
		assertFalse(classUnderTest.isSpider(null, "/test.txt"));
		assertFalse(classUnderTest.isSpider("mac", "/test.txt"));
	}
	
	@Test
	public void testIsToMonitorReturnsTrue() {
		classUnderTest = new SpiderProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		for (String spider: getTestSpiders()) {
			assertTrue(classUnderTest.isToMonitor(spider));
		}
	}
	
	@Test
	public void testIsToMonitorReturnsFalse() {
		Set<String> metricExcludes = new HashSet<String>();
		metricExcludes.add("yahoo");
		metricExcludes.add("slurp");
		
		classUnderTest = new SpiderProcessor(metricExcludes, 
				new HashSet<String>());
		
		List<String> testBrowsers = Arrays.asList("yahoo",
				"slurp");
		
		for (String browser: testBrowsers) {
			assertFalse(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testProcessMetricsNoMemberToDisplay() {
		classUnderTest = new SpiderProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		for (String spider: getTestSpiders()) {
			classUnderTest.processMetrics(spider, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getSpiderMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getSpiderMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestSpiders().size()), 
				testMetrics.getSpiderMetrics().getBandwidth());
		assertTrue(testMetrics.getSpiderMetrics().getMembers().isEmpty());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getSpiderMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getSpiderMetrics().getErrorRatePercentage());
	}
	
	@Test
	public void testProcessMetricsWithMembersToDisplay() {
		Set<String> displayIncludes = new HashSet<String>();
		displayIncludes.add("yahoo");
		displayIncludes.add("slurp");
		
		classUnderTest = new SpiderProcessor(new HashSet<String>(), 
				displayIncludes);
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		for (String spider: getTestSpiders()) {
			classUnderTest.processMetrics(spider, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getSpiderMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getSpiderMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestSpiders().size()), 
				testMetrics.getSpiderMetrics().getBandwidth());
		assertEquals(2, testMetrics.getSpiderMetrics().getMembers().size());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getSpiderMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getSpiderMetrics().getErrorRatePercentage());
		
		Metrics slurpMetrics = testMetrics.getSpiderMetrics().getMembers().get("slurp");
		assertEquals(BigInteger.valueOf(2), slurpMetrics.getHitCount());
		assertEquals(BigInteger.valueOf(2), slurpMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * 2), slurpMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),slurpMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,slurpMetrics.getErrorRatePercentage());
		
		Metrics yahooMetrics = testMetrics.getSpiderMetrics().getMembers().get("yahoo");
		assertEquals(BigInteger.ONE, yahooMetrics.getHitCount());
		assertEquals(BigInteger.ONE, yahooMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth), yahooMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),yahooMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,yahooMetrics.getErrorRatePercentage());
	}
	
	private List<String> getTestSpiders() {
		return Arrays.asList("yahoo", 
				"bot",
				"slurp",
				"slurp",
				"msnbot");
	}
}
