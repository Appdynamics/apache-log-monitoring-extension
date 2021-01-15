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
public class ApacheLogMetrics {
	
	private String apacheLogName;

	private GroupMetrics visitorMetrics = new GroupMetrics();

	private GroupMetrics spiderMetrics = new GroupMetrics();

	private GroupMetrics browserMetrics = new GroupMetrics();

	private GroupMetrics osMetrics = new GroupMetrics();
	
	private GroupMetrics pageMetrics = new GroupMetrics();
	
	private GroupMetrics responseCodeMetrics = new GroupMetrics();
	
	private GroupMetrics requestClassificationMetrics = new GroupMetrics();
	
	public String getApacheLogName() {
		return apacheLogName;
	}

	public void setApacheLogName(String apacheLogName) {
		this.apacheLogName = apacheLogName;
	}

	public GroupMetrics getVisitorMetrics() {
		return visitorMetrics;
	}

	public GroupMetrics getSpiderMetrics() {
		return spiderMetrics;
	}

	public GroupMetrics getBrowserMetrics() {
		return browserMetrics;
	}

	public GroupMetrics getOsMetrics() {
		return osMetrics;
	}

	public GroupMetrics getPageMetrics() {
		return pageMetrics;
	}

	public GroupMetrics getResponseCodeMetrics() {
		return responseCodeMetrics;
	}
	
	public GroupMetrics getRequestClassificationMetrics() {
		return requestClassificationMetrics;
	}
	
	public BigInteger getTotalHitCount() {
		return getVisitorMetrics().getHitCount()
				.add(getSpiderMetrics().getHitCount());
	}
	
	public BigInteger getTotalPageViewCount() {
		return getVisitorMetrics().getPageViewCount()
				.add(getSpiderMetrics().getPageViewCount());
	}
	
	public BigInteger getTotalBandwidth() {
		return getVisitorMetrics().getBandwidth()
				.add(getSpiderMetrics().getBandwidth());
	}
	
	public BigInteger getTotalFailureCount() {
		return getVisitorMetrics().getFailureCount()
				.add(getSpiderMetrics().getFailureCount());
	}
	
	public BigInteger getErrorRatePercentage() {
		BigInteger errorRate = BigInteger.ZERO;
		
		BigInteger totalRequests = getTotalHitCount().add(getTotalFailureCount());
		
		if(totalRequests != BigInteger.ZERO) {
			errorRate = (getTotalFailureCount().multiply(BigInteger.valueOf(100))).divide(totalRequests);
		}
		
		return errorRate;
	}
	
	public BigInteger getAvgResponseTime() {
		BigInteger totalResponseTime = getVisitorMetrics().getTotalResponseTime().add(getSpiderMetrics().getTotalResponseTime());
		BigInteger totalRequests = getTotalHitCount().add(getTotalFailureCount());
		
		BigInteger avgResponseTime = BigInteger.ZERO;
		if(totalRequests != BigInteger.ZERO && totalResponseTime != BigInteger.ZERO) {
			avgResponseTime = totalResponseTime.divide(totalRequests);
		}
		
		return avgResponseTime;
	}
	
	public BigInteger getResponseTimePercentile(Integer percentile) {
		List<Long> respTimeList = new ArrayList<Long>();
		respTimeList.addAll(getVisitorMetrics().getResponseTimeList());
		respTimeList.addAll(getSpiderMetrics().getResponseTimeList());
		
		Long percentileValue = ApacheLogMonitorUtil.getNthPercentile(respTimeList, percentile, 0L);
		return BigInteger.valueOf(percentileValue);
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
