/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.*;

import java.util.Set;
import java.util.regex.Pattern;

import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;

/**
 * @author Florencio Sarmiento
 *
 */
public class OsProcessor {
	
	private Pattern metricExcludesPattern;
	
	private Pattern displayIncludesPattern;

	public OsProcessor(Set<String> metricExcludes,
			Set<String> displayIncludes) {
		
		this.metricExcludesPattern = createPattern(metricExcludes);
		this.displayIncludesPattern = createPattern(displayIncludes);
	}
	
	public boolean isToMonitor(String os) {
		return isNotMatch(os, metricExcludesPattern);
	}
	
	public void processMetrics(String os, Integer bandwidth, 
			boolean isPageView, ApacheLogMetrics apacheLogMetrics, boolean isSuccessfulHit, Long responseTime) {
		
		if (isMatch(os, displayIncludesPattern)) {
			apacheLogMetrics.getOsMetrics()
				.incrementGroupAndMemberMetrics(os, bandwidth, isPageView, isSuccessfulHit, responseTime);
			
		} else {
			apacheLogMetrics.getOsMetrics()
				.incrementGroupMetrics(bandwidth, isPageView, isSuccessfulHit, responseTime);
		}
	}
}
