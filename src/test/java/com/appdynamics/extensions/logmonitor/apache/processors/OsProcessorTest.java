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

public class OsProcessorTest {
	
	private OsProcessor classUnderTest;
	
	@Test
	public void testIsToMonitorReturnsTrue() {
		classUnderTest = new OsProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		for (String os: getTestOs()) {
			assertTrue(classUnderTest.isToMonitor(os));
		}
	}
	
	@Test
	public void testIsToMonitorReturnsFalse() {
		Set<String> metricExcludes = new HashSet<String>();
		metricExcludes.add("Mac OS X");
		metricExcludes.add("Ubuntu");
		
		classUnderTest = new OsProcessor(metricExcludes, 
				new HashSet<String>());
		
		List<String> testBrowsers = Arrays.asList("Mac OS X",
				"Ubuntu");
		
		for (String browser: testBrowsers) {
			assertFalse(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testProcessMetricsNoMemberToDisplay() {
		classUnderTest = new OsProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		for (String os: getTestOs()) {
			classUnderTest.processMetrics(os, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getOsMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getOsMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestOs().size()), 
				testMetrics.getOsMetrics().getBandwidth());
		assertTrue(testMetrics.getOsMetrics().getMembers().isEmpty());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getOsMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getOsMetrics().getErrorRatePercentage());
	}
	
	@Test
	public void testProcessMetricsWithMembersToDisplay() {
		Set<String> displayIncludes = new HashSet<String>();
		displayIncludes.add("Mac OS X");
		displayIncludes.add("Ubuntu");
		
		classUnderTest = new OsProcessor(new HashSet<String>(), 
				displayIncludes);
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		for (String os: getTestOs()) {
			classUnderTest.processMetrics(os, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getOsMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getOsMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestOs().size()), 
				testMetrics.getOsMetrics().getBandwidth());
		assertEquals(2, testMetrics.getOsMetrics().getMembers().size());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getOsMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getOsMetrics().getErrorRatePercentage());
		
		Metrics macMetrics = testMetrics.getOsMetrics().getMembers().get("Mac OS X");
		assertEquals(BigInteger.valueOf(2), macMetrics.getHitCount());
		assertEquals(BigInteger.valueOf(2), macMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * 2), macMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),macMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,macMetrics.getErrorRatePercentage());
		
		Metrics ubuntuMetrics = testMetrics.getOsMetrics().getMembers().get("Ubuntu");
		assertEquals(BigInteger.ONE, ubuntuMetrics.getHitCount());
		assertEquals(BigInteger.ONE, ubuntuMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth), ubuntuMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),ubuntuMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,ubuntuMetrics.getErrorRatePercentage());
	}
	
	private List<String> getTestOs() {
		return Arrays.asList("Mac OS X", 
				"Windows",
				"Ubuntu",
				"Mac OS X",
				"Chrome OS");
	}
}
