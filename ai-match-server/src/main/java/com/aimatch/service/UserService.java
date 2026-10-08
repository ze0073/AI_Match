package com.aimatch.service;

import com.aimatch.model.entity.User;

public interface UserService {

    User findByPhone(String phone);

    User findByEmail(String email);

    User findByUsername(String username);
}
