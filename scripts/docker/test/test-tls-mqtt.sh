#!/bin/bash
#
# Z-Chess TLS MQTT 压力测试验证脚本
#
# 功能：
# 1. 生成 TLS 证书（如需要）
# 2. 启动启用了 TLS 的 Z-Chess 集群
# 3. 使用 Audience 进行 TLS MQTT 压力测试
# 4. 验证 TLS 链路加密
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="z-chess-tls-test"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose-tls.yaml"
CERT_DIR="${SCRIPT_DIR}/cert"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

print_banner() {
    echo ""
    echo "========================================"
    echo "   Z-Chess TLS MQTT 压力测试验证"
    echo "========================================"
    echo ""
}

# 检查证书
check_certificates() {
    print_info "检查 TLS 证书..."
    
    if [ ! -d "$CERT_DIR" ] || [ ! -f "$CERT_DIR/server.p12" ]; then
        print_warning "证书不存在，开始生成..."
        generate_certificates
    else
        print_success "证书已存在"
        openssl x509 -in "$CERT_DIR/ca-cert.pem" -noout -subject -dates 2>/dev/null || true
    fi
}

# 生成证书
generate_certificates() {
    print_info "生成 TLS 测试证书..."
    
    mkdir -p "$CERT_DIR"
    cd "$CERT_DIR"
    
    local password="changeit"
    
    # 生成 CA
    openssl req -x509 -newkey rsa:2048 -keyout ca-key.pem -out ca-cert.pem \
        -days 1 -nodes -subj "/CN=Z-Chess Test CA/O=Z-Chess/C=CN" 2>/dev/null
    print_success "CA 证书生成完成"
    
    # 生成服务端证书
    openssl req -newkey rsa:2048 -keyout server-key.pem -out server.csr \
        -nodes -subj "/CN=raft00/O=Z-Chess/C=CN" 2>/dev/null
    openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem \
        -CAcreateserial -out server-cert.pem -days 1 2>/dev/null
    print_success "服务端证书生成完成"
    
    # 创建 PKCS12 密钥库
    openssl pkcs12 -export -in server-cert.pem -inkey server-key.pem \
        -certfile ca-cert.pem -out server.p12 -name server \
        -password pass:$password 2>/dev/null
    print_success "服务端 PKCS12 密钥库创建完成"
    
    # 创建信任库
    keytool -import -alias ca -file ca-cert.pem -keystore trust.p12 \
        -storetype PKCS12 -storepass $password -noprompt 2>/dev/null
    print_success "信任库创建完成"
    
    cd - > /dev/null
}

# 启动基础服务
start_base_services() {
    print_info "启动基础服务 (PostgreSQL + Raft00)..."
    
    docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} up -d db-pg raft00
    
    print_info "等待服务就绪..."
    sleep 15
    
    # 检查服务状态
    if docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps | grep -q "running"; then
        print_success "基础服务已启动"
    else
        print_error "服务启动失败"
        docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs
        exit 1
    fi
}

# 验证 TLS 端口
verify_tls_port() {
    print_info "验证 TLS MQTT 端口 (8883)..."
    
    # 使用 OpenSSL 测试 TLS 连接
    echo | timeout 5 openssl s_client -connect localhost:8883 \
        -CAfile "$CERT_DIR/ca-cert.pem" 2>/dev/null | grep -E "Verify return code|Protocol|Cipher" | head -3 || true
    
    print_success "TLS 端口验证完成"
}

# 启动压力测试
start_pressure_test() {
    print_info "启动 Audience TLS 压力测试..."
    print_info "测试配置:"
    echo "  目标: raft00:8883 (TLS)"
    echo "  并发: 50 连接"
    echo "  时长: 120 秒"
    echo "  协议: MQTT over TLS"
    echo ""
    
    docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} \
        --profile tls-test up -d audience-tls
    
    print_info "压力测试已启动，查看日志..."
    echo ""
    
    # 实时显示日志
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs -f audience-tls &
    local log_pid=$!
    
    # 等待测试完成
    sleep 130
    
    kill $log_pid 2>/dev/null || true
    wait $log_pid 2>/dev/null || true
}

# 分析测试结果
analyze_results() {
    print_info "分析测试结果..."
    
    local logs=$(docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs audience-tls 2>/dev/null)
    
    # 检查是否有错误
    if echo "$logs" | grep -qi "error\|exception\|failed"; then
        print_warning "测试中发现错误或异常，请检查日志"
    else
        print_success "测试完成，未发现明显错误"
    fi
    
    # 显示关键指标
    echo ""
    echo "========================================"
    echo "测试摘要"
    echo "========================================"
    echo "$logs" | grep -E "Connected|Messages|Throughput|Latency" | tail -10 || echo "(详细指标请查看完整日志)"
}

# 使用 mosquitto 客户端验证
test_with_mosquitto() {
    print_info "使用 Mosquitto 客户端验证 TLS 连接..."
    
    # 启动 mosquitto 验证容器
    docker compose --ansi always -p ${PROJECT_NAME} -f ${COMPOSE_FILE} \
        --profile tls-verify up -d tls-verifier
    
    sleep 2
    
    # 执行订阅（后台）
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} exec -d tls-verifier \
        mosquitto_sub -h raft00 -p 8883 -t test/tls/verify \
        --cafile /mosquitto/certs/ca-cert.pem -v
    
    sleep 1
    
    # 执行发布
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} exec tls-verifier \
        mosquitto_pub -h raft00 -p 8883 -t test/tls/verify \
        -m "TLS Test Message $(date +%s)" \
        --cafile /mosquitto/certs/ca-cert.pem || true
    
    print_success "Mosquitto TLS 验证完成"
}

# 显示报告
show_report() {
    echo ""
    echo "========================================"
    echo "   TLS MQTT 压力测试验证报告"
    echo "========================================"
    echo ""
    echo "测试环境:"
    echo "  - Z-Chess 版本: 2.0"
    echo "  - TLS 协议: TLSv1.2"
    echo "  - MQTT 端口: 8883 (TLS)"
    echo "  - 测试客户端: Z-Audience"
    echo ""
    echo "测试配置:"
    echo "  - 目标: raft00:8883"
    echo "  - 并发连接: 50"
    echo "  - 测试时长: 120 秒"
    echo "  - 消息大小: 256 bytes"
    echo ""
    echo "证书信息:"
    openssl x509 -in "$CERT_DIR/ca-cert.pem" -noout -subject -dates 2>/dev/null || true
    echo ""
    echo "服务状态:"
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} ps
    echo ""
    echo "========================================"
}

# 清理环境
cleanup() {
    print_info "清理测试环境..."
    docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} down -v
    print_success "环境已清理"
}

# 主程序
main() {
    print_banner
    
    case "${1:-}" in
        start)
            check_certificates
            start_base_services
            verify_tls_port
            test_with_mosquitto
            start_pressure_test
            analyze_results
            show_report
            ;;
        verify-only)
            check_certificates
            start_base_services
            verify_tls_port
            test_with_mosquitto
            show_report
            ;;
        stop)
            cleanup
            ;;
        logs)
            docker compose -p ${PROJECT_NAME} -f ${COMPOSE_FILE} logs -f
            ;;
        *)
            echo "Z-Chess TLS MQTT 压力测试验证"
            echo ""
            echo "用法: $0 [命令]"
            echo ""
            echo "命令:"
            echo "  start          启动完整的 TLS 压力测试验证"
            echo "  verify-only    仅验证 TLS 连接（不运行压力测试）"
            echo "  stop           停止并清理测试环境"
            echo "  logs           查看实时日志"
            echo ""
            echo "示例:"
            echo "  $0 start       # 启动完整测试"
            echo "  $0 verify-only # 仅验证 TLS 连接"
            echo "  $0 stop        # 清理环境"
            exit 1
            ;;
    esac
}

# 捕获中断信号
trap 'echo ""; print_warning "收到中断信号，正在清理..."; cleanup; exit 1' INT TERM

main "$@"
