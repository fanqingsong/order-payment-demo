/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaConsumerErrorConfig.java                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】order-created 消费者的重试与死信（DLT）配置类。
 * 【为什么】演示消费失败时不无限阻塞分区，并把问题消息送到 DLT 便于排查。
 * 【怎么做】DefaultErrorHandler + 500ms×3 次重试 + DeadLetterPublishingRecoverer 写入 order-created.DLT。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   消息 ──X──► 重试(3) ──► order-created.DLT
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.config;

import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.messaging.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConsumerErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerErrorConfig.class);

    private static final long RETRY_DELAY_MILLIS = 500L;
    private static final long RETRY_COUNT = 3L;

    @Bean
    DefaultErrorHandler orderCreatedKafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(KafkaTopics.ORDER_CREATED_DLT, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_DELAY_MILLIS, RETRY_COUNT)
        );
        errorHandler.setRetryListeners(new RetryLoggingListener());
        return errorHandler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> orderCreatedKafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler orderCreatedKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(orderCreatedKafkaErrorHandler);
        return factory;
    }

    private static final class RetryLoggingListener implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
            OrderCreatedEvent event = eventFrom(record);
            log.warn("Kafka processing failed: eventId={}, orderId={}, attempt={}",
                    eventId(event), orderId(event), deliveryAttempt, exception);
        }

        @Override
        public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
            OrderCreatedEvent event = eventFrom(record);
            log.error("Kafka event sent to DLT: topic={}, eventId={}, orderId={}",
                    KafkaTopics.ORDER_CREATED_DLT, eventId(event), orderId(event), exception);
        }

        private static OrderCreatedEvent eventFrom(ConsumerRecord<?, ?> record) {
            return record.value() instanceof OrderCreatedEvent event ? event : null;
        }

        private static Object eventId(OrderCreatedEvent event) {
            return event == null ? "unknown" : event.eventId();
        }

        private static Object orderId(OrderCreatedEvent event) {
            return event == null ? "unknown" : event.orderId();
        }
    }
}
