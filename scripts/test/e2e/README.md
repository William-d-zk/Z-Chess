# Z-Chess 端到端测试套件

本目录包含 Z-Chess 集群的端到端测试脚本和工具。

## 目录结构

```
scripts/test/e2e/
├── README.md                      # 本文件
├── run-z-audience-e2e.sh          # 主要 E2E 测试脚本 (推荐)
├── quick-test-z-audience.sh       # 快速测试脚本
├── generate-100-devices.sh        # 生成 100 个测试设备
├── generate-mqtt-test-data.sh     # 生成 MQTT 测试数据
├── verify-device.sh               # 验证单个设备
├── data/                          # 测试数据
│   └── devices-100.json          # 100 个测试设备凭证
├── reports/                       # 测试报告输出
└── config/                        # 测试配置
```

## 快速开始

### 1. 准备环境

确保以下条件满足：
- Z-Chess 集群正在运行 (3 个 Raft 节点 + PostgreSQL)
- 已生成测试设备凭证
- Z-Audience 服务已部署

### 2. 生成测试设备（如尚未生成）

```bash
# 生成 100 个测试设备
./generate-100-devices.sh

# 验证设备已注册
cat data/devices-100.json | jq '.devices | length'
```

### 3. 快速测试

```bash
# 运行快速测试（约 30 秒）
./quick-test-z-audience.sh

# 指定 Z-Audience 地址
./quick-test-z-audience.sh --host 172.30.10.100 --port 8081
```

### 4. 完整 E2E 测试

```bash
# 运行完整测试（约 2-5 分钟，包含压力测试）
./run-z-audience-e2e.sh

# 指定目标集群地址
./run-z-audience-e2e.sh --raft-host 172.30.10.110

# 跳过压力测试（仅功能测试，约 1 分钟）
./run-z-audience-e2e.sh --skip-pressure
```

## 测试脚本说明

### run-z-audience-e2e.sh

主要端到端测试脚本，执行完整的测试套件。

**测试内容：**
1. 集群健康检查 - 验证所有 Raft 节点状态
2. API 功能测试 - 测试 REST API 端点
3. MQTT 连接测试 - 使用已注册设备测试 MQTT
4. 压力测试 - 并发连接和消息吞吐量测试

**参数说明：**
```bash
--host HOST                 # Z-Audience 主机 (默认: localhost)
--port PORT                 # Z-Audience 端口 (默认: 8081)
--raft-host HOST            # Raft 集群主机 (默认: localhost)
--raft-http PORT            # Raft HTTP 端口 (默认: 8080)
--raft-mqtt PORT            # Raft MQTT 端口 (默认: 1883)
--api-concurrency N         # API 并发数 (默认: 50)
--api-requests N            # API 每客户端请求数 (默认: 100)
--mqtt-devices N            # MQTT 测试设备数 (默认: 20)
--mqtt-conn-per-dev N       # MQTT 每设备连接数 (默认: 2)
--pressure-conc N           # 压力测试并发数 (默认: 100)
--pressure-duration S       # 压力测试持续时间 (默认: 30)
--skip-health               # 跳过健康检查
--skip-api                  # 跳过 API 测试
--skip-mqtt                 # 跳过 MQTT 测试
--skip-pressure             # 跳过压力测试
```

**输出示例：**
```
═══════════════════════════════════════════════════════════════
  Z-Audience 端到端测试
═══════════════════════════════════════════════════════════════

ℹ 配置:
  Z-Audience: localhost:8081
  Raft HTTP:  localhost:8080
  Raft MQTT:  localhost:1883

...

测试结果汇总:
───────────────────────────────────────────────────────────────
  ✓ health: PASSED (3/3 nodes)
  ✓ api_devices: PASSED
  ✓ api_stress: PASSED (99.8% success)
  ✓ mqtt_single: PASSED
  ✓ mqtt_stress: PASSED (96.5% success)
  ✓ pressure: PASSED (94.2% success)
───────────────────────────────────────────────────────────────

✓ 所有测试通过!

整体状态: PASSED

详细报告已保存: reports/z-audience-e2e-report-20260313_231730.json
```

### quick-test-z-audience.sh

快速功能验证脚本，用于快速检查系统状态。

**测试内容：**
1. Z-Audience 服务健康
2. 设备凭证加载
3. 集群健康检查
4. MQTT 单设备连接
5. API 快速测试
6. 压测配置检查

### generate-100-devices.sh

批量生成测试设备并注册到集群。

```bash
# 生成设备并保存凭证
./generate-100-devices.sh

# 输出：
# - data/devices-100.json: 设备凭证
# - data/devices-100.csv: CSV 格式
```

## 测试数据文件

### data/devices-100.json

包含 100 个测试设备的 MQTT 凭证，格式如下：

```json
{
  "devices": [
    {
      "index": 1,
      "username": "mqtt_test_xxx_1",
      "token": "...",
      "serial": "...",
      "public_key": "...",
      "mqtt_username": "...",
      "mqtt_password": "...",
      "mqtt_client_id": "..."
    }
  ]
}
```

## Z-Audience API 参考

### 压力测试控制

- `POST /api/stress/start` - 启动压力测试
- `POST /api/stress/stop` - 停止压力测试
- `GET /api/stress/status` - 获取测试状态
- `GET /api/stress/metrics` - 获取性能指标
- `GET /api/stress/config` - 获取配置

### API 测试

- `GET /api/test/health` - 集群健康检查
- `GET /api/test/devices` - 设备列表查询测试
- `POST /api/test/stress` - API 并发压力测试
- `POST /api/test/full` - 完整测试套件

### MQTT 设备测试

- `GET /api/mqtt/connect` - 单设备连接测试
- `POST /api/mqtt/stress` - 多设备并发测试
- `GET /api/mqtt/verify` - 验证所有设备
- `GET /api/mqtt/devices/count` - 设备数量
- `GET /api/mqtt/devices/sample` - 样本设备信息

## 故障排查

### MQTT 连接被拒绝

1. 检查设备是否已注册：
```bash
./verify-device.sh data/devices-100.json 0
```

2. 检查 Z-Audience 是否正确加载设备：
```bash
curl http://localhost:8081/api/mqtt/devices/count
curl http://localhost:8081/api/mqtt/devices/sample
```

3. 手动测试单个设备连接：
```bash
# 获取设备凭证
device=$(cat data/devices-100.json | jq -r '.devices[0]')
username=$(echo "$device" | jq -r '.mqtt_username')
password=$(echo "$device" | jq -r '.mqtt_password')
client_id=$(echo "$device" | jq -r '.mqtt_client_id')

# 测试连接
curl "http://localhost:8081/api/mqtt/connect?username=$username&password=$password&clientId=$client_id"
```

### 集群健康检查失败

1. 检查集群节点是否可达：
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

2. 检查网络连通性：
```bash
docker network ls
docker network inspect endpoint
```

### 报告问题

如遇到问题，请收集以下信息：
- 测试报告文件：`reports/z-audience-e2e-report-*.json`
- Z-Audience 日志：`docker logs audience`
- 集群节点状态
