/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.metrics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil;

/**
 * @author Florencio Sarmiento
 *
 */
public class Metrics {

	private BigInteger hitCount = BigInteger.ZERO;
	
	private BigInteger pageViewCount = BigInteger.ZERO;
	
	private BigInteger bandwidth = BigInteger.ZERO;
	
	private BigInteger failureCount =BigInteger.ZERO;
	
	private List<Long> responseTimeList = new ArrayList<Long>();
	
	private BigInteger totalResponseTime = BigInteger.ZERO;
	
	public void incrementHitCount() {
		this.hitCount = this.hitCount.add(BigInteger.ONE);
	}
	
	public void incrementPageViewCount() {
		this.pageViewCount = this.pageViewCount.add(BigInteger.ONE);
	}
	
	public void addBandwidth(Integer bandwidth) {
		this.bandwidth = this.bandwidth.add(BigInteger.valueOf(bandwidth));
	}
	
	
	public void incrementFailureCount() {
		this.failureCount = this.failureCount.add(BigInteger.ONE);
	}
	
	public void addResponseTime(Long responseTime) {
		responseTimeList.add(responseTime);
		totalResponseTime = totalResponseTime.add(BigInteger.valueOf(responseTime));
	}

	public List<Long> getResponseTimeList() {
		return responseTimeList;
	}

	public BigInteger getHitCount() {
		return hitCount;
	}

	public BigInteger getPageViewCount() {
		return pageViewCount;
	}

	public BigInteger getBandwidth() {
		return bandwidth;
	}
	
	public BigInteger getFailureCount() {
		return failureCount;
	}
	
	public BigInteger getTotalResponseTime() {
		return totalResponseTime;
	}
	
	public BigInteger getErrorRatePercentage() {
		BigInteger totalRequests = getHitCount().add(getFailureCount());
		
		BigInteger errorRate = BigInteger.ZERO;
		
		if(getFailureCount() != BigInteger.ZERO) {
			errorRate = (getFailureCount().multiply(BigInteger.valueOf(100))).divide(totalRequests);
		}
		
		return errorRate;
	}
	
	public BigInteger getAvgResponseTime() {
		BigInteger totalRequests = getHitCount().add(getFailureCount());
		BigInteger avgResponseTime = BigInteger.ZERO;
		
		if(getTotalResponseTime() != BigInteger.ZERO && totalRequests != BigInteger.ZERO) {
			avgResponseTime = getTotalResponseTime().divide(totalRequests);
		}
		
		return avgResponseTime;
	}
	
	public BigInteger getResponseTimePercentile(Integer percentile) {
		Long percentileValue = ApacheLogMonitorUtil.getNthPercentile(getResponseTimeList(), percentile, 0L);
		return BigInteger.valueOf(percentileValue);
	}
	
	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
