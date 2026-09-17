package com.example.paymentservice.config;

import com.example.paymentservice.event.OrderCreatedEvent;
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

@Configuration
public class KafkaConsumerErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerErrorConfig.class);
    private static final long RETRY_DELAY_MILLIS = 500L;
    private static final long RETRY_COUNT = 3L;

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
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

    private static final class RetryLoggingListener implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
            OrderCreatedEvent event = eventFrom(record);
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
