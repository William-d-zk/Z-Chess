#!/bin/bash
# 清理数据目录并重新启动

set -e

PLATFORM=${1:-aarch64}
cd "$(dirname "$0")/$PLATFORM"

echo "🧹 停止并清理容器..."
docker-compose down -v

echo "🗑️  清理数据目录..."
sudo rm -rf ~/Services/db/postgresql17
mkdir -p ~/Services/db/postgresql17/log

echo "🔧 设置权限..."
sudo chown -R 999:999 ~/Services/db/postgresql17

echo "🚀 重新启动服务..."
docker-compose up --build -d

echo "⏳ 等待 20 秒..."
sleep 20

echo "📊 检查状态..."
docker-compose ps
docker-compose logs --tail=20 db-pg
