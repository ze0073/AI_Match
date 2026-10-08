package com.aimatch.service.impl;

import com.aimatch.model.dto.LoginDTO;
import com.aimatch.model.dto.RegisterDTO;
import com.aimatch.model.entity.User;
import com.aimatch.security.JwtTokenProvider;
import com.aimatch.service.AuthService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final DataStores stores;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = stores.userStore.findOne(u -> u.getUsername().equals(loginDTO.getUsername()));

        if (user == null) {
            throw new BadCredentialsException("账号或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BadCredentialsException("账号已被禁用");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("账号或密码错误");
        }
        if (!user.getUserType().equalsIgnoreCase(loginDTO.getUserType())) {
            throw new BadCredentialsException("用户角色不匹配");
        }

        user.setLastLoginTime(LocalDateTime.now());
        stores.userStore.update(user);

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername(), user.getUserType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("userType", user.getUserType());
        result.put("realName", user.getRealName());
        return result;
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        User existing = stores.userStore.findOne(u ->
                registerDTO.getPhone().equals(u.getPhone()) || registerDTO.getPhone().equals(u.getUsername()));
        if (existing != null) {
            throw new RuntimeException("该手机号已注册");
        }

        User user = new User();
        user.setUsername(registerDTO.getPhone());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setUserType(registerDTO.getUserType());
        user.setRealName(registerDTO.getRealName());
        user.setCompany(registerDTO.getCompany());
        user.setStatus(1);
        stores.userStore.save(user);
    }

    @Override
    public Map<String, Object> getUserInfo(Long userId) {
        User user = stores.userStore.findById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", user.getPhone());
        result.put("email", user.getEmail());
        result.put("userType", user.getUserType());
        result.put("realName", user.getRealName());
        result.put("avatar", user.getAvatar());
        result.put("company", user.getCompany());
        result.put("lastLoginTime", user.getLastLoginTime());
        result.put("createTime", user.getCreateTime());
        return result;
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = stores.userStore.findById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        stores.userStore.update(user);
    }

    @Override
    public void resetPassword(String username, String newPassword) {
        User user = stores.userStore.findOne(u -> u.getUsername().equals(username) && u.getDeleted() == 0);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        stores.userStore.update(user);
    }
}
