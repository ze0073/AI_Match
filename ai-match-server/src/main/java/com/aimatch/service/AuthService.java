package com.aimatch.service;

import com.aimatch.model.dto.LoginDTO;
import com.aimatch.model.dto.RegisterDTO;
import java.util.Map;

public interface AuthService {

    Map<String, Object> login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    Map<String, Object> getUserInfo(Long userId);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void resetPassword(String username, String newPassword);

}