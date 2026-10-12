/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · RegisterRequest.java                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】注册请求 email/password record。
 * 【为什么】限制密码长度等基础安全。
 * 【怎么做】AuthController.register 入参。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   register body ──► AuthService
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/auth/register 请求体；密码长度 8～72 字符。 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
