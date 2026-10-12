/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · RegisterResponse.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】注册成功返回 id、email、role、createdAt（不含 Token）。
 * 【为什么】注册与登录分离，需再 login 取 JWT。
 * 【怎么做】AuthService.register 构造 record。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   register ──► 用户概要
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.dto;

import com.example.orderservice.entity.Role;

import java.time.Instant;
import java.util.UUID;

/** 注册成功返回的用户概要（不含密码、不含 Token；需再调用 login）。 */
public record RegisterResponse(UUID id, String email, Role role, Instant createdAt) {
}
