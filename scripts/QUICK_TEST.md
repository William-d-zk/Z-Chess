# Z-Chess Docker 集群快速测试

## 🚀 5 分钟快速开始

### 1. 环境准备

```bash
# 检查 Docker
docker --version
docker compose version

# 进入脚本目录
cd scripts
```

### 2. 启动集群

```bash
./bin/quick-start.sh
```

预期输出：
```
🚀 Z-Chess 单机测试集群启动脚本
================================
✓ 检测到 ARM64/AMD64 架构
📁 创建数据目录...
🔐 设置目录权限...
🧹 清理旧容器...
🐘 启动 PostgreSQL...
⏳ 等待 PostgreSQL 就绪...
✓ PostgreSQL 就绪

🎯 启动 Raft 集群...
⏳ 等待 Raft 服务启动 (15 秒)...

✅ 集群启动完成！

================================
服务地址:
  - Raft00 API:  http://localhost:8080
  - Raft00 Debug: localhost:8000
  - MQTT:        localhost:1883
  - PostgreSQL:  localhost:5432
...
```

### 3. 健康检查

```bash
./bin/health-check.sh
```

预期输出：
```
🏥 Z-Chess 集群健康检查
======================

1️⃣  容器状态检查
----------------
NAMES      STATUS         PORTS
raft00     Up 2 minutes   0.0.0.0:1883->1883/tcp, ...
db-pg      Up 2 minutes   0.0.0.0:5432->5432/tcp

2️⃣  端口检查
------------
检查 PostgreSQL (localhost:5432) ... ✓ 开放
检查 HTTP API (localhost:8080) ... ✓ 开放
...

🎉 所有检查通过！集群运行正常
```

### 4. 查看日志

```bash
# 实时查看所有日志
./bin/view-logs.sh

# 或查看特定服务
docker logs -f raft00
docker logs -f db-pg
```

### 5. 停止集群

```bash
./bin/stop-cluster.sh
```

---

## 🧪 手动验证步骤

### 测试 1: HTTP API

```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 集群状态
curl http://localhost:8080/api/cluster/status

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

### 测试 2: PostgreSQL

```bash
# 连接数据库
docker exec -it db-pg psql -U postgres -c "\l"

# 查看版本
docker exec -it db-pg psql -U postgres -c "SELECT version();"
```

### 测试 3: MQTT（可选）

```bash
# 安装 mosquitto 客户端
# Mac: brew install mosquitto
# Ubuntu: apt install mosquitto-clients

# 订阅测试
mosquitto_sub -h localhost -p 1883 -t "test/#" -v &

# 发布消息
mosquitto_pub -h localhost -p 1883 -t "test/hello" -m "world"
```

---

## 🔍 故障排查

### 问题 1: PostgreSQL 权限错误

```bash
# 错误: data directory has wrong ownership
# 解决:
sudo chown -R 999:999 ~/Services/db/postgresql17
sudo chmod 700 ~/Services/db/postgresql17
```

### 问题 2: 端口被占用

```bash
# 查找占用进程
lsof -i :8080
lsof -i :1883

# 结束进程或修改 docker-compose 端口映射
```

### 问题 3: 容器启动失败

```bash
# 查看详细日志
docker logs raft00
docker logs db-pg

# 检查资源
docker system df
docker stats --no-stream
```

### 问题 4: 网络问题

```bash
# 检查网络
docker network ls
docker network inspect z-chess-endpoint

# 测试容器间连通性
docker exec raft00 ping db-pg
docker exec raft00 ping raft01
```

---

## 📊 性能基准测试

```bash
# 安装 hey (HTTP 压测工具)
# Mac: brew install hey
# Linux: go install github.com/rakyll/hey@latest

# 压测命令
hey -n 10000 -c 100 http://localhost:8080/actuator/health

# 结果分析
# 平均响应时间 < 10ms: 优秀
# 平均响应时间 10-50ms: 良好
# 平均响应时间 > 50ms: 需要优化
```

---

## 📁 生成的文件

启动后会生成以下目录结构：

```
~/
├── Z-Chess/
│   ├── raft00/          # Raft 节点 0 数据
│   ├── raft01/          # Raft 节点 1 数据
│   └── raft02/          # Raft 节点 2 数据
└── Services/
    └── db/
        └── postgresql17/ # PostgreSQL 数据
```

---

## 🎉 测试完成

集群运行正常后，你可以：

1. **开发调试**: 使用 `localhost:8080` 访问 API
2. **MQTT 测试**: 使用 `localhost:1883` 连接 MQTT
3. **数据库查看**: 使用 `localhost:5432` 连接 PostgreSQL
4. **性能测试**: 运行压力测试脚本

---

**Happy Testing!** 🚀
