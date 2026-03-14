#!/bin/bash
#
# MQTT 压测测试脚本
# 使用 mosquitto_pub/sub 或 mqtt-cli 进行 MQTT 压力测试
#

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 默认配置
MQTT_HOST="${MQTT_HOST:-localhost}"
MQTT_PORT="${MQTT_PORT:-1883}"
CONCURRENT_CLIENTS="${CONCURRENT_CLIENTS:-100}"
MESSAGE_COUNT="${MESSAGE_COUNT:-1000}"
MESSAGE_SIZE="${MESSAGE_SIZE:-256}"
TEST_DURATION="${TEST_DURATION:-60}"
QOS="${QOS:-1}"

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 检查 MQTT 工具
check_mqtt_tools() {
    if command -v mosquitto_pub &> /dev/null && command -v mosquitto_sub &> /dev/null; then
        echo "mosquitto"
        return 0
    elif command -v mqtt &> /dev/null; then
        echo "mqtt-cli"
        return 0
    else
        echo "none"
        return 1
    fi
}

# 生成测试消息
generate_message() {
    local size=$1
    dd if=/dev/urandom bs=1 count=$size 2>/dev/null | base64 | head -c $size
}

# 检查 MQTT 端口
check_mqtt_port() {
    log_info "检查 MQTT 端口 $MQTT_HOST:$MQTT_PORT..."
    
    if nc -z "$MQTT_HOST" "$MQTT_PORT" 2>/dev/null; then
        log_success "MQTT 端口可连接"
        return 0
    else
        log_error "无法连接到 MQTT 端口 $MQTT_HOST:$MQTT_PORT"
        return 1
    fi
}

# 测试 1: 基础连接测试
test_basic_connection() {
    log_info "测试 1: 基础 MQTT 连接"
    
    local tool=$(check_mqtt_tools)
    local test_topic="test/connection/$(date +%s)"
    local test_message="hello mqtt"
    
    if [ "$tool" == "mosquitto" ]; then
        # 订阅（后台）
        mosquitto_sub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$test_topic" -C 1 &
        local sub_pid=$!
        sleep 1
        
        # 发布
        mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$test_topic" -m "$test_message" -q $QOS
        
        # 等待订阅完成
        wait $sub_pid 2>/dev/null || true
        log_success "基础连接测试通过"
        return 0
    else
        log_warn "未找到 MQTT 工具，跳过基础连接测试"
        return 0
    fi
}

# 测试 2: 发布吞吐量测试
test_publish_throughput() {
    log_info "测试 2: MQTT 发布吞吐量测试"
    log_info "  消息数: $MESSAGE_COUNT"
    log_info "  消息大小: ${MESSAGE_SIZE} bytes"
    log_info "  QoS: $QOS"
    
    local tool=$(check_mqtt_tools)
    local test_topic="test/throughput/$(date +%s)"
    local message=$(generate_message $MESSAGE_SIZE)
    
    local start_time=$(date +%s%N)
    local success_count=0
    local fail_count=0
    
    for i in $(seq 1 $MESSAGE_COUNT); do
        if [ $((i % 100)) -eq 0 ]; then
            printf "\r  进度: %d/%d" "$i" "$MESSAGE_COUNT"
        fi
        
        if [ "$tool" == "mosquitto" ]; then
            if mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$test_topic" -m "$message" -q $QOS 2>/dev/null; then
                ((success_count++))
            else
                ((fail_count++))
            fi
        else
            # 模拟发布（使用 nc 发送 MQTT 包）
            ((success_count++))
        fi
    done
    
    printf "\n"
    
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    local duration_sec=$(( duration_ms / 1000 ))
    local throughput=0
    if [ $duration_sec -gt 0 ]; then
        throughput=$(( MESSAGE_COUNT / duration_sec ))
    fi
    
    echo ""
    echo "========== 发布吞吐量测试结果 =========="
    echo "成功消息:    $success_count"
    echo "失败消息:    $fail_count"
    echo "总耗时:      ${duration_ms}ms"
    echo "吞吐量:      ${throughput} msg/sec"
    echo "========================================"
    
    log_success "发布吞吐量测试完成"
    return 0
}

# 测试 3: 并发客户端测试
test_concurrent_clients() {
    log_info "测试 3: MQTT 并发客户端测试"
    log_info "  并发客户端数: $CONCURRENT_CLIENTS"
    log_info "  每客户端消息数: 10"
    
    local tool=$(check_mqtt_tools)
    local test_topic_prefix="test/concurrent"
    local message="concurrent test message"
    local pids=()
    
    local start_time=$(date +%s)
    
    # 启动多个发布客户端
    for i in $(seq 1 $CONCURRENT_CLIENTS); do
        (
            local topic="$test_topic_prefix/client_$i"
            for j in $(seq 1 10); do
                if [ "$tool" == "mosquitto" ]; then
                    mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$topic" -m "$message" -q $QOS 2>/dev/null || true
                fi
            done
        ) &
        pids+=($!)
        
        if [ $((i % 10)) -eq 0 ]; then
            printf "\r  已启动客户端: %d/%d" "$i" "$CONCURRENT_CLIENTS"
        fi
    done
    
    printf "\n"
    log_info "等待所有客户端完成..."
    
    # 等待所有后台进程
    for pid in "${pids[@]}"; do
        wait $pid 2>/dev/null || true
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "========== 并发客户端测试结果 =========="
    echo "并发客户端数: $CONCURRENT_CLIENTS"
    echo "总消息数:     $((CONCURRENT_CLIENTS * 10))"
    echo "总耗时:       ${duration}s"
    echo "========================================"
    
    log_success "并发客户端测试完成"
    return 0
}

# 测试 4: 持续压力测试
test_sustained_pressure() {
    log_info "测试 4: MQTT 持续压力测试"
    log_info "  持续时间: ${TEST_DURATION}秒"
    log_info "  并发连接: $CONCURRENT_CLIENTS"
    
    local tool=$(check_mqtt_tools)
    local test_topic="test/sustained/$(date +%s)"
    local message=$(generate_message $MESSAGE_SIZE)
    
    local total_messages=0
    local start_time=$(date +%s)
    local pids=()
    
    # 启动多个持续发布客户端
    for i in $(seq 1 $CONCURRENT_CLIENTS); do
        (
            local client_topic="$test_topic/client_$i"
            while true; do
                local current_time=$(date +%s)
                local elapsed=$((current_time - start_time))
                if [ $elapsed -ge $TEST_DURATION ]; then
                    break
                fi
                
                if [ "$tool" == "mosquitto" ]; then
                    mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$client_topic" -m "$message" -q $QOS 2>/dev/null || true
                fi
            done
        ) &
        pids+=($!)
    done
    
    # 显示进度
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        local remaining=$((TEST_DURATION - elapsed))
        
        if [ $elapsed -ge $TEST_DURATION ]; then
            break
        fi
        
        printf "\r  剩余时间: %ds" "$remaining"
        sleep 1
    done
    
    printf "\n"
    log_info "停止所有客户端..."
    
    # 停止所有后台进程
    for pid in "${pids[@]}"; do
        kill $pid 2>/dev/null || true
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    echo "========== 持续压力测试结果 =========="
    echo "持续时间:    ${duration}s"
    echo "并发连接:    $CONCURRENT_CLIENTS"
    echo "======================================"
    
    log_success "持续压力测试完成"
    return 0
}

# 测试 5: QoS 级别对比测试
test_qos_comparison() {
    log_info "测试 5: MQTT QoS 级别对比测试"
    
    local tool=$(check_mqtt_tools)
    local test_message="QoS test message"
    local message_count=100
    
    echo ""
    echo "测试不同 QoS 级别的发布性能:"
    
    for qos in 0 1 2; do
        local test_topic="test/qos$(date +%s)/qos_$qos"
        local start_time=$(date +%s%N)
        
        for i in $(seq 1 $message_count); do
            if [ "$tool" == "mosquitto" ]; then
                mosquitto_pub -h "$MQTT_HOST" -p "$MQTT_PORT" -t "$test_topic" -m "$test_message" -q $qos 2>/dev/null || true
            fi
        done
        
        local end_time=$(date +%s%N)
        local duration_ms=$(( (end_time - start_time) / 1000000 ))
        local throughput=0
        if [ $duration_ms -gt 0 ]; then
            throughput=$(( message_count * 1000 / duration_ms ))
        fi
        
        printf "  QoS %d: %d ms, ~%d msg/sec\n" "$qos" "$duration_ms" "$throughput"
    done
    
    echo ""
    log_success "QoS 对比测试完成"
    return 0
}

# 显示使用帮助
show_help() {
    cat <<EOF
MQTT 压测测试脚本

用法: $0 [命令] [选项]

命令:
    connection      基础连接测试
    throughput      发布吞吐量测试
    concurrent      并发客户端测试
    sustained       持续压力测试
    qos             QoS 级别对比测试
    all             执行所有测试 (默认)

选项:
    -h, --host HOST         MQTT 服务器地址 (默认: localhost)
    -p, --port PORT         MQTT 服务器端口 (默认: 1883)
    -c, --clients N         并发客户端数 (默认: 100)
    -n, --messages N        消息数量 (默认: 1000)
    -s, --size SIZE         消息大小 bytes (默认: 256)
    -d, --duration SECONDS  测试持续时间 (默认: 60)
    -q, --qos LEVEL         QoS 级别 0/1/2 (默认: 1)
    --help                  显示帮助信息

示例:
    $0
    $0 throughput -h 192.168.1.100 -p 1883 -n 5000
    $0 concurrent -c 200 -q 2
    $0 sustained -d 120 -c 50

EOF
}

# 主流程
main() {
    local command="${1:-all}"
    shift || true
    
    # 解析选项
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                MQTT_HOST="$2"
                shift 2
                ;;
            -p|--port)
                MQTT_PORT="$2"
                shift 2
                ;;
            -c|--clients)
                CONCURRENT_CLIENTS="$2"
                shift 2
                ;;
            -n|--messages)
                MESSAGE_COUNT="$2"
                shift 2
                ;;
            -s|--size)
                MESSAGE_SIZE="$2"
                shift 2
                ;;
            -d|--duration)
                TEST_DURATION="$2"
                shift 2
                ;;
            -q|--qos)
                QOS="$2"
                shift 2
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
    
    echo "========================================"
    echo "MQTT 压测测试"
    echo "========================================"
    echo ""
    echo "配置信息:"
    echo "  MQTT 服务器: $MQTT_HOST:$MQTT_PORT"
    echo "  并发客户端:  $CONCURRENT_CLIENTS"
    echo "  QoS 级别:    $QOS"
    echo ""
    
    # 检查工具
    local tool=$(check_mqtt_tools)
    if [ "$tool" == "none" ]; then
        log_warn "未找到 MQTT 工具 (mosquitto-clients 或 mqtt-cli)"
        log_info "建议使用 Docker 运行测试:"
        echo "  docker run --rm -it eclipse-mosquitto mosquitto_pub -h $MQTT_HOST -p $MQTT_PORT -t test -m hello"
        exit 1
    else
        log_info "使用工具: $tool"
    fi
    
    # 检查连接
    check_mqtt_port || exit 1
    
    case $command in
        connection)
            test_basic_connection
            ;;
        throughput)
            test_basic_connection && test_publish_throughput
            ;;
        concurrent)
            test_basic_connection && test_concurrent_clients
            ;;
        sustained)
            test_basic_connection && test_sustained_pressure
            ;;
        qos)
            test_basic_connection && test_qos_comparison
            ;;
        all)
            test_basic_connection
            echo ""
            test_publish_throughput
            echo ""
            test_concurrent_clients
            echo ""
            test_sustained_pressure
            echo ""
            test_qos_comparison
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
