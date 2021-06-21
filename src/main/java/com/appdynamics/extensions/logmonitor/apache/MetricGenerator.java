package com.appdynamics.extensions.logmonitor.apache;

import com.appdynamics.extensions.logmonitor.apache.metrics.ApacheLogMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.GroupMetrics;
import com.appdynamics.extensions.logmonitor.apache.metrics.Metrics;
import com.appdynamics.extensions.metrics.Metric;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static com.appdynamics.extensions.logmonitor.apache.Constants.*;
import static com.appdynamics.extensions.logmonitor.apache.util.ApacheLogMonitorUtil.convertValueToZeroIfNullOrNegative;

public class MetricGenerator {

    private ApacheLogMetrics logMetrics;
    private String metricPrefix;
    private List<Metric> metricList;
    public MetricGenerator(ApacheLogMetrics logMetrics, String metricPrefix){
        this.logMetrics=logMetrics;
        this.metricPrefix=metricPrefix;
        metricList = Lists.newArrayList();
    }

    protected List<Metric> uploadMetrics() {
        uploadSummaryMetrics();
        uploadAllMetrics(BROWSER, logMetrics.getBrowserMetrics());
        uploadAllMetrics(OS, logMetrics.getOsMetrics());
        uploadAllMetrics(SPIDER, logMetrics.getSpiderMetrics());
        uploadAllMetrics(VISITOR, logMetrics.getVisitorMetrics());
        uploadPageMetrics(logMetrics.getPageMetrics());
        uploadResponseCodeMetrics(logMetrics.getResponseCodeMetrics());
        return metricList;
    }

    private void uploadSummaryMetrics() {
        printCollectiveObservedCurrent(metricPrefix + TOTAL_HITS, logMetrics.getTotalHitCount());
        printCollectiveObservedCurrent(metricPrefix + TOTAL_BANDWIDTH, logMetrics.getTotalBandwidth());
        printCollectiveObservedCurrent(metricPrefix + TOTAL_PAGES, logMetrics.getTotalPageViewCount());
    }

    private void uploadAllMetrics(String groupName, GroupMetrics groupMetrics) {
        String groupPrefix = createGroupPrefix(groupName);
        uploadGroupMetrics(groupPrefix, groupMetrics, true);
        uploadMemberMetrics(groupPrefix, groupMetrics, true);
    }

    private void uploadPageMetrics(GroupMetrics groupMetrics) {
        String groupPrefix = createGroupPrefix(PAGE);
        uploadGroupMetrics(groupPrefix, groupMetrics, false);
        uploadMemberMetrics(groupPrefix, groupMetrics, false);
    }

    private void uploadResponseCodeMetrics(GroupMetrics groupMetrics) {
        String groupPrefix = createGroupPrefix(RESPONSE_CODE);
        uploadMemberMetrics(groupPrefix, groupMetrics, true);
    }

    private void uploadGroupMetrics(String groupPrefix, GroupMetrics groupMetrics, boolean includePageMetrics) {
        printCollectiveObservedCurrent(groupPrefix + TOTAL_HITS, groupMetrics.getHitCount());
        printCollectiveObservedCurrent(groupPrefix + TOTAL_BANDWIDTH, groupMetrics.getBandwidth());
        if (includePageMetrics) {
            printCollectiveObservedCurrent(groupPrefix + TOTAL_PAGES, groupMetrics.getPageViewCount());
        }
    }

    private void uploadMemberMetrics(String groupPrefix, GroupMetrics groupMetrics, boolean includePageMetrics) {
        for (Map.Entry<String, Metrics> member : groupMetrics.getMembers().entrySet()) {
            String memberPrefix = String.format("%s%s%s", groupPrefix, member.getKey(), METRIC_PATH_SEPARATOR);

            Metrics metrics = member.getValue();
            printCollectiveObservedCurrent(memberPrefix + HITS, metrics.getHitCount());
            printCollectiveObservedCurrent(memberPrefix + BANDWIDTH, metrics.getBandwidth());

            if (includePageMetrics) {
                printCollectiveObservedCurrent(memberPrefix + PAGES, metrics.getPageViewCount());
            }
        }
    }

    private void printCollectiveObservedCurrent(String metricName, BigInteger metricValue) {
        metricList.add(new Metric(metricName, (convertValueToZeroIfNullOrNegative(metricValue)).toString(),
                metricName,MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE));
    }

    private String createGroupPrefix(String groupName) {
        return String.format("%s%s%s", metricPrefix, groupName, METRIC_PATH_SEPARATOR);
    }
}
