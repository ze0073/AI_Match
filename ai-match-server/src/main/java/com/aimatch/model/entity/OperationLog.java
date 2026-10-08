package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;

    private Long userId;

    private String username;

    private String userType;

    private String operationType;

    private String module;

    private String description;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String ipAddress;

    private Long costTime;

    private String result;

    private LocalDateTime createTime;
}
