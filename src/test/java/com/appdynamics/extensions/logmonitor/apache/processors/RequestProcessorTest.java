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

public class RequestProcessorTest {
	
	private RequestProcessor classUnderTest;
	
	@Test
	public void testRemoveParamFromRequestWithNoParam() {
		classUnderTest = new RequestProcessor(null, null, null);
		
		List<String> testUrls = Arrays.asList("/this/is/a/test/url/",
				"anotherUrl", "/blah");
		
		for (String url : testUrls) {
			String result = classUnderTest.removeParam(url);
			assertEquals(url, result);
		}
	}
	
	@Test
	public void testRemoveParamFromRequestWithParam() {
		classUnderTest = new RequestProcessor(null, null, null);
		
		String testUrl = "/this/is/a/test?myParam=param1&blah=blah";
		String result = classUnderTest.removeParam(testUrl);
		assertEquals("/this/is/a/test", result);
		
		testUrl = "/this/is/?/another";
		result = classUnderTest.removeParam(testUrl);
		assertEquals("/this/is/", result);
	}
	
	@Test
	public void testIsPageUsingDefaultExtensions() {
		classUnderTest = new RequestProcessor(null, null, null);
		
		List<String> imageUrls = Arrays.asList("/test/mypng.png",
				"/test/myjpg.jpg", "/favicon.ico");
		
		for (String url : imageUrls) {
			assertFalse(classUnderTest.isPage(url));
		}
		
		List<String> pageUrls = Arrays.asList("/test/url",
				"/test/mypage.html", "/test/page.pdf");
		
		for (String url : pageUrls) {
			assertTrue(classUnderTest.isPage(url));
		}
	}
	
	@Test
	public void testIsPageUsingNonDefaultExtensions() {
		Set<String> nonPageExtensions = new HashSet<String>();
		nonPageExtensions.add("jpg");
		nonPageExtensions.add("jpeg");
		
		classUnderTest = new RequestProcessor(null, null, nonPageExtensions);
		
		List<String> imageUrls = Arrays.asList("/test/myjpeg.jpeg",
				"/test/myjpg.jpg", "blah.jpeg");
		
		for (String url : imageUrls) {
			assertFalse(classUnderTest.isPage(url));
		}
		
		List<String> pageUrls = Arrays.asList("/test/mypng.png",
				"/favicon.ico", "/test/url", 
				"/test/mypage.html", "/test/page.pdf");
		
		for (String url : pageUrls) {
			assertTrue(classUnderTest.isPage(url));
		}
	}
	
	@Test
	public void testIsPageWithRobotsUrl() {
		classUnderTest = new RequestProcessor(null, null, null);
		assertFalse(classUnderTest.isPage("/robots.txt"));
	}
	
	@Test
	public void testIsToMonitorReturnsFalse() {
		Set<String> metricExcludes = new HashSet<String>();
		metricExcludes.add("/test.*");
		
		classUnderTest = new RequestProcessor(metricExcludes, 
				null, null);
		
		List<String> pageUrls = Arrays.asList("/test/mypng.png",
				"/test/url", "/test/mypage.html", "/test/page.pdf");
		
		for (String browser: pageUrls) {
			assertFalse(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testIsToMonitorReturnsTrue() {
		Set<String> metricExcludes = new HashSet<String>();
		metricExcludes.add("/test.*");
		
		classUnderTest = new RequestProcessor(metricExcludes, 
				null, null);
		
		List<String> pageUrls = Arrays.asList("/blah/mypng.png",
				"/blah/url", "/blah/mypage.html", "/blah/page.pdf");
		
		for (String browser: pageUrls) {
			assertTrue(classUnderTest.isToMonitor(browser));
		}
	}
	
	@Test
	public void testProcessMetricsNoMemberToDisplay() {
		classUnderTest = new RequestProcessor(null, null, null);
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		List<String> pageUrls = Arrays.asList("/todisplay/myjsp.jsp",
				"/test/url", "/test/mypage.html", "/todisplay/page.pdf");
		
		for (String page: pageUrls) {
			classUnderTest.processMetrics(page, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(4), testMetrics.getPageMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(testBandwidth * pageUrls.size()), 
				testMetrics.getPageMetrics().getBandwidth());
		assertTrue(testMetrics.getPageMetrics().getMembers().isEmpty());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getPageMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getPageMetrics().getErrorRatePercentage());
	}
	
	@Test
	public void testProcessMetricsWithMembersToDisplay() {
		Set<String> displayIncludes = new HashSet<String>();
		displayIncludes.add("/todisplay.*");
		
		classUnderTest = new RequestProcessor(null, displayIncludes, null);
		
		ApacheLogMetrics testMetrics = new ApacheLogMetrics();
		Integer testBandwidth = 15;
		Long testResponseTime = 100L;
		
		List<String> pageUrls = Arrays.asList("/todisplay/myjsp.jsp",
				"/test/url", "/test/mypage.html", "/todisplay/page.pdf");
		
		for (String page: pageUrls) {
			classUnderTest.processMetrics(page, testBandwidth, true, testMetrics,true,testResponseTime);
		}
		
		assertEquals(BigInteger.valueOf(4), testMetrics.getPageMetrics().getHitCount());
		assertEquals(BigInteger.valueOf(testBandwidth * pageUrls.size()), 
				testMetrics.getPageMetrics().getBandwidth());
		assertEquals(2, testMetrics.getPageMetrics().getMembers().size());
		assertEquals(BigInteger.valueOf(testResponseTime),testMetrics.getPageMetrics().getAvgResponseTime());
		assertEquals(BigInteger.ZERO,testMetrics.getPageMetrics().getErrorRatePercentage());
		
		Metrics jspMetrics = testMetrics.getPageMetrics().getMembers().get("/todisplay/myjsp.jsp");
		assertEquals(BigInteger.ONE, jspMetrics.getHitCount());
		assertEquals(BigInteger.valueOf(testBandwidth), jspMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),jspMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,jspMetrics.getErrorRatePercentage());
		
		Metrics pdfMetrics = testMetrics.getPageMetrics().getMembers().get("/todisplay/page.pdf");
		assertEquals(BigInteger.ONE, pdfMetrics.getHitCount());
		assertEquals(BigInteger.valueOf(testBandwidth), pdfMetrics.getBandwidth());
		assertEquals(BigInteger.valueOf(testResponseTime),pdfMetrics.getAvgResponseTime());
		assertEquals(BigInteger.ZERO,pdfMetrics.getErrorRatePercentage());
	}

}
