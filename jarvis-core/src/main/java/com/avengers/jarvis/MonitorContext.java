package com.avengers.jarvis;


import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class MonitorContext {

    private static ThreadLocal<Context> threadLocal = new ThreadLocal<>();

    private static Context getContext() {
        if (null == threadLocal.get()) {
            threadLocal.set(new Context());
        }
        return threadLocal.get();
    }


    public static void start(String bizName) {
        long startTime = System.currentTimeMillis();
        if (startTime < 1L) {
            return;
        }
        getContext().start(bizName, startTime);
    }

    public static Long getStartTime(String bizName) {
        return getContext().getStartTime(bizName);
    }

    public static void remove(String bizName) {
        if (StringUtils.isNotEmpty(bizName)) {
            getContext().remove(bizName);
        }
    }

    public static void set(String key, String value) {
        getContext().set(key, value);
    }

    public static void setStartTime(String key, Long startTime) {
        getContext().start(key, startTime);
    }

    public static String get(String key) {
        return getContext().get(key);
    }

    public static Map<String, String> getAllContext() {
        return getContext().getContextMap();
    }

    public static void clear() {
        threadLocal.remove();
    }

    private static class Context {

        private Map<String, Long> startTimeMap = new HashMap<>();

        private Map<String, String> contextMap = new HashMap<>();

        public void start(String bizName, Long timeMills) {
            startTimeMap.put(bizName, timeMills);
        }

        public Long getStartTime(String bizName) {
            return startTimeMap.get(bizName);
        }

        public void remove(String bizName) {
            startTimeMap.remove(bizName);
        }

        public void set(String key, String value) {
            contextMap.put(key, value);
        }

        public String get(String key) {
            return contextMap.get(key);
        }

        public Map<String, String> getContextMap() {
            return contextMap;
        }

        public Map<String, Long> getStartTimeMap() {
            return startTimeMap;
        }

    }


}
