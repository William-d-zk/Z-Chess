#!/bin/bash
# MIT License
#
# Copyright (c) 2016~2024. Z-Chess

# Z-Chess MQTT v3/v5 测试环境启动脚本
# 修复版本：使用嵌入配置文件的 Docker 镜像

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 显示帮助
show_help() {
    cat << EOF
Z-Chess MQTT v3/v5 测试环境启动脚本 (修复版)

用法: $0 [选项] [命令]

命令:
    up              启动测试环境 (默认)
    down            停止测试环境
    restart         重启测试环境
    logs            查看日志
    status          查看服务状态
    build           重新构建镜像
    clean           清理所有数据和日志

选项:
    -h, --help      显示帮助信息
    -d, --detach    后台运行
    --build         启动前重新构建镜像

说明:
    配置文件已嵌入 Docker 镜像，无需外部挂载
    数据库配置: jdbc:postgresql://db-pg:5432/isahl_9.x

示例:
    $0 up                   # 前台启动
    $0 up -d                # 后台启动
    $0 up --build           # 重建镜像后启动
    $0 status               # 查看状态
    $0 logs                 # 查看日志
    $0 down                 # 停止服务
EOF
}

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 检查依赖
check_dependencies() {
    log_info "检查依赖..."
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装"
        exit 1
    fi
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose 未安装"
        exit 1
    fi
    log_success "依赖检查通过"
}

# 创建必要目录
setup_directories() {
    log_info "创建必要的目录..."
    mkdir -p ~/Z-Chess/logs/{raft00,audience}
    mkdir -p ~/Z-Chess/raft00
    log_success "目录创建完成"
}

# 启动测试环境
start_environment() {
    local detach="$1"
    local build="$2"
    
    log_info "========================================"
    log_info "Z-Chess MQTT v3/v5 测试环境启动"
    log_info "版本: 1.0.21 (修复版)"
    log_info "配置方式: 配置文件嵌入镜像"
    log_info "========================================"
    echo
    
    check_dependencies
    setup_directories
    
    cd "${SCRIPT_DIR}"
    
    # 构建参数
    if [ "$build" = "true" ]; then
        log_info "重新构建镜像..."
        docker-compose -f docker-compose.mqtt-test.yaml build --no-cache
    fi
    
    # 启动命令
    if [ "$detach" = "true" ]; then
        log_info "后台启动测试环境..."
        docker-compose -f docker-compose.mqtt-test.yaml up -d
    else
        log_info "前台启动测试环境..."
        log_info "按 Ctrl+C 停止服务"
        echo
        docker-compose -f docker-compose.mqtt-test.yaml up
        return
    fi
    
    log_info ""
    log_info "等待服务启动..."
    sleep 10
    
    # 显示状态
    show_status
}

# 停止测试环境
stop_environment() {
    log_info "停止测试环境..."
    cd "${SCRIPT_DIR}"
    docker-compose -f docker-compose.mqtt-test.yaml down
    log_success "测试环境已停止"
}

# 查看日志
show_logs() {
    cd "${SCRIPT_DIR}"
    docker-compose -f docker-compose.mqtt-test.yaml logs -f
}

# 显示状态
show_status() {
    log_info "========================================"
    log_info "服务状态"
    log_info "========================================"
    
    cd "${SCRIPT_DIR}"
    
    echo
    docker-compose -f docker-compose.mqtt-test.yaml ps
    
    echo
    log_info "服务访问地址:"
    log_info "  raft00 (主节点):"
    log_info "    - HTTP API:  http://localhost:8080"
    log_info "    - MQTT:      tcp://localhost:1883"
    log_info "    - MQTT TLS:  tcp://localhost:2883"
    log_info ""
    log_info "  Audience 测试客户端:"
    log_info "    - HTTP API:  http://localhost:8090"
    log_info ""
    
    # 检查服务健康状态
    log_info "检查服务健康状态..."
    
    if curl -sf "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
        log_success "  ✓ raft00 运行正常"
        health=$(curl -s http://localhost:8080/actuator/health)
        echo "    状态: $health"
    else
        log_warn "  ✗ raft00 未就绪"
    fi
    
    echo
    log_info "查看日志: $0 logs"
    log_info "停止服务: $0 down"
}

# 重新构建镜像
build_images() {
    log_info "重新构建镜像..."
    cd "${SCRIPT_DIR}"
    docker-compose -f docker-compose.mqtt-test.yaml build --no-cache
    log_success "镜像构建完成"
}

# 清理环境
clean_environment() {
    log_warn "即将清理所有数据、日志和镜像!"
    read -p "确认继续? [y/N] " -n 1 -r
    echo
    
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        log_info "操作已取消"
        exit 0
    fi
    
    log_info "清理测试环境..."
    cd "${SCRIPT_DIR}"
    docker-compose -f docker-compose.mqtt-test.yaml down -v --rmi all
    
    # 清理本地数据
    rm -rf ~/Z-Chess/logs/raft00/* ~/Z-Chess/logs/audience/*
    
    log_success "清理完成"
}

# 主函数
main() {
    local command="${1:-up}"
    local detach="false"
    local build="false"
    
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            -d|--detach)
                detach="true"
                shift
                ;;
            --build)
                build="true"
                shift
                ;;
            up|down|restart|logs|status|build|clean)
                command="$1"
                shift
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    # 执行命令
    case "$command" in
        up)
            start_environment "$detach" "$build"
            ;;
        down)
            stop_environment
            ;;
        restart)
            stop_environment
            sleep 2
            start_environment "$detach" "$build"
            ;;
        logs)
            show_logs
            ;;
        status)
            show_status
            ;;
        build)
            build_images
            ;;
        clean)
            clean_environment
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
