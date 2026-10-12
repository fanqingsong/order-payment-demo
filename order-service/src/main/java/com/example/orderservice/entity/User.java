/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · User.java                                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】用户 JPA 实体（BCrypt 密码哈希）。
 * 【为什么】JWT 认证的用户主数据。
 * 【怎么做】UUID id、唯一 email、Role、createdAt。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   users 表 ◄── AuthService
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** 用户账户，映射表 {@code users}。密码字段存 BCrypt 哈希，非明文。 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String password, Role role, Instant createdAt) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
