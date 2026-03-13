#!/bin/bash
#
# Z-Audience Docker 镜像验证脚本
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="z-chess-test-audience"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "========================================"
echo "   Z-Audience Docker 镜像验证"
echo "========================================"
echo -e "${NC}"

# 1. 检查镜像是否存在
echo -e "${BLUE}[1/6] 检查镜像...${NC}"
if docker image inspect z-chess-audience:2.0-arm64 &> /dev/null; then
    echo -e "${GREEN}✓ Audience 镜像存在${NC}"
    docker images z-chess-audience:2.0-arm64 --format "  大小: {{.Size}}, 创建时间: {{.CreatedAt}}"
else
    echo -e "${YELLOW}⚠ Audience 镜像不存在，开始构建...${NC}"
    cd "${SCRIPT_DIR}/../.."
    docker build --platform linux/arm64 -t z-chess-audience:2.0-arm64 \
        -f docker/test/Dockerfile.audience .
    echo -e "${GREEN}✓ 镜像构建完成${NC}"
fi

# 2. 检查镜像层
echo -e "${BLUE}[2/6] 检查镜像层...${NC}"
docker history z-chess-audience:2.0-arm64 --format "table {{.CreatedBy}}\t{{.Size}}" | head -10
echo ""

# 3. 测试运行容器
echo -e "${BLUE}[3/6] 测试运行容器...${NC}"
docker run --rm --name audience-test -d \
    -p 18090:8080 \
    -e SPRING_PROFILES_ACTIVE=docker \
    z-chess-audience:2.0-arm64

# 等待容器启动
sleep 10

# 检查容器状态
if docker ps | grep -q "audience-test"; then
    echo -e "${GREEN}✓ 容器运行正常${NC}"
else
    echo -e "${RED}✗ 容器启动失败${NC}"
    docker logs audience-test 2>/dev/null || true
    exit 1
fi

# 4. 检查健康检查
echo -e "${BLUE}[4/6] 检查健康检查...${NC}"
health_status=$(docker inspect --format='{{.State.Health.Status}}' audience-test 2>/dev/null || echo "N/A")
if [ "$health_status" = "healthy" ] || [ "$health_status" = "starting" ]; then
    echo -e "${GREEN}✓ 健康检查状态: $health_status${NC}"
else
    echo -e "${YELLOW}⚠ 健康检查状态: $health_status${NC}"
fi

# 5. 检查应用端口
echo -e "${BLUE}[5/6] 检查应用端口...${NC}"
if nc -zv localhost 18090 2>/dev/null; then
    echo -e "${GREEN}✓ 端口 18090 可访问${NC}"
else
    echo -e "${YELLOW}⚠ 端口 18090 暂不可访问（可能需要更多启动时间）${NC}"
fi

# 6. 检查日志
echo -e "${BLUE}[6/6] 检查应用日志...${NC}"
echo "最近 20 行日志:"
docker logs --tail=20 audience-test 2>&1 | grep -E "Started|Listening|Error|Exception" | head -10 || echo "  (暂无关键日志)"

# 清理
echo ""
echo -e "${BLUE}清理测试容器...${NC}"
docker stop audience-test 2>/dev/null || true
docker rm audience-test 2>/dev/null || true
echo -e "${GREEN}✓ 测试容器已清理${NC}"

echo ""
echo -e "${GREEN}========================================"
echo "   Z-Audience 镜像验证完成"
echo "========================================${NC}"
