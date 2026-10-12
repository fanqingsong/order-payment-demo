#!/usr/bin/env python3
"""Generate java_file_header_meta.json for all Java files in the repo."""
import json
from pathlib import Path

OUT = Path(__file__).with_name("java_file_header_meta.json")


def m(what, why, how, diagram):
    return {"what": what, "why": why, "how": how, "diagram": diagram}


META = {
    **dict.fromkeys([], {}),
}

def add(path, what, why, how, diagram):
    META[path] = m(what, why, how, diagram)

# inventory-service (20)
add("inventory-service/src/main/java/com/example/inventoryservice/InventoryServiceApplication.java",
    "库存微服务的 Spring Boot 启动入口。", "独立进程消费 order-created、更新 inventory_db，承担 Saga「预占库存」步骤。",
    "main 启动 Spring 容器，注册 Kafka Listener、InventoryService、JPA 仓库等 Bean。",
    ["  ┌─────────────┐", "  │ Spring Boot │──► Kafka + PostgreSQL(inventory_db)", "  └─────────────┘"])
add("inventory-service/src/main/java/com/example/inventoryservice/config/KafkaConsumerErrorConfig.java",
    "order-created 消费者的重试与死信（DLT）配置类。", "演示消费失败时不无限阻塞分区，并把问题消息送到 DLT 便于排查。",
    "DefaultErrorHandler + 500ms×3 次重试 + DeadLetterPublishingRecoverer 写入 order-created.DLT。",
    ["  消息 ──X──► 重试(3) ──► order-created.DLT"])
add("inventory-service/src/main/java/com/example/inventoryservice/config/KafkaTopicConfig.java",
    "启动时声明库存服务需要的 Kafka Topic。", "Demo 环境可能未预建主题，避免 Listener 订阅时报错。",
    "@Configuration 中 @Bean NewTopic，TopicBuilder 指定分区/副本数。",
    ["  应用启动 ──► Kafka Admin ──► 创建 Topic"])
add("inventory-service/src/main/java/com/example/inventoryservice/entity/InventoryItem.java",
    "SKU 库存实体：可售 available 与已预占 reserved 数量。", "库存是 Saga 中被修改的核心数据；预占/确认/释放都作用于此表。",
    "JPA @Entity；reserve()/confirmReservation()/releaseReservation() 封装数量变更规则。",
    ["  available ──reserve──► reserved", "      ▲                  │", "      └──release◄──confirm"])
add("inventory-service/src/main/java/com/example/inventoryservice/entity/InventoryReservation.java",
    "单笔订单的预占记录（orderId、商品、数量、状态）。", "支付结果到达时需按 orderId 定位预占并确认或释放。",
    "UUID 主键 + ReservationStatus；提供 markConfirmed/markReleased 等领域方法。",
    ["  orderId ═══ 1 条 reservation 行"])
add("inventory-service/src/main/java/com/example/inventoryservice/entity/ProcessedEvent.java",
    "已处理 eventId 的幂等账本实体。", "Kafka 至少一次投递，重复消息不能重复扣库存。",
    "eventId 主键 + processedAt；处理前 existsById，成功后同事务 insert。",
    ["  eventId 已存在? ──是──► 跳过", "        └──否──► 业务 + 记账"])
add("inventory-service/src/main/java/com/example/inventoryservice/entity/ReservationStatus.java",
    "预占状态枚举：RESERVED / CONFIRMED / RELEASED。", "约束非法状态迁移（如对 RELEASED 再确认）。",
    "枚举存为字符串列，绑定在 InventoryReservation.status。",
    ["  RESERVED ──付成──► CONFIRMED", "      └──付败──► RELEASED"])
add("inventory-service/src/main/java/com/example/inventoryservice/event/InventoryReservationFailedEvent.java",
    "预占失败时发往 Kafka 的事件 record。", "通知 order-service 置 INVENTORY_FAILED，支付链路不会启动。",
    "含 orderId、product、quantity、reason；InventoryEventPublisher 发送。",
    ["  缺货 ──► inventory-reservation-failed ──► Order"])
add("inventory-service/src/main/java/com/example/inventoryservice/event/InventoryReservedEvent.java",
    "预占成功时发往 Kafka 的事件 record。", "同时触发 payment-service 与 order-service 的下一步。",
    "含 amount、reservationId、orderCreatedEventId 等追踪字段。",
    ["  预占 OK ──► inventory-reserved", "       ├────► Payment", "       └────► Order"])
add("inventory-service/src/main/java/com/example/inventoryservice/event/OrderCreatedEvent.java",
    "order-created 消息在本服务内的反序列化 DTO。", "各微服务独立编译，JSON 字段一致即可互通。",
    "Java record，OrderCreatedListener 从 Kafka 反序列化后传入 InventoryService。",
    ["  order-created(JSON) ──► OrderCreatedEvent"])
add("inventory-service/src/main/java/com/example/inventoryservice/event/PaymentCompletedEvent.java",
    "payment-completed 的入站 DTO（库存服务副本）。", "支付成功后需「确认」预占，实扣库存。",
    "Java record，PaymentResultListener 消费。",
    ["  payment-completed ──► confirm 库存"])
add("inventory-service/src/main/java/com/example/inventoryservice/event/PaymentFailedEvent.java",
    "payment-failed 的入站 DTO。", "支付失败时 Saga 补偿：释放预占。",
    "Java record，PaymentResultListener 消费。",
    ["  payment-failed ──► release 预占"])
add("inventory-service/src/main/java/com/example/inventoryservice/messaging/InventoryEventPublisher.java",
    "发布 inventory-reserved / inventory-reservation-failed 的组件。", "在本地 DB 事务提交后通知下游，完成库存出站步骤。",
    "注入 KafkaTemplate，send(topic, orderId, payload)。",
    ["  DB 提交 ──► KafkaTemplate.send"])
add("inventory-service/src/main/java/com/example/inventoryservice/messaging/KafkaTopics.java",
    "Kafka 主题名字面量常量集合。", "统一 Listener、DLT、配置中的拼写，避免 typo。",
    "final 类，仅 public static final String 常量。",
    ["  代码 ═══ broker Topic 名"])
add("inventory-service/src/main/java/com/example/inventoryservice/messaging/OrderCreatedListener.java",
    "订阅 order-created 的 Kafka 监听器。", "订单创建后自动进入「预占库存」Saga 步。",
    "@KafkaListener → InventoryService.reserveForOrder；商品 KAFKA_FAIL 可模拟失败。",
    ["  order-created ──► reserveForOrder()"])
add("inventory-service/src/main/java/com/example/inventoryservice/messaging/PaymentResultListener.java",
    "订阅 payment-completed / payment-failed 的监听器。", "支付终态后库存侧最终一致：确认扣减或释放。",
    "两个 @KafkaListener 方法分别调用 confirmAfterPayment / releaseAfterPaymentFailure。",
    ["  payment-* ──► confirm | release"])
add("inventory-service/src/main/java/com/example/inventoryservice/repository/InventoryItemRepository.java",
    "InventoryItem 的 Spring Data JPA Repository。", "持久化 SKU 库存行。",
    "extends JpaRepository<InventoryItem, String>，主键为 productName。",
    ["  Service ──► Repo ──► inventory_items 表"])
add("inventory-service/src/main/java/com/example/inventoryservice/repository/InventoryReservationRepository.java",
    "InventoryReservation 的 JPA Repository。", "按 orderId 查询预占记录。",
    "findByOrderId(UUID) + 标准 save/findById。",
    ["  orderId ──► Optional<Reservation>"])
add("inventory-service/src/main/java/com/example/inventoryservice/repository/ProcessedEventRepository.java",
    "processed_events 幂等表的 JPA 访问。", "支撑 existsById 去重。",
    "JpaRepository<ProcessedEvent, UUID>。",
    ["  eventId ──► 账本"])
add("inventory-service/src/main/java/com/example/inventoryservice/service/InventoryService.java",
    "库存领域服务：预占、确认、释放，含幂等与演示规则（OUT_OF_STOCK 等）。", "实现「先占库存再支付」，避免付了款没货。",
    "@Transactional 方法：幂等 → 改 Item/Reservation → ProcessedEvent → InventoryEventPublisher。",
    ["  OrderCreated", "      │", "      ▼", "  reserve ──OK──► inventory-reserved", "      └──FAIL──► reservation-failed"])
add("inventory-service/src/test/java/com/example/inventoryservice/service/InventoryServiceTest.java",
    "InventoryService 单元测试（预占成功/失败路径）。", "不启 Kafka/DB 即可验证领域逻辑与事件发布。",
    "Mockito mock 依赖；ArgumentCaptor 捕获发布的事件。",
    ["  @Test ──mock──► reserveForOrder ──verify"])

# notification-service (8)
add("notification-service/src/main/java/com/example/notificationservice/NotificationServiceApplication.java",
    "通知微服务 Spring Boot 启动类。", "演示 Saga 旁路：只消费支付结果，模拟邮件。",
    "@SpringBootApplication + main。",
    ["  Boot ──► Kafka Consumer only"])
add("notification-service/src/main/java/com/example/notificationservice/event/PaymentCompletedEvent.java",
    "payment-completed JSON 的本地 record DTO。", "与 payment-service 字段对齐，独立模块编译。",
    "Java record，Listener 反序列化用。",
    ["  JSON ──► PaymentCompletedEvent"])
add("notification-service/src/main/java/com/example/notificationservice/event/PaymentFailedEvent.java",
    "payment-failed JSON 的本地 record DTO。", "同上，跨服务靠 JSON 契约。",
    "Java record。",
    ["  JSON ──► PaymentFailedEvent"])
add("notification-service/src/main/java/com/example/notificationservice/messaging/KafkaTopics.java",
    "通知服务订阅的 Topic 常量。", "本服务不发布业务消息，只读支付结果。",
    "PAYMENT_COMPLETED、PAYMENT_FAILED 字符串。",
    ["  只读: payment-completed / payment-failed"])
add("notification-service/src/main/java/com/example/notificationservice/messaging/PaymentResultListener.java",
    "支付结果的 Kafka 监听器。", "与 Order/Inventory 并行消费（不同 consumer group）。",
    "@KafkaListener 转调 NotificationService。",
    ["  payment-* ──► NotificationService"])
add("notification-service/src/main/java/com/example/notificationservice/service/NotificationService.java",
    "用日志模拟发送邮件的服务。", "compose 日志中可观察 Saga 终态副作用。",
    "SLF4J info 输出 EMAIL SENT 模板。",
    ["  事件 ──► log \"EMAIL SENT\""])
add("notification-service/src/test/java/com/example/notificationservice/messaging/PaymentResultListenerTest.java",
    "PaymentResultListener  wiring 测试。", "确保监听器调用 NotificationService。",
    "Mock NotificationService + verify。",
    ["  listener ──verify──► send*"])
add("notification-service/src/test/java/com/example/notificationservice/service/NotificationServiceTest.java",
    "NotificationService 日志输出测试。", "回归 EMAIL SENT 文案。",
    "OutputCapture + AssertJ。",
    ["  service ──► 捕获日志"])

# order-service main
add("order-service/src/main/java/com/example/orderservice/OrderServiceApplication.java",
    "订单微服务 Spring Boot 启动类。", "聚合 REST、JWT、Outbox 定时任务、Kafka 消费/生产。",
    "@SpringBootApplication + @EnableScheduling（驱动 OutboxPublisher）。",
    ["  ┌──────────┐", "  │ :8080    │── HTTP + Outbox + Kafka", "  └──────────┘"])
add("order-service/src/main/java/com/example/orderservice/config/SecurityConfig.java",
    "Spring Security 过滤器链与 JWT 无状态配置。", "保护 /api/orders；/api/auth 公开；管理员路由需 ROLE_ADMIN。",
    "SecurityFilterChain：禁用 CSRF/session；JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前。",
    ["  Request ──► JWT Filter ──► 鉴权 ──► Controller"])
add("order-service/src/main/java/com/example/orderservice/controller/AdminOrderController.java",
    "管理员 REST：GET /api/admin/orders 列出全部订单。", "运营视角与用户 API 分离。",
    "@RestController + OrderService.getAllOrders()；需 ADMIN 角色。",
    ["  ADMIN + JWT ──► 全量订单列表"])
add("order-service/src/main/java/com/example/orderservice/controller/AuthController.java",
    "认证 REST：POST /api/auth/register、/login。", "前端获取 JWT 的公开入口。",
    "@Valid DTO → AuthService.register/login。",
    ["  register/login ──► JWT"])
add("order-service/src/main/java/com/example/orderservice/controller/OrderController.java",
    "用户订单 REST：创建、列表、按 ID 查询。", "从 JWT 取 userId，防止 body 伪造身份。",
    "@AuthenticationPrincipal AppUserPrincipal → OrderService。",
    ["  Bearer JWT ──► principal.id ──► OrderService"])
add("order-service/src/main/java/com/example/orderservice/dto/AuthResponse.java",
    "登录成功响应：accessToken、tokenType、expiresAt。", "客户端保存 Bearer Token 访问受保护 API。",
    "Java record，AuthService.login 返回。",
    ["  login ──► { accessToken, expiresAt }"])
add("order-service/src/main/java/com/example/orderservice/dto/CreateOrderRequest.java",
    "POST /api/orders 请求体 record。", "Bean Validation 校验商品名、数量、单价。",
    "@NotBlank/@Min/@DecimalMin 等注解。",
    ["  JSON ──@Valid──► createOrder"])
add("order-service/src/main/java/com/example/orderservice/dto/LoginRequest.java",
    "登录请求 email + password record。", "交给 AuthenticationManager 校验。",
    "@Email @NotBlank 字段。",
    ["  credentials ──► AuthService.login"])
add("order-service/src/main/java/com/example/orderservice/dto/OrderResponse.java",
    "订单 API 响应投影 record。", "隐藏 JPA 实体细节，稳定对外契约。",
    "OrderService 从 Order 实体映射字段。",
    ["  Order(entity) ──map──► OrderResponse"])
add("order-service/src/main/java/com/example/orderservice/dto/RegisterRequest.java",
    "注册请求 email/password record。", "限制密码长度等基础安全。",
    "AuthController.register 入参。",
    ["  register body ──► AuthService"])
add("order-service/src/main/java/com/example/orderservice/dto/RegisterResponse.java",
    "注册成功返回 id、email、role、createdAt（不含 Token）。", "注册与登录分离，需再 login 取 JWT。",
    "AuthService.register 构造 record。",
    ["  register ──► 用户概要"])
add("order-service/src/main/java/com/example/orderservice/entity/Order.java",
    "订单 JPA 实体（order_db.orders）。", "订单状态机在本服务落库，Saga 异步步回写此表。",
    "applyInventoryReserved/Failed、updatePaymentStatus；初始 PENDING_INVENTORY。",
    ["  PENDING_INVENTORY ──► PENDING_PAYMENT ──► PAID", "         └── INVENTORY_FAILED / PAYMENT_FAILED"])
add("order-service/src/main/java/com/example/orderservice/entity/OrderStatus.java",
    "订单生命周期枚举。", "与 inventory/payment 事件对齐，支持乱序支付结果。",
    "PENDING_INVENTORY、PENDING_PAYMENT、INVENTORY_FAILED、PAID、PAYMENT_FAILED。",
    ["  见 README.md 状态图"])
add("order-service/src/main/java/com/example/orderservice/entity/OutboxEvent.java",
    "事务性 Outbox 表行：待发 Kafka 的 JSON 载荷。", "下单与发 order-created 解耦，保证库与消息最终一致。",
    "含 eventType、payload、OutboxStatus；markPublished() 更新状态。",
    ["  同事务: Order + Outbox(PENDING)", "       └──► OutboxPublisher ──► PUBLISHED"])
add("order-service/src/main/java/com/example/orderservice/entity/OutboxStatus.java",
    "Outbox 行状态：PENDING / PUBLISHED。", "OutboxPublisher 只扫描 PENDING。",
    "枚举字段 on OutboxEvent.status。",
    ["  PENDING ──发送成功──► PUBLISHED"])
add("order-service/src/main/java/com/example/orderservice/entity/Role.java",
    "用户角色 USER / ADMIN。", "映射 Spring Security ROLE_* 权限。",
    "枚举存 users.role 列。",
    ["  USER │ ADMIN"])
add("order-service/src/main/java/com/example/orderservice/entity/User.java",
    "用户 JPA 实体（BCrypt 密码哈希）。", "JWT 认证的用户主数据。",
    "UUID id、唯一 email、Role、createdAt。",
    ["  users 表 ◄── AuthService"])
add("order-service/src/main/java/com/example/orderservice/event/InventoryReservationFailedEvent.java",
    "消费 inventory-reservation-failed 的 DTO。", "库存失败时更新订单状态。",
    "Java record → InventoryResultService。",
    ["  inv-fail event ──► INVENTORY_FAILED"])
add("order-service/src/main/java/com/example/orderservice/event/InventoryReservedEvent.java",
    "消费 inventory-reserved 的 DTO。", "库存成功 → PENDING_PAYMENT。",
    "Java record → InventoryResultService。",
    ["  inv-OK event ──► PENDING_PAYMENT"])
add("order-service/src/main/java/com/example/orderservice/event/OrderCreatedEvent.java",
    "下单后写入 Outbox/Kafka 的 OrderCreated 载荷。", "启动 inventory 预占链路。",
    "record：eventId、orderId、userId、product、quantity、amount。",
    ["  createOrder ──► OrderCreated ──► order-created"])
add("order-service/src/main/java/com/example/orderservice/event/PaymentCompletedEvent.java",
    "消费 payment-completed 的 DTO。", "订单置 PAID。",
    "PaymentResultListener → PaymentResultService。",
    ["  payment-completed ──► PAID"])
add("order-service/src/main/java/com/example/orderservice/event/PaymentFailedEvent.java",
    "消费 payment-failed 的 DTO。", "订单置 PAYMENT_FAILED。",
    "PaymentResultListener → PaymentResultService。",
    ["  payment-failed ──► PAYMENT_FAILED"])
add("order-service/src/main/java/com/example/orderservice/exception/ApiError.java",
    "统一 JSON 错误体 record。", "前端一致处理 4xx/5xx。",
    "timestamp、status、error、message、path、validationErrors。",
    ["  Exception ──► ApiError JSON"])
add("order-service/src/main/java/com/example/orderservice/exception/DuplicateEmailException.java",
    "注册邮箱已存在时抛出的运行时异常。", "映射 HTTP 409 Conflict。",
    "GlobalExceptionHandler 捕获并 buildResponse。",
    ["  dup email ──► 409"])
add("order-service/src/main/java/com/example/orderservice/exception/GlobalExceptionHandler.java",
    "@RestControllerAdvice 全局异常 → ApiError。", "集中 404/400/409/401 等响应。",
    "多个 @ExceptionHandler + buildResponse 辅助方法。",
    ["  throw ──► @ControllerAdvice ──► JSON"])
add("order-service/src/main/java/com/example/orderservice/exception/OrderNotFoundException.java",
    "订单不存在或无权访问异常。", "返回 404，避免泄露资源存在性。",
    "携带 orderId 构造消息。",
    ["  无订单 ──► 404"])
add("order-service/src/main/java/com/example/orderservice/messaging/InventoryResultListener.java",
    "消费 inventory-reserved / inventory-reservation-failed。", "把库存 Saga 结果写回 order_db。",
    "委托 InventoryResultService.handle*。",
    ["  inventory-* ──► InventoryResultService"])
add("order-service/src/main/java/com/example/orderservice/messaging/KafkaTopicConfig.java",
    "order-service 相关 Kafka Topic 自动声明。", "Demo 集群预建主题。",
    "@Bean NewTopic：order-created、inventory-*、payment-* 等。",
    ["  启动 ──► 创建 Topic"])
add("order-service/src/main/java/com/example/orderservice/messaging/KafkaTopics.java",
    "订单服务使用的 Topic 名字常量。", "与 inventory/payment/notification 共享命名契约。",
    "final 类静态字符串。",
    ["  常量 ═══ broker"])
add("order-service/src/main/java/com/example/orderservice/messaging/OrderEventPublisher.java",
    "向 order-created 发送 OrderCreatedEvent。", "由 OutboxPublisher 在异步任务中调用。",
    "KafkaTemplate.send(topic, orderId, event)。",
    ["  Outbox ──► order-created"])
add("order-service/src/main/java/com/example/orderservice/messaging/OutboxPublisher.java",
    "定时扫描 PENDING Outbox 并投递 Kafka。", "实现 Transactional Outbox，支持 Kafka 短暂不可用重试。",
    "@Scheduled + 反序列化 payload + publishOrderCreated().get() 成功后 markPublished。",
    ["  ┌─────────┐  poll   ┌───────┐", "  │ outbox  │───────►│ Kafka │", "  └─────────┘        └───────┘"])
add("order-service/src/main/java/com/example/orderservice/messaging/PaymentResultListener.java",
    "消费 payment-completed / payment-failed。", "异步更新订单支付终态。",
    "PaymentResultService.handleCompleted/Failed。",
    ["  payment-* ──► PaymentResultService"])
add("order-service/src/main/java/com/example/orderservice/repository/OrderRepository.java",
    "Order 实体的 Spring Data JPA。", "用户维度列表与按 id+userId 查询。",
    "findAllByUserIdOrderByCreatedAtDesc、findByIdAndUserId。",
    ["  OrderService ──► orders 表"])
add("order-service/src/main/java/com/example/orderservice/repository/OutboxEventRepository.java",
    "Outbox 表访问；轮询待发送记录。", "多实例下 pessimistic lock 降低重复发送风险。",
    "findTop100ByStatusOrderByCreatedAtAsc + @Lock PESSIMISTIC_WRITE。",
    ["  PENDING 行 ──FOR UPDATE──► 发送"])
add("order-service/src/main/java/com/example/orderservice/repository/UserRepository.java",
    "User 表 JPA；按邮箱查找。", "注册/登录唯一性。",
    "findByEmail、existsByEmail。",
    ["  email ──► User"])
add("order-service/src/main/java/com/example/orderservice/security/AppUserDetailsService.java",
    "Spring Security UserDetailsService 实现。", "登录时按邮箱加载用户。",
    "email 小写化 → UserRepository → AppUserPrincipal.from。",
    ["  login email ──► UserDetails"])
add("order-service/src/main/java/com/example/orderservice/security/AppUserPrincipal.java",
    "JWT 与 SecurityContext 中的用户主体 record。", "统一 id、email、role 与 ROLE_* 权限。",
    "实现 UserDetails；from(User) 工厂方法。",
    ["  User ──► Principal ──► SecurityContext"])
add("order-service/src/main/java/com/example/orderservice/security/JwtAuthenticationFilter.java",
    "每请求解析 Authorization: Bearer 的过滤器。", "无状态 API 建立 Authentication。",
    "OncePerRequestFilter：parseToken → 设置 SecurityContext。",
    ["  Bearer JWT ──► SecurityContext"])
add("order-service/src/main/java/com/example/orderservice/security/JwtService.java",
    "JWT 签发与校验（JJWT）。", "login 发 token；filter 验 token。",
    "HMAC 密钥 + TTL；claims 含 userId、email、role。",
    ["  login ──sign──► JWT ──verify──► filter"])
add("order-service/src/main/java/com/example/orderservice/service/AuthService.java",
    "注册与登录业务：BCrypt、AuthenticationManager、发 JWT。", "密码明文不落库；邮箱规范化防重复。",
    "register 查重+encode；login authenticate 后 jwtService.generateToken。",
    ["  register ──► users表", "  login ──► JWT"])
add("order-service/src/main/java/com/example/orderservice/service/InventoryResultService.java",
    "根据库存 Kafka 事件更新 Order 状态。", "连接 inventory Saga 与 order 聚合。",
    "@Transactional handleReserved/Failed；仅 PENDING_INVENTORY 可迁移。",
    ["  inventory event ──► order.status"])
add("order-service/src/main/java/com/example/orderservice/service/OrderService.java",
    "订单 CRUD 核心；createOrder 同事务写 Outbox。", "避免双写：不在下单事务内直调 Kafka。",
    "计算金额 → save Order(PENDING_INVENTORY) → save Outbox(JSON OrderCreated)。",
    ["  POST order", "     │", "     ▼", "  TX[ orders + outbox ]", "     └──► (异步) order-created"])
add("order-service/src/main/java/com/example/orderservice/service/PaymentResultService.java",
    "根据支付 Kafka 事件更新 Order。", "完成 Saga 支付腿在 order_db 的状态。",
    "handleCompleted/Failed；允许 PENDING_PAYMENT 或乱序 PENDING_INVENTORY。",
    ["  payment event ──► PAID | PAYMENT_FAILED"])

# order-service tests
add("order-service/src/test/java/com/example/orderservice/OrderPersistenceIntegrationTest.java",
    "订单持久化 + MockMvc + Testcontainers Postgres 集成测试。", "不启 Kafka 验证 HTTP、安全、Outbox 落库。",
    "SpringBootTest；禁用 listener/outbox；@WithUser MockMvc 断言 DB。",
    ["  Postgres TC ──► MockMvc ──► assert DB"])
add("order-service/src/test/java/com/example/orderservice/messaging/OrderEventPublisherTest.java",
    "OrderEventPublisher 单元测试。", "断言 Kafka message key 为 orderId。",
    "Mock KafkaTemplate + verify send。",
    ["  publish ──verify──► send(key=orderId)"])
add("order-service/src/test/java/com/example/orderservice/messaging/OutboxPublisherTest.java",
    "OutboxPublisher 成功/失败路径测试。", "发送失败时保持 PENDING 可重试。",
    "Mock 依赖；CompletableFuture 成功/异常分支。",
    ["  send OK ──► PUBLISHED", "  send FAIL ──► 仍 PENDING"])
add("order-service/src/test/java/com/example/orderservice/messaging/PaymentResultListenerTest.java",
    "PaymentResultListener 委托 PaymentResultService 测试。", "薄适配层回归。",
    "Mock service + verify handle*。",
    ["  listener ──► verify service"])
add("order-service/src/test/java/com/example/orderservice/security/JwtServiceTest.java",
    "JWT 签发解析往返测试。", "claims 含 userId/email/role/过期。",
    "测试密钥 Reflection 或 @TestPropertySource。",
    ["  sign ──► parse ──► assert claims"])
add("order-service/src/test/java/com/example/orderservice/security/OrderSecurityTest.java",
    "@WebMvcTest 安全规则：401/403、角色隔离。", "防止订单 API 权限回归。",
    "MockMvc + mock JwtService/OrderService。",
    ["  无 JWT─401  USER访问admin─403"])
add("order-service/src/test/java/com/example/orderservice/service/AuthServiceTest.java",
    "AuthService 单元测试（邮箱规范化、重复注册）。", "无 Spring 上下文快速验证。",
    "Mock UserRepository；BCrypt 真实编码一例。",
    ["  register ──► hash + save"])
add("order-service/src/test/java/com/example/orderservice/service/OrderServiceTest.java",
    "OrderService.createOrder 单元测试。", "断言状态、金额、Outbox 行。",
    "Mock repos + ObjectMapper；ArgumentCaptor。",
    ["  createOrder ──► PENDING_INVENTORY + outbox"])
add("order-service/src/test/java/com/example/orderservice/service/PaymentResultServiceTest.java",
    "PaymentResultService 状态迁移与幂等忽略测试。", "支付 listener 领域逻辑安全网。",
    "Mock OrderRepository；构造 pending 订单断言 status。",
    ["  PAID / FAIL ──► status"])

# payment-service
add("payment-service/src/main/java/com/example/paymentservice/PaymentServiceApplication.java",
    "支付微服务 Spring Boot 启动类。", "无 HTTP，仅 Kafka 消费 inventory-reserved 并发布支付结果。",
    "标准 main 启动。",
    ["  Boot ──► Kafka in/out only"])
add("payment-service/src/main/java/com/example/paymentservice/config/KafkaConsumerErrorConfig.java",
    "inventory-reserved 消费者重试与 DLT。", "与 inventory 侧 order-created 错误处理对称。",
    "Recoverer → inventory-reserved.DLT。",
    ["  fail ──► retry ──► .DLT"])
add("payment-service/src/main/java/com/example/paymentservice/config/KafkaTopicConfig.java",
    "支付服务 Kafka Topic 声明。", "含 inventory-reserved 与 DLT、payment-*。",
    "@Bean NewTopic。",
    ["  启动 ──► 创建 topics"])
add("payment-service/src/main/java/com/example/paymentservice/entity/Payment.java",
    "payment_db 中的支付记录 JPA 实体。", "每笔订单扣款尝试一条记录。",
    "UUID id、orderId、amount、PaymentStatus、时间戳。",
    ["  Payment 行 / 订单"])
add("payment-service/src/main/java/com/example/paymentservice/entity/PaymentStatus.java",
    "支付状态 PROCESSING / COMPLETED / FAILED。", "Demo 立即根据金额规则设终态。",
    "字符串枚举列。",
    ["  金额规则 ──► COMPLETED|FAILED"])
add("payment-service/src/main/java/com/example/paymentservice/entity/ProcessedEvent.java",
    "以 InventoryReserved 的 eventId 幂等。", "防止重复扣款。",
    "eventId PK + processedAt。",
    ["  dup eventId ──skip"])
add("payment-service/src/main/java/com/example/paymentservice/event/InventoryReservedEvent.java",
    "inventory-reserved 入站 DTO。", "支付只在预占成功后启动（不消费 order-created）。",
    "Java record → PaymentService.processInventoryReserved。",
    ["  inventory-reserved ──► pay"])
add("payment-service/src/main/java/com/example/paymentservice/event/PaymentCompletedEvent.java",
    "支付成功出站 Kafka DTO。", "广播给 order、inventory、notification。",
    "PaymentEventPublisher.publishCompleted。",
    ["  pay OK ──► payment-completed"])
add("payment-service/src/main/java/com/example/paymentservice/event/PaymentFailedEvent.java",
    "支付失败出站 Kafka DTO。", "触发订单失败与库存释放。",
    "PaymentEventPublisher.publishFailed。",
    ["  pay FAIL ──► payment-failed"])
add("payment-service/src/main/java/com/example/paymentservice/messaging/InventoryReservedListener.java",
    "订阅 inventory-reserved 的监听器。", "Saga 支付步入口。",
    "PaymentService.processInventoryReserved；KAFKA_FAIL 模拟失败。",
    ["  inventory-reserved ──► PaymentService"])
add("payment-service/src/main/java/com/example/paymentservice/messaging/KafkaTopics.java",
    "支付服务 Topic 常量（含 DLT 名）。", "Listener 与 ErrorHandler 共用。",
    "静态 final 字符串。",
    ["  topic 名字"])
add("payment-service/src/main/java/com/example/paymentservice/messaging/PaymentEventPublisher.java",
    "发布 payment-completed / payment-failed。", "多 consumer group 并行订阅。",
    "KafkaTemplate.send(orderId key) + 日志。",
    ["  PaymentService ──► payment-*"])
add("payment-service/src/main/java/com/example/paymentservice/repository/PaymentRepository.java",
    "Payment 实体 JPA Repository。", "持久化扣款记录。",
    "JpaRepository<Payment, UUID>。",
    ["  save Payment"])
add("payment-service/src/main/java/com/example/paymentservice/repository/ProcessedEventRepository.java",
    "processed_events JPA。", "existsById 幂等。",
    "JpaRepository<ProcessedEvent, UUID>。",
    ["  eventId 账本"])
add("payment-service/src/main/java/com/example/paymentservice/service/PaymentService.java",
    "处理 InventoryReserved：幂等、落 Payment、按 demo 规则发结果。", "金额 ≤10000 成功，否则失败。",
    "@Transactional processInventoryReserved → save + publishCompleted/Failed。",
    ["  amount≤10000 ──► COMPLETED", "  else ──► FAILED"])
add("payment-service/src/test/java/com/example/paymentservice/PaymentKafkaIntegrationTest.java",
    "Testcontainers Kafka+Postgres 端到端支付流测试。", "验证消费、落库、发布 payment-completed。",
    "SpringBootTest + KafkaTemplate 发消息 + poll 断言。",
    ["  TC Kafka ──► listener ──► DB ──► event"])
add("payment-service/src/test/java/com/example/paymentservice/messaging/InventoryReservedListenerTest.java",
    "InventoryReservedListener 委托与 KAFKA_FAIL 测试。", "文档化故障注入钩子。",
    "Mock PaymentService；KAFKA_FAIL 应抛错且不 process。",
    ["  KAFKA_FAIL ──► exception"])
add("payment-service/src/test/java/com/example/paymentservice/messaging/PaymentEventPublisherTest.java",
    "PaymentEventPublisher 单元测试。", "publishCompleted 使用 orderId 作 key。",
    "Mock KafkaTemplate verify。",
    ["  publishCompleted ──► send(key)"])
add("payment-service/src/test/java/com/example/paymentservice/service/PaymentServiceTest.java",
    "PaymentService 边界金额与幂等单元测试。", "10000.00 OK / 10000.01 FAIL 等。",
    "Mock repos/publisher + captors。",
    ["  10000.00 OK │ 10000.01 FAIL"])

if __name__ == "__main__":
    OUT.write_text(json.dumps(META, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(META)} entries to {OUT}")
