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

public class BrowserProcessorTest {
	
	private BrowserProcessor classUnderTest;
	
	@Test
	public void testIsToMonitorReturnsTrue() {
		classUnderTest = new BrowserProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		for (String browser: getTestBrowsers()) {
			assertTrue(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testIsToMonitorReturnsFalse() {
		Set<String> metricExcludes = new HashSet<String>();
		metricExcludes.add("Google Chrome");
		metricExcludes.add("Firefox");
		
		classUnderTest = new BrowserProcessor(metricExcludes, 
				new HashSet<String>());
		
		List<String> testBrowsers = Arrays.asList("Google Chrome",
				"Firefox");
		
		for (String browser: testBrowsers) {
			assertFalse(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testProcessMetricsNoMemberToDisplay() {
		classUnderTest = new BrowserProcessor(new HashSet<String>(), 
				new HashSet<String>());
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		
		for (String browser: getTestBrowsers()) {
			classUnderTest.processMetrics(browser, testBandwidth, true, testMetrics);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getBrowserMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getBrowserMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestBrowsers().size()), 
				testMetrics.getBrowserMetrics().getBandwidth());
		assertTrue(testMetrics.getBrowserMetrics().getMembers().isEmpty());
	}
	
	@Test
	public void testProcessMetricsWithMembersToDisplay() {
		Set<String> displayIncludes = new HashSet<String>();
		displayIncludes.add("Google Chrome");
		displayIncludes.add("Fire.*");
		
		classUnderTest = new BrowserProcessor(new HashSet<String>(), 
				displayIncludes);
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		
		for (String browser: getTestBrowsers()) {
			classUnderTest.processMetrics(browser, testBandwidth, true, testMetrics);
		}
		
		assertEquals(BigInteger.valueOf(5), testMetrics.getBrowserMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(5), testMetrics.getBrowserMetrics().getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * getTestBrowsers().size()), 
				testMetrics.getBrowserMetrics().getBandwidth());
		assertEquals(2, testMetrics.getBrowserMetrics().getMembers().size());
		
		Metrics firefoxMetrics = testMetrics.getBrowserMetrics().getMembers().get("Firefox");
		assertEquals(BigInteger.valueOf(2), firefoxMetrics.getHitCount());
		assertEquals(BigInteger.valueOf(2), firefoxMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth * 2), firefoxMetrics.getBandwidth());
		
		Metrics chromeMetrics = testMetrics.getBrowserMetrics().getMembers().get("Google Chrome");
		assertEquals(BigInteger.ONE, chromeMetrics.getHitCount());
		assertEquals(BigInteger.ONE, chromeMetrics.getPageViewCount());
		assertEquals(BigInteger.valueOf(testBandwidth), chromeMetrics.getBandwidth());
	}
	
	private List<String> getTestBrowsers() {
		return Arrays.asList("Google Chrome", 
				"IE",
				"Firefox",
				"Safari",
				"Firefox");
	}
}
