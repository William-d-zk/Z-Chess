# Z-Audience 压测客户端使用指南

Z-Audience 是独立的压测客户端，支持 MQTT、WebSocket 和 ZChat 协议的压力测试。

## 目录结构

```
Z-Audience/
├── Dockerfile                      # Docker 镜像构建文件
├── src/main/resources/
│   ├── application-docker.yml      # Docker 环境配置
│   ├── audience.properties         # 客户端配置
│   └── pressure-test.properties    # 压测默认配置
└── src/main/java/.../stress/
    ├── PressureTestController.java # REST API 控制器
    ├── PressureTestConfig.java     # 配置类
    ├── PressureTestValidator.java  # 验证器
    └── PressureMetrics.java        # 指标收集
```

## 快速开始

### 1. 构建并启动 Z-Audience

```bash
# 进入项目目录
cd Z-Chess

# 构建并启动（默认连接到 host.docker.internal:1883）
./scripts/bin/start-audience.sh start

# 指定目标服务器
./scripts/bin/start-audience.sh start -t 192.168.1.100 -p 1883

# 指定并发数和持续时间
./scripts/bin/start-audience.sh start -t host.docker.internal -c 2000 -d PT120S
```

### 2. 查看状态

```bash
# 查看 Audience 状态
./scripts/bin/start-audience.sh status

# 查看日志
./scripts/bin/start-audience.sh logs
```

### 3. 停止服务

```bash
./scripts/bin/start-audience.sh stop
```

## REST API 接口

### 健康检查

```bash
curl http://localhost:8081/actuator/health
```

### 获取配置

```bash
curl http://localhost:8081/api/stress/config
```

### 更新配置

```bash
curl -X POST http://localhost:8081/api/stress/config \
  -H 'Content-Type: application/json' \
  -d '{
    "targetHost": "192.168.1.100",
    "targetPort": 1883,
    "concurrency": 2000,
    "durationSeconds": 120,
    "protocol": "mqtt"
  }'
```

### 启动压测

```bash
curl -X POST http://localhost:8081/api/stress/start \
  -H 'Content-Type: application/json' \
  -d '{
    "concurrency": 1000,
    "durationSeconds": 60,
    "requestsPerSecondPerClient": 10
  }'
```

### 查看压测状态

```bash
curl http://localhost:8081/api/stress/status
```

响应示例：
```json
{
  "code": 0,
  "data": {
    "state": "RUNNING",
    "activeClients": 1000,
    "totalRequests": 50000,
    "qps": 950.5,
    "successRate": 99.8
  }
}
```

### 获取压测报告

```bash
curl http://localhost:8081/api/stress/report
```

### 停止压测

```bash
curl -X POST http://localhost:8081/api/stress/stop
```

## 端到端测试

### 运行所有测试

```bash
./scripts/bin/start-audience.sh test
```

或直接运行：

```bash
./scripts/test/e2e/audience-stress-test.sh
```

### HTTP API 压测

```bash
# 基本测试
./scripts/test/e2e/api-stress-test.sh

# 指定目标
./scripts/test/e2e/api-stress-test.sh -e http://192.168.1.100:8080 -c 200 -n 2000

# 持续压力测试
./scripts/test/e2e/api-stress-test.sh sustained -d 120
```

### MQTT 压测

```bash
# 基本测试（需要安装 mosquitto-clients）
./scripts/test/e2e/mqtt-stress-test.sh

# 指定目标
./scripts/test/e2e/mqtt-stress-test.sh -h 192.168.1.100 -p 1883 -c 200

# 吞吐量测试
./scripts/test/e2e/mqtt-stress-test.sh throughput -n 5000 -s 1024
```

## Docker Compose 配置

### 使用 Docker Compose 启动

```bash
cd scripts/docker

# 基本启动
docker compose -f docker-compose.audience.yml up -d

# 指定目标服务器
TARGET_HOST=192.168.1.100 TARGET_PORT=1883 CONCURRENCY=2000 \
  docker compose -f docker-compose.audience.yml up -d

# 带监控（Prometheus + Grafana）
docker compose -f docker-compose.audience.yml --profile monitoring up -d
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `TARGET_HOST` | 目标服务器地址 | host.docker.internal |
| `TARGET_PORT` | 目标服务器端口 | 1883 |
| `CONCURRENCY` | 并发连接数 | 1000 |
| `DURATION` | 测试持续时间 | PT60S |
| `PROTOCOL` | 协议类型 (mqtt/websocket/zchat) | mqtt |

## 配置参数

### pressure-test.properties

```properties
# 目标服务器
z.chess.pressure.target.host=127.0.0.1
z.chess.pressure.target.port=1883

# 并发配置
z.chess.pressure.concurrency=3000
z.chess.pressure.requests-per-second-per-client=10
z.chess.pressure.connection-rate=100

# 测试时长
z.chess.pressure.duration=PT60S
z.chess.pressure.warm-up-seconds=5

# 超时配置
z.chess.pressure.connect-timeout=PT10S
z.chess.pressure.request-timeout=PT5S

# 协议类型
z.chess.pressure.protocol=mqtt
```

## 监控指标

### 压测指标

- **activeClients**: 活跃连接数
- **totalRequests**: 总请求数
- **successRequests**: 成功请求数
- **failedRequests**: 失败请求数
- **qps**: 每秒查询率
- **successRate**: 成功率
- **avgLatency**: 平均延迟
- **p99Latency**: P99 延迟

### Prometheus 指标

访问 http://localhost:9090 查看 Prometheus 监控数据。

### Grafana 仪表板

访问 http://localhost:3000 (admin/admin) 查看可视化仪表板。

## 常见问题

### 1. 连接被拒绝

检查目标服务器是否运行：
```bash
nc -zv <target_host> <target_port>
```

### 2. Docker 无法连接到主机

在 macOS/Windows 上使用 `host.docker.internal`：
```bash
TARGET_HOST=host.docker.internal ./scripts/bin/start-audience.sh start
```

在 Linux 上使用主机 IP：
```bash
TARGET_HOST=172.17.0.1 ./scripts/bin/start-audience.sh start
```

### 3. 内存不足

调整 JVM 参数：
```bash
JAVA_OPTS="-Xmx2g -XX:+UseG1GC" ./scripts/bin/start-audience.sh start
```

### 4. 端口冲突

修改 `application-docker.yml` 中的 `server.port` 或映射端口。

## 注意事项

1. **生产环境慎用**：压测会产生大量流量，请勿对生产环境直接压测
2. **网络带宽**：确保网络带宽足够支持压测流量
3. **文件描述符**：高并发需要调整系统文件描述符限制
4. **防火墙**：确保防火墙允许压测客户端访问目标端口
