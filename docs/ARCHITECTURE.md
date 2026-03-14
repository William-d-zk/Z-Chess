# Z-Chess 系统架构说明

## 1. 系统概述

Z-Chess 是一个分布式 IoT 消息中间件平台，采用 Chess 棋子的命名规则，按照功能模块的远近关系进行分层设计。系统基于 Java 17 + Spring Boot 3.5.9 构建，提供高并发、高可用的设备接入和消息处理能力。

## 2. 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                         接入层 (Gateway)                         │
│                     Z-Arena [网关/负载均衡]                       │
├─────────────────────────────────────────────────────────────────┤
│                         业务层 (Business)                        │
│                     Z-Player [Open API 业务]                    │
├─────────────────────────────────────────────────────────────────┤
│                         端点层 (Endpoint)                        │
│              Z-Pawn [MQTT/Websocket 设备接入服务]                │
├─────────────────────────────────────────────────────────────────┤
│                         集群层 (Cluster)                         │
│    Z-Knight [RAFT 一致性集群管理 + Spring Boot REST 服务]        │
├─────────────────────────────────────────────────────────────────┤
│                         协议层 (Protocol)                        │
│         Z-Bishop [MQTT v5.0/v3.1.1, Websocket, ZChat]          │
├─────────────────────────────────────────────────────────────────┤
│                         IO 层 (I/O Core)                         │
│        Z-Queen [AIO 异步网络通信核心，支持 TCP/TLS/ZLS]          │
├─────────────────────────────────────────────────────────────────┤
│                         存储层 (Storage)                         │
│         Z-Rook [JPA/PostgreSQL + EhCache 缓存层]                │
├─────────────────────────────────────────────────────────────────┤
│                         基础层 (Foundation)                      │
│    Z-King [事件总线, 时间轮, ZUID, 加密工具, CRC 计算]           │
├─────────────────────────────────────────────────────────────────┤
│                         元数据层 (Metadata)                      │
│              Z-Board [注解处理器, 序列化工厂]                     │
├─────────────────────────────────────────────────────────────────┤
│                         测试层 (Test)                            │
│                   Z-Audience [测试用例集合]                      │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 模块详细说明

### 3.1 Z-Board (基础元数据)
- **职责**: 编译期注解处理，序列化代码生成
- **核心功能**:
  - `@ISerialGenerator` 注解处理
  - 协议序列化工厂自动生成
  - 减少运行时反射，提升性能

### 3.2 Z-King (基础组件)
- **职责**: 提供全系统共享的基础设施
- **核心组件**:
  - **Disruptor 事件总线**: 高性能异步事件处理
  - **TimeWheel 时间轮**: 大容量定时任务调度（最小粒度1秒）
  - **ZUID**: 64位分布式唯一ID生成器
  - **ZProgress/ZResponse**: 异步进度追踪和响应包装
  - **CryptoUtil**: 加密工具（AES-256-GCM, SHA-256, SecureRandom）
  - **CrcUtil**: 高性能 CRC 计算（750万 ops/sec）
  - **ByteBuf**: 自定义缓冲区实现

### 3.3 Z-Queen (IO 核心)
- **职责**: 异步非阻塞网络通信
- **核心功能**:
  - AIO (Asynchronous I/O) Socket 实现
  - 支持 TCP、TLS 1.3、ZLS (Z-Chess 自定义安全协议)
  - ServerCore: 对外服务核心（Socket + 集群能力）
  - ClusterCore: 纯集群通信核心（用于 REST 微服务）
  - 链式过滤器架构 (FilterChain)
  - 会话管理 (AioSession)

### 3.4 Z-Bishop (协议处理)
- **职责**: 协议编解码和业务抽象
- **支持协议**:
  - **MQTT**: v3.1.1 / v5.0（完整协议实现）
  - **Websocket**: RFC 6455 完整支持
  - **ZChat**: 内部服务通信协议
  - **ZLS**: Z-Chess 轻量级安全传输协议
- **核心组件**:
  - 协议工厂 (ProtocolFactory)
  - 过滤器链 (FrameFilter, CommandFilter)
  - SSL/TLS 上下文管理 (ReloadableSSLContext, CertificateWatcher)

### 3.5 Z-Knight (集群管理)
- **职责**: RAFT 一致性算法实现和集群管理
- **核心功能**:
  - **RAFT 共识**: Leader 选举、日志复制、成员变更
  - **集群通信**: 基于 ZChat 协议的 P2P 通信
  - **一致性服务**: 分布式配置管理
  - **Spring Boot 集成**: REST API + JPA

### 3.6 Z-Rook (数据存储)
- **职责**: 数据持久化和缓存管理
- **核心功能**:
  - **JPA 支持**: PostgreSQL 默认数据库
  - **EhCache**: 多级缓存架构
  - **BaseRepository**: 通用 CRUD 抽象
  - **健康检查**: DatabaseHealthIndicator

### 3.7 Z-Pawn (设备端点)
- **职责**: IoT 设备接入服务
- **核心功能**:
  - **MQTT Broker**: 设备消息路由
  - **设备管理**: 设备注册、认证、状态维护
  - **消息存储**: 离线消息持久化
  - **双数据库架构**: 
    - Central DB: 中心设备数据 (PostgreSQL)
    - Local DB: 本地 SQLite 状态存储

### 3.8 Z-Player (业务 API)
- **职责**: 对外业务接口层
- **核心功能**:
  - RESTful API 控制器
  - 设备管理接口
  - 消息推送接口
  - 一致性查询接口

### 3.9 Z-Arena (网关)
- **职责**: 统一入口和负载均衡
- **核心功能**:
  - 网关路由
  - 缓存查询接口
  - 服务聚合

### 3.10 Z-Audience (测试)
- **职责**: 全模块测试用例集合
- **覆盖范围**:
  - 单元测试
  - 集成测试
  - SSL/TLS 压力测试
  - MQTT 协议测试

## 4. 数据流架构

```
设备 → MQTT/Websocket → Z-Pawn → RAFT 集群 (Z-Knight)
                              ↓
                         Z-Player (API)
                              ↓
                         Z-Arena (网关)
                              ↓
                         外部系统/管理端
```

## 5. 安全架构

### 5.1 传输层安全
- **TLS 1.3**: 默认加密传输
- **证书热重载**: 支持运行时证书更新
- **SSL Provider 优先级**: WolfSSL → OpenSSL → JDK

### 5.2 应用层安全
- **AES-256-GCM**: 对称加密
- **SHA-256**: 摘要算法
- **定时安全**: SecureRandom 用于密钥生成
- **防时序攻击**: constantTimeEquals 实现

### 5.3 认证授权
- **MQTT 认证**: 设备 ID + 密码验证
- **ZLS 认证**: 基于 NTRU 的后量子加密

## 6. 高可用设计

### 6.1 集群架构
- **RAFT 共识**: 3/5 节点集群，自动 Leader 选举
- **数据复制**: 强一致性日志复制
- **故障转移**: Leader 故障自动切换（< 1秒）

### 6.2 无状态设计
- 服务端不保存设备会话状态
- 会话状态持久化到 PostgreSQL
- 支持水平扩展

### 6.3 DNS 自适应
- 基于 DNS SRV 记录的服务发现
- 支持云环境 IP 动态变更

## 7. 性能特性

| 组件 | 性能指标 |
|------|---------|
| CRC 计算 | 750万 ops/sec |
| Disruptor | 单线程 100万+ TPS |
| TLS 握手 | WolfSSL 优化 |
| 连接数 | 10万+ 并发连接 |

## 8. 技术栈

- **Java**: 17 (LTS)
- **Spring Boot**: 3.5.9
- **数据库**: PostgreSQL 42.7.8
- **缓存**: EhCache 3.11.1
- **消息**: Disruptor 4.0.0
- **JSON**: Jackson 2.18.6
- **构建**: Maven 3.9+

## 9. 模块依赖关系

```
Z-Board (编译期)
    ↓
Z-King → 所有模块依赖
    ↓
Z-Queen ← Z-King
    ↓
Z-Bishop ← Z-Queen
    ↓
Z-Knight ← Z-Bishop
    ↓
Z-Rook ← Z-Knight
Z-Pawn ← Z-Knight
    ↓
Z-Player ← Z-Pawn
    ↓
Z-Arena ← Z-Player
    ↓
Z-Audience (测试所有模块)
```
