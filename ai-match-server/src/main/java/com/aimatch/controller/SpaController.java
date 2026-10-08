package com.aimatch.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = {
        "/login", "/register", "/reset-password",
        "/jobseeker/**", "/hr/**", "/admin/**", "/match/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
