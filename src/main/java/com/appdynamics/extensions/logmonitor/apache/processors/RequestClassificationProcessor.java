/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.processors;

import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.isMatch;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.appdynamics.extensions.logmonitor.apache.config.RequestClassification;
import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;

/**
 * @author Florencio Sarmiento
 *
 */
public class RequestClassificationProcessor {
	
	private List<RequestClassification> requestClassifications;

	public RequestClassificationProcessor(List<RequestClassification> requestClassifications) {
		this.requestClassifications = requestClassifications;
	}
	
	public String removeParam(String request) {
		if (StringUtils.isNotBlank(request)) {
			String[] results = request.split("\\?");
			return results[0];
		}
		
		return request;
	}
	
	public void processMetrics(String rawRequest, String httpMethod, Integer bandwidth, 
			boolean isPageView, ApacheLogMetrics apacheLogMetrics, boolean isSuccessfulHit, Long responseTime) {
		
		for(RequestClassification requestClass : requestClassifications) {
			if (isPageView) {
				if (isMatch(rawRequest, Pattern.compile(requestClass.getUrlPattern()))
						&& requestClass.getHttpMethod().equals(httpMethod)) {
					apacheLogMetrics.getRequestClassificationMetrics()
						.incrementGroupAndMemberMetrics(requestClass.getRequestName(), bandwidth, isPageView, isSuccessfulHit, responseTime);
					
				}
			}
		}
	}
	
}
