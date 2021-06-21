# AppDynamics Apache Log Monitoring Extension

This extension works only with the standalone machine agent.

## Use Case

<p>Monitors Apache access log file and reports metrics such as successful hits, bandwidth and page access count of visitors, spiders, browsers and operating systems. 

Has the ability to display individual metrics per visitor, spider, browser, operating system, response code and page request.

**Note: By default, the Machine agent can only send a fixed number of metrics to the controller. This extension can potentially report thousands of metrics, so to change this limit, please follow the instructions mentioned [here](https://docs.appdynamics.com/21.6/en/application-monitoring/administer-app-server-agents/metrics-limits).** 

## Prerequisite

1. Before the extension is installed, the prerequisites mentioned [here](https://community.appdynamics.com/t5/Knowledge-Base/Extensions-Prerequisites-Guide/ta-p/35213) need to be met. Please do not proceed with the extension installation if the specified prerequisites are not met.
2. The extension must be deployed on same box as the one with apache logs files you wish to monitor.

## Installation

1. Run 'mvn clean install' from apache-log-monitoring-extension directory
2. Copy and unzip ApacheLogMonitor-\<version\>.zip from 'target' directory into \<machine_agent_dir\>/monitors/
3. Edit config.yml file in ApacheLogMonitor/conf and provide the required configuration (see Configuration section)
4. Restart the Machine Agent.

Please place the extension in the "monitors" directory of your Machine Agent installation directory. Do not place the extension in the "extensions" directory of your Machine Agent installation directory.

## Configuration

### config.yml

**Note: Please avoid using tab (\t) when editing yaml files. You may want to validate the yaml file using a [yaml validator](http://yamllint.com/).**

#### Configure Metric Prefix
Please follow section 2.1 of the [Document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) to set up metric prefix.
~~~
# Use this only if SIM is enabled
#metricPrefix: "Custom Metrics|Apache Log Monitor|"

# If SIM is not enabled, then use this
metricPrefix:  "Server|Component:<TIER_ID>|Custom Metrics|Apache Log Monitor|"
~~~

#### Apache log configurations

| Param | Description | Default Value | Example |
| ----- | ----- | ----- | ----- |
| displayName | The alias name of this log, used in metric path. |  | "Staging Apache" |
| logDirectory | The full directory path access log |  | "/var/log/apache2" |
| logName | The access log filename. Supports wildcard (\*) for dynamic filename |  | **Static name:**<br/>"access.log" <br/><br/>**Dynamic name:**<br/>"access\*.log" |
| logPattern | The grok pattern used for parsing the log. See examples for pre-defined pattern you can use.<br/><br/>If you're using a custom log format, you can create your own grok pattern to match this, see Grok Expressions section.  |  | **"%{COMMONAPACHELOG}"** - for common log format<br/><br/>**"%{COMBINEDAPACHELOG}"** - for combined log format |
| hitResponseCodes | The response codes used to determine a successful hit. Leave null to use default values. | 200, 304 | 200, 201, 304, 305 |
| nonPageExtensions | The URL extensions used to determine if request is for non-page access, e.g. image. Leave null to use default values | "ico", "css", "js",<br/>"class","gif","jpg",<br/>"jpeg","png","bmp",<br/>"rss","xml","swf" | "pdf","jpg" |
| **metricsFilterForCalculation** | **Filters unwanted metrics** | ----- | ----- |
| excludeVisitors | The list of visitor hosts to exclude. Note, this supports regex, so period must be escaped. |  | **Specific  Host:**<br/>"10\\\\.10\\\\.10\\\\.5",<br/>"127\\\\.1\\\\.1\\\\.0"<br/><br/>**Host Regex:**<br/>"10.\*", "127.\*" |
| excludeSpiders* | The list of spider names to exclude. Note, this supports regex. |  | **Specific Spider:**<br/>"GoogleBot",<br/>"Yahoo"<br/><br/>**Spider Regex:**<br/>"Google.\*" |
| excludeUrls | The list of request URLs (any files) to exclude. Note, this supports regex. |  | **Specific URL:**<br/>"/test.html",<br/>"/test/admin.html"<br/><br/>**URL Regex:**<br/>"/test/.\*" |
| excludeBrowsers* | The list of browser names to exclude. Note, this supports regex. |  | **Specific URL:**<br/>"Chrome",<br/>"Safari"<br/><br/>**URL Regex:**<br/>"Chro.\*" |
| excludeOs* | The list of OS names to exclude. Note, this supports regex. |  | **Specific OS:**<br/>"MAC OS X"<br/><br/>**OS Regex:**<br/>"MAC.\*" |
| **individualMetricsToDisplay** | **Displays individual metrics** | ----- | ----- |
| includeVisitors | The list of visitor hosts to display. Note, this supports regex, so period must be escaped. |  | **Specific  Host:**<br/>"10\\\\.10\\\\.10\\\\.5",<br/>"127\\\\.1\\\\.1\\\\.0"<br/><br/>**Host Regex:**<br/>"10.\*", "127.\*" |
| includeSpiders* | The list of spider names to display. Note, this supports regex. |  | **Specific Spider:**<br/>"GoogleBot",<br/>"Yahoo"<br/><br/>**Spider Regex:**<br/>"Google.\*" |
| includePages | The list of pages to display. Note, this supports regex. |  | **Specific Page:**<br/>"/test.html",<br/>"/test/admin.html"<br/><br/>**Page Regex:**<br/>"/test/.\*" |
| includeBrowsers* | The list of browser names to display. Note, this supports regex. |  | **Specific URL:**<br/>"Chrome",<br/>"Safari"<br/><br/>**URL Regex:**<br/>"Chro.\*" |
| includeOs* | The list of OS names to display. Note, this supports regex. |  | **Specific OS:**<br/>"MAC OS X"<br/><br/>**OS Regex:**<br/>"MAC.\*" |
| includeResponseCodes* | The list of response codes to display. |  | 200, 304, 404 500 |
| ----- | ----- | ----- | ----- |
| numberOfThreads | The no of threads used to process multiple apache logs concurrently | 3 | 3 |


**\*Requires user-agent details in the log, e.g. use combined log pattern in apache + specify logPattern as "%{COMBINEDAPACHELOG}" in this config.yaml.**

### sample config.yaml with static filename and dynamic filename

~~~
apacheLogs:
  - name: "StaticName"
    logDirectory: "/var/log/apache2"
    logName: "access.log"
    logPattern: "%{COMMONAPACHELOG}"
    hitResponseCodes: [ ] #leave null to use default values
    nonPageExtensions: [ ] #leave null to use default values
    
    metricsFilterForCalculation:
       excludeVisitors: [ ]
       excludeSpiders: [ ]
       excludeUrls: [ ]
       excludeBrowsers: [ ]
       excludeOs: [ ]
      
    individualMetricsToDisplay:
       includeVisitors: ["10\\.10.*" ]
       includeSpiders: ["Google.*" ]
       includePages: ["/test/.*" ]
       includeBrowsers: ["Chrome.*" ]
       includeOs: ["MAC.*" ]
       includeResponseCodes: [200, 305, 304, 400, 401, 500 ]
       
  - name: "DynamicLog"
    logDirectory: "/usr/log/apache2"
    logName: "access*.log"
    logPattern: "%{COMBINEDAPACHELOG}"
    hitResponseCodes: [ ] #leave null to use default values
    nonPageExtensions: [ ] #leave null to use default values
    
    metricsFilterForCalculation:
       excludeVisitors: [ ]
       excludeSpiders: [ ]
       excludeUrls: [ ]
       excludeBrowsers: [ ]
       excludeOs: [ ]
      
    individualMetricsToDisplay:
       includeVisitors: [ ]
       includeSpiders: [ ]
       includePages: [ ]
       includeBrowsers: [ ]
       includeOs: [ ]
       includeResponseCodes: [ ]
        
numberOfThreads: 5        

metricPrefix: "Custom Metrics|Apache Log Monitor|"
~~~

### Grok Expressions
Grok is a way to define and use complex, nested regular expressions in an easy to read and use format. Regexes defining discrete elements in a log file are mapped to grok-pattern names, which can also be used to create more complex patterns.

Grok file is located in **ApacheLogMonitor/conf/patterns/grok-patterns.grok**.

To add your own custom grok expression, simply edit the file above. Note, you must ensure that mandatory fields are captured:

- clientip
- response
- bytes
- request

Optional field used to determine the browser, os and spider details is as follow:

- agent

For example:

~~~
COMMONAPACHELOG %{IPORHOST:clientip} %{USER:ident} %{USER:auth} \[%{HTTPDATE:timestamp}\] "(?:%{WORD:verb} %{NOTSPACE:request}(?: HTTP/%{NUMBER:httpversion})?|%{DATA:rawrequest})" %{NUMBER:response} (?:%{NUMBER:bytes}|-)

MYCUSTOMAPACHELOG %{COMMONAPACHELOG} %{QS:referrer} %{QS:agent}
~~~

You can use [Grok Debugger](https://grokdebug.herokuapp.com/) to validate your expression.

Then, define your custom expression in logPattern field in config.yaml, e.g.

~~~
...
logPattern: "%{MYCUSTOMAPACHELOG}"
...
~~~

### monitor.xml
~~~
<argument name="config-file" is-required="true" default-value="monitors/ApacheLogMonitor/conf/config.yml" />
<argument name="grok-pattern-file" is-required="true" default-value="monitors/ApacheLogMonitor/conf/patterns/grok-patterns.grok" />
<argument name="user-agent-regex-file" is-required="true" default-value="monitors/ApacheLogMonitor/conf/patterns/user-agent-regexes.yaml" />
~~~

## Metrics

### Definition
| Metric | Description |
| ----- | ----- |
| Hits | No of any file requests where response code matches the defined hitResponseCode |
| Bandwidth (bytes) | File size in bytes |
| Pages | The no of page requests, excluding files where extensions are defined in nonPageExtensions. |

Typical Metric Path: **Application Infrastructure Performance|\<Tier\>|Custom Metrics|Apache Log Monitor|\<Log Name\>|** followed by the individual categories/metrics below:

| Metric | Description |
| ----- | ----- |
| Total Hits | Overall Total Hits (Visitor Hits + Spider Hits) |
| Total Bandwidth (bytes) | Overall Total Bandwidth (Visitor Bandwidth + Spider Bandwidth) |
| Total Pages | Overall Total Pages (Visitor Pages + Spider Pages) |

### Visitor, Spider, OS and Browser

| Metric | Description |
| ----- | ----- |
| Total Hits | No of hits |
| Total Bandwidth (bytes) | Bandwidth size |
| Total Pages | No of Pages |

### Page

| Metric | Description |
| ----- | ----- |
| Total Hits | No of hits |
| Total Bandwidth (bytes) | Bandwidth size |

### Response Code

| Metric | Description |
| ----- | ----- |
| Hits | No of times this response code is returned for any file request|
| Bandwidth (bytes) | Bandwidth size |
| Pages | No of times this response code is returned for any page request |

## Extensions Workbench
Workbench is an inbuilt feature provided with each extension in order to assist you to fine tune the extension setup before you actually deploy it on the controller. Please review the following document on [How to use the Extensions WorkBench](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-use-the-Extensions-WorkBench/ta-p/30130).

## Custom Dashboard Example
![image](http://community.appdynamics.com/t5/image/serverpage/image-id/1560iF816F2875A51A315/image-size/original?v=mpbl-1&px=-1)

## Troubleshooting
Please follow the steps listed in this [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) in order to troubleshoot your issue. These are a set of common issues that customers might have faced during the installation of the extension. If these don't solve your issue, please follow the last step on the [troubleshooting-document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) to contact the support team.

## Support Tickets

If after going through the [Troubleshooting Document](https://community.appdynamics.com/t5/Knowledge-Base/How-do-I-troubleshoot-missing-custom-metrics-or-extensions/ta-p/28695) you have not been able to get your extension working, please file a ticket with the following information:

1. Stop the running machine agent.
2. Delete all existing logs under <MachineAgent>/logs.
3. Please enable debug logging by editing the file <MachineAgent>/conf/logging/log4j.xml. Change the level value of the following <logger> elements to debug.
    ```
    <logger name="com.singularity">
    <logger name="com.appdynamics">
   ```
4. Start the machine agent and please let it run for 10 mins. Then zip and upload all the logs in the directory <MachineAgent>/logs/*.
   Attach the zipped <MachineAgent>/conf/* directory.
5. Attach the zipped <MachineAgent>/monitors/ExtensionFolderYouAreHavingIssuesWith directory.

For any support related questions, you can also contact help@appdynamics.com.

## Contributing

Always feel free to fork and contribute any changes directly via [GitHub](https://github.com/Appdynamics/apache-log-monitoring-extension).

## Version
|          Name            |  Version   |
|--------------------------|------------|
|Extension Version         |2.0.0       |
|Controller Compatibility  |4.5 or Later|
|Machine Agent Version     |4.5.13+     |
|Last Update               |21/06/2021  |
