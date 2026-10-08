package com.aimatch.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobDTO(
    Long jobId,
    String title,
    String company,
    String department,
    String industry,
    String city,
    String salary,
    String education,
    Integer workYears,
    String description,
    String requirement,
    String skillRequirements,
    Integer headCount,
    Object parsedData
) {}