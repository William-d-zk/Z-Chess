# Plan: Phase 2 - 高级调度与容错

**Phase:** 2
**Wave:** 1
**Date:** 2026-03-19

## Overview

实现分组分发、组内认领和失败重试机制。

**Requirements:** DIST-03, DIST-04, DIST-06

**Success Criteria:**
1. 边缘节点可分组，任务可定向发送到指定组
2. 组内边缘节点可竞争认领任务
3. 任务失败后边缘端自动重试
4. 节点失败后冗余算力自动接管
5. 超时机制防止死锁

---

## Plan 01: 节点分组管理

**Objective:** 实现边缘节点分组管理能力

### Tasks

#### Task 1.1: 创建节点分组领域类

```xml
<task>
<name>Create NodeGroup domain classes</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/
</read_first>
<action>
在 Z-Knight scheduler/domain 下创建:

1. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/NodeGroup.java`
   - 字段: groupId (String), groupName (String), nodeIds (List<String>), type (NodeGroupType enum), createdAt (Instant)
   - 方法: addNode(String nodeId), removeNode(String nodeId), getNodeCount()

2. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/NodeGroupType.java`
   - EXCLUSIVE (专属组，节点只能属于一个组)
   - SHARED (共享组，节点可属于多个组)
</action>
<acceptance_criteria>
- [ ] NodeGroup.java exists
- [ ] NodeGroupType enum defined
- [ ] Maven build succeeds: mvn compile -pl Z-Knight
</acceptance_criteria>
</task>
```

#### Task 1.2: 创建节点分组仓储

```xml
<task>
<name>Create NodeGroup Repository</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/repository/
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/repository/NodeGroupRepository.java`

- extends JpaRepository<NodeGroup, String>
- 方法: findByGroupName(String name), findByNodeId(String nodeId)
</action>
<acceptance_criteria>
- [ ] NodeGroupRepository.java exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 02: 分组调度器实现

**Objective:** 扩展调度器支持分组分发

**Depends on:** Plan 01

### Tasks

#### Task 2.1: 创建分组调度接口

```xml
<task>
<name>Create GroupScheduler interface</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/GroupScheduler.java`

```java
public interface GroupScheduler {
    // 分组定向分发
    Task dispatchToGroup(String taskId, String payload, String groupId, List<String> targetNodes, int timeoutSeconds);
    
    // 分组广播分发（组内所有节点）
    Task broadcastToGroup(String taskId, String payload, String groupId, int timeoutSeconds);
    
    // 获取组的待认领任务数
    int getGroupPendingCount(String groupId);
}
```
</action>
<acceptance_criteria>
- [ ] GroupScheduler interface exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.2: 实现分组调度器

```xml
<task>
<name>Implement GroupDispatchScheduler</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/GroupScheduler.java
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/repository/NodeGroupRepository.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/GroupDispatchScheduler.java`

实现:
1. dispatchToGroup() - 从NodeGroupRepository获取组内节点，向这些节点分发
2. broadcastToGroup() - 类似，但分发到组内所有节点
3. getGroupPendingCount() - 查询组内待认领任务数
</action>
<acceptance_criteria>
- [ ] GroupDispatchScheduler.java exists
- [ ] dispatchToGroup() works correctly
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 03: 组内认领机制

**Objective:** 实现组内竞争认领

**Depends on:** Plan 02

### Tasks

#### Task 3.1: 扩展TaskPool支持分组

```xml
<task>
<name>Extend TaskPool with group support</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskPool.java
</read_first>
<action>
扩展 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskPool.java`:

添加方法:
```java
// 组内竞争认领
Optional<SubTask> claimFromGroup(String groupId, String nodeId, int maxCount);

// 按组统计待认领数
int getPendingCountByGroup(String groupId);

// 获取所有组ID
Set<String> getAllGroups();
```
</action>
<acceptance_criteria>
- [ ] TaskPool supports group operations
- [ ] claimFromGroup() works correctly
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 3.2: 实现组内认领调度器

```xml
<task>
<name>Implement GroupClaimScheduler</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskPool.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/GroupClaimScheduler.java`

实现:
1. 使用扩展的TaskPool支持组内认领
2. claimSubTasks() 调用 TaskPool.claimFromGroup()
</action>
<acceptance_criteria>
- [ ] GroupClaimScheduler.java exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 04: 失败重试机制

**Objective:** 实现任务失败重试和节点失败接管

**Depends on:** Plan 02

### Tasks

#### Task 4.1: 创建重试配置和策略

```xml
<task>
<name>Create Retry configuration</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/
</read_first>
<action>
在 scheduler/domain 下创建:

1. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/RetryStrategy.java`
   - EDGE_RETRY (边缘端重试)
   - REDUNDANT_RETRY (冗余算力重试)
   - TIMEOUT_RETRY (超时重试)

2. `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/domain/RetryConfig.java`
   - maxRetries: int (默认3)
   - retryDelayMs: long (默认1000)
   - timeoutMs: long (默认30000)
</action>
<acceptance_criteria>
- [ ] RetryStrategy enum defined
- [ ] RetryConfig class exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 4.2: 实现重试调度器

```xml
<task>
<name>Implement RetryScheduler</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/DispatchScheduler.java
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/RetryConfig.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/RetryScheduler.java`

功能:
1. 包装原有调度器，增加重试逻辑
2. 任务失败 -> EDGE_RETRY -> 边缘端重试
3. 节点失败(心跳超时) -> REDUNDANT_RETRY -> 分配到其他节点
4. 超时 -> TIMEOUT_RETRY -> 重新分发

实现:
- 重试计数检查 (maxRetries)
- 重试延迟 (exponential backoff)
- 失败类型判断
</action>
<acceptance_criteria>
- [ ] RetryScheduler.java exists
- [ ] Retry logic works correctly
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 05: 超时检测

**Objective:** 实现超时检测防止死锁

**Depends on:** Plan 04

### Tasks

#### Task 5.1: 创建超时检测器

```xml
<task>
<name>Create TimeoutChecker</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/repository/TaskRepository.java
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TaskScheduler.java
</read_first>
<action>
创建 `cluster.z-knight/src/main/java/com/isahl/chess/knight/scheduler/core/TimeoutChecker.java`

功能:
1. 定时检查超时任务 (使用@Scheduled)
2. 查找超时的SubTask
3. 处理超时: 标记为FAILED，触发重试
4. 防止死锁

实现:
- @Scheduled(fixedRate = 5000) 每5秒检查
- 使用TaskRepository.findByDeadlineBefore()
</action>
<acceptance_criteria>
- [ ] TimeoutChecker.java exists
- [ ] Scheduled annotation works
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Verification

**must_haves:**
1. ✓ 分组分发: 任务可定向发送到指定组
2. ✓ 组内认领: 组内节点可竞争认领任务
3. ✓ 失败重试: 任务/节点失败后自动重试
4. ✓ 超时机制: 超时任务被检测并处理

**Verification Commands:**
```bash
# Build
mvn compile -pl Z-Knight

# Unit tests
mvn test -pl Z-Knight
```

---
*Plan created: 2026-03-19*
