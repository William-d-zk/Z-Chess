# Requirements: 边缘算力调度与即时通讯平台

**Defined:** 2026-03-18
**Core Value:** 让闲置的边缘算力被用起来 — 百万级工业边缘设备平时低负载运行，平台将计算任务拆分托管到边缘节点执行，结果汇总后通过IM触达员工

## v1 Requirements

### Distributed Computing (DIST)

- [ ] **DIST-01**: 分发型调度 — 中心节点主动将任务拆分并分发到边缘节点执行
- [ ] **DIST-02**: 认领型调度 — 边缘节点从任务池主动认领任务，减少中心负载
- [ ] **DIST-03**: 分组分发 — 按边缘节点分组，定向分发任务到指定组
- [ ] **DIST-04**: 组内认领 — 组内边缘节点竞争认领，实现负载均衡
- [ ] **DIST-05**: 结果汇总 — 集群协调者收集并合并所有子任务结果
- [ ] **DIST-06**: 失败重试 — 任务失败在边缘端重试，节点失败由冗余算力重试

### IM Services (IM)

- [ ] **IM-01**: 用户认证 — 员工身份验证（内部SSO/LDAP集成）
- [ ] **IM-02**: 群组聊天 — 群组对话、群管理（创建/解散/成员管理）
- [ ] **IM-03**: 消息推送 — 在线消息实时推送，离线消息持久化
- [ ] **IM-04**: 设备操作 — IM中可直接下发设备控制指令到边缘节点
- [ ] **IM-05**: 状态监控 — IM中可订阅设备状态，变更推送到对应会话
- [ ] **IM-06**: 数据订阅 — 聊天主题关联数据订阅，变换后自动推送到会话
- [ ] **IM-07**: 工作流提醒 — 任务状态变更、报表生成等事件通过IM提醒
- [ ] **IM-08**: 4/5G接入时SIM/eSIM绑定 — 设备SIM卡与边缘节点身份绑定

### Latency Requirements (LAT)

- [ ] **LAT-01**: 设备控制指令延迟 < 40ms（端到端，含网络传输）
- [ ] **LAT-02**: AI模型推理延迟 10-30秒（视模型复杂度）
- [ ] **LAT-03**: 数据分析报表延迟 12小时（批处理场景）

## v2 Requirements

Deferred to future release.

### Distributed Computing (DIST)

- **DIST-07**: 算力感知调度 — 根据节点CPU/内存/GPU动态分配任务
- **DIST-08**: 任务亲和性优化 — 数据局部性优化，减少传输开销
- **DIST-09**: 边缘自治 — 断网情况下边缘节点独立运行

### IM Services (IM)

- **IM-09**: 智能体协作 — IM中AI智能体参与讨论和任务协作
- **IM-10**: 多租户隔离 — 不同组织数据完全隔离

### System (SYS)

- **SYS-01**: 工作流编排 — 多个任务按依赖关系编排执行
- **SYS-02**: 任务优先级 — 紧急任务抢占调度
- **SYS-03**: 分层Broker架构 — 百万终端接入时的连接管理优化

## Out of Scope

| Feature | Reason |
|---------|--------|
| 公有云部署 | 专注内部工业场景 |
| 通用任务市场 | 专注工业边缘，非通用计算 |
| 视频/流媒体处理 | 带宽消耗大，非核心需求 |
| 移动端IM | 内部员工PC为主 |
| 第三方IM集成 | 自建控制力更强 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DIST-01 | TBD | Pending |
| DIST-02 | TBD | Pending |
| DIST-03 | TBD | Pending |
| DIST-04 | TBD | Pending |
| DIST-05 | TBD | Pending |
| DIST-06 | TBD | Pending |
| IM-01 | TBD | Pending |
| IM-02 | TBD | Pending |
| IM-03 | TBD | Pending |
| IM-04 | TBD | Pending |
| IM-05 | TBD | Pending |
| IM-06 | TBD | Pending |
| IM-07 | TBD | Pending |
| IM-08 | TBD | Pending |
| LAT-01 | TBD | Pending |
| LAT-02 | TBD | Pending |
| LAT-03 | TBD | Pending |

**Coverage:**
- v1 requirements: 17 total
- Mapped to phases: 0 ⚠️
- Unmapped: 17

---
*Requirements defined: 2026-03-18*
*Last updated: 2026-03-18 after initial definition*
