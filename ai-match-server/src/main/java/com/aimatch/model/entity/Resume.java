package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Resume {

    private Long id;

    private Long userId;

    private String fileName;

    private String filePath;

    private String fileType;

    private String name;

    private String phone;

    private String email;

    private String education;

    private String school;

    private String major;

    private Integer workYears;

    private String currentCompany;

    private String currentPosition;

    private String jobIntention;

    private String expectedSalary;

    private String expectedCity;

    private String workExperience;

    private String projectExperience;

    private String skills;

    private String certificates;

    private String languages;

    private String summary;

    private String rawText;

    private String parsedJson;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
