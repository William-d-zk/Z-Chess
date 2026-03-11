# Z-Chess Docker 测试集群

本文档介绍如何使用 Docker 测试集群进行 Z-Chess 的功能验证和压力测试。

## 架构概览

测试集群包含以下组件：

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker 测试集群                           │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   raft00    │  │   raft01    │  │   raft02    │         │
│  │  (主节点)    │  │             │  │             │         │
│  │  HTTP:8080  │  │  HTTP:8081  │  │  HTTP:8082  │         │
│  │  MQTT:1883  │  │  MQTT:1884  │  │  MQTT:1885  │         │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘         │
│         │                │                │                 │
│         └────────────────┼────────────────┘                 │
│                          │                                  │
│  ┌───────────────────────┴───────────────────────┐         │
│  │            Z-Chess Raft 集群                 │         │
│  └───────────────────────┬───────────────────────┘         │
│                          │                                  │
│         ┌────────────────┼────────────────┐                 │
│         │                │                │                 │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐         │
│  │  audience   │  │  tls-tester │  │   db-pg     │         │
│  │  压力测试    │  │  TLS测试    │  │  PostgreSQL │         │
│  │  HTTP:8090  │  │             │  │  Port:5432  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- ARM64 架构 (Apple Silicon 或 ARM 服务器)
- 至少 8GB 可用内存
- 至少 20GB 可用磁盘空间

### 2. 启动测试集群

```bash
cd scripts/test/docker

# 启动完整测试集群（包含 Audience 测试客户端）
./test-cluster.sh start

# 查看集群状态
./test-cluster.sh status
```

### 3. 访问服务

| 服务 | 地址 | 说明 |
|------|------|------|
| Raft00 | http://localhost:8080 | 主节点 API |
| Raft01 | http://localhost:8081 | 节点1 API |
| Raft02 | http://localhost:8082 | 节点2 API |
| Audience | http://localhost:8090 | 测试控制 API |
| PostgreSQL | localhost:5432 | 数据库 |
| MQTT (Raft00) | localhost:1883 | MQTT Broker |
| MQTT (Raft01) | localhost:1884 | MQTT Broker |
| MQTT (Raft02) | localhost:1885 | MQTT Broker |

## 测试功能

### MQTT 压力测试

Audience 服务会自动对 Raft 集群进行 MQTT 压力测试：

```bash
# 查看压力测试配置
curl http://localhost:8090/actuator/configprops

# 查看测试状态
curl http://localhost:8090/actuator/health
```

默认测试参数：
- 目标：raft00:1883
- 并发连接：100
- 测试时长：60秒
- 协议：MQTT
- 消息大小：256字节

### TLS 加密连接测试

```bash
# 启动 TLS 测试环境
./test-cluster.sh tls-test

# 进入 TLS 测试容器
docker compose -p z-chess-test -f docker-compose.yaml exec tls-tester sh

# 在容器内测试 TLS 连接
openssl s_client -connect raft00:1883 -CAfile /app/cert/ca-cert.pem
```

## 管理命令

```bash
# 启动集群
./test-cluster.sh start

# 停止集群
./test-cluster.sh stop

# 重启集群
./test-cluster.sh restart

# 查看状态
./test-cluster.sh status

# 查看日志
./test-cluster.sh logs [服务名]
./test-cluster.sh logs raft00
./test-cluster.sh logs audience

# 运行压力测试
./test-cluster.sh test

# 运行 TLS 测试
./test-cluster.sh tls-test

# 重新构建镜像
./test-cluster.sh build

# 清理环境（删除所有数据）
./test-cluster.sh clean
```

## 配置文件

### Audience 测试配置

编辑 `Z-Audience/src/main/resources/application-docker.yml`：

```yaml
z:
  chess:
    pressure:
      target:
        host: raft00      # 测试目标主机
        port: 1883        # MQTT 端口
      concurrency: 100     # 并发连接数
      duration: PT60S      # 测试时长
      protocol: mqtt       # 测试协议
```

### 环境变量

通过环境变量覆盖配置：

```yaml
# docker-compose 中设置
environment:
  - Z_CHESS_PRESSURE_TARGET_HOST=raft01
  - Z_CHESS_PRESSURE_CONCURRENCY=200
  - Z_CHESS_PRESSURE_DURATION=PT120S
  - Z_CHESS_PRESSURE_PROTOCOL=mqtt
```

## 日志查看

```bash
# 实时查看 Audience 测试日志
docker logs -f audience

# 查看所有服务日志
./test-cluster.sh logs

# 查看特定服务日志
./test-cluster.sh logs raft00
```

日志文件位置：
- `~/Z-Chess/logs/raft00/` - Raft00 日志
- `~/Z-Chess/logs/raft01/` - Raft01 日志
- `~/Z-Chess/logs/raft02/` - Raft02 日志
- `~/Z-Chess/logs/audience/` - Audience 日志

## 网络配置

测试集群使用两个 Docker 网络：

- **endpoint** (172.30.10.0/24): 服务端点网络
- **gate** (172.30.11.0/24): 网关网络

服务 IP 分配：
- db-pg: 172.30.10.254
- raft00: 172.30.10.110
- raft01: 172.30.10.111
- raft02: 172.30.10.112
- audience: 172.30.10.120
- tls-tester: 172.30.10.121

## 故障排查

### 1. 容器启动失败

```bash
# 检查日志
docker logs raft00
docker logs audience

# 检查资源使用
docker stats
```

### 2. Raft 集群无法选举

```bash
# 检查节点间网络连通性
docker exec -it raft00 ping raft01
docker exec -it raft00 ping raft02

# 检查 Raft 日志
docker logs raft00 | grep -i raft
docker logs raft01 | grep -i raft
docker logs raft02 | grep -i raft
```

### 3. 压力测试连接失败

```bash
# 检查 MQTT 端口是否开放
docker exec -it audience nc -zv raft00 1883

# 检查 Audience 配置
docker exec -it audience env | grep Z_CHESS
```

### 4. 数据库连接问题

```bash
# 检查 PostgreSQL 状态
docker compose -p z-chess-test ps db-pg

# 测试数据库连接
docker exec -it raft00 nc -zv db-pg.isahl.com 5432
```

## 性能调优

### 调整 JVM 参数

```yaml
# docker-compose.yaml
environment:
  - JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0
```

### 调整资源限制

```yaml
# docker-compose.yaml
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 2G
    reservations:
      cpus: '1.0'
      memory: 1G
```

### 调整压力测试参数

```bash
# 高并发测试 (500 连接，5分钟)
./test-cluster.sh stop
export Z_CHESS_PRESSURE_CONCURRENCY=500
export Z_CHESS_PRESSURE_DURATION=PT5M
./test-cluster.sh start
```

## 高级用法

### 自定义测试镜像

```bash
# 修改 Dockerfile.audience 后重新构建
docker build --platform linux/arm64 -t z-chess-audience:custom \
    -f scripts/aarch64/Dockerfile.audience .
```

### 添加自定义测试脚本

```bash
# 创建测试脚本目录
mkdir -p scripts/aarch64/test-scripts

# 编写测试脚本
cat > scripts/aarch64/test-scripts/mqtt-stress.sh << 'EOF'
#!/bin/bash
# 自定义 MQTT 压力测试
mosquitto_pub -h raft00 -p 1883 -t test/topic -m "test message" -r
echo "测试完成"
EOF

# 挂载到容器
volumes:
  - ./test-scripts:/app/scripts
```

### 集成 CI/CD

```yaml
# .github/workflows/docker-test.yml
- name: Run Docker Test Cluster
  run: |
    cd scripts/test/docker
    ./test-cluster.sh start
    sleep 60
    ./test-cluster.sh test
    ./test-cluster.sh stop
```

## 安全注意事项

1. **生产环境禁用**: 此测试集群仅用于开发和测试，不要用于生产环境
2. **默认密码**: 数据库和证书使用默认密码，测试环境请修改
3. **网络隔离**: 测试集群使用独立 Docker 网络，与主机隔离
4. **数据持久化**: 数据存储在 `~/Z-Chess/`，清理前请备份重要数据

## 参考文档

- [TLS 配置指南](TLS_SETUP_GUIDE.md)
- [集群配置检查](CLUSTER_CONFIG_CHECK.md)
- [Docker 升级指南](DOCKER_UPGRADE_GUIDE.md)

## 问题反馈

遇到问题请提交 Issue 或联系开发团队。
