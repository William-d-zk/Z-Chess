# Plan: Phase 3 - 边缘Agent

**Phase:** 3
**Wave:** 1
**Date:** 2026-03-19

## Overview

开发边缘节点Agent，实现注册、执行、监控能力。

**Success Criteria:**
1. Edge Agent可在边缘设备上部署（Docker/二进制）
2. Agent可向调度器注册算力信息
3. Agent可接收/执行子任务
4. Agent可上报执行结果
5. Agent心跳监控正常

---

## Plan 01: 创建Z-Edge模块

**Objective:** 创建边缘Agent的Maven模块结构

### Tasks

#### Task 1.1: 创建Z-Edge模块

```xml
<task>
<name>Create Z-Edge module structure</name>
<read_first>
- pom.xml (root)
</read_first>
<action>
1. 创建 `Z-Edge/` 目录结构:
```
Z-Edge/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/isahl/chess/edge/
│   │   │   ├── EdgeAgent.java (主入口)
│   │   │   ├── config/ (配置)
│   │   │   ├── client/ (REST/MQTT客户端)
│   │   │   ├── executor/ (任务执行器)
│   │   │   └── model/ (数据传输对象)
│   │   └── resources/
│   │       └── application.properties
│   └── test/
```

2. 创建 `Z-Edge/pom.xml`:
- parent: framework.z-chess
- artifact: edge.z-edge
- 依赖: Z-Bishop (MQTT), Z-King (基础工具)
</action>
<acceptance_criteria>
- [ ] Z-Edge/pom.xml exists
- [ ] Directory structure created
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Plan 02: Edge Agent核心实现

**Objective:** 实现边缘Agent核心功能

### Tasks

#### Task 2.1: 创建Agent主入口和配置

```xml
<task>
<name>Create EdgeAgent main class</name>
<read_first>
- Z-Arena/src/main/java/com/isahl/chess/arena/start/ApplicationArena.java
</read_first>
<action>
创建:

1. `Z-Edge/src/main/java/com/isahl/chess/edge/EdgeAgent.java`
   - Spring Boot应用主入口
   - @EnableScheduling
   - 启动时自动注册

2. `Z-Edge/src/main/java/com/isahl/chess/edge/config/EdgeConfig.java`
   - 配置节点ID、调度器地址等
   - @ConfigurationProperties(prefix = "z.chess.edge")

3. `Z-Edge/src/main/resources/application.properties`
   - z.chess.edge.node-id
   - z.chess.edge.scheduler-url
   - z.chess.edge.heartbeat-interval
</action>
<acceptance_criteria>
- [ ] EdgeAgent.java exists
- [ ] EdgeConfig.java exists
- [ ] application.properties created
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.2: 创建注册和心跳客户端

```xml
<task>
<name>Create Registration and Heartbeat client</name>
<read_first>
- Z-Edge/src/main/java/com/isahl/chess/edge/config/EdgeConfig.java
</read_first>
<action>
创建:

1. `Z-Edge/src/main/java/com/isahl/chess/edge/client/EdgeClient.java`
   - RestTemplate调用调度器API
   - registerNode() - 注册节点信息
   - sendHeartbeat() - 发送心跳
   - reportResult() - 上报结果

2. `Z-Edge/src/main/java/com/isahl/chess/edge/client/TaskReceiver.java`
   - 订阅MQTT任务Topic
   - 接收子任务分发

3. `Z-Edge/src/main/java/com/isahl/chess/edge/model/NodeInfo.java`
   - 节点信息DTO (nodeId, cpu, memory, status)
</action>
<acceptance_criteria>
- [ ] EdgeClient.java exists
- [ ] TaskReceiver.java exists
- [ ] NodeInfo.java exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.3: 创建任务执行器

```xml
<task>
<name>Create Task Executor</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/domain/SubTask.java
</read_first>
<action>
创建:

1. `Z-Edge/src/main/java/com/isahl/chess/edge/executor/TaskExecutor.java`
   - 接口: void execute(SubTask task)
   - 执行子任务
   - 返回执行结果

2. `Z-Edge/src/main/java/com/isahl/chess/edge/executor/DefaultTaskExecutor.java`
   - 实现默认执行器
   - 支持命令执行、脚本运行

3. `Z-Edge/src/main/java/com/isahl/chess/edge/executor/ScriptTaskExecutor.java`
   - 支持脚本执行
</action>
<acceptance_criteria>
- [ ] TaskExecutor interface exists
- [ ] DefaultTaskExecutor exists
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

#### Task 2.4: 创建心跳定时器

```xml
<task>
<name>Create Heartbeat scheduler</name>
<read_first>
- Z-Knight/src/main/java/com/isahl/chess/knight/scheduler/core/TimeoutChecker.java
</read_first>
<action>
创建 `Z-Edge/src/main/java/com/isahl/chess/edge/client/HeartbeatScheduler.java`

- @Scheduled(fixedRateString = "${z.chess.edge.heartbeat-interval:30000}")
- 定期发送心跳
- 包含节点状态信息
</action>
<acceptance_criteria>
- [ ] HeartbeatScheduler.java exists
- [ ] @Scheduled annotation works
- [ ] Maven build succeeds
</acceptance_criteria>
</task>
```

---

## Verification

**must_haves:**
1. ✓ 边缘Agent模块创建完成
2. ✓ 注册功能实现
3. ✓ 任务接收功能实现
4. ✓ 结果上报功能实现
5. ✓ 心跳监控功能实现

**Verification Commands:**
```bash
# Build
mvn compile -pl Z-Edge

# Run (requires Z-Knight running)
# java -jar Z-Edge/target/edge.z-edge-1.0.22.jar
```

---
*Plan created: 2026-03-19*
