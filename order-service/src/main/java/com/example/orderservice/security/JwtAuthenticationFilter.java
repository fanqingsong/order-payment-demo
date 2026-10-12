/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · JwtAuthenticationFilter.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】每请求解析 Authorization: Bearer 的过滤器。
 * 【为什么】无状态 API 建立 Authentication。
 * 【怎么做】OncePerRequestFilter：parseToken → 设置 SecurityContext。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Bearer JWT ──► SecurityContext
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.example.orderservice.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 每个 HTTP 请求执行一次：从 {@code Authorization: Bearer <token>} 解析 JWT，建立 Spring Security 身份。
 * <p>
 * 若无 Token 或解析失败，不中断请求链——由后续 {@code authorizeHttpRequests} 决定是否 401。
 * 密码字段在 JWT 场景下留空字符串，因为认证已由 Token 完成。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtService.parseToken(authorization.substring(7));
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AppUserPrincipal principal = new AppUserPrincipal(
                        UUID.fromString(claims.get("userId", String.class)),
                        claims.get("email", String.class),
                        "",
                        Role.valueOf(claims.get("role", String.class))
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
