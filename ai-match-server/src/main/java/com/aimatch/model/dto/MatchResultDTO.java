package com.aimatch.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MatchResultDTO(
    Long matchId,
    Long jobId,
    String jobTitle,
    String company,
    String salary,
    String city,
    String industry,
    BigDecimal matchScore,
    Object matchDetail
) {
    public static MatchResultDTO from(Map<String, Object> map) {
        return new MatchResultDTO(
            toLong(map.get("matchId")),
            toLong(map.get("jobId")),
            (String) map.get("jobTitle"),
            (String) map.get("company"),
            (String) map.get("salary"),
            (String) map.get("city"),
            (String) map.get("industry"),
            map.get("matchScore") instanceof BigDecimal bd ? bd : null,
            map.get("matchDetail")
        );
    }

    private static Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        return null;
    }
}
