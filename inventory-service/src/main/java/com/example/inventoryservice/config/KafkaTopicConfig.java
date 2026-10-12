/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaTopicConfig.java                                      ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】启动时声明库存服务需要的 Kafka Topic。
 * 【为什么】Demo 环境可能未预建主题，避免 Listener 订阅时报错。
 * 【怎么做】@Configuration 中 @Bean NewTopic，TopicBuilder 指定分区/副本数。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   应用启动 ──► Kafka Admin ──► 创建 Topic
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.config;

import com.example.inventoryservice.messaging.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderCreatedTopic() {
        return topic(KafkaTopics.ORDER_CREATED);
    }

    @Bean
    NewTopic orderCreatedDltTopic() {
        return topic(KafkaTopics.ORDER_CREATED_DLT);
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
