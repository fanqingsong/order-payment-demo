/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · PaymentKafkaIntegrationTest.java                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】Testcontainers Kafka+Postgres 端到端支付流测试。
 * 【为什么】验证消费、落库、发布 payment-completed。
 * 【怎么做】SpringBootTest + KafkaTemplate 发消息 + poll 断言。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   TC Kafka ──► listener ──► DB ──► event
 *
 * 详见仓库 docs/architecture.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.messaging.KafkaTopics;
import com.example.paymentservice.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PaymentKafkaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine")
    )
            .withDatabaseName("payment_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:4.1.0")
    );

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<UUID, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void consumesInventoryReservedPersistsPaymentAndPublishesResult() throws Exception {
        UUID orderId = UUID.randomUUID();
        InventoryReservedEvent inventoryEvent = new InventoryReservedEvent(
                UUID.randomUUID(),
                "InventoryReserved",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                "Integration Test Product",
                1,
                new BigDecimal("125.00"),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        try (KafkaConsumer<UUID, String> resultConsumer = paymentResultConsumer()) {
            resultConsumer.subscribe(List.of(KafkaTopics.PAYMENT_COMPLETED));
            resultConsumer.poll(Duration.ofMillis(500));

            kafkaTemplate.send(KafkaTopics.INVENTORY_RESERVED, orderId, inventoryEvent).get();

            Payment payment = waitForPayment(orderId);
            ConsumerRecord<UUID, String> result = waitForPaymentResult(resultConsumer, orderId);

            assertThat(payment.getOrderId()).isEqualTo(orderId);
            assertThat(payment.getAmount()).isEqualByComparingTo("125.00");
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
            assertThat(result.key()).isEqualTo(orderId);
            assertThat(result.value()).contains("\"eventType\":\"PaymentCompleted\"");
            assertThat(result.value()).contains("\"orderId\":\"" + orderId + "\"");
        }
    }

    private KafkaConsumer<UUID, String> paymentResultConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "payment-integration-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class
        ));
    }

    private Payment waitForPayment(UUID orderId) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            Payment payment = paymentRepository.findAll().stream()
                    .filter(candidate -> candidate.getOrderId().equals(orderId))
                    .findFirst()
                    .orElse(null);
            if (payment != null) {
                return payment;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Payment was not persisted within 15 seconds");
    }

    private ConsumerRecord<UUID, String> waitForPaymentResult(
            KafkaConsumer<UUID, String> consumer,
            UUID orderId
    ) {
        Instant deadline = Instant.now().plusSeconds(15);
        while (Instant.now().isBefore(deadline)) {
            ConsumerRecords<UUID, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<UUID, String> record : records) {
                if (orderId.equals(record.key())) {
                    return record;
                }
            }
        }
        throw new AssertionError("Payment result was not published within 15 seconds");
    }
}
