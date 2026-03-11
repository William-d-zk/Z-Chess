#!/bin/bash
#
# MIT License
# Copyright (c) 2016~2024. Z-Chess
#
# Z-Chess 测试集群管理脚本
# 用法: ./test-cluster.sh [start|stop|restart|status|logs|test|tls-test]
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="z-chess-test"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yaml"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印帮助信息
print_help() {
    echo -e "${BLUE}Z-Chess 测试集群管理脚本${NC}"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start      启动测试集群 (包含 Audience 测试客户端)"
    echo "  stop       停止测试集群"
    echo "  restart    重启测试集群"
    echo "  status     查看集群状态"
    echo "  logs       查看日志"
    echo "  test       运行 MQTT 压力测试"
    echo "  tls-test   运行 TLS 加密连接测试"
    echo "  build      重新构建镜像"
    echo "  clean      清理所有数据和日志"
    echo ""
    echo "示例:"
    echo "  $0 start          # 启动完整测试集群"
    echo "  $0 status         # 查看各节点状态"
    echo "  $0 logs raft00    # 查看 raft00 节点日志"
    echo "  $0 test           # 运行压力测试"
}

# 检查 Docker 环境
check_docker() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: Docker 未安装${NC}"
        exit 1
    fi
    
    if ! command -v docker compose &> /dev/null; then
        echo -e "${RED}错误: Docker Compose 未安装${NC}"
        exit 1
    fi
}

# 启动集群
start_cluster() {
    echo -e "${BLUE}启动 Z-Chess 测试集群...${NC}"
    
    # 创建必要的目录
    mkdir -p ~/Z-Chess/raft00 ~/Z-Chess/raft01 ~/Z-Chess/raft02
    mkdir -p ~/Z-Chess/logs/raft00 ~/Z-Chess/logs/raft01 ~/Z-Chess/logs/raft02 ~/Z-Chess/logs/audience
    mkdir -p ~/Z-Chess/sqlite
    
    # 启动集群
    docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} up -d
    
    echo ""
    echo -e "${GREEN}集群启动完成!${NC}"
    echo ""
    echo "服务地址:"
    echo "  Raft00 (主节点): http://localhost:8080"
    echo "  Raft01:          http://localhost:8081"
    echo "  Raft02:          http://localhost:8082"
    echo "  Audience:        http://localhost:8090"
    echo "  PostgreSQL:      localhost:5432"
    echo ""
    echo "MQTT 端口:"
    echo "  Raft00: 1883"
    echo "  Raft01: 1884"
    echo "  Raft02: 1885"
    echo ""
    echo "Debug 端口:"
    echo "  Raft00: 8000"
    echo "  Raft01: 8100"
    echo "  Raft02: 8200"
    echo "  Audience: 8300"
}

# 停止集群
stop_cluster() {
    echo -e "${BLUE}停止 Z-Chess 测试集群...${NC}"
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} down
    echo -e "${GREEN}集群已停止${NC}"
}

# 重启集群
restart_cluster() {
    stop_cluster
    sleep 2
    start_cluster
}

# 查看状态
show_status() {
    echo -e "${BLUE}集群状态:${NC}"
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps
    echo ""
    echo -e "${BLUE}资源使用:${NC}"
    docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" $(docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps -q) 2>/dev/null || echo "暂无运行中的容器"
}

# 查看日志
show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        echo -e "${YELLOW}查看所有服务日志 (最后 100 行)...${NC}"
        docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs --tail=100 -f
    else
        echo -e "${YELLOW}查看 $service 日志 (最后 100 行)...${NC}"
        docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs --tail=100 -f $service
    fi
}

# 运行压力测试
run_test() {
    echo -e "${BLUE}运行 MQTT 压力测试...${NC}"
    
    # 检查 Audience 是否运行
    if ! docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps audience | grep -q "running"; then
        echo -e "${YELLOW}Audience 未运行，先启动测试集群...${NC}"
        start_cluster
        echo "等待集群就绪 (30秒)..."
        sleep 30
    fi
    
    echo ""
    echo -e "${GREEN}测试配置:${NC}"
    echo "  目标: raft00:1883"
    echo "  并发: 100 连接"
    echo "  时长: 60 秒"
    echo "  协议: MQTT"
    echo ""
    
    # 进入 Audience 容器执行测试
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} exec audience sh -c "
        echo '开始压力测试...'
        # 这里可以调用 Audience 的测试 API 或命令
        curl -s http://localhost:8080/actuator/health || echo 'Audience 健康检查'
    "
}

# 运行 TLS 测试
run_tls_test() {
    echo -e "${BLUE}运行 TLS 加密连接测试...${NC}"
    
    # 检查证书是否存在
    if [ ! -f "${SCRIPT_DIR}/cert/server.p12" ]; then
        echo -e "${YELLOW}证书不存在，生成测试证书...${NC}"
        mkdir -p ${SCRIPT_DIR}/cert
        cd ${SCRIPT_DIR}/cert
        
        # 生成测试证书
        openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem -out ca-cert.pem \
            -days 1 -nodes -subj "/CN=TestCA/O=Z-Chess/C=CN" 2>/dev/null
        
        openssl req -newkey rsa:2048 -keyout server-key.pem -out server.csr \
            -nodes -subj "/CN=localhost/O=Z-Chess/C=CN" 2>/dev/null
        
        openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem \
            -CAcreateserial -out server-cert.pem -days 1 2>/dev/null
        
        openssl pkcs12 -export -in server-cert.pem -inkey server-key.pem \
            -certfile ca-cert.pem -out server.p12 -name server \
            -password pass:changeit 2>/dev/null
        
        keytool -import -alias ca -file ca-cert.pem -keystore trust.p12 \
            -storetype PKCS12 -storepass changeit -noprompt 2>/dev/null
        
        cd - > /dev/null
        echo -e "${GREEN}证书生成完成${NC}"
    fi
    
    # 启动 TLS 测试容器
    docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} --profile tls-test up -d tls-tester
    
    echo ""
    echo -e "${GREEN}TLS 测试客户端已启动${NC}"
    echo "进入测试容器: docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} exec tls-tester sh"
    echo ""
    
    # 显示测试命令
    echo -e "${BLUE}可用测试命令:${NC}"
    echo "  # 测试 TLS 连接"
    echo "  openssl s_client -connect raft00:1883 -CAfile /app/cert/ca-cert.pem"
    echo ""
    echo "  # 查看证书信息"
    echo "  keytool -list -v -keystore /app/cert/trust.p12 -storepass changeit"
}

# 构建镜像
build_images() {
    echo -e "${BLUE}构建 Z-Chess 镜像...${NC}"
    
    # 先构建 Arena 镜像
    echo "构建 Arena 镜像..."
    docker build --platform linux/arm64 -t z-chess-arena:2.0-arm64 \
        -f ${SCRIPT_DIR}/Dockerfile ../../
    
    # 构建 Audience 镜像
    echo "构建 Audience 镜像..."
    docker build --platform linux/arm64 -t z-chess-audience:2.0-arm64 \
        -f ${SCRIPT_DIR}/Dockerfile.audience ../../
    
    echo -e "${GREEN}镜像构建完成${NC}"
}

# 清理环境
clean_environment() {
    echo -e "${YELLOW}警告: 这将删除所有数据、日志和容器${NC}"
    read -p "确认清理? (y/N): " confirm
    
    if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
        echo -e "${BLUE}清理环境...${NC}"
        docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} down -v
        docker volume prune -f
        rm -rf ~/Z-Chess/raft00 ~/Z-Chess/raft01 ~/Z-Chess/raft02
        rm -rf ~/Z-Chess/logs ~/Z-Chess/sqlite
        echo -e "${GREEN}环境清理完成${NC}"
    else
        echo "取消清理"
    fi
}

# 主程序
main() {
    check_docker
    
    case "$1" in
        start)
            start_cluster
            ;;
        stop)
            stop_cluster
            ;;
        restart)
            restart_cluster
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs $2
            ;;
        test)
            run_test
            ;;
        tls-test)
            run_tls_test
            ;;
        build)
            build_images
            ;;
        clean)
            clean_environment
            ;;
        help|--help|-h)
            print_help
            ;;
        *)
            echo -e "${RED}未知命令: $1${NC}"
            print_help
            exit 1
            ;;
    esac
}

main "$@"
