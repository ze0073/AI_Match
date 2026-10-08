package com.aimatch.service;

import com.aimatch.model.entity.MatchRecord;
import java.util.List;
import java.util.Map;

public interface MatchService {

    Map<String, Object> matchPersonToJobs(Long resumeId, Long userId);

    Map<String, Object> matchJobToCandidates(Long jobId);

    List<Map<String, Object>> getRecommendationsForJobseeker(Long userId, int limit);

    List<Map<String, Object>> getRecommendationsForHr(Long jobId, int limit);

    Map<String, Object> getMatchDetail(Long matchId);
}
