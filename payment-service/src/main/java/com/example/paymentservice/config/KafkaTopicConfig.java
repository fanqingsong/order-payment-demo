/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopicConfig.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】支付服务 Kafka Topic 声明。
 * 【为什么】含 inventory-reserved 与 DLT、payment-*。
 * 【怎么做】@Bean NewTopic。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   启动 ──► 创建 topics
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.config;

import com.example.paymentservice.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 应用启动时在 Kafka 集群上<strong>声明主题</strong>（若不存在则创建）。
 * <p>
 * 本 demo 使用 1 分区、1 副本，适合本地 Docker；生产环境需按吞吐与可用性调整。
 * {@link KafkaTopics#INVENTORY_RESERVED_DLT} 用于消费失败后的死信，便于人工排查与重放。
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic inventoryReservedTopic() {
        return topic(KafkaTopics.INVENTORY_RESERVED);
    }

    @Bean
    NewTopic inventoryReservedDltTopic() {
        return topic(KafkaTopics.INVENTORY_RESERVED_DLT);
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
