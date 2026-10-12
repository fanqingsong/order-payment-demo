/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · KafkaConsumerErrorConfig.java                              ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】inventory-reserved 消费者重试与 DLT。
 * 【为什么】与 inventory 侧 order-created 错误处理对称。
 * 【怎么做】Recoverer → inventory-reserved.DLT。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   fail ──► retry ──► .DLT
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.config;

import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.messaging.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka 消费者<strong>错误处理</strong>：固定间隔重试 + 死信主题（DLT）。
 * <p>
 * 当 {@link com.example.paymentservice.messaging.InventoryReservedListener} 抛异常时：
 * <ol>
 *   <li>{@link DefaultErrorHandler} 按 {@link FixedBackOff} 重试 {@link #RETRY_COUNT} 次</li>
 *   <li>仍失败则 {@link DeadLetterPublishingRecoverer} 把原消息发到 {@code inventory-reserved.DLT}</li>
 * </ol>
 * 避免无限重试阻塞分区，同时保留「坏消息」供运维处理。
 */
@Configuration
public class KafkaConsumerErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerErrorConfig.class);

    /** 每次重试间隔（毫秒）。 */
    private static final long RETRY_DELAY_MILLIS = 500L;

    /** 除首次投递外，额外重试次数（Spring Kafka FixedBackOff 语义）。 */
    private static final long RETRY_COUNT = 3L;

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(KafkaTopics.INVENTORY_RESERVED_DLT, record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_DELAY_MILLIS, RETRY_COUNT)
        );
        errorHandler.setRetryListeners(new RetryLoggingListener());
        return errorHandler;
    }

    /** 把重试与进入 DLT 的过程打到日志，方便初学者对照 Kafka 行为。 */
    private static final class RetryLoggingListener implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
            InventoryReservedEvent event = eventFrom(record);
            if (deliveryAttempt == 1) {
                log.warn("Kafka processing failed: eventId={}, orderId={}, attempt={}",
                        eventId(event), orderId(event), deliveryAttempt, exception);
            } else {
                log.warn("Retrying Kafka event: eventId={}, orderId={}, retry={}/{}",
                        eventId(event), orderId(event), deliveryAttempt - 1, RETRY_COUNT, exception);
            }
        }

        @Override
        public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
            InventoryReservedEvent event = eventFrom(record);
            log.error("Kafka event sent to DLT: topic={}, eventId={}, orderId={}",
                    KafkaTopics.INVENTORY_RESERVED_DLT, eventId(event), orderId(event), exception);
        }

        private static InventoryReservedEvent eventFrom(ConsumerRecord<?, ?> record) {
            return record.value() instanceof InventoryReservedEvent event ? event : null;
        }

        private static Object eventId(InventoryReservedEvent event) {
            return event == null ? "unknown" : event.eventId();
        }

        private static Object orderId(InventoryReservedEvent event) {
            return event == null ? "unknown" : event.orderId();
        }
    }
}
