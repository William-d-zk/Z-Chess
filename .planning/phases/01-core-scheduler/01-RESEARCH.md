# Phase 1 Research: 核心调度框架

**Phase:** 1 - 核心调度框架
**Date:** 2026-03-18

## Research Question

How to implement Phase 1: 核心调度框架 - 实现基础任务调度能力，支持分发型和认领型调度

## Context from Requirements

**Phase Requirements:**
- DIST-01: 分发型调度 — 中心节点主动将任务拆分并分发到边缘节点执行
- DIST-02: 认领型调度 — 边缘节点从任务池主动认领任务，减少中心负载
- DIST-05: 结果汇总 — 集群协调者收集并合并所有子任务结果

**Success Criteria:**
1. 中心节点可创建任务并拆分
2. 任务可通过分发型模式分发到指定边缘节点
3. 边缘节点可主动认领任务
4. 子任务结果可被协调者汇总
5. 任务状态可跟踪（Pending → Running → Complete）

## Implementation Approach

### 1. 任务调度器扩展 (Z-Knight)

**现有Z-Knight能力：**
- RAFT共识算法实现
- Spring Boot REST服务
- 集群通信（ZChat协议）

**扩展点：**
```
Z-Knight/
├── scheduler/           # 新增调度模块
│   ├── api/             # 调度REST API
│   ├── core/            # 调度核心逻辑
│   │   ├── TaskScheduler.java      # 调度器接口
│   │   ├── DispatchScheduler.java   # 分发型调度实现
│   │   ├── ClaimScheduler.java     # 认领型调度实现
│   │   └── TaskAggregator.java     # 结果汇总器
│   ├── domain/          # 领域模型
│   │   ├── Task.java               # 任务
│   │   ├── SubTask.java            # 子任务
│   │   ├── TaskStatus.java         # 任务状态
│   │   └── TaskResult.java         # 执行结果
│   └── repository/      # 任务仓储
```

### 2. 任务领域模型

```java
// 任务
public class Task {
    String taskId;           // 任务唯一ID
    String type;             // 任务类型 (DISPATCH, CLAIM)
    TaskStatus status;       // PENDING, RUNNING, PARTIAL_COMPLETE, COMPLETE, FAILED
    List<SubTask> subTasks;  // 子任务列表
    int timeout;             // 超时时间(秒)
    Instant createdAt;
    Instant deadline;
}

// 子任务
public class SubTask {
    String subTaskId;
    String taskId;
    String targetNode;       // 分发型:指定节点, 认领型:null
    String payload;          // 执行载荷
    SubTaskStatus status;    // PENDING, RUNNING, COMPLETE, FAILED
    String result;           // 执行结果
    int retryCount;
    Instant startedAt;
    Instant completedAt;
}
```

### 3. 调度接口设计

```java
public interface TaskScheduler {
    // 分发型: 主动分发任务到指定节点
    Task dispatch(String taskId, List<String> targetNodes);
    
    // 认领型: 节点认领任务
    Optional<SubTask> claimTask(String nodeId, int maxCount);
    
    // 获取任务状态
    TaskStatus getTaskStatus(String taskId);
    
    // 获取节点待认领任务数
    int getPendingTaskCount();
}

// 任务池 (认领型)
public interface TaskPool {
    void addTask(SubTask task);
    Optional<SubTask> claim(String nodeId);
    List<SubTask> claimBatch(String nodeId, int maxCount);
    void release(String subTaskId);  // 释放未完成任务
}
```

### 4. 调度流程

**分发型流程：**
```
1. 用户提交任务 (POST /api/tasks)
2. 调度器拆分任务为N个子任务
3. 分发给指定边缘节点 (通过MQTT)
4. 边缘节点执行并上报结果
5. 协调者收集结果
6. 所有子任务完成 -> 汇总结果
7. 更新任务状态为COMPLETE
```

**认领型流程：**
```
1. 用户提交任务 (POST /api/tasks)
2. 调度器拆分任务为N个子任务，加入任务池
3. 边缘节点心跳中携带"待认领数"
4. 边缘节点认领任务 (POST /api/tasks/claim)
5. 边缘节点执行并上报结果
6. 协调者收集结果
7. 所有子任务完成 -> 汇总结果
```

### 5. MQTT通道设计

**Topic结构：**
```
zchess/task/dispatch/{nodeId}     # 分发到指定节点
zchess/task/claim/{nodeId}         # 认领响应
zchess/task/result/{nodeId}       # 结果上报
zchess/task/pool/pending          # 待认领任务池 (共享订阅)
zchess/task/status/{taskId}        # 任务状态更新
```

### 6. 结果汇总策略

```java
public interface ResultAggregator {
    // 合并同构子任务结果
    Object aggregate(List<SubTaskResult> results);
    
    // 部分完成时检查是否可以提前汇总
    boolean canFinalize(Task task, double completionRatio);
    
    // 失败处理
    boolean shouldRetry(SubTask task);
    Optional<String> getRedundantNode(SubTask failedTask);
}
```

### 7. 状态机

```
PENDING ─────────────────────────────────────────────────────
    │ create_task()
    ▼
RUNNING ◄────────────────────────────────────────────────────
    │ all_subtasks_complete()
    ▼
COMPLETE
    │
    │ any_subtask_failed() && !can_retry()
    ▼
FAILED

RUNNING ◄────────────────────────────────────────────────────
    │ subtask_failed() && can_retry()
    ▼
PARTIAL_COMPLETE (可视为RUNNING)
```

## Key Technical Decisions

| Decision | Option | Recommended | Rationale |
|----------|--------|-------------|-----------|
| 任务状态存储 | PostgreSQL | ✓ | 需要持久化，RAFT内存有限 |
| 分发机制 | MQTT | ✓ | 复用Z-Chess MQTT层 |
| 认领负载均衡 | 公平竞争 | ✓ | 简单，实现认领型必需 |
| 结果合并时机 | 全量完成 | ✓ | 简化设计，Phase 1 |

## Open Questions

1. 任务超时时间默认值？
2. 单个子任务失败是否导致整体失败？
3. 是否需要任务优先级？

## Validation Architecture

**Phase 1验证需要：**
1. 单元测试覆盖调度逻辑
2. 集成测试验证MQTT分发/认领
3. 模拟边缘节点的行为测试
4. 结果汇总正确性验证

## Dependencies

- Z-Knight (RAFT集群) - 已完成
- Z-Bishop (MQTT) - 已完成
- Z-Queen (AIO) - 已完成

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| 调度器性能瓶颈 | 低 | 高 | 任务批量处理 |
| MQTT消息丢失 | 中 | 高 | QoS2 + 确认机制 |
| 结果汇总死锁 | 中 | 高 | 超时 + 冗余机制 |

---
*Research by: manual*
