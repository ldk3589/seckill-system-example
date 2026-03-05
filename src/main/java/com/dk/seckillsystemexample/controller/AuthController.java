package com.dk.seckillsystemexample.controller;


import com.dk.seckillsystemexample.common.ApiResponse;
import com.dk.seckillsystemexample.dto.LoginReq;
import com.dk.seckillsystemexample.dto.RegisterReq;
import com.dk.seckillsystemexample.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterReq req) {
        authService.register(req.username, req.password);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@Valid @RequestBody LoginReq req) {
        String token = authService.login(req.username, req.password);
        return ApiResponse.ok(Map.of("token", token));
    }
}