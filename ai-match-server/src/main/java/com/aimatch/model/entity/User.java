package com.aimatch.model.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String phone;

    private String email;

    private String realName;

    private String userType;

    private Integer status;

    private String avatar;

    private String company;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer deleted;
}
