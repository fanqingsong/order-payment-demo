/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AppUserPrincipal.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】JWT 与 SecurityContext 中的用户主体 record。
 * 【为什么】统一 id、email、role 与 ROLE_* 权限。
 * 【怎么做】实现 UserDetails；from(User) 工厂方法。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   User ──► Principal ──► SecurityContext
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.security;

import com.example.orderservice.entity.Role;
import com.example.orderservice.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 应用在 Spring Security 中的「当前用户」模型。
 * <p>
 * 同时用于：数据库登录（含 password 哈希）与 JWT 请求（password 可为空）。
 * {@code ROLE_} 前缀是 Spring Security 对 {@code hasRole("ADMIN")} 的约定。
 */
public record AppUserPrincipal(UUID id, String email, String password, Role role) implements UserDetails {

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
