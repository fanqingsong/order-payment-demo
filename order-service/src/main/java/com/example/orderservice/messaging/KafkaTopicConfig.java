package com.example.orderservice.messaging;

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
