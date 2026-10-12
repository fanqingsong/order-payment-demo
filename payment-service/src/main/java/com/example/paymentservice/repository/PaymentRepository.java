/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentRepository.java                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Payment 实体 JPA Repository。
 * 【为什么】持久化扣款记录。
 * 【怎么做】JpaRepository<Payment, UUID>。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   save Payment
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
