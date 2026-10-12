# 文档索引

本目录是 **Order Payment Demo** 的详细说明；根目录 [README.md](../README.md) 只保留快速上手与链接。

## 建议阅读顺序

| 顺序 | 文档 | 适合谁 |
| --- | --- | --- |
| 1 | [architecture.md](architecture.md) | 想先搞清微服务边界、Saga 时序、订单状态与 Kafka 主题 |
| 2 | [code-guide.md](code-guide.md) | 打开 IDE 跟链路读 Java；配合各服务 `src/main` 内中文注释 |
| 3 | [runbook.md](runbook.md) | 本地/Docker 启动、查 Kafka/Outbox、DLT 演示、跑测试 |

## 文档分工（避免重复）

| 主题 | 写在哪里 |
| --- | --- |
| 先占库存再支付、序列图、状态图、分布式事务含义 | **architecture.md**（唯一出处） |
| 读哪些类、目录结构、概念 ↔ 代码 | **code-guide.md** |
| 命令、端口、Compose、console consumer、SQL | **runbook.md** |
| 一句话启动、端口表 | **../README.md** |

## 语言说明

- 根 **README.md**：英文，便于仓库首屏与国际协作。
- **docs/** 下三篇：中文（命令与代码路径保持原文）。
