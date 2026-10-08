package com.aimatch.service;

import java.util.Map;

public interface CapabilityGraphService {

    Map<String, Object> buildPersonCapabilityGraph(Long userId, Long resumeId);

    Map<String, Object> buildJobCapabilityGraph(Long jobId);

    Map<String, Object> getCapabilityGraphData();

    Map<String, Object> getMyGraphData(Long userId);

    Map<String, Object> getGapGraph(Long resumeId, Long jobId);
}
