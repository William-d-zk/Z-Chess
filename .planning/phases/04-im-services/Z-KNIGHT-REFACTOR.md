# Z-Knight 重构设计

## 目标

将 Z-Knight 定位为核心调度引擎，提供通用调度能力；业务逻辑下沉到 Z-Player。

## 重构后的 Z-Knight 结构

### 1. 核心引擎接口 (engine)

```
com.isahl.chess.knight.engine
├── SchedulerEngine.java        # 调度引擎核心接口
├── Policy.java                 # 策略接口
├── SchedulingRule.java         # 调度规则接口
├── NodeRegistry.java           # 节点注册表接口
└── TaskContext.java           # 调度上下文
```

### 2. 策略模型 (policy)

```
com.isahl.chess.knight.policy
├── RetryPolicy.java            # 重试策略
├── TimeoutPolicy.java          # 超时策略
├── LoadBalancePolicy.java      # 负载均衡策略
└── FailoverPolicy.java        # 故障转移策略
```

### 3. 调度规则配置 (rule)

```
com.isahl.chess.knight.rule
├── DispatchRule.java          # 分发规则
├── ClaimRule.java             # 认领规则
└── GroupRule.java             # 分组规则
```

### 4. 服务接口 (service)

```
com.isahl.chess.knight.service
├── SchedulerService.java       # 调度服务接口
├── NodeService.java           # 节点服务接口
└── MetricsService.java       # 指标服务接口
```

### 5. 基础设施 (infra)

```
com.isahl.chess.knight.infra
├── TaskPool.java             # 任务池
├── ResultCollector.java      # 结果收集器
└── NodeRegistryImpl.java    # 节点注册表实现
```

## Z-Player 业务层结构

### 业务调度器

```
com.isahl.chess.player.scheduler
├── BusinessScheduler.java    # 业务调度器（使用Z-Knight引擎）
├── DeviceTaskPolicy.java     # 设备任务策略
└── AITaskPolicy.java         # AI任务策略
```

### 业务规则

```
com.isahl.chess.player.rule
├── DeviceDispatchRule.java   # 设备分发规则
└── ReportGenerationRule.java # 报表生成规则
```

## 接口定义

### SchedulerEngine

```java
public interface SchedulerEngine {
    // 提交任务
    String submitTask(TaskContext context);
    
    // 取消任务
    boolean cancelTask(String taskId);
    
    // 获取任务状态
    TaskStatus getTaskStatus(String taskId);
    
    // 注册节点
    void registerNode(NodeInfo node);
    
    // 节点心跳
    void nodeHeartbeat(String nodeId);
    
    // 获取调度策略
    Policy getPolicy(String taskType);
}
```

### Policy

```java
public interface Policy {
    String getPolicyId();
    String getPolicyType();
    boolean validate();
}
```

## 重构步骤

1. **提取引擎接口** - 从现有 TaskScheduler 提取核心接口到 engine 包
2. **抽象策略模型** - 将 RetryStrategy 等改为 Policy 接口
3. **定义调度规则** - 创建 SchedulingRule 接口和实现
4. **迁移业务模型** - 将 Task/SubTask 等移到 Z-Player
5. **清理 Z-Knight** - 只保留引擎、策略、服务接口
