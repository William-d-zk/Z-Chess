#!/bin/bash
#
# Z-Chess Docker 测试集群快速启动脚本
# 一键启动测试环境并验证
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="z-chess-test"
COMPOSE_FILE="${SCRIPT_DIR}/Docker-Compose-Test.yaml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}"
echo "========================================"
echo "   Z-Chess Docker 测试集群快速启动"
echo "========================================"
echo -e "${NC}"

# 检查环境
echo -e "${BLUE}[1/5] 检查 Docker 环境...${NC}"
if ! command -v docker &> /dev/null; then
    echo -e "${RED}错误: Docker 未安装${NC}"
    exit 1
fi
if ! command -v docker compose &> /dev/null; then
    echo -e "${RED}错误: Docker Compose 未安装${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker 环境正常${NC}"

# 创建必要目录
echo -e "${BLUE}[2/5] 创建必要目录...${NC}"
mkdir -p ~/Z-Chess/raft00 ~/Z-Chess/raft01 ~/Z-Chess/raft02
mkdir -p ~/Z-Chess/logs/raft00 ~/Z-Chess/logs/raft01 ~/Z-Chess/logs/raft02 ~/Z-Chess/logs/audience
mkdir -p ~/Z-Chess/sqlite
echo -e "${GREEN}✓ 目录创建完成${NC}"

# 检查镜像
echo -e "${BLUE}[3/5] 检查 Docker 镜像...${NC}"
if ! docker image inspect z-chess-arena:2.0-arm64 &> /dev/null; then
    echo -e "${YELLOW}⚠ Arena 镜像不存在，需要构建...${NC}"
    echo "正在构建镜像（可能需要几分钟）..."
    cd "${SCRIPT_DIR}/../../.."
    mvn clean package -DskipTests -q || {
        echo -e "${RED}构建失败，请检查 Maven 配置${NC}"
        exit 1
    }
    docker build --platform linux/arm64 -t z-chess-arena:2.0-arm64 \
        -f docker/aarch64/Dockerfile .
fi
echo -e "${GREEN}✓ 镜像检查完成${NC}"

# 启动集群
echo -e "${BLUE}[4/5] 启动测试集群...${NC}"
docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} up -d

echo "等待服务启动..."
sleep 10

# 检查服务状态
echo -e "${BLUE}[5/5] 验证服务状态...${NC}"
echo ""

# 检查每个服务
services=("db-pg" "raft00" "raft01" "raft02" "audience")
all_ready=true

for service in "${services[@]}"; do
    status=$(docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps -q $service 2>/dev/null)
    if [ -n "$status" ]; then
        health=$(docker inspect --format='{{.State.Status}}' $status 2>/dev/null)
        if [ "$health" = "running" ]; then
            echo -e "  ${GREEN}✓${NC} $service: 运行中"
        else
            echo -e "  ${YELLOW}⚠${NC} $service: $health"
            all_ready=false
        fi
    else
        echo -e "  ${RED}✗${NC} $service: 未启动"
        all_ready=false
    fi
done

echo ""

if [ "$all_ready" = true ]; then
    echo -e "${GREEN}========================================"
    echo "   测试集群启动成功！"
    echo "========================================${NC}"
    echo ""
    echo "服务地址:"
    echo "  Raft00 (主节点): http://localhost:8080"
    echo "  Raft01:          http://localhost:8081"
    echo "  Raft02:          http://localhost:8082"
    echo "  Audience:        http://localhost:8090"
    echo ""
    echo "MQTT 端口:"
    echo "  Raft00: localhost:1883"
    echo "  Raft01: localhost:1884"
    echo "  Raft02: localhost:1885"
    echo ""
    echo "常用命令:"
    echo "  查看状态: ./test-cluster.sh status"
    echo "  查看日志: ./test-cluster.sh logs raft00"
    echo "  运行测试: ./test-cluster.sh test"
    echo "  停止集群: ./test-cluster.sh stop"
    echo ""
    echo -e "${YELLOW}注意: Audience 测试客户端将在 30 秒后自动启动压力测试${NC}"
else
    echo -e "${YELLOW}部分服务尚未就绪，请查看日志: ./test-cluster.sh logs${NC}"
fi
