/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.*;
import static com.appdynamics.extensions.logmonitor.apache.Constants.*;

import java.util.Set;
import java.util.regex.Pattern;

import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;

/**
 * @author Florencio Sarmiento
 *
 */
public class SpiderProcessor {
	
	private Pattern metricExcludesPattern;
	
	private Pattern displayIncludesPattern;

	public SpiderProcessor(Set<String> metricExcludes,
			Set<String> displayIncludes) {
		
		this.metricExcludesPattern = createPattern(metricExcludes);
		this.displayIncludesPattern = createPattern(displayIncludes);
	}
	
	public boolean isSpider(String deviceName, String request) {
		return SPIDER.equalsIgnoreCase(deviceName) ||
				SPIDER_REQUEST.equals(request);
	}
	
	public boolean isToMonitor(String agentName) {
		return isNotMatch(agentName, metricExcludesPattern);
	}
	
	public void processMetrics(String agentName, Integer bandwidth, 
			boolean isPageView, ApacheLogMetrics apacheLogMetrics, boolean isSuccessfulHit, Long responseTime) {
		
		if (isMatch(agentName, displayIncludesPattern)) {
			apacheLogMetrics.getSpiderMetrics()
				.incrementGroupAndMemberMetrics(agentName, bandwidth, isPageView, isSuccessfulHit, responseTime);
			
		} else {
			apacheLogMetrics.getSpiderMetrics()
				.incrementGroupMetrics(bandwidth, isPageView, isSuccessfulHit, responseTime);
		}
	}
}
