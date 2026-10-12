/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AuthController.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】认证 REST：POST /api/auth/register、/login。
 * 【为什么】前端获取 JWT 的公开入口。
 * 【怎么做】@Valid DTO → AuthService.register/login。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   register/login ──► JWT
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.controller;

import com.example.orderservice.dto.AuthResponse;
import com.example.orderservice.dto.LoginRequest;
import com.example.orderservice.dto.RegisterRequest;
import com.example.orderservice.dto.RegisterResponse;
import com.example.orderservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 API：注册与登录。路径在 {@link com.example.orderservice.config.SecurityConfig} 中配置为匿名可访问。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /** 成功返回 JWT；失败由 Spring Security 抛出 {@link org.springframework.security.core.AuthenticationException}。 */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
