/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · SecurityConfig.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Spring Security 过滤器链与 JWT 无状态配置。
 * 【为什么】保护 /api/orders；/api/auth 公开；管理员路由需 ROLE_ADMIN。
 * 【怎么做】SecurityFilterChain：禁用 CSRF/session；JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Request ──► JWT Filter ──► 鉴权 ──► Controller
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.config;

import com.example.orderservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security 配置：无状态 JWT、URL 授权规则、密码加密。
 * <p>
 * 请求流程：{@link JwtAuthenticationFilter} 解析 Bearer Token → 填充 SecurityContext →
 * 控制器通过 {@code @AuthenticationPrincipal} 取当前用户。
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            AuthenticationProvider authenticationProvider
    ) throws Exception {
        return http
                // REST API 通常关闭 CSRF；若用 Cookie 存 Token 则需另议
                .csrf(csrf -> csrf.disable())
                // 不使用 HttpSession 存登录态，完全依赖 JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied")))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/orders").hasRole("ADMIN")
                        .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                        .anyRequest().denyAll())
                .authenticationProvider(authenticationProvider)
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** 登录时校验邮箱+密码：从数据库加载 UserDetails 并比对 BCrypt 哈希。 */
    @Bean
    AuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
