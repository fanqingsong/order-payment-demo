/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryServiceTest.java                                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】InventoryService 单元测试（预占成功/失败路径）。
 * 【为什么】不启 Kafka/DB 即可验证领域逻辑与事件发布。
 * 【怎么做】Mockito mock 依赖；ArgumentCaptor 捕获发布的事件。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   @Test ──mock──► reserveForOrder ──verify
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.InventoryItem;
import com.example.inventoryservice.entity.InventoryReservation;
import com.example.inventoryservice.event.InventoryReservationFailedEvent;
import com.example.inventoryservice.event.InventoryReservedEvent;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.messaging.InventoryEventPublisher;
import com.example.inventoryservice.repository.InventoryItemRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import com.example.inventoryservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private InventoryEventPublisher eventPublisher;

    @Test
    void publishesReservedWhenStockIsAvailable() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent orderEvent = orderEvent(orderId, "Keyboard", 2);
        when(processedEventRepository.existsById(orderEvent.eventId())).thenReturn(false);
        when(inventoryItemRepository.findById("Keyboard"))
                .thenReturn(Optional.of(new InventoryItem("Keyboard", 10, 0)));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new InventoryService(
                inventoryItemRepository,
                reservationRepository,
                processedEventRepository,
                eventPublisher
        ).reserveForOrder(orderEvent);

        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(eventPublisher).publishReserved(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(orderId);
        verify(eventPublisher, never()).publishReservationFailed(any());
    }

    @Test
    void publishesFailedWhenStockIsInsufficient() {
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent orderEvent = orderEvent(orderId, "OUT_OF_STOCK", 1);
        when(processedEventRepository.existsById(orderEvent.eventId())).thenReturn(false);
        when(inventoryItemRepository.findById("OUT_OF_STOCK"))
                .thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new InventoryService(
                inventoryItemRepository,
                reservationRepository,
                processedEventRepository,
                eventPublisher
        ).reserveForOrder(orderEvent);

        verify(eventPublisher).publishReservationFailed(any(InventoryReservationFailedEvent.class));
        verify(eventPublisher, never()).publishReserved(any());
        verify(reservationRepository, never()).save(any());
    }

    private OrderCreatedEvent orderEvent(UUID orderId, String productName, int quantity) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                "OrderCreated",
                1,
                Instant.now(),
                orderId,
                UUID.randomUUID(),
                productName,
                quantity,
                new BigDecimal("10.00")
        );
    }
}
