package com.avengers.jarvis.extension;

import java.util.Map;

public interface Reporter {

    /**
     * 上报qps
     * @param metric
     * @param tags
     */
    void qps(String metric, Map<String, String> tags);

    /**
     * 上报rt
     * @param metric
     * @param rt
     * @param tags
     */
    void rt(String metric, Long rt, Map<String, String> tags);
}
