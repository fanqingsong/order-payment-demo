/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopics.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Kafka 主题名字面量常量集合。
 * 【为什么】统一 Listener、DLT、配置中的拼写，避免 typo。
 * 【怎么做】final 类，仅 public static final String 常量。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   代码 ═══ broker Topic 名
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.messaging;

public final class KafkaTopics {

    public static final String ORDER_CREATED = "order-created";
    public static final String ORDER_CREATED_DLT = "order-created.DLT";
    public static final String INVENTORY_RESERVED = "inventory-reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory-reservation-failed";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";

    private KafkaTopics() {
    }
}
