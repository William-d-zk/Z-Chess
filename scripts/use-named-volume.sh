#!/bin/bash
# 使用命名卷方案（避免权限问题）

PLATFORM=${1:-aarch64}
cd "$(dirname "$0")/$PLATFORM"

echo "🧹 清理旧环境..."
docker-compose down -v --remove-orphans 2>/dev/null || true
docker volume rm -f ${PLATFORM}_postgres_data 2>/dev/null || true
sudo rm -rf ~/Services/db/postgresql17

echo "🔧 修改配置使用命名卷..."
# 备份并修改
cp Docker-Compose.yaml Docker-Compose.yaml.bak
sed -i '' \
    -e 's|type: bind|type: volume|g' \
    -e 's|source: ~/Services/db/postgresql17|source: postgres_data|g' \
    Docker-Compose.yaml

echo "🚀 启动服务..."
docker-compose up --build -d

echo "⏳ 等待 30 秒..."
sleep 30

echo "📊 查看日志..."
docker-compose logs db-pg

echo ""
echo "✅ 如果成功，可以恢复原始配置:"
echo "   cp Docker-Compose.yaml.bak Docker-Compose.yaml"
