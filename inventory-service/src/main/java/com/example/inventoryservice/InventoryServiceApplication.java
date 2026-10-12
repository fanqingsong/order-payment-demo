/*
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  源码导读 · InventoryServiceApplication.java                           ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * 【是什么】库存微服务的 Spring Boot 启动入口。
 * 【为什么】独立进程消费 order-created、更新 inventory_db，承担 Saga「预占库存」步骤。
 * 【怎么做】main 启动 Spring 容器，注册 Kafka Listener、InventoryService、JPA 仓库等 Bean。
 *
 * 示意图（inventory-service · 8083 · inventory_db）：
 *   ┌─────────────┐
 *   │ Spring Boot │──► Kafka + PostgreSQL(inventory_db)
 *   └─────────────┘
 *
 * 详见仓库 README.md · docs/code-guide.md
 * (FILE_GUIDE_HEADER)
 */
package com.example.inventoryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 库存服务：消费 {@code order-created} 预占库存并发布 {@code inventory-reserved}；
 * 支付成功/失败后确认扣减或释放预占。
 */
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
