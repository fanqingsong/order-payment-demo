/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · UserRepository.java                                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】User 表 JPA；按邮箱查找。
 * 【为什么】注册/登录唯一性。
 * 【怎么做】findByEmail、existsByEmail。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   email ──► User
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.repository;

import com.example.orderservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** 用户表 CRUD；邮箱唯一索引由数据库迁移脚本保证。 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
