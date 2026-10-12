/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopics.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付服务 Topic 常量（含 DLT 名）。
 * 【为什么】Listener 与 ErrorHandler 共用。
 * 【怎么做】静态 final 字符串。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   topic 名字
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.messaging;

/**
 * payment-service 使用的 Kafka 主题名。
 * {@link #ORDER_CREATED_DLT} 为死信队列，命名惯例为原主题 + {@code .DLT}。
 */
public final class KafkaTopics {

    public static final String INVENTORY_RESERVED = "inventory-reserved";
    public static final String INVENTORY_RESERVED_DLT = "inventory-reserved.DLT";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";

    private KafkaTopics() {
    }
}
