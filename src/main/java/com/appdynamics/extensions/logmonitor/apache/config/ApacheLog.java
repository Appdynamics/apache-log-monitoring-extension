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
public class ApacheLog {

	private String displayName;

	private String logDirectory;

	private String logName;

	private String logPattern;

	private Set<Integer> hitResponseCodes;

	private Set<String> nonPageExtensions;

	private MetricsFilterForCalculation metricsFilterForCalculation;

	private IndividualMetricsToDisplay individualMetricsToDisplay;
	
	private List<RequestClassification> requestClassifications;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getLogDirectory() {
		return logDirectory;
	}

	public void setLogDirectory(String logDirectory) {
		this.logDirectory = logDirectory;
	}

	public String getLogName() {
		return logName;
	}

	public void setLogName(String logName) {
		this.logName = logName;
	}

	public String getLogPattern() {
		return logPattern;
	}

	public void setLogPattern(String logPattern) {
		this.logPattern = logPattern;
	}

	public Set<Integer> getHitResponseCodes() {
		return hitResponseCodes;
	}

	public void setHitResponseCodes(Set<Integer> hitResponseCodes) {
		this.hitResponseCodes = hitResponseCodes;
	}

	public Set<String> getNonPageExtensions() {
		return nonPageExtensions;
	}

	public void setNonPageExtensions(Set<String> nonPageExtensions) {
		this.nonPageExtensions = nonPageExtensions;
	}

	public MetricsFilterForCalculation getMetricsFilterForCalculation() {
		return metricsFilterForCalculation;
	}

	public void setMetricsFilterForCalculation(
			MetricsFilterForCalculation metricsFilterForCalculation) {
		this.metricsFilterForCalculation = metricsFilterForCalculation;
	}

	public IndividualMetricsToDisplay getIndividualMetricsToDisplay() {
		return individualMetricsToDisplay;
	}

	public void setIndividualMetricsToDisplay(
			IndividualMetricsToDisplay individualMetricsToDisplay) {
		this.individualMetricsToDisplay = individualMetricsToDisplay;
	}
	
	public List<RequestClassification> getRequestClassifications() {
		return requestClassifications;
	}

	public void setRequestClassifications(List<RequestClassification> requestClassifications) {
		this.requestClassifications = requestClassifications;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
