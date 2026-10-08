package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Job {

    private Long id;

    private Long hrId;

    private String title;

    private String company;

    private String department;

    private String jobType;

    private String industry;

    private String salary;

    private String city;

    private String education;

    private Integer workYears;

    private String description;

    private String requirement;

    private String skillRequirements;

    private String parsedJson;

    private String rawText;

    private Integer headCount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
