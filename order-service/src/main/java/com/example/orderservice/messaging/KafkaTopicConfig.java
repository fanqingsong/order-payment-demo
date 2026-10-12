/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopicConfig.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】order-service 相关 Kafka Topic 自动声明。
 * 【为什么】Demo 集群预建主题。
 * 【怎么做】@Bean NewTopic：order-created、inventory-*、payment-* 等。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   启动 ──► 创建 Topic
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 订单服务作为部分主题的生产者/消费者，在启动时声明会用到的 Topic。
 * DLT 主题由 inventory-service 创建（消费 order-created 失败时写入）。
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return topic(KafkaTopics.ORDER_CREATED);
    }

    @Bean
    NewTopic inventoryReservedTopic() {
        return topic(KafkaTopics.INVENTORY_RESERVED);
    }

    @Bean
    NewTopic inventoryReservationFailedTopic() {
        return topic(KafkaTopics.INVENTORY_RESERVATION_FAILED);
    }

    @Bean
    NewTopic paymentCompletedTopic() {
        return topic(KafkaTopics.PAYMENT_COMPLETED);
    }

    @Bean
    NewTopic paymentFailedTopic() {
        return topic(KafkaTopics.PAYMENT_FAILED);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(1).replicas(1).build();
    }
}
