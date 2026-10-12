/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · JwtService.java                                            ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】JWT 签发与校验（JJWT）。
 * 【为什么】login 发 token；filter 验 token。
 * 【怎么做】HMAC 密钥 + TTL；claims 含 userId、email、role。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   login ──sign──► JWT ──verify──► filter
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT 签发与校验。Token 载荷包含 userId、email、role，供 API 授权与 {@link AppUserPrincipal} 重建。
 * <p>
 * {@code jwt.secret} 必须足够长以符合 HMAC 密钥要求；生产环境务必用环境变量注入强密钥。
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMilliseconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-milliseconds}") long expirationMilliseconds
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMilliseconds = expirationMilliseconds;
    }

    /** 登录成功后调用：生成带过期时间的签名 Token。 */
    public TokenDetails generateToken(AppUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(expirationMilliseconds);
        String token = Jwts.builder()
                .subject(principal.email())
                .claim("userId", principal.id().toString())
                .claim("email", principal.email())
                .claim("role", principal.role().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new TokenDetails(token, expiresAt);
    }

    /** 校验签名与过期时间，返回 Claims；非法 Token 抛 {@link JwtException}。 */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record TokenDetails(String token, Instant expiresAt) {
    }
}
