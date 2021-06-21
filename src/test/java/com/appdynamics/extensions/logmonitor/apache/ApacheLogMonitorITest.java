/*
 * Copyright 2015. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.logmonitor.apache;

import static com.appdynamics.extensions.logmonitor.apache.Constants.FILEPOINTER_FILENAME;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import com.appdynamics.extensions.logmonitor.apache.ApacheLogMonitor;
import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.TaskOutput;

public class ApacheLogMonitorITest {
	
	private ApacheLogMonitor classUnderTest = new ApacheLogMonitor();
	
	@Test
	public void testMetricsCollection() throws Exception {
		Map<String, String> args = Maps.newHashMap();
		args.put("config-file","src/test/resources/conf/config.yml");
		args.put("grok-pattern-file","src/test/resources/conf/grok-patterns.grok");
		args.put("user-agent-regex-file","src/test/resources/conf/user-agent-regexes.yaml");

		TaskOutput result = classUnderTest.execute(args, null);
		assertTrue(result.getStatusMessage().contains("ApacheLogMonitor completes"));
	}
	
	@After
	public void deleteFilePointerFile() throws Exception {
		File filePointerFile = new File("./target/classes/com/appdynamics/extensions/logmonitor/apache/" + 
					FILEPOINTER_FILENAME);
		
		if (filePointerFile.exists()) {
			filePointerFile.delete();
		}
	}

}
