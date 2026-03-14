# Z-Audience 端到端测试指南

本文档介绍如何使用 Z-Audience 进行 Z-Chess 集群的端到端测试。

## 功能特性

Z-Audience 现在支持以下测试功能：

### 1. 压力测试 (Pressure Test)
- **端点**: `/api/stress/*`
- 支持 MQTT/WebSocket/ZChat 多协议
- 最高 5000 并发连接
- 实时性能指标收集

### 2. API 测试
- **端点**: `/api/test/*`
- 集群健康检查
- REST API 端点测试
- 并发 HTTP 压力测试

### 3. MQTT 设备测试
- **端点**: `/api/mqtt/*`
- 使用已注册设备测试 MQTT 连接
- 单设备连接测试
- 多设备并发压力测试
- 设备凭证验证

## 快速开始

### 使用 Docker Compose

1. **构建镜像**
```bash
cd Z-Audience
mvn clean package -DskipTests
docker build -t z-chess-audience:1.0.22 .
```

2. **启动 Z-Audience**
```bash
cd scripts/docker
docker compose -f docker-compose.audience.yml up -d
```

3. **等待服务就绪**
```bash
until curl -sf http://localhost:8081/actuator/health; do
  echo "等待 Z-Audience 就绪..."
  sleep 2
done
```

### 运行测试脚本

```bash
cd scripts/test/e2e

# 运行完整的端到端测试
./run-z-audience-e2e.sh

# 指定目标集群地址
./run-z-audience-e2e.sh --raft-host 172.30.10.110

# 跳过压力测试（仅运行功能测试）
./run-z-audience-e2e.sh --skip-pressure

# 调整测试参数
./run-z-audience-e2e.sh \
  --api-concurrency 100 \
  --mqtt-devices 50 \
  --pressure-conc 200 \
  --pressure-duration 60
```

## API 端点参考

### 压力测试控制

```bash
# 启动压力测试
curl -X POST http://localhost:8081/api/stress/start \
  -H "Content-Type: application/json" \
  -d '{
    "concurrency": 1000,
    "durationSeconds": 60,
    "targetHost": "localhost",
    "targetPort": 1883,
    "protocol": "mqtt"
  }'

# 获取测试状态
curl http://localhost:8081/api/stress/status

# 获取详细指标
curl http://localhost:8081/api/stress/metrics

# 停止测试
curl -X POST http://localhost:8081/api/stress/stop
```

### API 功能测试

```bash
# 集群健康检查
curl "http://localhost:8081/api/test/health?host=localhost&port=8080&nodes=3"

# 设备列表查询测试
curl "http://localhost:8081/api/test/devices?host=localhost&port=8080&iterations=10"

# API 压力测试
curl -X POST http://localhost:8081/api/test/stress \
  -H "Content-Type: application/json" \
  -d '{
    "host": "localhost",
    "port": 8080,
    "endpoint": "/actuator/health",
    "concurrency": 50,
    "requestsPerClient": 100
  }'

# 完整 API 测试套件
curl -X POST http://localhost:8081/api/test/full \
  -H "Content-Type: application/json" \
  -d '{
    "host": "localhost",
    "startPort": 8080,
    "nodes": 3
  }'
```

### MQTT 设备测试

```bash
# 单设备连接测试（随机选择已注册设备）
curl "http://localhost:8081/api/mqtt/connect?host=localhost&port=1883"

# 使用指定凭证测试
curl "http://localhost:8081/api/mqtt/connect?host=localhost&port=1883&username=xxx&password=yyy&clientId=zzz"

# 多设备并发测试
curl -X POST http://localhost:8081/api/mqtt/stress \
  -H "Content-Type: application/json" \
  -d '{
    "host": "localhost",
    "port": 1883,
    "deviceCount": 20,
    "connectionsPerDevice": 2
  }'

# 验证设备凭证
curl "http://localhost:8081/api/mqtt/verify?host=localhost&port=1883&limit=10"

# 获取可用设备数量
curl http://localhost:8081/api/mqtt/devices/count

# 获取随机设备信息（脱敏）
curl http://localhost:8081/api/mqtt/devices/sample
```

## 环境变量配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `TARGET_HOST` | 目标服务器地址 | `host.docker.internal` |
| `TARGET_PORT` | MQTT 端口 | `1883` |
| `TARGET_PORT_WS` | WebSocket 端口 | `1889` |
| `PRESSURE_CONCURRENCY` | 压测并发数 | `1000` |
| `PRESSURE_DURATION` | 压测持续时间 | `PT60S` |
| `PRESSURE_CONNECTION_RATE` | 连接建立速率 | `50` |
| `PRESSURE_PROTOCOL` | 压测协议 | `mqtt` |

## 测试报告

测试脚本会自动生成 JSON 格式的测试报告，保存在 `reports/` 目录：

```json
{
  "testRun": {
    "timestamp": "2026-03-13T23:17:30+08:00",
    "targetHost": "172.30.10.110"
  },
  "configuration": {
    "apiConcurrency": 50,
    "mqttDevices": 20
  },
  "results": [
    "health: PASSED (3/3 nodes)",
    "api_devices: PASSED",
    "mqtt_single: PASSED",
    "pressure: PASSED (94.5% success)"
  ],
  "overallStatus": "PASSED"
}
```

## 故障排查

### MQTT 连接被拒绝

1. 检查设备是否已正确注册：
```bash
# 在数据库中检查
docker exec -it postgres psql -U chess -d chess -c "SELECT COUNT(*) FROM isahl.zc_id_device;"
```

2. 检查设备凭证文件是否正确挂载：
```bash
docker exec audience ls -la /app/data/
```

3. 验证设备连接：
```bash
curl http://localhost:8081/api/mqtt/devices/count
curl http://localhost:8081/api/mqtt/devices/sample
```

### API 测试失败

1. 检查集群健康状态：
```bash
curl http://localhost:8081/api/test/health
```

2. 检查目标端口是否可访问：
```bash
# 从 Z-Audience 容器内测试
docker exec -it audience curl http://raft00:8080/actuator/health
```

### 性能问题

1. 查看实时指标：
```bash
watch -n 5 'curl -s http://localhost:8081/api/stress/status | jq .'
```

2. 检查日志：
```bash
docker logs -f audience
```

## 注意事项

1. **测试设备**: 运行 MQTT 测试前确保已生成测试设备凭证：
```bash
cd scripts/test/e2e
./generate-100-devices.sh  # 生成 100 个测试设备
```

2. **网络连接**: Z-Audience 需要和 Z-Chess 集群在同一个 Docker 网络中。

3. **资源限制**: 大规模压力测试（>1000 并发）建议分配足够的 CPU 和内存。

4. **测试隔离**: 建议在独立的测试环境中运行压力测试，避免影响生产服务。
