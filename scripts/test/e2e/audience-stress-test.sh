#!/bin/bash
#
# Z-Audience 压测客户端端到端测试脚本
# 测试 API 和 MQTT 协议的压力测试功能
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
AUDIENCE_API="http://localhost:8081"
TARGET_HOST="${TARGET_HOST:-host.docker.internal}"
TARGET_PORT="${TARGET_PORT:-1883}"
TEST_DURATION="${TEST_DURATION:-30}"
CONCURRENCY="${CONCURRENCY:-100}"

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# HTTP 请求工具
http_get() {
    curl -s -m 10 "$1" 2>/dev/null || echo '{"error": "request failed"}'
}

http_post() {
    curl -s -m 10 -X POST \
        -H "Content-Type: application/json" \
        -d "$2" \
        "$1" 2>/dev/null || echo '{"error": "request failed"}'
}

# ==================== 测试函数 ====================

# 测试 1: 检查 Audience 服务健康状态
test_audience_health() {
    log_info "测试 1: Audience 服务健康检查"
    
    local response=$(http_get "${AUDIENCE_API}/actuator/health")
    local status=$(echo "$response" | jq -r '.status // "DOWN"' 2>/dev/null || echo "DOWN")
    
    if [ "$status" == "UP" ]; then
        log_success "Audience 服务运行正常"
        return 0
    else
        log_error "Audience 服务未就绪: $response"
        return 1
    fi
}

# 测试 2: 获取压测配置
test_get_config() {
    log_info "测试 2: 获取压测配置"
    
    local response=$(http_get "${AUDIENCE_API}/api/stress/config")
    local target_host=$(echo "$response" | jq -r '.data.target.host // "null"' 2>/dev/null)
    
    if [ "$target_host" != "null" ] && [ "$target_host" != "" ]; then
        log_success "配置获取成功，目标服务器: $target_host"
        echo "当前配置:"
        echo "$response" | jq '.data' 2>/dev/null || echo "$response"
        return 0
    else
        log_error "配置获取失败: $response"
        return 1
    fi
}

# 测试 3: 更新压测配置
test_update_config() {
    log_info "测试 3: 更新压测配置"
    
    local config='{
        "targetHost": "'"$TARGET_HOST"'",
        "targetPort": '"$TARGET_PORT"',
        "concurrency": '"$CONCURRENCY"',
        "durationSeconds": '"$TEST_DURATION"',
        "requestsPerSecondPerClient": 10,
        "protocol": "mqtt"
    }'
    
    local response=$(http_post "${AUDIENCE_API}/api/stress/config" "$config")
    local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "配置更新成功"
        return 0
    else
        log_error "配置更新失败: $response"
        return 1
    fi
}

# 测试 4: 启动 MQTT 压测
test_start_mqtt_stress() {
    log_info "测试 4: 启动 MQTT 压测 (${CONCURRENCY} 并发, ${TEST_DURATION} 秒)"
    
    local request='{
        "concurrency": '"$CONCURRENCY"',
        "durationSeconds": '"$TEST_DURATION"',
        "requestsPerSecondPerClient": 10,
        "targetHost": "'"$TARGET_HOST"'",
        "targetPort": '"$TARGET_PORT"',
        "protocol": "mqtt"
    }'
    
    local response=$(http_post "${AUDIENCE_API}/api/stress/start" "$request")
    local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "MQTT 压测启动成功"
        echo "响应: $(echo "$response" | jq -c '.data' 2>/dev/null || echo "$response")"
        return 0
    else
        log_error "MQTT 压测启动失败: $response"
        return 1
    fi
}

# 测试 5: 监控压测状态
test_monitor_status() {
    log_info "测试 5: 监控压测状态"
    
    local duration=${TEST_DURATION}
    local interval=5
    local elapsed=0
    
    while [ $elapsed -lt $duration ]; do
        local response=$(http_get "${AUDIENCE_API}/api/stress/status")
        local state=$(echo "$response" | jq -r '.data.state // "UNKNOWN"' 2>/dev/null)
        local qps=$(echo "$response" | jq -r '.data.qps // 0' 2>/dev/null)
        local active_clients=$(echo "$response" | jq -r '.data.activeClients // 0' 2>/dev/null)
        local total_requests=$(echo "$response" | jq -r '.data.totalRequests // 0' 2>/dev/null)
        local success_rate=$(echo "$response" | jq -r '.data.successRate // 0' 2>/dev/null)
        
        printf "\r  状态: %-10s | 活跃连接: %-5s | 总请求: %-8s | QPS: %-6.1f | 成功率: %-5.1f%% | 时间: %ss/%ss" \
            "$state" "$active_clients" "$total_requests" "$qps" "$success_rate" "$elapsed" "$duration"
        
        if [ "$state" == "COMPLETED" ] || [ "$state" == "FAILED" ]; then
            echo ""
            log_info "压测结束，状态: $state"
            break
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
    done
    
    echo ""
    log_success "压测监控完成"
    return 0
}

# 测试 6: 获取压测报告
test_get_report() {
    log_info "测试 6: 获取压测报告"
    
    # 等待测试完成
    sleep 2
    
    local response=$(http_get "${AUDIENCE_API}/api/stress/report")
    local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "报告获取成功"
        echo "========== 压测报告 =========="
        echo "$response" | jq -r '.data' 2>/dev/null || echo "$response"
        echo "=============================="
        return 0
    else
        log_warn "报告获取失败，尝试获取指标..."
        local metrics=$(http_get "${AUDIENCE_API}/api/stress/metrics")
        echo "指标数据:"
        echo "$metrics" | jq '.' 2>/dev/null || echo "$metrics"
        return 0
    fi
}

# 测试 7: 停止压测
test_stop_stress() {
    log_info "测试 7: 停止压测"
    
    local response=$(http_post "${AUDIENCE_API}/api/stress/stop" '{}')
    local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "压测停止成功"
        return 0
    else
        log_warn "压测停止请求返回: $response"
        return 0
    fi
}

# 测试 8: WebSocket 压测测试
test_start_ws_stress() {
    log_info "测试 8: 启动 WebSocket 压测"
    
    local request='{
        "concurrency": 50,
        "durationSeconds": 30,
        "requestsPerSecondPerClient": 5,
        "targetHost": "'"$TARGET_HOST"'",
        "targetPort": 1889,
        "protocol": "websocket"
    }'
    
    local response=$(http_post "${AUDIENCE_API}/api/stress/config" "$request")
    log_info "WebSocket 配置已更新"
    
    local start_response=$(http_post "${AUDIENCE_API}/api/stress/start" '{}')
    local code=$(echo "$start_response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "WebSocket 压测启动成功"
        sleep 5
        test_stop_stress > /dev/null 2>&1 || true
        return 0
    else
        log_warn "WebSocket 压测启动失败: $start_response"
        return 1
    fi
}

# ==================== 主流程 ====================

show_help() {
    cat <<EOF
Z-Audience 压测客户端端到端测试脚本

用法: $0 [选项]

选项:
    -t, --target HOST       目标服务器地址 (默认: host.docker.internal)
    -p, --port PORT         目标服务器端口 (默认: 1883)
    -c, --concurrency N     并发连接数 (默认: 100)
    -d, --duration SECONDS  测试持续时间 (默认: 30)
    -h, --help              显示帮助信息

示例:
    # 基本测试
    $0

    # 指定目标和并发数
    $0 -t 192.168.1.100 -p 1883 -c 500 -d 60

    # 测试 Docker 内部的目标
    $0 -t host.docker.internal -p 1883

EOF
}

parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -t|--target)
                TARGET_HOST="$2"
                shift 2
                ;;
            -p|--port)
                TARGET_PORT="$2"
                shift 2
                ;;
            -c|--concurrency)
                CONCURRENCY="$2"
                shift 2
                ;;
            -d|--duration)
                TEST_DURATION="$2"
                shift 2
                ;;
            -h|--help)
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
}

main() {
    parse_args "$@"
    
    echo "========================================"
    echo "Z-Audience 压测客户端端到端测试"
    echo "========================================"
    echo ""
    echo "配置信息:"
    echo "  Audience API: $AUDIENCE_API"
    echo "  目标服务器:   $TARGET_HOST:$TARGET_PORT"
    echo "  并发连接数:   $CONCURRENCY"
    echo "  测试时长:     ${TEST_DURATION}秒"
    echo ""
    
    local passed=0
    local failed=0
    
    # 等待 Audience 服务就绪
    log_info "等待 Audience 服务就绪..."
    for i in {1..30}; do
        if curl -sf "${AUDIENCE_API}/actuator/health" > /dev/null 2>&1; then
            break
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    
    # 执行测试
    test_audience_health && ((passed++)) || ((failed++))
    test_get_config && ((passed++)) || ((failed++))
    test_update_config && ((passed++)) || ((failed++))
    test_start_mqtt_stress && ((passed++)) || ((failed++))
    test_monitor_status && ((passed++)) || ((failed++))
    test_get_report && ((passed++)) || ((failed++))
    test_stop_stress && ((passed++)) || ((failed++))
    test_start_ws_stress && ((passed++)) || ((failed++))
    
    # 汇总
    echo ""
    echo "========================================"
    echo "测试结果汇总"
    echo "========================================"
    echo -e "通过: ${GREEN}$passed${NC}"
    echo -e "失败: ${RED}$failed${NC}"
    
    if [ $failed -eq 0 ]; then
        echo -e "${GREEN}所有测试通过!${NC}"
        exit 0
    else
        echo -e "${YELLOW}部分测试失败${NC}"
        exit 1
    fi
}

main "$@"
