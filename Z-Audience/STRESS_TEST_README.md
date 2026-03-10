# Z-Audience 压力测试验证器

基于 AIO 的高并发压力测试验证器，支持单机 3000+ 并发客户端连接。

## 特性

- **高并发**：利用 Java AIO 实现单机 5000+ 并发连接
- **多协议**：支持 MQTT、WebSocket、ZChat 协议
- **精确控制**：支持连接速率控制、请求速率控制
- **实时监控**：实时 QPS、延迟、成功率统计
- **REST API**：HTTP 接口控制测试 lifecycle

## 架构设计

```
PressureTestValidator (主控制器)
    ├── PressureTestConfig (配置)
    ├── PressureMetrics (指标收集)
    ├── TimeWheel (定时任务)
    └── Map<Long, StressClient> (客户端管理)
            └── StressClient (单个客户端)
                    ├── ISession (AIO 会话)
                    ├── PendingRequests (待处理请求)
                    └── ICancelable (定时任务)
```

## 快速开始

### 1. 配置文件

编辑 `src/main/resources/pressure-test.properties`：

```properties
# 目标服务器
z.chess.pressure.target.host=127.0.0.1
z.chess.pressure.target.port=1883

# 并发配置
z.chess.pressure.concurrency=3000
z.chess.pressure.requests-per-second-per-client=10

# 测试时长
z.chess.pressure.duration=PT60S

# 协议类型: mqtt, websocket, zchat
z.chess.pressure.protocol=mqtt
```

### 2. REST API 控制

启动测试：
```bash
curl -X POST http://localhost:8080/api/stress/start \
  -H "Content-Type: application/json" \
  -d '{
    "concurrency": 3000,
    "durationSeconds": 60,
    "requestsPerSecondPerClient": 10
  }'
```

查看状态：
```bash
curl http://localhost:8080/api/stress/status
```

获取报告：
```bash
curl http://localhost:8080/api/stress/report
```

停止测试：
```bash
curl -X POST http://localhost:8080/api/stress/stop
```

### 3. 编程方式使用

```java
@Autowired
private PressureTestValidator validator;

public void runTest() throws Exception {
    // 配置测试
    PressureTestConfig config = validator.getConfig();
    config.setConcurrency(3000);
    config.setDuration(Duration.ofSeconds(60));
    
    // 设置实时回调
    validator.setOnStatsUpdate(snapshot -> {
        System.out.printf("QPS: %.1f, Latency: %.2fms%n", 
            snapshot.qps, snapshot.avgLatencyMs);
    });
    
    // 启动测试
    CompletableFuture<PressureMetrics.Snapshot> future = validator.startTest();
    
    // 等待完成
    PressureMetrics.Snapshot result = future.get();
    System.out.println(result.toReport());
}
```

## 性能指标

| 指标 | 说明 |
|------|------|
| 并发连接数 | 活跃客户端连接数 |
| QPS | 每秒请求数 |
| 延迟 | 平均/最小/最大响应时间 |
| 成功率 | 成功请求百分比 |
| 吞吐量 | Mbps |

## 测试场景示例

### 场景 1：基础压测（3000 并发）

```java
config.setConcurrency(3000);
config.setDuration(Duration.ofSeconds(60));
config.setRequestsPerSecondPerClient(10);
// Target QPS: 30,000
```

### 场景 2：高吞吐（5000 并发）

```java
config.setConcurrency(5000);
config.setDuration(Duration.ofSeconds(120));
config.setRequestsPerSecondPerClient(100);
config.setPayloadSize(128);
// Target QPS: 500,000
```

### 场景 3：WebSocket 压测

```java
config.setConcurrency(1000);
config.setProtocol("websocket");
config.getTarget().setPort(8080);
config.getTarget().setPath("/ws");
```

## 注意事项

1. **系统限制**：
   - 调整 `ulimit -n`（文件描述符限制）
   - 调整 `/proc/sys/net/core/somaxconn`
   - 确保足够的内存（每个连接约 10KB）

2. **网络限制**：
   - 本地测试注意端口范围限制
   - 防火墙设置

3. **目标服务器**：
   - 确保目标服务器能处理测试负载
   - 建议先在测试环境验证

## 文件结构

```
Z-Audience/src/main/java/com/isahl/chess/audience/client/stress/
├── PressureTestConfig.java      # 配置类
├── PressureMetrics.java         # 指标收集
├── StressClient.java            # 客户端封装
├── PressureTestValidator.java   # 验证器主类
├── StressClientHandler.java     # 消息处理器
└── PressureTestController.java  # REST API

Z-Audience/src/main/resources/
└── pressure-test.properties     # 配置文件
```

## 依赖

- Z-Queen: AIO 基础设施
- Z-Bishop: 协议实现（MQTT/WebSocket）
- Z-King: 工具类（TimeWheel、日志等）
- Spring Boot: REST API 和依赖注入
