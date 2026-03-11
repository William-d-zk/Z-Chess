#!/bin/bash
# Docker Compose 测试脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLATFORM=${1:-amd64}  # 默认 amd64，可传 aarch64

echo "======================================"
echo "Z-Chess Docker Compose 测试脚本"
echo "平台: $PLATFORM"
echo "======================================"
echo ""

# 检查 docker 和 docker-compose
echo "[1/7] 检查 Docker 环境..."
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装"
    exit 1
fi
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose 未安装"
    exit 1
fi
echo "✅ Docker 环境正常"
echo ""

# 进入目录
cd "$SCRIPT_DIR/$PLATFORM"
echo "[2/7] 当前目录: $(pwd)"
echo ""

# 准备数据目录
echo "[3/7] 准备数据目录..."
mkdir -p ~/Services/db/postgresql17/log
mkdir -p ~/Z-Chess/raft00 ~/Z-Chess/raft01 ~/Z-Chess/raft02
mkdir -p ~/Z-Chess/logs/raft00 ~/Z-Chess/logs/raft01 ~/Z-Chess/logs/raft02
echo "✅ 数据目录准备完成"
echo ""

# 构建并启动
echo "[4/7] 构建并启动服务..."
docker-compose down -v 2>/dev/null || true
docker-compose up --build -d
echo "✅ 服务启动命令已执行"
echo ""

# 等待服务启动
echo "[5/7] 等待服务启动 (30秒)..."
sleep 5
echo "  - 已等待 5 秒"
docker-compose ps
echo ""
sleep 10
echo "  - 已等待 15 秒"
docker-compose ps
echo ""
sleep 15
echo "  - 已等待 30 秒"
echo ""

# 健康检查
echo "[6/7] 执行健康检查..."
echo ""

# 检查 PostgreSQL
echo "  → 检查 PostgreSQL..."
if docker-compose exec -T db-pg pg_isready -U chess -d isahl_9.x > /dev/null 2>&1; then
    echo "    ✅ PostgreSQL 健康"
    docker-compose exec -T db-pg psql -U chess -d isahl_9.x -c "SELECT version();" 2>/dev/null | head -3
else
    echo "    ❌ PostgreSQL 未就绪"
    docker-compose logs --tail=20 db-pg
fi
echo ""

# 检查数据库和 schema
echo "  → 检查数据库和 schema..."
docker-compose exec -T db-pg psql -U chess -d isahl_9.x -c "\l" 2>/dev/null | grep isahl || echo "    ⚠️ 数据库列表查询失败"
docker-compose exec -T db-pg psql -U chess -d isahl_9.x -c "\dn" 2>/dev/null || echo "    ⚠️ Schema 查询失败"
echo ""

# 检查应用节点
echo "  → 检查应用节点 raft00..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "    ✅ raft00 健康检查通过"
    curl -s http://localhost:8080/actuator/health | head -5
else
    echo "    ⚠️ raft00 健康检查失败或尚未就绪"
    docker-compose logs --tail=10 raft00
fi
echo ""

# 显示状态
echo "[7/7] 服务状态汇总"
echo "======================================"
docker-compose ps
echo ""
echo "======================================"
echo "测试完成！"
echo ""
echo "常用命令:"
echo "  查看日志:     docker-compose logs -f [服务名]"
echo "  停止服务:     docker-compose down"
echo "  重启服务:     docker-compose restart"
echo "  进入容器:     docker-compose exec db-pg sh"
echo ""
