package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TalentFavorite {

    private Long id;

    private Long hrId;

    private Long resumeId;

    private String category;

    private String notes;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
