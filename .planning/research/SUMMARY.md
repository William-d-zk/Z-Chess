# Research Summary: 边缘算力调度与即时通讯平台

## Key Findings

### Stack

**核心技术栈：**
- 基于Z-Chess现有架构扩展（MQTT v5.0、RAFT集群、AIO网络通信）
- 自研调度器（契合边缘设备特殊约束）
- Go-based Edge Agent（轻量、跨平台）
- 自研WebSocket IM服务（复用Z-Queen AIO能力）
- PostgreSQL + TimescaleDB存储

**不推荐：** Kafka（延迟高）、Redis Cluster（部署复杂）、gRPC（边缘NAT穿透问题）

### Table Stakes

**分布式计算：**
- 任务分发/认领机制
- 结果汇总与失败重试
- 全链路状态跟踪

**IM基础：**
- 用户认证（内部SSO/LDAP）
- 群组聊天 + 消息推送
- 在线/离线消息管理

### Differentiators

- **智能调度**：分组分发、组内认领、算力感知调度
- **设备联动**：IM直接操作设备、状态订阅、数据推送
- **工作流编排**：任务依赖关系编排

### Watch Out For

| Pitfall | Warning Signs | Prevention |
|---------|---------------|------------|
| 任务碎片化 | 调度延迟 > 执行时间 | 任务最小粒度10ms+ |
| 结果汇总死锁 | PARTIAL_COMPLETE超时 | 超时 + 冗余算力 |
| 消息洪泛 | 队列积压、延迟飙升 | 消息合并、限速 |
| 40ms延迟无法保证 | 控制指令卡顿 | 专用快速通道 + 边缘预置 |
| 网络分区脑裂 | 多Leader、数据重复 | RAFT majority + 幂等设计 |

### Build Order

1. **Phase 1**: 核心调度（分发/认领/结果汇总）
2. **Phase 2**: 边缘Agent（注册/执行/监控）
3. **Phase 3**: IM基础（WebSocket/群组/认证）
4. **Phase 4**: 设备联动（IM操作/订阅/推送）
5. **Phase 5**: 高级调度（分组/算力感知/重试优化）

## Decisions Pending

- [ ] 边缘Agent运行时选型（Go vs WasmEdge）
- [ ] IM协议选型（自研WebSocket vs Matrix）
- [ ] 存储架构（PostgreSQL + TimescaleDB vs Cassandra）
- [ ] 任务最小粒度阈值

## Files

- `.planning/research/STACK.md`
- `.planning/research/FEATURES.md`
- `.planning/research/ARCHITECTURE.md`
- `.planning/research/PITFALLS.md`

---
*Synthesized by: sonnet*
*Date: 2026-03-18*
