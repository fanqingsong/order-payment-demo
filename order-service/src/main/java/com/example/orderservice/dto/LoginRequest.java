/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · LoginRequest.java                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】登录请求 email + password record。
 * 【为什么】交给 AuthenticationManager 校验。
 * 【怎么做】@Email @NotBlank 字段。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   credentials ──► AuthService.login
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /api/auth/login 请求体。 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
