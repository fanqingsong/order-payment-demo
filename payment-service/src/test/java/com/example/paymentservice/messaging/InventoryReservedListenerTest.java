/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryReservedListenerTest.java                         ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】InventoryReservedListener 委托与 KAFKA_FAIL 测试。
 * 【为什么】文档化故障注入钩子。
 * 【怎么做】Mock PaymentService；KAFKA_FAIL 应抛错且不 process。
 *
 * 示意图（payment-service · 8081 · payment_db）：
 *   KAFKA_FAIL ──► exception
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.paymentservice.messaging;

import com.example.paymentservice.event.InventoryReservedEvent;
import com.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InventoryReservedListenerTest {

    @Test
    void delegatesConsumedEventToPaymentService() {
        PaymentService paymentService = mock(PaymentService.class);
        InventoryReservedListener listener = new InventoryReservedListener(paymentService);
        InventoryReservedEvent event = inventoryEvent("Keyboard");

        listener.onInventoryReserved(event);

        verify(paymentService).processInventoryReserved(event);
    }

    @Test
    void intentionallyFailsForKafkaFailProduct() {
        PaymentService paymentService = mock(PaymentService.class);
        InventoryReservedListener listener = new InventoryReservedListener(paymentService);
        InventoryReservedEvent event = inventoryEvent("KAFKA_FAIL");

        assertThatThrownBy(() -> listener.onInventoryReserved(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated Kafka consumer failure");
        verify(paymentService, never()).processInventoryReserved(event);
    }

    private InventoryReservedEvent inventoryEvent(String productName) {
        return new InventoryReservedEvent(
                UUID.randomUUID(),
                "InventoryReserved",
                1,
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                productName,
                1,
                new BigDecimal("99.99"),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
