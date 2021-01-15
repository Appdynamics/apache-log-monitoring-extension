/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.metrics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class GroupMetrics extends Metrics {
	
	private Map<String, Metrics> members = new ConcurrentHashMap<String, Metrics>();
	
	public void incrementGroupAndMemberMetrics(String memberKey, Integer bandwidth, boolean isPageView, boolean isSuccessfulHit, Long responseTime) {
		incrementGroupMetrics(bandwidth, isPageView, isSuccessfulHit, responseTime);
		incrementMemberMetrics(memberKey, bandwidth, isPageView, isSuccessfulHit, responseTime);
	}
	
	public void incrementGroupMetrics(Integer bandwidth, boolean isPageView, boolean isSuccessfulHit, Long responseTime) {
		incrementMetricsCounter(this, bandwidth, isPageView, isSuccessfulHit, responseTime);
	}
	
	public void incrementMemberMetrics(String memberKey, Integer bandwidth, boolean isPageView, boolean isSuccessfulHit, Long responseTime) {
		if (StringUtils.isNotBlank(memberKey)) {
			Metrics metricsCounter = this.members.get(memberKey);
			
			if (metricsCounter == null) {
				metricsCounter = new Metrics();
			}
			
			incrementMetricsCounter(metricsCounter, bandwidth, isPageView, isSuccessfulHit, responseTime);
			this.members.put(memberKey, metricsCounter);
		}
	}
	
	private void incrementMetricsCounter(Metrics counter, Integer bandwidth, boolean isPageView, boolean isSuccessfulHit, Long responseTime) {
		if (isPageView) {
			counter.incrementPageViewCount();
		}
		
		if (bandwidth != null) {
			counter.addBandwidth(bandwidth);
		}
		
		if(isSuccessfulHit) {
			counter.incrementHitCount();
		}else {
			counter.incrementFailureCount();
		}
		
		counter.addResponseTime(responseTime);
	}
	
	public Map<String, Metrics> getMembers() {
		return this.members;
	}
	
	public void addMember(String memberKey) {
		if (StringUtils.isNotBlank(memberKey)) {
			Metrics metricsCounter = this.members.get(memberKey);
			
			if (metricsCounter == null) {
				metricsCounter = new Metrics();
			}
			
			this.members.put(memberKey, metricsCounter);
		}
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
}
