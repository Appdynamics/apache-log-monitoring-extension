/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache.config;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author Florencio Sarmiento
 *
 */
public class Configuration {

	private String metricPrefix;

	private List<ApacheLog> apacheLogs;

	private int noOfThreads;
	
	private Set<Integer> includeResponseTimePercentiles;
	
	private String lastReadPointerFileLocation;

	public Set<Integer> getIncludeResponseTimePercentiles() {
		return includeResponseTimePercentiles;
	}

	public void setIncludeResponseTimePercentiles(Set<Integer> includeResponseTimePercentiles) {
		this.includeResponseTimePercentiles = includeResponseTimePercentiles;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}

	public List<ApacheLog> getApacheLogs() {
		return apacheLogs;
	}

	public void setApacheLogs(List<ApacheLog> apacheLogs) {
		this.apacheLogs = apacheLogs;
	}

	public int getNoOfThreads() {
		return noOfThreads;
	}

	public void setNoOfThreads(int noOfThreads) {
		this.noOfThreads = noOfThreads;
	}
	
	public boolean hasPercentiles() {
		return getIncludeResponseTimePercentiles() != null && getIncludeResponseTimePercentiles().size()>0;
	}

	public String getLastReadPointerFileLocation() {
		return lastReadPointerFileLocation;
	}

	public void setLastReadPointerFileLocation(String lastReadPointerFileLocation) {
		this.lastReadPointerFileLocation = lastReadPointerFileLocation;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
