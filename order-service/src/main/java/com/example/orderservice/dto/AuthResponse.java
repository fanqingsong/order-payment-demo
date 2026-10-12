/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AuthResponse.java                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】登录成功响应：accessToken、tokenType、expiresAt。
 * 【为什么】客户端保存 Bearer Token 访问受保护 API。
 * 【怎么做】Java record，AuthService.login 返回。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   login ──► { accessToken, expiresAt }
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import java.time.Instant;

/**
 * 登录成功响应。客户端应在后续请求头携带：{@code Authorization: Bearer <accessToken>}。
 */
public record AuthResponse(String accessToken, String tokenType, Instant expiresAt) {
}
