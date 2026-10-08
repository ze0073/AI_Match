package com.aimatch.service.impl;

import com.aimatch.model.dto.LoginDTO;
import com.aimatch.model.dto.RegisterDTO;
import com.aimatch.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Test
    void registerAndLogin_shouldSucceed() {
        String phone = "138" + (System.currentTimeMillis() % 100000000);
        RegisterDTO register = new RegisterDTO();
        register.setPhone(phone);
        register.setPassword("Test123456");
        register.setUserType("JOBSEEKER");

        authService.register(register);

        LoginDTO login = new LoginDTO();
        login.setUsername(phone);
        login.setPassword("Test123456");

        var result = authService.login(login);
        assertNotNull(result);
        assertNotNull(result.get("token"));
    }
}