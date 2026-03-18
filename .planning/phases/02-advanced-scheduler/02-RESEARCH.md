# Phase 2 Research: 高级调度与容错

**Phase:** 2 - 高级调度与容错
**Date:** 2026-03-19

## Research Question

How to implement Phase 2: 高级调度与容错 - 实现分组分发、组内认领和失败重试机制

## Context from Requirements

**Phase Requirements:**
- DIST-03: 分组分发 — 按边缘节点分组，定向分发任务到指定组
- DIST-04: 组内认领 — 组内边缘节点竞争认领，实现负载均衡
- DIST-06: 失败重试 — 任务失败在边缘端重试，节点失败由冗余算力重试

**Success Criteria:**
1. 边缘节点可分组，任务可定向发送到指定组
2. 组内边缘节点可竞争认领任务
3. 任务失败后边缘端自动重试
4. 节点失败后冗余算力自动接管
5. 超时机制防止死锁

## Implementation Approach

### 1. 节点分组管理

```java
public class NodeGroup {
    String groupId;
    String groupName;
    List<String> nodeIds;
    NodeGroupType type; // EXCLUSIVE, SHARED
}
```

**分组策略：**
- 按地理位置分组（华东、华南）
- 按设备类型分组（传感器、执行器）
- 按算力等级分组（高、中、低）

### 2. 分组分发调度器

扩展 DispatchScheduler:
```java
// 分组定向分发
Task dispatchToGroup(String taskId, String payload, String groupId, int timeoutSeconds);

// 分组广播分发
Task broadcastToGroup(String taskId, String payload, String groupId, int timeoutSeconds);
```

### 3. 组内认领机制

基于 TaskPool 扩展:
```java
public interface GroupTaskPool {
    // 组内竞争认领
    Optional<SubTask> claimFromGroup(String groupId, String nodeId, int maxCount);
    
    // 获取组内待认领数量
    int getPendingCount(String groupId);
}
```

### 4. 失败重试策略

```java
public enum RetryStrategy {
    EDGE_RETRY,      // 边缘端重试（任务失败）
    REDUNDANT_RETRY, // 冗余算力重试（节点失败）
    TIMEOUT_RETRY    // 超时重试
}

public class RetryConfig {
    int maxRetries = 3;
    long retryDelayMs = 1000;
    long timeoutMs = 30000;
}
```

**重试决策：**
- 任务失败 → 边缘端重试（本地）
- 节点失败（心跳超时） → 冗余节点接管
- 超时（deadline过了） → 重新分发

### 5. 超时检测

```java
public class TimeoutChecker {
    // 定期检查超时任务
    void checkTimeouts();
    
    // 超时处理：重新分发或标记失败
    void handleTimeout(Task task, SubTask subTask);
}
```

## Key Technical Decisions

| Decision | Option | Recommended | Rationale |
|----------|--------|-------------|-----------|
| 分组存储 | PostgreSQL | ✓ | 持久化，支持动态修改 |
| 组内认领 | 公平竞争 | ✓ | 简单实现，负载均衡 |
| 重试次数 | 3次 | ✓ | 平衡可靠性与开销 |
| 超时检测 | 定时任务 | ✓ | 简单可靠 |

## Dependencies

- Phase 1 TaskScheduler interface
- DispatchScheduler, ClaimScheduler implementations
- TaskPool for claim management

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| 分组不存在 | 低 | 中 | 预创建组，或动态创建 |
| 重试风暴 | 中 | 高 | 指数退避 |
| 节点分组不均 | 中 | 中 | 动态负载感知 |

---
*Research by: manual*
