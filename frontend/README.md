# frontend

Angular 22 单页应用：注册/登录、JWT 调用 **order-service** 下单与查单。

| 项 | 值 |
| --- | --- |
| 端口（Compose / `ng serve`） | **4200** |
| 后端 | order-service **8080**（`proxy.conf.json` 开发代理） |

业务链路为事件驱动 Saga，前端只与订单 API 同步交互；库存、支付、通知由 Kafka 在服务端异步完成。详见 [README.md](../README.md#架构与-saga)。

## 在架构中的位置

```text
Browser → Angular (4200) --JWT/HTTP--> Order Service (8080)
```

用户创建订单后，订单状态由服务端随 `inventory-*`、`payment-*` 事件更新；刷新或列表接口可见 `PENDING_INVENTORY` → `PENDING_PAYMENT` → `PAID` 等（见 [order-service/README.md](../order-service/README.md)）。

## 演示规则（UI 侧可触发）

| 场景 | 用法 |
| --- | --- |
| 缺货 | 商品名 **`OUT_OF_STOCK`** |
| 支付失败 | 订单总金额 **> 10000** |
| Kafka 消费 DLT（库存） | 商品名 **`KAFKA_FAIL`**（库存消费失败，不会进入支付） |

## 开发

```bash
npm install
ng serve
# 浏览器 http://localhost:4200/
```

生产构建与容器内由 Compose/nginx 提供静态资源；全栈启动见 [docker/README.md](../docker/README.md)。

```bash
ng build
ng test -- --watch=false
```

Angular CLI 参考：[Angular CLI Overview](https://angular.dev/tools/cli)。
