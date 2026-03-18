# Plan: Phase 1 - 核心调度框架

**Phase:** 1
**Wave:** 1
**Date:** 2026-03-18

## Overview

实现基础任务调度能力，支持分发型和认领型调度。

**Requirements:** DIST-01, DIST-02, DIST-05

**Success Criteria:**
1. 中心节点可创建任务并拆分
2. 任务可通过分发型模式分发到指定边缘节点
3. 边缘节点可主动认领任务
4. 子任务结果可被协调者汇总
5. 任务状态可跟踪（Pending → Running → Complete）

---

## Plan 01: 任务领域模型

**Objective:** 定义任务、子任务、状态等核心领域模型

### Tasks

#### Task 1.1: 创建任务领域类

```xml
<task>
<name>Create Task domain classes</name>
<read_first>
- Z-Knight/pom.xml (check existing module structure)
- Z-Knight/src/main/java/com/isahl/chess/knight/ (existing patterns)
</read_first>
<action>
在 Z-Knight 模块下创建:

1. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/Task.java`
   - 字段: taskId (String), type (TaskType enum: DISPATCH/CLAIM), status (TaskStatus enum), subTasks (List<SubTask>), timeout (int秒), createdAt (Instant), deadline (Instant)
   - 方法: createSubTasks(int count), addResult(SubTaskResult), isComplete()

2. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/SubTask.java`
   - 字段: subTaskId (String), taskId (String), targetNode (String nullable), payload (String), status (SubTaskStatus enum), result (String nullable), retryCount (int), startedAt (Instant nullable), completedAt (Instant nullable)
   - 方法: markRunning(), markComplete(String result), markFailed()

3. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/TaskType.java` - DISPATCH, CLAIM

4. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/TaskStatus.java` - PENDING, RUNNING, PARTIAL_COMPLETE, COMPLETE, FAILED

5. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/SubTaskStatus.java` - PENDING, RUNNING, COMPLETE, FAILED

6. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/TaskResult.java`
   - 字段: taskId, status, aggregatedResult (String), subTaskResults (List<SubTaskResultEntry>)
</action>
<acceptance_criteria>
- [ ] Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/Task.java exists
- [ ] Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/SubTask.java exists
- [ ] TaskType, TaskStatus, SubTaskStatus enums defined
- [ ] TaskResult class exists
- [ ] Maven build succeeds: mvn compile -pl Z-Knight
</acceptance_criteria>
</task>
```

#### Task 1.2: 创建任务仓储

```xml
<task>
<name>Create Task Repository</name>
<read_first>
- Z-Rook/src/main/java/com/isahl/chess/rook/storage/db/repository/ (existing repository patterns)
</read_first>
<action>
创建 JPA Repository 用于持久化任务状态:

1. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/repository/TaskRepository.java`
   - extends JpaRepository<Task, String>
   - 方法: findByStatus(TaskStatus status), findByDeadlineBefore(Instant now)

2. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/repository/SubTaskRepository.java`
   - extends JpaRepository<SubTask, String>
   - 方法: findByTaskId(String taskId), findByStatusAndTargetNode(SubTaskStatus status, String nodeId)

在 Z-Knight 的 pom.xml 添加 JPA 依赖 (如果还没有)
</action>
<acceptance_criteria>
- [ ] TaskRepository.java exists with required methods
- [ ] SubTaskRepository.java exists with required methods
- [ ] JPA dependencies in Z-Knight/pom.xml
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 02: 调度器核心实现

**Objective:** 实现 TaskScheduler 接口和分发/认领逻辑

**Depends on:** Plan 01

### Tasks

#### Task 2.1: 创建调度器接口

```xml
<task>
<name>Create TaskScheduler interface</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/ (cluster service patterns)
</read_first>
<action>
创建调度器核心接口:

`cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java`

```java
public interface TaskScheduler {
    // 分发型: 创建任务并分发到指定节点
    Task dispatchTask(String taskId, String payload, List<String> targetNodes, int timeoutSeconds);
    
    // 认领型: 创建任务加入任务池
    Task claimTask(String taskId, String payload, int subTaskCount, int timeoutSeconds);
    
    // 节点认领任务 (认领型)
    Optional<SubTask> claimSubTasks(String nodeId, int maxCount);
    
    // 上报子任务结果
    void reportResult(String subTaskId, String result, boolean success);
    
    // 获取任务状态
    TaskStatus getTaskStatus(String taskId);
    
    // 获取任务结果
    TaskResult getTaskResult(String taskId);
}
```
</action>
<acceptance_criteria>
- [ ] TaskScheduler interface exists in core package
- [ ] All methods defined with correct signatures
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.2: 实现分发调度器

```xml
<task>
<name>Implement DispatchScheduler</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java (interface)
- Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/ (MQTT patterns)
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/DispatchScheduler.java`

实现分发型调度:

1. 实现 TaskScheduler 接口
2. dispatchTask() 方法:
   - 创建 Task 对象
   - 按 targetNodes 数量拆分 subTasks
   - 设置每个 SubTask.targetNode
   - 通过 MQTT (Z-Bishop) 发送分发消息到各节点
   - 保存 Task 到数据库
3. reportResult() 方法:
   - 更新 SubTask 状态和结果
   - 检查是否所有子任务完成
   - 完成则汇总结果
4. getTaskStatus() / getTaskResult() 查询方法

MQTT Topic:
- 分发: `zchess/task/dispatch/{nodeId}`
- 结果: `zchess/task/result/{nodeId}`
</action>
<acceptance_criteria>
- [ ] DispatchScheduler.java exists
- [ ] dispatchTask() creates task and sub-tasks correctly
- [ ] sub-tasks are distributed to correct nodes
- [ ] MQTT message sent for each dispatch
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.3: 实现认领调度器和任务池

```xml
<task>
<name>Implement ClaimScheduler and TaskPool</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java (interface)
- Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/ (MQTT patterns)
</read_first>
<action>
创建两个类:

1. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskPool.java`
   - 内存或Redis存储待认领任务
   - ConcurrentHashMap 存储任务
   - claim(nodeId, maxCount): 公平竞争，谁先来谁拿
   - release(subTaskId): 释放未完成的任务回池

2. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/ClaimScheduler.java`
   - 实现 TaskScheduler 接口
   - claimTask(): 创建任务，将所有子任务加入 TaskPool
   - claimSubTasks(nodeId, maxCount): 从 TaskPool 批量认领
   - reportResult(): 同 DispatchScheduler

MQTT Topic:
- 待认领通知: `zchess/task/pool/pending` (共享订阅)
- 结果: `zchess/task/result/{nodeId}`
</action>
<acceptance_criteria>
- [ ] TaskPool.java exists with concurrent access
- [ ] ClaimScheduler.java implements TaskScheduler
- [ ] claimSubTasks() returns correct batch
- [ ] released tasks return to pool
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.4: 实现结果汇总器

```xml
<task>
<name>Implement ResultAggregator</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/TaskResult.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/ResultAggregator.java`

```java
public interface ResultAggregator {
    // 合并同构子任务结果
    String aggregate(List<SubTaskResultEntry> results);
    
    // 检查是否可以完成
    boolean canComplete(Task task);
}
```

实现 DefaultResultAggregator:
- aggregate(): 将所有 result 用 JSON 数组包装
- canComplete(): 所有 SubTask 状态为 COMPLETE
</action>
<acceptance_criteria>
- [ ] ResultAggregator interface exists
- [ ] DefaultResultAggregator implements interface
- [ ] aggregate() returns JSON array string
- [ ] canComplete() checks all subTasks complete
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 03: REST API

**Objective:** 提供任务管理的REST接口

**Depends on:** Plan 02

### Tasks

#### Task 3.1: 创建调度REST控制器

```xml
<task>
<name>Create Scheduler REST Controller</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/ (existing REST patterns)
- Z-Player/src/main/java/com/isahl/chess/player/api/ (API patterns)
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/api/SchedulerController.java`

REST Endpoints:

POST /api/scheduler/tasks/dispatch
- Body: { taskId, payload, targetNodes[], timeoutSeconds }
- Response: { taskId, status, subTaskCount }

POST /api/scheduler/tasks/claim
- Body: { taskId, payload, subTaskCount, timeoutSeconds }
- Response: { taskId, status, subTaskCount }

POST /api/scheduler/tasks/{taskId}/claim
- Query: nodeId, maxCount
- Response: { subTasks[] } or 204 No Content

POST /api/scheduler/results
- Body: { subTaskId, result, success }
- Response: 200 OK

GET /api/scheduler/tasks/{taskId}
- Response: { taskId, status, result, progress }

使用 @RestController, @RequestMapping
</action>
<acceptance_criteria>
- [ ] SchedulerController.java exists
- [ ] POST /dispatch endpoint works
- [ ] POST /claim endpoint works
- [ ] POST /claim (node claim) endpoint works
- [ ] POST /results endpoint works
- [ ] GET /tasks/{id} endpoint works
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Verification

**must_haves:**
1. ✓ 分发型调度: 任务可分发到指定边缘节点
2. ✓ 认领型调度: 边缘节点可认领任务
3. ✓ 结果汇总: 子任务结果可汇总
4. ✓ 状态跟踪: 任务状态可查询

**Verification Commands:**
```bash
# Build
mvn compile -pl Z-Knight

# Unit tests (to be added)
mvn test -pl Z-Knight

# Integration test manual:
# 1. Start Z-Knight
# 2. POST /api/scheduler/tasks/dispatch with test data
# 3. Verify subTasks created and MQTT messages sent
```

---
*Plan created: 2026-03-18*
