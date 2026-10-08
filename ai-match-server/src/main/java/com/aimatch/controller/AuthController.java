package com.aimatch.controller;

import com.aimatch.model.Result;
import com.aimatch.model.dto.LoginDTO;
import com.aimatch.model.dto.RegisterDTO;
import com.aimatch.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.aimatch.store.DataStores;
import com.aimatch.model.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@Tag(name = "认证授权", description = "用户登录、注册、个人信息管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DataStores stores;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginDTO loginDTO) {
        return Result.success(authService.login(loginDTO));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        authService.register(registerDTO);
        return Result.success("注册成功");
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    public Result<?> userInfo(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        return Result.success(authService.getUserInfo(userId));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<?> updatePassword(Principal principal,
                                     @Parameter(description = "旧密码") @RequestParam String oldPassword,
                                     @Parameter(description = "新密码") @RequestParam String newPassword) {
        Long userId = Long.parseLong(principal.getName());
        authService.updatePassword(userId, oldPassword, newPassword);
        return Result.success("密码修改成功");
    }

    @Operation(summary = "重置密码")
    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String newPassword = body.get("newPassword");
        if (username == null || username.isEmpty()) return Result.error("请输入用户名");
        if (newPassword == null || newPassword.length() < 6) return Result.error("密码至少6位");
        authService.resetPassword(username, newPassword);
        return Result.success("密码已重置，请重新登录");
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/userinfo")
    public Result<?> updateUserInfo(Principal principal, @RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(principal.getName());
        User user = stores.userStore.findById(userId);
        if (user == null) return Result.error("User not found");
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        if (body.containsKey("email")) user.setEmail((String) body.get("email"));
        if (body.containsKey("realName")) user.setRealName((String) body.get("realName"));
        if (body.containsKey("company")) user.setCompany((String) body.get("company"));
        stores.userStore.update(user);
        return Result.success("Updated");
    }
}