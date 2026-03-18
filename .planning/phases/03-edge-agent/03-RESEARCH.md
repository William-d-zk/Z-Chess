# Phase 3 Research: 边缘Agent

**Phase:** 3 - 边缘Agent
**Date:** 2026-03-19

## Research Question

How to implement Phase 3: 边缘Agent - 开发边缘节点Agent，实现注册、执行、监控能力

## Context

**Phase Requirements (Success Criteria):**
1. Edge Agent可在边缘设备上部署（Docker/二进制）
2. Agent可向调度器注册算力信息
3. Agent可接收/执行子任务
4. Agent可上报执行结果
5. Agent心跳监控正常

## Architecture Options

### Option 1: Java Module (Z-Edge)

**Pros:**
- 与Z-Chess架构统一
- 代码复用方便
- 调试方便

**Cons:**
- JVM内存占用较高（边缘设备资源有限）
- 部署包较大

### Option 2: Go Binary

**Pros:**
- 内存占用低（<50MB）
- 二进制部署简单
- 交叉编译支持好

**Cons:**
- 与Z-Chess代码不共享
- 需要维护两套语言栈

### Option 3: WasmEdge

**Pros:**
- 多语言支持
- 沙箱隔离

**Cons:**
- 复杂度高
- 生态系统不成熟

## Recommended Approach

**Phase 3: Java Module (Z-Edge)**
- 保持与Z-Chess架构统一
- 使用轻量级Spring Boot
- 支持Docker部署

**Future (Phase 5+): Go迁移**
- 如果内存成为瓶颈，迁移到Go

## Implementation Design

### 边缘Agent架构

```
┌─────────────────────────────────────┐
│           Edge Agent                 │
├─────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐ │
│  │ Registration│  │  Heartbeat │ │
│  │   Client    │  │   Sender   │ │
│  └─────────────┘  └─────────────┘ │
│  ┌─────────────┐  ┌─────────────┐ │
│  │    Task     │  │   Result   │ │
│  │  Receiver   │  │   Reporter  │ │
│  └─────────────┘  └─────────────┘ │
│  ┌─────────────────────────────────┐ │
│  │        Task Executor             │ │
│  │  (支持可扩展的任务执行器)        │ │
│  └─────────────────────────────────┘ │
└─────────────────────────────────────┘
        │                    │
        ▼                    ▼
   Z-Knight            Z-Knight
  (注册/心跳)          (任务分发)
```

### MQTT通信

边缘Agent通过MQTT与中心调度器通信：
- 订阅: `zchess/edge/{nodeId}/tasks` - 接收任务
- 发布: `zchess/edge/{nodeId}/results` - 上报结果
- 发布: `zchess/edge/{nodeId}/heartbeat` - 心跳

### REST通信

同时支持REST API：
- 注册: `POST /api/edge/register`
- 心跳: `POST /api/edge/heartbeat`
- 结果上报: `POST /api/edge/results`

## Dependencies

- Z-Knight (调度器) - Phase 1-2已完成
- MQTT Broker (Z-Bishop) - 已完成

---
*Research by: manual*
