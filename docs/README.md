# 文档索引

本目录是 **Order Payment Demo** 的补充说明；**架构与 Saga** 在根目录 [README.md](../README.md#架构与-saga)。

## 建议阅读顺序

| 顺序 | 文档 | 适合谁 |
| --- | --- | --- |
| 1 | [../README.md](../README.md) + 各服务 `../<service>/README.md` | 微服务边界、Saga 时序、订单状态与 Kafka 主题 |
| 2 | [code-guide.md](code-guide.md) | 打开 IDE 跟链路读 Java；配合各服务 `src/main` 内中文注释 |
| 3 | [runbook.md](runbook.md) | 本地/Docker 启动、查 Kafka/Outbox、DLT 演示、跑测试 |

## 文档分工（避免重复）

| 主题 | 写在哪里 |
| --- | --- |
| 各服务职责、端口、DB、Kafka 入出站、Outbox/幂等要点 | **各子项目 README**（如 `order-service/README.md`） |
| 跨服务序列图、状态图、Kafka 总表、Saga/一致性与演示规则汇总 | **../README.md** |
| 读哪些类、目录结构、概念 ↔ 代码 | **code-guide.md** |
| 命令、端口、Compose、console consumer、SQL | **runbook.md** |

## 语言说明

- 根 **README.md** 与 **docs/** 下文档均为中文（命令与代码路径保持原文）。
