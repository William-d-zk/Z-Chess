# Z-Chess 测试脚本

本目录包含 Z-Chess 的各种测试脚本和工具。

## 目录结构

```
test/
├── README.md                    # 本文件
├── TLS_MQTT_TEST_GUIDE.md       # TLS MQTT 压力测试指南 ⭐
├── docker/                      # Docker 测试集群
│   ├── README.md
│   ├── docker-compose.yaml      # 标准测试集群
│   ├── docker-compose-tls.yaml  # TLS MQTT 测试集群 ⭐
│   ├── test-cluster.sh
│   ├── quick-start-test.sh
│   ├── verify-audience.sh
│   └── test-tls-mqtt.sh         # TLS MQTT 测试脚本 ⭐
├── e2e/                         # 端到端测试
│   ├── e2e-test.sh
│   ├── register-devices.sh
│   └── ...
└── tls/                         # TLS 测试
    ├── generate-ssl-certs.sh
    ├── verify-mqtt-tls.sh       # MQTT TLS 验证 ⭐
    └── README.md
```

## 快速开始

### 标准测试集群

```bash
# 使用统一入口
./scripts/test.sh docker start
./scripts/test.sh docker status
./scripts/test.sh docker stop

# 或直接访问
cd scripts/test/docker
./test-cluster.sh start
```

### TLS MQTT 压力测试 (推荐)

```bash
cd scripts/test/docker

# 一键启动完整的 TLS MQTT 压力测试
./test-tls-mqtt.sh start

# 仅验证 TLS 连接
./test-tls-mqtt.sh verify-only

# 查看实时日志
./test-tls-mqtt.sh logs

# 停止测试
./test-tls-mqtt.sh stop
```

### TLS 证书管理

```bash
cd scripts/test/tls

# 生成测试证书
./generate-ssl-certs.sh dev changeit

# 验证 MQTT TLS 连接
./verify-mqtt-tls.sh
```

## 测试类型说明

### 1. 标准 Docker 测试集群

用于常规功能测试，包含：
- 3 节点 Raft 集群
- PostgreSQL 数据库
- Audience 压力测试客户端
- MQTT 明文端口 (1883)

### 2. TLS MQTT 测试集群 ⭐

用于验证 TLS 加密链路，包含：
- 3 节点 Raft 集群 (启用 TLS)
- PostgreSQL 数据库
- **Audience TLS 压力测试客户端**
- MQTT TLS 端口 (8883)
- 自动证书管理

**使用场景：**
- 验证 TLS 加密链路稳定性
- 测试 TLS 握手性能
- 验证证书配置正确性
- 压力测试加密通道

### 3. 端到端测试

完整业务流程测试，包含：
- 设备注册
- 消息收发
- 状态同步

### 4. TLS 独立测试

证书生成和验证工具。

## 服务访问地址

### 标准集群

| 服务 | 地址 | 说明 |
|------|------|------|
| Raft00 | http://localhost:8080 | 主节点 API |
| Raft01 | http://localhost:8081 | 节点1 API |
| Raft02 | http://localhost:8082 | 节点2 API |
| MQTT | localhost:1883 | 明文 MQTT |

### TLS 集群

| 服务 | 地址 | 说明 |
|------|------|------|
| Raft00 | http://localhost:8080 | 主节点 API |
| MQTT TLS | localhost:8883 | **TLS 加密 MQTT** ⭐ |
| Audience | http://localhost:8090 | 压力测试控制 |

## 详细文档

- [TLS MQTT 压力测试指南](TLS_MQTT_TEST_GUIDE.md) - 完整 TLS 测试教程
- [Docker 测试集群](docker/README.md) - 标准集群使用指南
- [TLS 配置指南](../../TLS_SETUP_GUIDE.md) - TLS/SSL 配置说明

## 常见问题

### Q: 如何仅测试 TLS 而不运行压力测试？

```bash
cd scripts/test/docker
./test-tls-mqtt.sh verify-only
```

### Q: 如何调整压力测试参数？

编辑 `docker-compose-tls.yaml`：

```yaml
environment:
  - Z_CHESS_PRESSURE_CONCURRENCY=100    # 并发数
  - Z_CHESS_PRESSURE_DURATION=PT300S    # 测试时长
```

### Q: 如何使用自定义证书？

```bash
# 替换证书文件
cp /path/to/your/certs/* scripts/test/docker/cert/

# 重启服务
cd scripts/test/docker
./test-tls-mqtt.sh stop
./test-tls-mqtt.sh start
```

### Q: 测试失败如何排查？

```bash
# 查看详细日志
./test-tls-mqtt.sh logs

# 验证 TLS 端口
echo | openssl s_client -connect localhost:8883 -CAfile cert/ca-cert.pem

# 检查证书
openssl x509 -in cert/ca-cert.pem -noout -text
```

## 更新日志

### 2024-03-11

- ✅ 新增 TLS MQTT 压力测试集群
- ✅ 新增 `test-tls-mqtt.sh` 一键测试脚本
- ✅ 新增 `verify-mqtt-tls.sh` TLS 验证工具
- ✅ 新增 TLS_MQTT_TEST_GUIDE.md 完整教程
- ✅ 优化目录结构，测试脚本统一归类
