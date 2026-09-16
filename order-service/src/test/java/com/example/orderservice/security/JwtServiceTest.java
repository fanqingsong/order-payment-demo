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
