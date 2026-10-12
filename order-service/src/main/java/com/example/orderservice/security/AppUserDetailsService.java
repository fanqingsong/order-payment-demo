/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · AppUserDetailsService.java                                 ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Spring Security UserDetailsService 实现。
 * 【为什么】登录时按邮箱加载用户。
 * 【怎么做】email 小写化 → UserRepository → AppUserPrincipal.from。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   login email ──► UserDetails
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.security;

import com.example.orderservice.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Spring Security 登录入口：按邮箱（username）从数据库加载用户。
 * 邮箱统一转小写，与 {@link com.example.orderservice.service.AuthService} 注册时规范化一致。
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AppUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
