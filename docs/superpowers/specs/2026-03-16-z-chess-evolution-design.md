# Z-Chess 演进规划 v1.1.0 - v1.3.0 设计文档

**日期**: 2026-03-16  
**版本**: 1.0  
**状态**: Draft  
**作者**: william.d.zk

---

## 1. 概述

### 1.1 项目背景

Z-Chess 是一个分布式 IoT 消息中间件平台，采用 Chess 棋子的命名规则，按照功能模块的远近关系进行分层设计。系统基于 Java 17 + Spring Boot 3.5.9 构建，提供高并发、高可用的设备接入和消息处理能力。

### 1.2 演进目标

本次演进聚焦于两个核心方向：

**A 方向 - 完善 IoT 中间件能力**：
1. MQTT 5.0 完整支持
2. 集群能力完善
3. 可观测性建设

**D 方向 - 安全与合规**：
1. 后量子加密（NTRU）完整落地
2. 数据加密体系建设
3. 审计合规能力

### 1.3 约束条件

- **项目性质**：个人业余项目
- **发布策略**：大版本发布
- **兼容性要求**：保持向后兼容，不引入 breaking changes
- **范围调整**：认证授权体系托管给外部 SSO 项目

---

## 2. 架构设计

### 2.1 整体架构

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
│              ┌─────────────────────────────────────┐            │
│              │  MQTT 5.0 Broker (增强)              │            │
│              │  - 共享订阅                          │            │
│              │  - 消息过期                          │            │
│              │  - Payload 格式指示                  │            │
│              │  - 用户属性                          │            │
│              │  - 订阅标识符                        │            │
│              └─────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────────┤
│                         集群层 (Cluster)                         │
│    Z-Knight [RAFT 一致性集群管理 + Spring Boot REST 服务]        │
│              ┌─────────────────────────────────────┐            │
│              │  集群增强                            │            │
│              │  - 动态成员变更                      │            │
│              │  - 集群监控                          │            │
│              │  - 故障诊断                          │            │
│              └─────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────────┤
│                         协议层 (Protocol)                        │
│         Z-Bishop [MQTT v5.0/v3.1.1, Websocket, ZChat]          │
│              ┌─────────────────────────────────────┐            │
│              │  安全增强                            │            │
│              │  - NTRU 后量子加密                    │            │
│              │  - ZLS 协议增强                      │            │
│              └─────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────────┤
│                         存储层 (Storage)                         │
│         Z-Rook [JPA/PostgreSQL + EhCache 缓存层]                │
│              ┌─────────────────────────────────────┐            │
│              │  数据加密                            │            │
│              │  - 字段级加密                        │            │
│              │  - KMS 接口                          │            │
│              │  - 敏感数据脱敏                      │            │
│              └─────────────────────────────────────┘            │
├─────────────────────────────────────────────────────────────────┤
│                         基础层 (Foundation)                      │
│    Z-King [事件总线，时间轮，ZUID, 加密工具，CRC 计算]           │
│              ┌─────────────────────────────────────┐            │
│              │  可观测性                            │            │
│              │  - Prometheus 指标                   │            │
│              │  - OpenTelemetry 追踪                │            │
│              │  - 结构化日志                        │            │
│              └─────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系

```
Z-King (增强：可观测性、NTRU 加密)
    ↓
Z-Queen (增强：安全传输)
    ↓
Z-Bishop (增强：MQTT 5.0、ZLS 协议)
    ↓
Z-Knight (增强：集群管理)
    ↓
Z-Rook (增强：数据加密)
    ↓
Z-Pawn (增强：MQTT 5.0 Broker)
    ↓
Z-Player (增强：审计 API)
    ↓
Z-Arena (增强：监控端点)
```

---

## 3. 详细设计

### 3.1 MQTT 5.0 完整支持

#### 3.1.1 现状分析

当前 Z-Bishop 已支持 MQTT v3.1.1 和 v5.0 的基础功能，但尚有大量协议规范的 broker 行为未完全支持。

#### 3.1.2 需要实现的功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 共享订阅 | 多个消费者共享同一订阅，负载均衡消费消息 | P0 |
| 消息过期 | 消息 TTL 控制，过期自动丢弃 | P0 |
| Payload 格式指示 | 指示 payload 是 JSON、XML 还是二进制 | P1 |
| 用户属性 | 消息级别的键值对元数据 | P1 |
| 订阅标识符 | 为订阅分配标识符，用于消息路由追踪 | P1 |
| 请求/响应模式 | 内置请求响应相关属性 | P2 |
| 订阅选项 | 保留消息推送、通知发送等选项 | P1 |
| 服务器断开原因码 | 详细的断开原因说明 | P1 |
| 消息大小限制协商 | 客户端和服务器协商最大消息大小 | P2 |

#### 3.1.3 实现方案

**共享订阅**：
```java
// 共享订阅主题格式：$share/{shareName}/{topic}
// 示例：$share/consumer-group1/sensors/temperature

public class SharedSubscriptionManager {
    // 按 shareName 分组管理订阅者
    private final Map<String, List<MqttSession>> _sharedGroups = new ConcurrentHashMap<>();
    
    // 轮询或随机选择消费者
    public MqttSession selectConsumer(String shareName);
}
```

**消息过期**：
```java
public class MqttMessage {
    private long _expiryInterval;  // 消息过期时间（秒）
    private long _publishTime;     // 发布时间戳
    
    public boolean isExpired() {
        return _expiryInterval > 0 && 
               System.currentTimeMillis() - _publishTime > _expiryInterval * 1000;
    }
}
```

**Payload 格式与用户属性**：
```java
public class MqttV5Properties {
    private PayloadFormatIndicator _payloadFormat; // 0=二进制，1=UTF-8
    private long _messageExpiryInterval;
    private String _contentType;
    private String _responseTopic;
    private byte[] _correlationData;
    private Map<String, String> _userProperties;
}
```

#### 3.1.4 测试策略

- 使用 MQTT.org 官方兼容性测试套件
- 编写集成测试覆盖所有新增功能
- 性能测试确保共享订阅的负载均衡效果

---

### 3.2 集群能力完善

#### 3.2.1 现状分析

当前 Z-Knight 已集成 RAFT 一致性算法，但集群管理职能尚在开发中。

#### 3.2.2 需要实现的功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 动态成员变更 | 运行时添加/移除集群节点 | P0 |
| 集群监控 | 节点状态、选举状态、日志复制延迟 | P0 |
| 故障诊断 | 自动检测并报告集群问题 | P1 |
| Leader 选举优化 | 减少选举时间，目标 < 500ms | P1 |
| 集群配置管理 | 统一的集群配置存储和同步 | P1 |

#### 3.2.3 实现方案

**动态成员变更**：
```java
public interface ClusterManagementService {
    /**
     * 添加集群节点
     */
    CompletableFuture<Boolean> addNode(ClusterNode node);
    
    /**
     * 移除集群节点
     */
    CompletableFuture<Boolean> removeNode(String nodeId);
    
    /**
     * 获取集群成员列表
     */
    List<ClusterNode> getMembers();
    
    /**
     * 获取集群状态
     */
    ClusterStatus getStatus();
}
```

**集群监控指标**：
```java
public class ClusterMetrics {
    private int _clusterSize;
    private int _quorumSize;
    private String _leaderId;
    private long _lastElectionTime;
    private long _commitIndex;
    private Map<String, NodeStatus> _nodeStatusMap;
    private Map<String, Long> _replicationLag; // 每个节点的日志复制延迟
}
```

#### 3.2.4 故障转移优化

- 调整 RAFT 选举超时时间（150ms-300ms）
- 实现预投票（Pre-Vote）扩展，防止网络分区导致的无效选举
- 日志复制使用批量发送和流水线技术

---

### 3.3 可观测性

#### 3.3.1 监控指标（Prometheus）

**连接层指标**：
```
zchess_connections_active_total      # 当前活跃连接数
zchess_connections_total             # 累计连接总数
zchess_connections_failed_total      # 连接失败次数
zchess_connection_duration_seconds   # 连接时长分布
```

**MQTT 层指标**：
```
zchess_mqtt_messages_published_total # 发布消息总数
zchess_mqtt_messages_received_total  # 接收消息总数
zchess_mqtt_messages_dropped_total   # 丢弃消息数
zchess_mqtt_subscriptions_total      # 订阅总数
zchess_mqtt_shared_subscriptions     # 共享订阅数
```

**集群层指标**：
```
zchess_raft_leader_changes_total     # Leader 变更次数
zchess_raft_commit_index             # 当前提交索引
zchess_raft_replication_lag_seconds  # 复制延迟
zchess_raft_election_duration_seconds # 选举耗时
```

**系统层指标**：
```
zchess_jvm_memory_used_bytes         # JVM 内存使用
zchess_jvm_gc_pause_seconds          # GC 暂停时间
zchess_thread_pool_active_threads    # 线程池活跃线程数
```

#### 3.3.2 分布式追踪（OpenTelemetry）

**Span 定义**：
```java
@WithSpan("mqtt.message.publish")
public void publish(MqttMessage message) {
    Span span = Span.current();
    span.setAttribute("mqtt.topic", message.getTopic());
    span.setAttribute("mqtt.qos", message.getQos());
    span.setAttribute("mqtt.message.id", message.getMessageId());
    // ... 处理逻辑
}
```

**Trace 传播**：
- MQTT 消息通过用户属性传播 traceparent/tracestate
- 集群间通信携带追踪上下文
- 数据库查询自动注入 span

#### 3.3.3 结构化日志

```java
// 使用 SLF4J 结构化日志
logger.info("mqtt.message.published",
    kv("topic", message.getTopic()),
    kv("qos", message.getQos()),
    kv("clientId", session.getClientId()),
    kv("messageId", message.getMessageId()));
```

**日志字段规范**：
- `trace_id`: 追踪 ID
- `span_id`: Span ID
- `level`: 日志级别
- `timestamp`: 时间戳
- `service`: 服务名称
- `component`: 组件名称
- `event`: 事件类型

---

### 3.4 后量子加密（NTRU）

#### 3.4.1 现状分析

Z-King 已包含 NTRU 加密算法组件，ZLS 协议使用 NTRU(RC4) 模式，但需注意秘钥强度。

#### 3.4.2 需要实现的功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| NTRU 密钥强度提升 | 使用 NTRU-256 或更高强度 | P0 |
| ZLS 协议增强 | 完整集成 NTRU 到 ZLS 协议 | P0 |
| 密钥交换优化 | 改进密钥协商过程 | P1 |
| 性能基准测试 | 评估 NTRU 对延迟的影响 | P1 |

#### 3.4.3 实现方案

**NTRU 密钥管理**：
```java
public class NtruCryptoService {
    // 密钥对生成
    public NtruKeyPair generateKeyPair(NtruParameterSet params);
    
    // 加密
    public byte[] encrypt(PublicKey publicKey, byte[] plaintext);
    
    // 解密
    public byte[] decrypt(PrivateKey privateKey, byte[] ciphertext);
    
    // 密钥封装
    public EncapsulatedKey encapsulate(PublicKey publicKey);
    
    // 密钥解封装
    public byte[] decapsulate(PrivateKey privateKey, EncapsulatedKey encapsulatedKey);
}
```

**ZLS 协议增强**：
```
ZLS 握手流程（增强版）：
1. Client Hello (支持的后量子算法列表)
2. Server Hello (选定的后量子算法 + 服务器证书)
3. Server Key Exchange (NTRU 公钥)
4. Client Key Exchange (NTRU 封装的预主密钥)
5. 双方派生会话密钥
6. 切换到加密通信
```

#### 3.4.4 性能优化

- 提供 NTRU 和传统 ECC 的混合模式
- 会话复用减少密钥交换开销
- 异步加密操作避免阻塞 IO 线程

---

### 3.5 数据加密

#### 3.5.1 需要实现的功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 字段级加密 | 敏感字段自动加密存储 | P0 |
| KMS 接口抽象 | 可对接不同密钥管理服务 | P0 |
| 敏感数据脱敏 | 日志和 API 响应中的脱敏 | P1 |
| 加密审计日志 | 记录加密密钥使用情况 | P1 |

#### 3.5.2 实现方案

**字段级加密**：
```java
@EncryptColumn(algorithm = "AES-256-GCM", keyId = "device-token-key")
private String deviceToken;

@EncryptColumn(algorithm = "NTRU-256", keyId = "ntru-master-key")
private String sensitiveConfig;
```

**KMS 接口**：
```java
public interface KeyManagementService {
    /**
     * 获取数据密钥
     */
    DataKey getDataKey(String keyId);
    
    /**
     * 加密数据密钥（使用主密钥）
     */
    byte[] encryptDataKey(String keyId, byte[] plaintextKey);
    
    /**
     * 解密数据密钥
     */
    byte[] decryptDataKey(String keyId, byte[] encryptedKey);
    
    /**
     * 密钥轮换
     */
    void rotateKey(String keyId);
}
```

**本地 KMS 实现**：
```java
public class LocalKmsService implements KeyManagementService {
    private final Map<String, MasterKey> _masterKeys = new ConcurrentHashMap<>();
    private final SecureRandom _secureRandom = new SecureRandom();
    
    @Override
    public DataKey getDataKey(String keyId) {
        MasterKey masterKey = _masterKeys.get(keyId);
        byte[] plaintextKey = new byte[32]; // 256-bit
        _secureRandom.nextBytes(plaintextKey);
        
        byte[] encryptedKey = encryptWithMasterKey(masterKey, plaintextKey);
        
        return new DataKey(plaintextKey, encryptedKey, keyId);
    }
}
```

#### 3.5.3 数据库支持

- PostgreSQL pgcrypto 扩展集成
- 应用层加密（推荐，更灵活）
- 加密字段索引支持（使用确定性加密或 HMAC）

---

### 3.6 审计合规

#### 3.6.1 需要实现的功能

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 操作审计日志 | 记录所有敏感操作 | P0 |
| 安全事件检测 | 异常行为检测和告警 | P1 |
| 合规报告 | 导出审计日志用于合规检查 | P2 |

#### 3.6.2 审计事件模型

```java
public class AuditEvent {
    private String eventId;
    private Instant timestamp;
    private String eventType;      // LOGIN, LOGOUT, CONFIG_CHANGE, DATA_ACCESS
    private String principal;      // 操作者 ID
    private String resource;       // 被操作的资源
    private String action;         // CREATE, READ, UPDATE, DELETE
    private String outcome;        // SUCCESS, FAILURE
    private String sourceIp;       // 来源 IP
    private Map<String, String> context; // 额外上下文
}
```

#### 3.6.3 审计日志存储

```java
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_principal", columnList = "principal"),
    @Index(name = "idx_audit_event_type", columnList = "event_type")
})
public class AuditEventEntity {
    @Id
    private String eventId;
    
    private Instant timestamp;
    private String eventType;
    private String principal;
    private String resource;
    private String action;
    private String outcome;
    private String sourceIp;
    
    @Column(columnDefinition = "jsonb")
    private Map<String, String> context;
}
```

#### 3.6.4 安全事件检测规则

```java
public class SecurityEventDetector {
    // 规则：短时间多次登录失败
    @Rule("login.failure.threshold")
    public boolean detectBruteForce(List<AuditEvent> events) {
        return events.stream()
            .filter(e -> "LOGIN".equals(e.getEventType()))
            .filter(e -> "FAILURE".equals(e.getOutcome()))
            .filter(e -> e.getTimestamp().isAfter(Instant.now().minusMinutes(5)))
            .count() > 5;
    }
    
    // 规则：异常时间访问
    @Rule("access.unusual.time")
    public boolean detectUnusualTime(AuditEvent event) {
        LocalTime accessTime = LocalTime.ofInstant(event.getTimestamp(), ZoneId.systemDefault());
        return accessTime.isBefore(LocalTime.of(6, 0)) || 
               accessTime.isAfter(LocalTime.of(22, 0));
    }
}
```

---

## 4. 发布计划

### 4.1 v1.1.0 - IoT 能力增强

**预计周期**: 2-3 个月

**包含内容**:
- MQTT 5.0 完整支持（共享订阅、消息过期、用户属性等）
- 集群能力完善（动态成员变更、集群监控、故障诊断）

**交付价值**:
- 协议完整性达到 MQTT 5.0 规范要求
- 集群高可用能力，故障转移 < 500ms

### 4.2 v1.2.0 - 安全增强

**预计周期**: 2-3 个月

**包含内容**:
- 后量子加密（NTRU 完整落地、ZLS 协议增强）
- 数据加密（字段级加密、KMS 接口）

**交付价值**:
- 后量子安全能力，差异化竞争优势
- 数据加密满足合规要求

### 4.3 v1.3.0 - 可观测性与审计

**预计周期**: 1-2 个月

**包含内容**:
- 可观测性（Prometheus 指标、OpenTelemetry 追踪、结构化日志）
- 审计合规（审计日志、安全事件检测）

**交付价值**:
- 运维友好，完整的监控和诊断能力
- 满足等保 2.0 等合规要求

---

## 5. 测试策略

### 5.1 单元测试

- 所有新增功能必须有单元测试覆盖
- 代码覆盖率要求：行覆盖 ≥ 80%，分支覆盖 ≥ 70%

### 5.2 集成测试

- MQTT 5.0 兼容性测试（使用官方测试套件）
- 集群故障转移测试
- 加密功能正确性测试

### 5.3 性能测试

| 测试项 | 目标指标 |
|--------|----------|
| MQTT 消息吞吐量 | ≥ 10 万消息/秒 |
| 共享订阅延迟 | P99 < 10ms |
| 集群故障转移时间 | < 500ms |
| NTRU 密钥交换延迟 | P99 < 100ms |
| 加密字段查询性能 | 下降 < 20% |

### 5.4 安全测试

- NTRU 实现安全性审计
- 加密密钥管理安全性验证
- 渗透测试

---

## 6. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 个人项目时间不足 | 开发周期延长 | 分阶段发布，每阶段独立交付价值 |
| MQTT 5.0 测试复杂 | 兼容性风险 | 早期引入官方测试套件，持续测试 |
| NTRU 性能开销 | 延迟增加 | 提供混合模式，性能基准测试优化 |
| 数据加密影响查询 | 性能下降 | 仅加密敏感字段，提供索引友好方案 |
| 集群脑裂 | 数据不一致 | 实现预投票扩展，优化网络分区处理 |

---

## 7. 成功标准

| 方向 | 指标 | 目标值 |
|------|------|--------|
| MQTT 5.0 | 官方兼容性测试通过率 | 100% |
| 集群能力 | 故障转移时间 | < 500ms |
| 集群能力 | 支持最大节点数 | ≥ 5 节点 |
| 可观测性 | Prometheus 指标覆盖率 | ≥ 90% |
| 后量子加密 | NTRU 密钥强度 | ≥ 256-bit |
| 后量子加密 | 密钥交换延迟 | P99 < 100ms |
| 数据加密 | 支持加密算法 | AES-256-GCM, NTRU-256 |
| 审计合规 | 审计事件完整性 | 100% 敏感操作可追溯 |

---

## 8. 附录

### 8.1 参考文档

- MQTT 5.0 规范：https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html
- RAFT 论文：https://raft.github.io/raft.pdf
- NTRU 规范：https://ntru.org/
- OpenTelemetry: https://opentelemetry.io/
- Prometheus: https://prometheus.io/

### 8.2 术语表

| 术语 | 解释 |
|------|------|
| NTRU | 一种后量子公钥加密算法 |
| ZLS | Z-Chess 自定义轻量级安全传输协议 |
| KMS | 密钥管理服务（Key Management Service） |
| QoS | MQTT 服务质量等级（Quality of Service） |

---

## 9. 审批记录

| 角色 | 姓名 | 日期 | 状态 | 意见 |
|------|------|------|------|------|
| 作者 | william.d.zk | 2026-03-16 | ✅ | - |
| 技术审查 | - | - | ⏳ | - |
| 用户审批 | - | - | ⏳ | - |
