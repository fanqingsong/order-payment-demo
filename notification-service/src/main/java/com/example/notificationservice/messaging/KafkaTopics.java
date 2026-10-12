/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopics.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】通知服务订阅的 Topic 常量。
 * 【为什么】本服务不发布业务消息，只读支付结果。
 * 【怎么做】PAYMENT_COMPLETED、PAYMENT_FAILED 字符串。
 *
 * 示意图（notification-service · 8082 · 无库）：
 *   只读: payment-completed / payment-failed
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.notificationservice.messaging;

/** 通知服务只订阅支付结果，不发布业务主题。 */
public final class KafkaTopics {

    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";

    private KafkaTopics() {
    }
}
