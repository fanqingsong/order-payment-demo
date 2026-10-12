/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · JwtServiceTest.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】JWT 签发解析往返测试。
 * 【为什么】claims 含 userId/email/role/过期。
 * 【怎么做】测试密钥 Reflection 或 @TestPropertySource。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   sign ──► parse ──► assert claims
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.security;

import com.example.orderservice.entity.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatedTokenContainsIdentityRoleAndExpiration() {
        JwtService jwtService = new JwtService(
                "test-secret-that-is-at-least-thirty-two-bytes-long",
                3_600_000
        );
        UUID userId = UUID.randomUUID();
        AppUserPrincipal principal = new AppUserPrincipal(
                userId,
                "learner@example.com",
                "encoded-password",
                Role.USER
        );

        JwtService.TokenDetails tokenDetails = jwtService.generateToken(principal);
        Claims claims = jwtService.parseToken(tokenDetails.token());

        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("learner@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getExpiration().toInstant()).isAfter(Instant.now());
        assertThat(tokenDetails.expiresAt()).isAfter(Instant.now());
    }
}
