# Z-Chess 项目文档

本文档由 code-documenter skill 自动生成，基于对 Z-Chess 代码库的静态分析。

## 文档导航

### 📋 [代码库分析摘要](./SUMMARY.md)
- 项目概述统计
- 模块结构详解
- 类分布统计

### 🏗️ [架构文档](./ARCHITECTURE.md)
- 系统架构概览
- 模块划分说明
- 包依赖关系图
- 核心组件交互流程

### 📖 [API 参考](./API_REFERENCE.md)
- 按字母顺序排列的类索引
- 包含所有公共类、接口、枚举
- 标注文件位置和行号

### 🚀 [入口点文档](./ENTRY_POINTS.md)
- 应用启动入口
- 核心服务入口
- 业务服务入口
- 配置入口
- 测试入口

## 项目统计

| 指标 | 数量 |
|------|------|
| Java 文件数 | 729 |
| 总代码行数 | 98,462 |
| 包数量 | 216 |
| 类数量 | 624 |
| 接口数量 | 214 |
| 方法数量 | 1,549 |
| 字段数量 | 8,856 |

## 模块概览

| 模块 | 文件数 | 主要职责 |
|------|--------|----------|
| Z-Bishop | 170 | 消息处理组件 (MQTT/Modbus/WebSocket) |
| Z-Queen | 126 | 核心 IO 处理组件 (AIO Socket/集群通信) |
| Z-King | 109 | 事件处理组件 (Disruptor/TimeWheel/加密) |
| Z-Audience | 94 | 测试与观察者组件 |
| Z-Knight | 80 | Raft 集群组件 |
| Z-Player | 52 | Open API 服务 |
| Z-Pawn | 47 | 边缘服务设备接入 |
| Z-Rook | 24 | 数据存储组件 (JPA/Cache) |
| Z-Board | 16 | 注解处理器 |
| Z-Arena | 3 | 网关应用 |

## 核心架构

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Z-Pawn    │ 边缘服务 - 设备接入
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Z-King    │ 事件处理 - Disruptor 总线
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Z-Knight   │ 集群同步 - Raft 协议
└──────┬──────┘
       │
       ▼
┌─────────────┐
│   Z-Rook    │ 持久化 - PostgreSQL/JPA
└─────────────┘
```

## 技术栈

- **基础框架**: Spring Boot, JPA, Ehcache
- **通讯协议**: MQTT 3.1.1/5.0, Modbus TCP/RTU, WebSocket, ZChat
- **集群**: 自研 Raft 实现
- **事件处理**: Disruptor, TimeWheel
- **数据库**: PostgreSQL
- **加密**: NTRU, AES-GCM, SSL/TLS

## 文档生成说明

本文档使用 [code-documenter skill](https://github.com/anomalyco/opencode/tree/main/skills/code-documenter) 生成：

```bash
# 重新生成文档
python3 scripts/simple_analyzer.py <source_directory>
```

文档输出目录：`docs_output/`

---
**生成时间**: 2026-03-19  
**代码版本**: Git HEAD  
**注意**: 本文档由 AI 基于静态分析生成，仅供参考，请以实际代码为准。
