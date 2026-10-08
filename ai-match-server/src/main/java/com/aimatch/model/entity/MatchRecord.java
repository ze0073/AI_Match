package com.aimatch.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MatchRecord {

    private Long id;

    private Long resumeId;

    private Long jobId;

    private Long userId;

    private BigDecimal matchScore;

    private String matchDetail;

    private String matchType;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
