/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopics.java                                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单服务使用的 Topic 名字常量。
 * 【为什么】与 inventory/payment/notification 共享命名契约。
 * 【怎么做】final 类静态字符串。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   常量 ═══ broker
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

/**
 * 本服务涉及的 Kafka 主题名称常量。
 * <p>
 * 各微服务各自维护一份同名常量，主题名必须在生产者与消费者之间<strong>字符串完全一致</strong>。
 */
public final class KafkaTopics {

    /** 订单创建后发布，inventory-service 消费。 */
    public static final String ORDER_CREATED = "order-created";

    /** 库存预占成功，payment-service 与 order-service 消费。 */
    public static final String INVENTORY_RESERVED = "inventory-reserved";

    /** 库存预占失败。 */
    public static final String INVENTORY_RESERVATION_FAILED = "inventory-reservation-failed";

    /** 支付成功，order-service 与 notification-service 消费。 */
    public static final String PAYMENT_COMPLETED = "payment-completed";

    /** 支付失败。 */
    public static final String PAYMENT_FAILED = "payment-failed";

    private KafkaTopics() {
    }
}
