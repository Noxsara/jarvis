package com.avengers.jarvis;


import com.avengers.jarvis.constant.MetricsConstants;
import com.avengers.jarvis.exception.ReporterNotFoundException;
import com.avengers.jarvis.extension.Reporter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;


public class MethodMonitor {

    private static final Logger logger = LoggerFactory.getLogger(MethodMonitor.class);

    private static final Long DEFAULT_EXPECTED_TIME = 8000L;

    private static List<Reporter> reporters = new ArrayList<>();

    static {
        ServiceLoader<Reporter> loader = ServiceLoader.load(Reporter.class);
        Iterator<Reporter> iterator = loader.iterator();
        while (iterator.hasNext()) {
            Reporter reporter = iterator.next();
            reporters.add(reporter);
        }

        if (CollectionUtils.isEmpty(reporters)) {
           throw new ReporterNotFoundException("no report found.");
        }
    }

    public static void start(String bizName) {
        if (StringUtils.isEmpty(bizName)) {
            logger.warn("MethodMonitor.start, empty bizName.");
            return;
        }
        MonitorContext.start(bizName);
    }

    public static void end(String bizName) {
        end(bizName, DEFAULT_EXPECTED_TIME);
    }

    public static void end(String bizName, Long expectedExecuteTime) {
        end(bizName, expectedExecuteTime, null);
    }

    public static void end(String bizName, Long expectedExecuteTime, Map<String, String> tags) {
        if (StringUtils.isEmpty(bizName)) {
            logger.warn("MethodMonitor.end, empty bizName.");
            return;
        }

        Long startTime = MonitorContext.getStartTime(bizName);
        if (startTime == null || 0L == startTime) {
            logger.warn("MethodMonitor.end, empty start time.");
            return;
        }

        Long endTime = System.currentTimeMillis();
        Long responseTime = endTime - startTime;
        if (expectedExecuteTime != null && responseTime > expectedExecuteTime) {
            logger.warn("MethodMonitor.end, bizName:{} exceed expected time:{}, realTime:{}", bizName, expectedExecuteTime, responseTime);
        }

        for (Reporter reporter : reporters) {
            reporter.qps(bizName + MetricsConstants.TOTAL_QPS_SUFFIX, tags);
            reporter.rt(bizName + MetricsConstants.RT_SUFFIX, responseTime, tags);
        }

        MonitorContext.remove(bizName);
    }

    public static void exception(String bizName) {
        exception(bizName, null);
    }

    public static void exception(String bizName, int code) {
        Map<String, String> tags = new HashMap<>();
        tags.put("code", String.valueOf(code));

        //手动埋入的context值作为tag上报
        tags.putAll(MonitorContext.getAllContext());

        exception(bizName, tags);
    }

    public static void exception(String bizName, Map<String, String> tags) {
        for (Reporter reporter : reporters) {
            reporter.qps(bizName + MetricsConstants.FAIL_QPS_SUFFIX, tags);
        }
    }

}