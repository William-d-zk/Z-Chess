#!/bin/bash
# 清理数据目录并重新启动

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker/docker-compose.yaml"

echo "🧹 停止并清理容器..."
docker compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true

echo "🗑️  清理数据目录..."
rm -rf ~/Services/db/postgresql17
mkdir -p ~/Services/db/postgresql17/log

echo "🔧 设置权限..."
if command -v sudo > /dev/null 2>&1; then
    sudo chown -R 999:999 ~/Services/db/postgresql17 2>/dev/null || true
fi

echo "🚀 重新启动服务..."
docker compose -f "$COMPOSE_FILE" up --build -d

echo "⏳ 等待 20 秒..."
sleep 20

echo "📊 检查状态..."
docker compose -f "$COMPOSE_FILE" ps
docker compose -f "$COMPOSE_FILE" logs --tail=20 db-pg
