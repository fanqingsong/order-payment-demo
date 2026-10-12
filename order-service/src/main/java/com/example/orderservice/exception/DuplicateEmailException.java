/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · DuplicateEmailException.java                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】注册邮箱已存在时抛出的运行时异常。
 * 【为什么】映射 HTTP 409 Conflict。
 * 【怎么做】GlobalExceptionHandler 捕获并 buildResponse。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   dup email ──► 409
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.exception;

/** 注册邮箱已存在，映射 HTTP 409 Conflict。 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account already exists for email: " + email);
    }
}
