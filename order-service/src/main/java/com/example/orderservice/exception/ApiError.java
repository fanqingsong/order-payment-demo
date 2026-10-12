/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · ApiError.java                                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】统一 JSON 错误体 record。
 * 【为什么】前端一致处理 4xx/5xx。
 * 【怎么做】timestamp、status、error、message、path、validationErrors。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Exception ──► ApiError JSON
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.exception;

import java.time.Instant;
import java.util.Map;

/** 统一错误响应结构（时间戳、HTTP 状态、消息、请求路径、字段级校验错误）。 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}
