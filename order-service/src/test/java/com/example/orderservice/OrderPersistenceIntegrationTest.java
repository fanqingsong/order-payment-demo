/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · OrderPersistenceIntegrationTest.java                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】订单持久化 + MockMvc + Testcontainers Postgres 集成测试。
 * 【为什么】不启 Kafka 验证 HTTP、安全、Outbox 落库。
 * 【怎么做】SpringBootTest；禁用 listener/outbox；@WithUser MockMvc 断言 DB。
 *
 * 示意图（order-service · 8080 · order_db）：
 *   Postgres TC ──► MockMvc ──► assert DB
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.orderservice;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.OutboxStatus;
import com.example.orderservice.entity.Role;
import com.example.orderservice.repository.OutboxEventRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.security.AppUserPrincipal;
import com.example.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "outbox.publisher.enabled=false"
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

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void commitsOrderAndOutboxRecordTogether() {
        UUID userId = UUID.randomUUID();
        OrderResponse created = orderService.createOrder(
                userId,
                new CreateOrderRequest("Mechanical Keyboard", 2, new BigDecimal("79.50"))
        );

        OrderResponse retrieved = orderService.getOrderForUser(created.id(), userId);
        OutboxEvent outboxEvent = outboxEventRepository.findAll().getFirst();

        assertThat(retrieved.id()).isEqualTo(created.id());
        assertThat(retrieved.userId()).isEqualTo(userId);
        assertThat(retrieved.productName()).isEqualTo("Mechanical Keyboard");
        assertThat(retrieved.quantity()).isEqualTo(2);
        assertThat(retrieved.totalAmount()).isEqualByComparingTo("159.00");
        assertThat(retrieved.status()).isEqualTo(OrderStatus.PENDING_INVENTORY);
        assertThat(outboxEvent.getAggregateId()).isEqualTo(created.id());
        assertThat(outboxEvent.getEventType()).isEqualTo("OrderCreated");
        assertThat(outboxEvent.getPayload()).contains("\"orderId\":\"" + created.id() + "\"");
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEvent.getPublishedAt()).isNull();
    }

    @Test
    void usersSeeOnlyTheirOrdersWhileAdminSeesAllOrders() throws Exception {
        AppUserPrincipal userA = principal(UUID.randomUUID(), "user-a@example.com", Role.USER);
        AppUserPrincipal userB = principal(UUID.randomUUID(), "user-b@example.com", Role.USER);
        AppUserPrincipal admin = principal(UUID.randomUUID(), "admin@example.com", Role.ADMIN);

        UUID userAOrderId = createOrder(userA, "User A Keyboard");
        UUID userBOrderId = createOrder(userB, "User B Mouse");

        mockMvc.perform(get("/api/orders").with(user(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(userAOrderId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userA.id().toString()));

        mockMvc.perform(get("/api/orders/{id}", userBOrderId).with(user(userA)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/orders/{id}", userAOrderId).with(user(userB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/admin/orders").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(
                        userAOrderId.toString(),
                        userBOrderId.toString()
                )));

        mockMvc.perform(get("/api/orders/{id}", userAOrderId).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userAOrderId.toString()));

        mockMvc.perform(get("/api/orders/{id}", userBOrderId).with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userBOrderId.toString()));

        mockMvc.perform(get("/api/admin/orders").with(user(userA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUsersCannotReadOrders() throws Exception {
        UUID orderId = UUID.randomUUID();

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    private UUID createOrder(AppUserPrincipal principal, String productName) throws Exception {
        String response = mockMvc.perform(post("/api/orders")
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "%s",
                                  "quantity": 1,
                                  "unitPrice": 49.95
                                }
                                """.formatted(productName)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, OrderResponse.class).id();
    }

    private AppUserPrincipal principal(UUID id, String email, Role role) {
        return new AppUserPrincipal(id, email, "encoded-password", role);
    }
}
