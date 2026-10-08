package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Skill {

    private Long id;

    private String name;

    private Long parentId;

    private String category;

    private Integer level;

    private String description;

    private String skillType;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
