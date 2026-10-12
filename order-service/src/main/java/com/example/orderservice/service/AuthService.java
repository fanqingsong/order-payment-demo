/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AuthService.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】注册与登录业务：BCrypt、AuthenticationManager、发 JWT。
 * 【为什么】密码明文不落库；邮箱规范化防重复。
 * 【怎么做】register 查重+encode；login authenticate 后 jwtService.generateToken。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   register ──► users表
 *   login ──► JWT
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.service;

import com.example.orderservice.dto.AuthResponse;
import com.example.orderservice.dto.LoginRequest;
import com.example.orderservice.dto.RegisterRequest;
import com.example.orderservice.dto.RegisterResponse;
import com.example.orderservice.entity.Role;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.DuplicateEmailException;
import com.example.orderservice.repository.UserRepository;
import com.example.orderservice.security.AppUserPrincipal;
import com.example.orderservice.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

/**
 * 用户注册与登录。密码仅存 BCrypt 哈希，明文不落库。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = new User(email, passwordEncoder.encode(request.password()), Role.USER, Instant.now());
        User savedUser = userRepository.save(user);
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    /**
     * 委托 {@link AuthenticationManager} 校验凭证；通过后签发 JWT。
     * 故意不区分「用户不存在」与「密码错误」，降低邮箱枚举风险。
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password())
        );
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        JwtService.TokenDetails tokenDetails = jwtService.generateToken(principal);
        return new AuthResponse(tokenDetails.token(), "Bearer", tokenDetails.expiresAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
