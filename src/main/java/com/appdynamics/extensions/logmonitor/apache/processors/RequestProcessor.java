/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.*;
import static com.appdynamics.extensions.logmonitor.apache.config.DefaultValues.*;
import static com.appdynamics.extensions.logmonitor.apache.Constants.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;

/**
 * @author Florencio Sarmiento
 *
 */
public class RequestProcessor {
	
	private Pattern metricExcludesPattern;
	
	private Pattern displayIncludesPattern;
	
	private Pattern nonPageExtensionPattern;

	public RequestProcessor(Set<String> metricExcludes,
			Set<String> displayIncludes,
			Set<String> nonPageExtensions) {
		
		this.metricExcludesPattern = createPattern(metricExcludes);
		this.displayIncludesPattern = createPattern(displayIncludes);
		initialiseNonPageExtensionPatternPattern(nonPageExtensions);
	}
	
	public String removeParam(String request) {
		if (StringUtils.isNotBlank(request)) {
			String[] results = request.split("\\?");
			return results[0];
		}
		
		return request;
	}
	
	public boolean isPage(String request) {
		return !SPIDER_REQUEST.equals(request) &&
				isNotMatch(request, nonPageExtensionPattern);
	}
	
	public boolean isToMonitor(String request) {
		return isNotMatch(request, metricExcludesPattern);
	}
	
	public void processMetrics(String page, Integer bandwidth, 
			boolean isPageView, ApacheLogMetrics apacheLogMetrics, boolean isSuccessfulHit, Long responseTime) {
		
		if (isPageView) {
			if (isMatch(page, displayIncludesPattern)) {
				apacheLogMetrics.getPageMetrics()
					.incrementGroupAndMemberMetrics(page, bandwidth, isPageView, isSuccessfulHit, responseTime);
				
			} else {
				apacheLogMetrics.getPageMetrics()
					.incrementGroupMetrics(bandwidth, isPageView, isSuccessfulHit, responseTime);
			}
		}	
	}
	
	private void initialiseNonPageExtensionPatternPattern(Set<String> rawNonPageExtensions) {
		if (rawNonPageExtensions == null || rawNonPageExtensions.isEmpty()) {
			rawNonPageExtensions = getDefaultNonPageExtensions();
		}
		
		Set<String> nonPageExtensions = new HashSet<String>();
		
		for (String extension : rawNonPageExtensions) {
			if (extension.startsWith(".")) {
				nonPageExtensions.add(".*" + Pattern.quote(extension));
			} else {
				nonPageExtensions.add(".*\\." + Pattern.quote(extension));
			}
		}
		
		this.nonPageExtensionPattern = createPattern(nonPageExtensions);
	}
}
