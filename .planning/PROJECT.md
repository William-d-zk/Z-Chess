# 边缘算力调度与即时通讯平台

## What This Is

在Z-Chess基础上扩展，构建利用边缘设备闲置算力进行分布式计算的IoT平台，同时提供面向组织员工的即时通讯服务，整合设备操作、监控、智能体协作和数据订阅推送能力。

## Core Value

**让闲置的边缘算力被用起来** — 百万级工业边缘设备平时低负载运行，平台将计算任务拆分托管到边缘节点执行，结果汇总后通过IM触达员工。

## Current State (v1.0 Shipped)

**v1.0 MVP** shipped 2026-03-19 with:
- Distributed task scheduling (dispatch + claim modes)
- Edge agent (Z-Square) with heartbeat monitoring
- IM core services (auth, groups, messages with WebSocket)

## Requirements

### Validated (v1.0)

- ✓ 分发型调度 — v1.0
- ✓ 认领型调度 — v1.0
- ✓ 分组分发 — v1.0
- ✓ 组内认领 — v1.0
- ✓ 结果汇总 — v1.0
- ✓ 失败重试 — v1.0
- ✓ 用户认证 — v1.0
- ✓ 群组聊天 — v1.0
- ✓ 消息推送 — v1.0

### Active (v1.1)

- [ ] **设备操作**: IM中可直接下发设备控制指令到边缘节点 (IM-04)
- [ ] **状态监控**: IM中可订阅设备状态，变更推送到对应会话 (IM-05)
- [ ] **数据订阅**: 聊天主题关联数据订阅，变换后推送到对应会话 (IM-06)
- [ ] **工作流提醒**: 任务状态变更、报表生成等事件通过IM提醒 (IM-07)
- [ ] **SIM/eSIM绑定**: 设备SIM卡与边缘节点身份绑定 (IM-08)
- [ ] **延迟分级保障**: 设备控制 < 40ms，AI推理 10-30s，报表 12h (LAT-01 to LAT-03)

### Out of Scope

- 公网部署和服务外部客户
- 通用分布式任务调度框架（专注工业边缘场景）
- 移动端IM应用

## Context

**技术基础：**
- Z-Chess现有模块：Z-Board（基础框架）、Z-King（基础组件）、Z-Queen（网络通信）、Z-Bishop（MQTT协议）、Z-Knight（RAFT集群）、Z-Rook（持久化存储）、Z-Pawn（设备接入）、Z-Player（业务API）、Z-Arena（网关）
- Z-Knight 新增: SchedulerEngine, SchedulingRule, Policy 接口
- Z-Square 新增: Edge Agent 模块
- Z-Player 新增: IM 服务层
- PostgreSQL + EhCache存储
- MQTT v5.0/v3.1.1协议支持
- RAFT一致性集群

**已知限制：**
- ZLS协议仅支持单边认证，NTRU(RC4)需注意密钥强度
- MQTT 5.0 broker行为部分未实现
- Modbus TLS Phase 3未完成

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 混合调度模式 | 工业场景多样化，灵活适配不同任务类型 | ✓ Validated v1.0 |
| Z-Chess扩展 | 复用现有MQTT、集群、存储能力 | ✓ Validated v1.0 |
| 结果结构一致性 | 简化汇总逻辑，支持并行处理 | ✓ Validated v1.0 |
| 边缘端任务重试 | 减少网络往返，降低协调者负载 | ✓ Validated v1.0 |
| Z-Knight架构重构 | 提取 SchedulerEngine 接口，解耦调度逻辑 | ✓ Validated v1.0 |

## Next Milestone Goals

**v1.1** — 设备联动 (Phase 5-6):
- IM中操作设备、订阅状态、数据推送
- 延迟分级保障

---
*Last updated: 2026-03-19 after v1.0 milestone shipped*
