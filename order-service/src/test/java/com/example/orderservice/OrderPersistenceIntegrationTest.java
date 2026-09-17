package com.example.orderservice;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.messaging.OrderEventPublisher;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false"
})
class OrderPersistenceIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:17-alpine")
    )
            .withDatabaseName("order_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private OrderService orderService;

    @Test
    void persistsAndRetrievesOrder() {
        UUID userId = UUID.randomUUID();
        OrderResponse created = orderService.createOrder(
                userId,
                new CreateOrderRequest("Mechanical Keyboard", 2, new BigDecimal("79.50"))
        );

        OrderResponse retrieved = orderService.getOrder(created.id());

        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.productName()).isEqualTo("Mechanical Keyboard");
        assertThat(retrieved.quantity()).isEqualTo(2);
        assertThat(retrieved.totalAmount()).isEqualByComparingTo("159.00");
        assertThat(retrieved.status()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }
}
