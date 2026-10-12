/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · Role.java                                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】用户角色 USER / ADMIN。
 * 【为什么】映射 Spring Security ROLE_* 权限。
 * 【怎么做】枚举存 users.role 列。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   USER │ ADMIN
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

/** 用户角色，对应 Spring Security {@code ROLE_USER} / {@code ROLE_ADMIN}。 */
public enum Role {
    USER,
    ADMIN
}
