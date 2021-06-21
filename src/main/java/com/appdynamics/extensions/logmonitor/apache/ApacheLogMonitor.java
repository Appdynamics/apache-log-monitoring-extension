/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.Constants.*;
import java.util.List;
import java.util.Map;

import com.appdynamics.extensions.ABaseMonitor;
import com.appdynamics.extensions.TasksExecutionServiceProvider;
import com.appdynamics.extensions.conf.MonitorContextConfiguration;
import com.appdynamics.extensions.logging.ExtensionsLoggerFactory;
import com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil;
import com.appdynamics.extensions.util.AssertUtils;
import com.google.common.collect.Maps;

import com.appdynamics.extensions.logmonitor.apache.config.ApacheLog;
import com.appdynamics.extensions.logmonitor.apache.processors.FilePointerProcessor;
import org.slf4j.Logger;

/**
 * @author Florencio Sarmiento
 *
 */
public class ApacheLogMonitor extends ABaseMonitor {

	private static final Logger logger = ExtensionsLoggerFactory.getLogger(ApacheLogMonitor.class);
	private MonitorContextConfiguration contextConfiguration;
	private Map<String,?> configYml = Maps.newHashMap();
	private volatile FilePointerProcessor filePointerProcessor;
	String grokPatternFilePath;
	String userAgentPatternFilePath;

	@Override
	protected String getDefaultMetricPrefix() {
		return DEFAULT_METRIC_PREFIX;
	}

	@Override
	public String getMonitorName() {
		return MONITOR_NAME;
	}

	@Override
	protected void initializeMoreStuff(Map<String, String> args) {
		contextConfiguration = getContextConfiguration();
		configYml = contextConfiguration.getConfigYml();
		grokPatternFilePath = args.get(GROK_PATTERN_FILE);
		userAgentPatternFilePath = args.get(USER_AGENT_REGEX_FILE);
		AssertUtils.assertNotNull(configYml,"The config.yml is not available");
	}

	@Override
	protected void doRun(TasksExecutionServiceProvider tasksExecutionServiceProvider) {
		List<Map<String,?>> apacheLogsFromConfig = (List<Map<String, ?>>) configYml.get(APACHE_LOGS);
		List<ApacheLog> logsToMonitor = ApacheLogMonitorUtil.getLogsFromConfig(apacheLogsFromConfig);
		filePointerProcessor = new FilePointerProcessor();
		for(ApacheLog apacheLogConfig: logsToMonitor){
			logger.info("Starting monitoring task for "+ apacheLogConfig.getDisplayName());
			ApacheLogMonitorTask task = new ApacheLogMonitorTask(contextConfiguration, tasksExecutionServiceProvider.getMetricWriteHelper(),filePointerProcessor, grokPatternFilePath, userAgentPatternFilePath,apacheLogConfig);
			tasksExecutionServiceProvider.submit(apacheLogConfig.getDisplayName(),task);
		}
	}

	@Override
	protected List<Map<String, ?>> getServers() {
		return (List<Map<String,?>>)configYml.get(APACHE_LOGS);
	}

//	public static void main(String[] args) throws TaskExecutionException {
//
//		ApacheLogMonitor monitor = new ApacheLogMonitor();
//		final Map<String, String> taskArgs = new HashMap<>();
//		taskArgs.put(CONFIG_FILE, "src/main/resources/conf/config.yml");
//		taskArgs.put(GROK_PATTERN_FILE, "src/main/resources/conf/patterns/grok-patterns.grok");
//		taskArgs.put(USER_AGENT_REGEX_FILE, "src/main/resources/conf/patterns/user-agent-regexes.yaml");
//		monitor.execute(taskArgs, null);
//	}
}
