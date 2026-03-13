# Z-Chess Docker 测试集群

Docker Compose 配置文件和脚本，用于本地测试和验证 Z-Chess 功能。

## 快速开始

### 方式一：使用标准测试脚本（推荐）

```bash
cd /path/to/Z-Chess

# 启动标准测试集群
./scripts/test.sh docker start

# 查看状态
./scripts/test.sh docker status

# 停止测试集群
./scripts/test.sh docker stop
```

### 方式二：使用 test-cluster.sh

```bash
cd scripts/test/docker

# 启动标准测试集群
./test-cluster.sh start

# 快速测试（含 Audience 验证）
./quick-start-test.sh

# 停止测试集群
./test-cluster.sh stop
```

### 方式三：直接使用 docker-compose

```bash
cd scripts/test/docker

# 启动标准集群
docker compose up -d db-pg raft00

# 启动完整集群（含 Audience）
docker compose --profile test up -d

# 查看日志
docker compose logs -f raft00
```

## TLS MQTT 压力测试 ⭐

使用 `test-tls-mqtt.sh` 脚本进行 TLS 加密 MQTT 的压力测试：

```bash
cd scripts/test/docker

# 一键启动完整的 TLS MQTT 压力测试
./test-tls-mqtt.sh start

# 仅验证 TLS 连接（不跑压力测试）
./test-tls-mqtt.sh verify-only

# 查看实时日志
./test-tls-mqtt.sh logs

# 停止测试
./test-tls-mqtt.sh stop

# 获取帮助
./test-tls-mqtt.sh help
```

详细说明请参考 [TLS MQTT 测试指南](../TLS_MQTT_TEST_GUIDE.md)

## 文件说明

### 配置文件

| 文件 | 说明 |
|------|------|
| `docker-compose.yaml` | 标准测试集群配置 |
| `docker-compose-tls.yaml` | **TLS 加密 MQTT 测试集群** ⭐ |
| `.env` | 环境变量配置 |

### 脚本文件

| 脚本 | 功能 |
|------|------|
| `test-cluster.sh` | 标准测试集群管理脚本 |
| `quick-start-test.sh` | 快速启动并验证测试 |
| `verify-audience.sh` | Audience 压力测试验证 |
| `test-tls-mqtt.sh` | **TLS MQTT 一键测试脚本** ⭐ |

### 辅助脚本

| 脚本 | 功能 |
|------|------|
| `setup-network.sh` | 网络配置脚本 |
| `pull-images.sh` | 镜像拉取脚本 |

## 端口映射

### 标准集群

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | Raft00 | 主节点 API |
| 8081 | Raft01 | 节点1 API |
| 8082 | Raft02 | 节点2 API |
| 1883 | Raft00 | MQTT 明文 |
| 5432 | PostgreSQL | 数据库 |

### TLS 集群

| 端口 | 服务 | 说明 |
|------|------|------|
| 8080 | Raft00 | 主节点 API |
| 8883 | Raft00 | **MQTT TLS 加密** ⭐ |
| 1883 | Raft00 | MQTT 明文 |
| 5432 | PostgreSQL | 数据库 |

## 服务配置

### PostgreSQL

- 用户名: `z-chess`
- 密码: `z-chess`
- 数据库: `z-chess-db`

### Z-Chess Arena

- 默认绑定: `0.0.0.0`
- API 端口: `2808` (容器内)
- MQTT 端口: `1883` (容器内)
- MQTTS 端口: `8883` (容器内，启用 TLS 时)

## 验证测试

### 标准集群

```bash
# 检查健康状态
curl -s http://localhost:8080/api/health | jq

# 测试 MQTT
cd scripts/test/docker
docker compose exec audience-mqtt mosquitto_sub -h raft00 -p 1883 -t test/topic &
docker compose exec audience-mqtt mosquitto_pub -h raft00 -p 1883 -t test/topic -m "hello"
```

### TLS 集群

```bash
# 验证 TLS 端口
echo | openssl s_client -connect localhost:8883 -CAfile cert/ca-cert.pem

# 测试 MQTT TLS
cd scripts/test/docker
docker compose -f docker-compose-tls.yaml exec audience-tls mosquitto_pub \
    -h raft00 -p 8883 -t test/topic -m "hello tls" --cafile /app/cert/ca-cert.pem
```

## 常见问题

### 端口冲突

如果端口被占用，修改 `.env` 文件中的端口映射：

```bash
HOST_PORT=8080          # API 端口
HOST_MQTT_PORT=1883     # MQTT 端口
HOST_MQTTS_PORT=8883    # MQTTS 端口
HOST_PG_PORT=5432       # PostgreSQL 端口
```

### 镜像拉取失败

```bash
# 手动构建镜像
cd /path/to/Z-Chess
./scripts/build.sh docker
```

### TLS 连接失败

1. 检查证书是否生成
2. 检查证书有效期
3. 检查端口映射
4. 查看日志: `./test-tls-mqtt.sh logs`

## 相关链接

- [TLS MQTT 测试指南](../TLS_MQTT_TEST_GUIDE.md)
- [端到端测试](../e2e/README.md)
- [TLS 配置指南](../../../TLS_SETUP_GUIDE.md)
