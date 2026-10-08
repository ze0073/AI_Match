package com.aimatch.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResumeDTO(
    Long resumeId,
    String name,
    String phone,
    String email,
    String education,
    String school,
    String major,
    Integer workYears,
    String currentCompany,
    String currentPosition,
    String skills,
    String certificates,
    String jobIntention,
    String expectedSalary,
    String expectedCity,
    Object parsedData
) {}