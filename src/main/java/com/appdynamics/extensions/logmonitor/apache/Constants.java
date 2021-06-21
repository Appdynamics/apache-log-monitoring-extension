/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import java.io.File;

/**
 * @author Florencio Sarmiento
 *
 */
public final class Constants {

	//defaults
	public static final String DEFAULT_METRIC_PREFIX = "Custom Metrics|Apache Log Monitor|";
	public static final String MONITOR_NAME = "ApacheLogMonitor";
	public static final String METRIC_PATH_SEPARATOR = "|";

	//task args names from monitor
	public static final String CONFIG_FILE = "config-file";
	public static final String GROK_PATTERN_FILE = "grok-pattern-file";
	public static final String USER_AGENT_REGEX_FILE = "user-agent-regex-file";

	//config constants
	public static final String APACHE_LOGS = "apacheLogs";
	public static final String DISPLAY_NAME = "displayName";
	public static final String LOG_DIRECTORY = "logDirectory";
	public static final String LOG_NAME = "logName";
	public static final String LOG_PATTERN = "logPattern";
	public static final String HIT_RESPONSE_CODES = "hitResponseCodes";
	public static final String NON_PAGE_EXTENSIONS = "nonPageExtensions";

	////metric filter for calculations
	public static final String METRIC_FILTER_FOR_CALCULATION = "metricsFilterForCalculation";
	public static final String EXCLUDE_VISITORS = "excludeVisitors";
	public static final String EXCLUDE_SPIDERS = "excludeSpiders";
	public static final String EXCLUDE_URLS = "excludeUrls";
	public static final String EXCLUDE_BROWSERS = "excludeBrowsers";
	public static final String EXCLUDE_OS = "excludeOs";

	////individual metric to display
	public static final String INDIVIDUAL_METRIC_TO_DISPLAY = "individualMetricsToDisplay";
	public static final String INCLUDE_VISITORS = "includeVisitors";
	public static final String INCLUDE_SPIDERS = "includeSpiders";
	public static final String INCLUDE_PAGES = "includePages";
	public static final String INCLUDE_BROWSERS = "includeBrowsers";
	public static final String INCLUDE_OS = "includeOs";
	public static final String INCLUDE_RESPONSE_CODES = "includeResponseCodes";

	//filepointer constant
	public static final String FILEPOINTER_FILENAME = "filepointer.json";

	//apache log metric related constants
	public static final String RESPONSE = "response";
	public static final String BYTES = "bytes";
	public static final String REQUEST = "request";
	public static final String AGENT = "agent";
	public static final String HOST = "clientip";
	public static final String SPIDER = "Spider";
	public static final String SPIDER_REQUEST = "/robots.txt";
	public static final int OK_RESPONSE = 200;
	public static final int NOT_MODIFIED_RESPONSE = 304;
	public static final String TOTAL_HITS = "Total Hits";
	public static final String TOTAL_PAGES = "Total Pages";
	public static final String TOTAL_BANDWIDTH = "Total Bandwidth (bytes)";
	public static final String HITS = "Hits";
	public static final String PAGES = "Pages";
	public static final String BANDWIDTH = "Bandwidth (bytes)";
	public static final String VISITOR = "Visitor";
	public static final String BROWSER = "Browser";
	public static final String OS = "OS";
	public static final String PAGE = "Page";
	public static final String RESPONSE_CODE = "Response Code";
}
