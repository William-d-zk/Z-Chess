# Z-Chess 单机测试 Docker 集群指南

## 一、前置准备

### 1.1 系统要求

| 组件 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 4 核 | 8 核 |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 20 GB SSD | 50 GB SSD |
| Docker | 20.10+ | 24.0+ |
| Docker Compose | 2.0+ | 2.20+ |

### 1.2 检查 Docker 环境

```bash
# 检查 Docker 版本
docker --version
docker compose version

# 检查可用资源
docker system info | grep -E " CPUs|Total Memory"
```

### 1.3 创建必要目录

```bash
#!/bin/bash
# setup-local-test.sh

# 创建数据目录
mkdir -p ~/Services/db/postgresql17/log
mkdir -p ~/Z-Chess/raft00/logs
mkdir -p ~/Z-Chess/raft01/logs
mkdir -p ~/Z-Chess/raft02/logs

# 设置 PostgreSQL 目录权限 (999 是 postgres 用户 UID)
sudo chown -R 999:999 ~/Services/db/postgresql17
sudo chmod 700 ~/Services/db/postgresql17

# 验证目录
tree -L 3 ~/Z-Chess ~/Services
```

---

## 二、快速启动（使用预构建镜像）

### 2.1 一键启动脚本

```bash
#!/bin/bash
# quick-start.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 Z-Chess 单机测试集群启动脚本"
echo "================================"

# 选择架构
ARCH=$(uname -m)
if [ "$ARCH" == "aarch64" ] || [ "$ARCH" == "arm64" ]; then
    COMPOSE_FILE="aarch64/Docker-Compose.yaml"
    echo "✓ 检测到 ARM64 架构"
else
    COMPOSE_FILE="amd64/Docker-Compose.yaml"
    echo "✓ 检测到 AMD64 架构"
fi

# 清理旧容器
echo "🧹 清理旧容器..."
docker compose -f "$COMPOSE_FILE" down --volumes --remove-orphans 2>/dev/null || true

# 创建网络（如果不存在）
echo "🌐 创建 Docker 网络..."
docker network create z-chess-endpoint 2>/dev/null || true
docker network create z-chess-gate 2>/dev/null || true

# 启动 PostgreSQL 先启动
echo "🐘 启动 PostgreSQL..."
docker compose -f "$COMPOSE_FILE" up -d db-pg

# 等待 PostgreSQL 就绪
echo "⏳ 等待 PostgreSQL 就绪..."
sleep 10
docker compose -f "$COMPOSE_FILE" exec -T db-pg pg_isready -U postgres -d postgres

# 启动 Raft 集群
echo "🎯 启动 Raft 集群..."
docker compose -f "$COMPOSE_FILE" up -d raft10 raft11 raft12

# 等待服务启动
echo "⏳ 等待服务启动..."
sleep 15

echo ""
echo "✅ 集群启动完成！"
echo ""
echo "服务地址:"
echo "  - Raft00: http://localhost:8080 (API)"
echo "  - MQTT:   localhost:1883"
echo "  - Debug:  localhost:8000"
echo "  - DB:     localhost:5432"
echo ""
echo "查看日志: ./view-logs.sh"
echo "停止集群: ./stop-cluster.sh"
```

### 2.2 查看日志脚本

```bash
#!/bin/bash
# view-logs.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ARCH=$(uname -m)
if [ "$ARCH" == "aarch64" ] || [ "$ARCH" == "arm64" ]; then
    COMPOSE_FILE="aarch64/Docker-Compose.yaml"
else
    COMPOSE_FILE="amd64/Docker-Compose.yaml"
fi

echo "📋 Z-Chess 集群日志查看"
echo "======================"
echo ""
echo "选项:"
echo "  1) 所有服务日志"
echo "  2) 仅 PostgreSQL"
echo "  3) 仅 raft00"
echo "  4) 仅 raft01"
echo "  5) 仅 raft02"
echo "  6) 退出"
echo ""
read -p "选择 [1-6]: " choice

case $choice in
    1)
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100
        ;;
    2)
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100 db-pg
        ;;
    3)
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100 raft10
        ;;
    4)
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100 raft11
        ;;
    5)
        docker compose -f "$COMPOSE_FILE" logs -f --tail=100 raft12
        ;;
    6)
        exit 0
        ;;
    *)
        echo "无效选择"
        exit 1
        ;;
esac
```

### 2.3 停止集群脚本

```bash
#!/bin/bash
# stop-cluster.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ARCH=$(uname -m)
if [ "$ARCH" == "aarch64" ] || [ "$ARCH" == "arm64" ]; then
    COMPOSE_FILE="aarch64/Docker-Compose.yaml"
else
    COMPOSE_FILE="amd64/Docker-Compose.yaml"
fi

echo "🛑 停止 Z-Chess 集群..."
docker compose -f "$COMPOSE_FILE" down

echo "✅ 集群已停止"
echo ""
read -p "是否删除数据卷? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf ~/Z-Chess/raft00/* ~/Z-Chess/raft01/* ~/Z-Chess/raft02/*
    rm -rf ~/Services/db/postgresql17/*
    echo "🗑️  数据已清理"
fi
```

---

## 三、从源码构建并启动

### 3.1 构建脚本

```bash
#!/bin/bash
# build-and-run.sh

set -e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT_DIR="$PROJECT_ROOT/scripts"
cd "$PROJECT_ROOT"

echo "🔨 构建 Z-Chess 项目..."
echo "======================"

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装"
    exit 1
fi

# 构建 Z-Board (基础模块)
echo "📦 构建 Z-Board..."
cd "$PROJECT_ROOT/Z-Board"
mvn clean install -P dev -DskipTests

# 构建整个项目
echo "📦 构建整个项目..."
cd "$PROJECT_ROOT"
mvn clean package -P dev -DskipTests -pl Z-Arena -am

echo ""
echo "✅ 构建完成！"

# 启动集群
cd "$SCRIPT_DIR"
./quick-start.sh
```

---

## 四、验证步骤

### 4.1 健康检查脚本

```bash
#!/bin/bash
# health-check.sh

set -e

echo "🏥 Z-Chess 集群健康检查"
echo "======================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

# 检查函数
check_service() {
    local name=$1
    local url=$2
    
    echo -n "检查 $name ... "
    if curl -s -o /dev/null -w "%{http_code}" "$url" | grep -q "200\|401"; then
        echo -e "${GREEN}✓ 正常${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ 异常${NC}"
        ((FAIL++))
    fi
}

check_port() {
    local name=$1
    local host=$2
    local port=$3
    
    echo -n "检查 $name ($host:$port) ... "
    if nc -z "$host" "$port" 2>/dev/null; then
        echo -e "${GREEN}✓ 端口开放${NC}"
        ((PASS++))
    else
        echo -e "${RED}✗ 端口关闭${NC}"
        ((FAIL++))
    fi
}

# 1. 检查容器运行状态
echo "1️⃣  容器状态检查"
echo "----------------"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(raft|db-pg)" || true
echo ""

# 2. 端口检查
echo "2️⃣  端口检查"
echo "------------"
check_port "PostgreSQL" "localhost" "5432"
check_port "HTTP API" "localhost" "8080"
check_port "Debug" "localhost" "8000"
check_port "MQTT" "localhost" "1883"
echo ""

# 3. HTTP API 健康检查
echo "3️⃣  HTTP API 检查"
echo "----------------"
check_service "Raft00 Actuator" "http://localhost:8080/actuator/health"
check_service "Raft00 API" "http://localhost:8080/api/cluster/status" || true
echo ""

# 4. PostgreSQL 连接检查
echo "4️⃣  PostgreSQL 连接检查"
echo "-----------------------"
if docker exec db-pg pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "PostgreSQL ... ${GREEN}✓ 就绪${NC}"
    ((PASS++))
else
    echo -e "PostgreSQL ... ${RED}✗ 未就绪${NC}"
    ((FAIL++))
fi
echo ""

# 5. Raft 集群状态检查（通过 logs）
echo "5️⃣  Raft 状态检查"
echo "----------------"
for node in raft00 raft01 raft02; do
    echo -n "$node 角色: "
    role=$(docker logs "$node" 2>&1 | grep -oP "state:\s*\w+" | tail -1 || echo "unknown")
    if [ "$role" != "unknown" ]; then
        echo -e "${GREEN}$role${NC}"
        ((PASS++))
    else
        echo -e "${YELLOW}启动中或日志已轮转${NC}"
    fi
done
echo ""

# 汇总
echo "======================"
echo "检查结果:"
echo -e "  通过: ${GREEN}$PASS${NC}"
echo -e "  失败: ${RED}$FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}🎉 所有检查通过！集群运行正常${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  部分检查未通过，请查看日志${NC}"
    echo "查看日志: ./view-logs.sh"
    exit 1
fi
```

### 4.2 功能测试脚本

```bash
#!/bin/bash
# functional-test.sh

set -e

echo "🧪 Z-Chess 功能测试"
echo "=================="
echo ""

BASE_URL="http://localhost:8080"

# 1. 测试集群状态 API
echo "1️⃣  测试集群状态 API"
echo "-------------------"
curl -s "$BASE_URL/api/cluster/status" | jq . || echo "API 响应异常"
echo ""

# 2. 测试 MQTT 连接
echo "2️⃣  测试 MQTT 连接"
echo "-----------------"
if command -v mosquitto_pub &> /dev/null; then
    # 发布测试消息
    mosquitto_pub -h localhost -p 1883 -t "test/topic" -m "hello" -q 1 || echo "MQTT 连接失败"
    echo "MQTT 消息发送完成"
else
    echo "跳过 (未安装 mosquitto clients)"
fi
echo ""

# 3. 测试数据库连接
echo "3️⃣  测试数据库连接"
echo "-----------------"
if command -v psql &> /dev/null; then
    psql -h localhost -p 5432 -U postgres -c "SELECT version();" || echo "PostgreSQL 连接失败"
else
    docker exec -i db-pg psql -U postgres -c "SELECT version();"
fi
echo ""

# 4. 压力测试（可选）
echo "4️⃣  压力测试 (100 请求)"
echo "---------------------"
seq 100 | xargs -n1 -P10 -I{} curl -s -o /dev/null -w "%{http_code}\n" "$BASE_URL/actuator/health" | sort | uniq -c
echo ""

echo "✅ 功能测试完成"
```

---

## 五、常用操作命令

### 5.1 容器管理

```bash
# 查看所有容器
docker compose -f aarch64/Docker-Compose.yaml ps

# 进入容器内部
docker exec -it raft00 /bin/sh
docker exec -it db-pg psql -U postgres

# 查看资源使用
docker stats raft00 raft01 raft12 db-pg

# 重启单个服务
docker compose -f aarch64/Docker-Compose.yaml restart raft10

# 查看网络
docker network inspect z-chess-endpoint
docker network inspect z-chess-gate
```

### 5.2 日志管理

```bash
# 实时查看日志
docker compose -f aarch64/Docker-Compose.yaml logs -f

# 查看最后 100 行
docker compose -f aarch64/Docker-Compose.yaml logs --tail=100

# 查看特定时间段的日志
docker compose -f aarch64/Docker-Compose.yaml logs --since="2024-01-01T00:00:00"

# 导出日志
docker compose -f aarch64/Docker-Compose.yaml logs > z-chess-logs-$(date +%Y%m%d).txt
```

### 5.3 数据管理

```bash
# 备份 PostgreSQL
docker exec db-pg pg_dumpall -U postgres > backup-$(date +%Y%m%d).sql

# 恢复 PostgreSQL
cat backup-20240101.sql | docker exec -i db-pg psql -U postgres

# 备份 Raft 数据
tar czvf raft-backup-$(date +%Y%m%d).tar.gz ~/Z-Chess/raft00 ~/Z-Chess/raft01 ~/Z-Chess/raft02
```

---

## 六、常见问题排查

### 6.1 启动失败

```bash
# 问题: PostgreSQL 权限错误
# 解决:
sudo chown -R 999:999 ~/Services/db/postgresql17
sudo chmod 700 ~/Services/db/postgresql17

# 问题: 端口被占用
# 解决:
sudo lsof -i :8080  # 查看占用进程
sudo kill -9 <PID>   # 结束进程

# 或使用不同端口
docker compose -f aarch64/Docker-Compose.yaml -p z-chess-2 up -d
```

### 6.2 网络问题

```bash
# 检查容器间网络连通性
docker exec -it raft00 ping raft01
docker exec -it raft00 ping db-pg

# 检查 hosts 配置
docker exec raft00 cat /etc/hosts

# 重新创建网络
docker network rm z-chess-endpoint z-chess-gate 2>/dev/null || true
docker compose -f aarch64/Docker-Compose.yaml up -d
```

### 6.3 性能问题

```bash
# 查看资源限制
docker system info | grep -i memory
docker system info | grep -i cpu

# 调整 Docker Desktop 资源（Mac/Windows）
# Preferences -> Resources

# 限制容器资源
docker update --memory=4g --cpus=2 raft00
```

---

## 七、清理环境

```bash
#!/bin/bash
# cleanup.sh

echo "🧹 清理 Z-Chess 测试环境"
echo "========================"

# 停止并删除容器
docker compose -f aarch64/Docker-Compose.yaml down --volumes --remove-orphans

# 删除镜像（可选）
read -p "是否删除镜像? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker rmi img.z-chess.arena.aarch64:2.0 isahl/postgres:17.2-arm64 2>/dev/null || true
fi

# 清理数据（可选）
read -p "是否删除数据目录? [y/N] " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -rf ~/Z-Chess ~/Services
    echo "🗑️  数据已删除"
fi

echo "✅ 清理完成"
```

---

## 八、快速参考

| 命令 | 说明 |
|------|------|
| `./quick-start.sh` | 快速启动集群 |
| `./stop-cluster.sh` | 停止集群 |
| `./view-logs.sh` | 查看日志 |
| `./health-check.sh` | 健康检查 |
| `./functional-test.sh` | 功能测试 |
| `./cleanup.sh` | 清理环境 |

---

**测试完成！** 🎉
