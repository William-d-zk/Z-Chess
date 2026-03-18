# Roadmap: 边缘算力调度与即时通讯平台

**Created:** 2026-03-18
**Granularity:** Standard (5-8 phases)
**Mode:** YOLO (auto-approve)

## Phase Overview

| # | Phase | Goal | Requirements | Success Criteria |
|---|-------|------|--------------|------------------|
| 1 | 核心调度框架 | 实现基础任务调度能力，支持分发型和认领型调度 | DIST-01, DIST-02, DIST-05 | 任务可分发到边缘节点，边缘可认领任务，结果可汇总 |
| 2 | 高级调度与容错 | 实现分组分发、组内认领和失败重试机制 | DIST-03, DIST-04, DIST-06 | 支持按组调度，组内负载均衡，失败自动重试 |
| 3 | 边缘Agent | 开发边缘节点Agent，实现注册、执行、监控能力 | (支撑Phase 1-2) | Edge Agent可在边缘设备部署运行 |
| 4 | IM基础服务 | 构建IM核心能力：认证、群组、消息推送 | IM-01, IM-02, IM-03 | 用户可登录、创建群组、收发消息 |
| 5 | 设备联动 | 实现IM与设备操作、状态监控、数据订阅集成 | IM-04, IM-05, IM-06, IM-07, IM-08 | IM中可操作设备，订阅状态变更，推送数据到会话 |
| 6 | 延迟保障 | 优化关键路径延迟，满足40ms/10-30s/12h分级要求 | LAT-01, LAT-02, LAT-03 | 三级延迟要求均满足 |

## Phase Details

### Phase 1: 核心调度框架

**Goal:** 实现基础任务调度能力，支持分发型和认领型调度

**Requirements:**
- DIST-01: 分发型调度
- DIST-02: 认领型调度
- DIST-05: 结果汇总

**Success Criteria:**
1. 中心节点可创建任务并拆分
2. 任务可通过分发型模式分发到指定边缘节点
3. 边缘节点可主动认领任务
4. 子任务结果可被协调者汇总
5. 任务状态可跟踪（Pending → Running → Complete）

---

### Phase 2: 高级调度与容错

**Goal:** 实现分组分发、组内认领和失败重试机制

**Requirements:**
- DIST-03: 分组分发
- DIST-04: 组内认领
- DIST-06: 失败重试

**Success Criteria:**
1. 边缘节点可分组，任务可定向发送到指定组
2. 组内边缘节点可竞争认领任务
3. 任务失败后边缘端自动重试
4. 节点失败后冗余算力自动接管
5. 超时机制防止死锁

---

### Phase 3: 边缘Agent

**Goal:** 开发边缘节点Agent，实现注册、执行、监控能力

**Success Criteria:**
1. Edge Agent可在边缘设备上部署（Docker/二进制）
2. Agent可向调度器注册算力信息
3. Agent可接收/执行子任务
4. Agent可上报执行结果
5. Agent心跳监控正常

**Notes:**
- 此阶段是基础设施，支撑Phase 1-2的实际运行
- 可与Phase 1-2并行开发

---

### Phase 4: IM基础服务

**Goal:** 构建IM核心能力：认证、群组、消息推送

**Requirements:**
- IM-01: 用户认证
- IM-02: 群组聊天
- IM-03: 消息推送

**Success Criteria:**
1. 员工可通过内部SSO/LDAP登录
2. 用户可创建/解散群组、管理群成员
3. 消息实时推送，延迟 < 500ms
4. 离线消息可持久化，用户上线后收到
5. 群组消息顺序一致

---

### Phase 5: 设备联动

**Goal:** 实现IM与设备操作、状态监控、数据订阅集成

**Requirements:**
- IM-04: 设备操作
- IM-05: 状态监控
- IM-06: 数据订阅
- IM-07: 工作流提醒
- IM-08: 4/5G SIM/eSIM绑定

**Success Criteria:**
1. IM中可直接下发设备控制指令
2. 用户可订阅设备状态，变更推送到会话
3. 聊天主题可关联数据订阅，变换后推送
4. 任务状态变更、报表生成等事件通过IM提醒
5. 设备SIM卡与边缘节点身份绑定验证

---

### Phase 6: 延迟保障

**Goal:** 优化关键路径延迟，满足40ms/10-30s/12h分级要求

**Requirements:**
- LAT-01: 设备控制指令 < 40ms
- LAT-02: AI推理 10-30秒
- LAT-03: 报表 12小时

**Success Criteria:**
1. 设备控制指令端到端延迟 < 40ms（不含设备执行时间）
2. AI推理任务在边缘执行，延迟 10-30秒
3. 报表批处理任务 12小时内完成
4. 关键路径有监控告警

---

## Dependencies

```
Phase 1 (核心调度)
    ↓
Phase 2 (高级调度) ← Phase 3 (Edge Agent)
    ↓
Phase 4 (IM基础) ← Phase 3 (Edge Agent)
    ↓
Phase 5 (设备联动)
    ↓
Phase 6 (延迟保障)
```

## Coverage

- v1 requirements: 17 total
- Mapped to phases: 17 ✓
- Unmapped: 0

---
*Roadmap created: 2026-03-18*
*Last updated: 2026-03-18 after initial roadmap*
