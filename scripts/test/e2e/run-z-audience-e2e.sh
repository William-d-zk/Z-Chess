#!/bin/bash
#
# Z-Audience 端到端测试脚本
# 
# 功能：
# 1. 健康检查 - 测试所有集群节点
# 2. API 测试 - 测试 REST API 端点
# 3. MQTT 测试 - 使用已注册设备测试 MQTT 连接
# 4. 压力测试 - 并发连接和消息测试
# 5. 生成测试报告
#

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
AUDIENCE_HOST="${AUDIENCE_HOST:-localhost}"
AUDIENCE_PORT="${AUDIENCE_PORT:-8081}"
RAFT_HOST="${RAFT_HOST:-localhost}"
RAFT_HTTP_PORT="${RAFT_HTTP_PORT:-8080}"
RAFT_MQTT_PORT="${RAFT_MQTT_PORT:-1883}"
REPORT_DIR="${REPORT_DIR:-./reports}"
DEVICE_FILE="${DEVICE_FILE:-./data/devices-100.json}"

# 测试配置
API_CONCURRENCY="${API_CONCURRENCY:-50}"
API_REQUESTS="${API_REQUESTS:-100}"
MQTT_DEVICES="${MQTT_DEVICES:-20}"
MQTT_CONNECTIONS_PER_DEVICE="${MQTT_CONNECTIONS_PER_DEVICE:-2}"
PRESSURE_CONCURRENCY="${PRESSURE_CONCURRENCY:-100}"
PRESSURE_DURATION="${PRESSURE_DURATION:-30}"

# 测试结果
TEST_RESULTS=()
OVERALL_SUCCESS=true

# 打印函数
print_header() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# 检查 Z-Audience 是否可用
check_audience() {
    print_header "检查 Z-Audience 服务"
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/actuator/health" > /dev/null 2>&1; then
            print_success "Z-Audience 服务已就绪 (http://${AUDIENCE_HOST}:${AUDIENCE_PORT})"
            return 0
        fi
        
        echo "等待 Z-Audience 服务... ($attempt/$max_attempts)"
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "Z-Audience 服务不可用"
    return 1
}

# 测试 1: 健康检查
test_health_check() {
    print_header "测试 1: 集群健康检查"
    
    local result
    result=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/test/health?host=${RAFT_HOST}&port=${RAFT_HTTP_PORT}&nodes=3" 2>&1) || {
        print_error "健康检查失败"
        TEST_RESULTS+=("health: FAILED")
        return 1
    }
    
    echo "$result" | jq '.'
    
    local healthy_nodes
    healthy_nodes=$(echo "$result" | jq -r '.data.healthyNodes // 0')
    
    if [ "$healthy_nodes" -ge 1 ]; then
        print_success "健康检查通过: $healthy_nodes/3 节点健康"
        TEST_RESULTS+=("health: PASSED ($healthy_nodes/3 nodes)")
    else
        print_error "健康检查失败: 没有可用节点"
        TEST_RESULTS+=("health: FAILED (0/3 nodes)")
        OVERALL_SUCCESS=false
    fi
}

# 测试 2: API 测试
test_api() {
    print_header "测试 2: REST API 测试"
    
    # 2.1 设备列表查询
    print_info "测试设备列表查询..."
    local device_result
    device_result=$(curl -sf -X GET "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/test/devices?host=${RAFT_HOST}&port=${RAFT_HTTP_PORT}&iterations=5" 2>&1) || {
        print_error "设备列表查询失败"
        TEST_RESULTS+=("api_devices: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo "$device_result" | jq '.'
    TEST_RESULTS+=("api_devices: PASSED")
    
    # 2.2 并发压力测试
    print_info "测试 API 并发压力..."
    local stress_result
    stress_result=$(curl -sf -X POST "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/test/stress" \
        -H "Content-Type: application/json" \
        -d "{
            \"host\": \"${RAFT_HOST}\",
            \"port\": ${RAFT_HTTP_PORT},
            \"endpoint\": \"/actuator/health\",
            \"concurrency\": ${API_CONCURRENCY},
            \"requestsPerClient\": ${API_REQUESTS}
        }" 2>&1) || {
        print_error "API 压力测试失败"
        TEST_RESULTS+=("api_stress: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo "$stress_result" | jq '.'
    
    local success_rate
    success_rate=$(echo "$stress_result" | jq -r '.data.successRate // 0')
    
    if (( $(echo "$success_rate >= 95.0" | bc -l) )); then
        print_success "API 压力测试通过: 成功率 ${success_rate}%"
        TEST_RESULTS+=("api_stress: PASSED (${success_rate}% success)")
    else
        print_error "API 压力测试失败: 成功率 ${success_rate}% 低于 95%"
        TEST_RESULTS+=("api_stress: FAILED (${success_rate}% success)")
        OVERALL_SUCCESS=false
    fi
}

# 测试 3: MQTT 连接测试
test_mqtt() {
    print_header "测试 3: MQTT 设备连接测试"
    
    # 3.1 单设备连接测试
    print_info "测试单设备 MQTT 连接..."
    local single_result
    single_result=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/mqtt/connect?host=${RAFT_HOST}&port=${RAFT_MQTT_PORT}" 2>&1) || {
        print_error "单设备 MQTT 连接测试失败"
        TEST_RESULTS+=("mqtt_single: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo "$single_result" | jq '.'
    
    local connected
    connected=$(echo "$single_result" | jq -r '.data.connected // false')
    
    if [ "$connected" = "true" ]; then
        print_success "单设备 MQTT 连接成功"
        TEST_RESULTS+=("mqtt_single: PASSED")
    else
        print_error "单设备 MQTT 连接失败"
        TEST_RESULTS+=("mqtt_single: FAILED")
        OVERALL_SUCCESS=false
    fi
    
    # 3.2 多设备并发测试
    print_info "测试多设备 MQTT 并发连接..."
    local stress_result
    stress_result=$(curl -sf -X POST "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/mqtt/stress" \
        -H "Content-Type: application/json" \
        -d "{
            \"host\": \"${RAFT_HOST}\",
            \"port\": ${RAFT_MQTT_PORT},
            \"deviceCount\": ${MQTT_DEVICES},
            \"connectionsPerDevice\": ${MQTT_CONNECTIONS_PER_DEVICE}
        }" 2>&1) || {
        print_error "MQTT 并发测试失败"
        TEST_RESULTS+=("mqtt_stress: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo "$stress_result" | jq '.'
    
    local success_rate
    success_rate=$(echo "$stress_result" | jq -r '.data.successRate // 0')
    
    if (( $(echo "$success_rate >= 90.0" | bc -l) )); then
        print_success "MQTT 并发测试通过: 成功率 ${success_rate}%"
        TEST_RESULTS+=("mqtt_stress: PASSED (${success_rate}% success)")
    else
        print_error "MQTT 并发测试失败: 成功率 ${success_rate}% 低于 90%"
        TEST_RESULTS+=("mqtt_stress: FAILED (${success_rate}% success)")
        OVERALL_SUCCESS=false
    fi
}

# 测试 4: 协议压力测试
test_pressure() {
    print_header "测试 4: 协议压力测试"
    
    print_info "启动 MQTT 压力测试 (${PRESSURE_CONCURRENCY} 并发, ${PRESSURE_DURATION}秒)..."
    
    # 启动压力测试
    local start_result
    start_result=$(curl -sf -X POST "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/stress/start" \
        -H "Content-Type: application/json" \
        -d "{
            \"concurrency\": ${PRESSURE_CONCURRENCY},
            \"durationSeconds\": ${PRESSURE_DURATION},
            \"targetHost\": \"${RAFT_HOST}\",
            \"targetPort\": ${RAFT_MQTT_PORT},
            \"protocol\": \"mqtt\"
        }" 2>&1) || {
        print_error "启动压力测试失败"
        TEST_RESULTS+=("pressure: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo "$start_result" | jq '.'
    
    # 等待测试完成
    local wait_time=$PRESSURE_DURATION
    while [ $wait_time -gt 0 ]; do
        sleep 5
        wait_time=$((wait_time - 5))
        
        local status
        status=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/stress/status" 2>&1) || continue
        
        local state
        state=$(echo "$status" | jq -r '.data.state // "UNKNOWN"')
        local qps
        qps=$(echo "$status" | jq -r '.data.qps // 0')
        local active_clients
        active_clients=$(echo "$status" | jq -r '.data.activeClients // 0')
        
        echo "状态: $state | 活跃连接: $active_clients | QPS: $qps | 剩余: ${wait_time}s"
        
        if [ "$state" = "COMPLETED" ] || [ "$state" = "IDLE" ]; then
            break
        fi
    done
    
    # 获取最终结果
    sleep 2
    local final_status
    final_status=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/stress/status" 2>&1) || {
        print_error "获取压力测试结果失败"
        TEST_RESULTS+=("pressure: FAILED")
        OVERALL_SUCCESS=false
        return 1
    }
    
    echo -e "\n${BLUE}压力测试结果:${NC}"
    echo "$final_status" | jq '.'
    
    local success_rate
    success_rate=$(echo "$final_status" | jq -r '.data.successRate // 0')
    
    if (( $(echo "$success_rate >= 85.0" | bc -l) )); then
        print_success "压力测试通过: 成功率 ${success_rate}%"
        TEST_RESULTS+=("pressure: PASSED (${success_rate}% success)")
    else
        print_error "压力测试失败: 成功率 ${success_rate}% 低于 85%"
        TEST_RESULTS+=("pressure: FAILED (${success_rate}% success)")
        OVERALL_SUCCESS=false
    fi
}

# 生成测试报告
generate_report() {
    print_header "测试报告"
    
    local timestamp
    timestamp=$(date '+%Y%m%d_%H%M%S')
    local report_file="${REPORT_DIR}/z-audience-e2e-report-${timestamp}.json"
    
    # 创建报告目录
    mkdir -p "$REPORT_DIR"
    
    # 生成 JSON 报告
    cat > "$report_file" << EOF
{
    "testRun": {
        "timestamp": "$(date -Iseconds)",
        "audienceHost": "${AUDIENCE_HOST}",
        "audiencePort": ${AUDIENCE_PORT},
        "targetHost": "${RAFT_HOST}",
        "targetHttpPort": ${RAFT_HTTP_PORT},
        "targetMqttPort": ${RAFT_MQTT_PORT}
    },
    "configuration": {
        "apiConcurrency": ${API_CONCURRENCY},
        "apiRequestsPerClient": ${API_REQUESTS},
        "mqttDevices": ${MQTT_DEVICES},
        "mqttConnectionsPerDevice": ${MQTT_CONNECTIONS_PER_DEVICE},
        "pressureConcurrency": ${PRESSURE_CONCURRENCY},
        "pressureDuration": ${PRESSURE_DURATION}
    },
    "results": [
$(printf '        "%s",\n' "${TEST_RESULTS[@]}" | sed '$s/,$//')
    ],
    "overallStatus": "$([ "$OVERALL_SUCCESS" = true ] && echo "PASSED" || echo "FAILED")"
}
EOF
    
    echo -e "\n${BLUE}测试结果汇总:${NC}"
    echo "───────────────────────────────────────────────────────────────"
    for result in "${TEST_RESULTS[@]}"; do
        if [[ "$result" == *"PASSED"* ]]; then
            echo -e "  ${GREEN}✓${NC} $result"
        else
            echo -e "  ${RED}✗${NC} $result"
        fi
    done
    echo "───────────────────────────────────────────────────────────────"
    
    if [ "$OVERALL_SUCCESS" = true ]; then
        print_success "所有测试通过!"
        echo -e "\n${GREEN}整体状态: PASSED${NC}"
    else
        print_error "部分测试失败"
        echo -e "\n${RED}整体状态: FAILED${NC}"
    fi
    
    print_info "详细报告已保存: $report_file"
}

# 显示使用帮助
show_usage() {
    cat << EOF
Z-Audience 端到端测试脚本

用法: $0 [选项]

选项:
    -h, --help              显示帮助信息
    --host HOST             Z-Audience 主机 (默认: localhost)
    --port PORT             Z-Audience 端口 (默认: 8081)
    --raft-host HOST        Raft 集群主机 (默认: localhost)
    --raft-http PORT        Raft HTTP 端口 (默认: 8080)
    --raft-mqtt PORT        Raft MQTT 端口 (默认: 1883)
    --api-concurrency N     API 并发数 (默认: 50)
    --api-requests N        API 每客户端请求数 (默认: 100)
    --mqtt-devices N        MQTT 测试设备数 (默认: 20)
    --mqtt-conn-per-dev N   MQTT 每设备连接数 (默认: 2)
    --pressure-conc N       压力测试并发数 (默认: 100)
    --pressure-duration S   压力测试持续时间秒 (默认: 30)
    --skip-health           跳过健康检查
    --skip-api              跳过 API 测试
    --skip-mqtt             跳过 MQTT 测试
    --skip-pressure         跳过压力测试

示例:
    $0                                      # 运行所有测试
    $0 --host 172.30.10.100                 # 指定 Z-Audience 地址
    $0 --raft-host 172.30.10.110            # 指定 Raft 集群地址
    $0 --skip-pressure                      # 跳过压力测试

EOF
}

# 解析命令行参数
SKIP_HEALTH=false
SKIP_API=false
SKIP_MQTT=false
SKIP_PRESSURE=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_usage
            exit 0
            ;;
        --host)
            AUDIENCE_HOST="$2"
            shift 2
            ;;
        --port)
            AUDIENCE_PORT="$2"
            shift 2
            ;;
        --raft-host)
            RAFT_HOST="$2"
            shift 2
            ;;
        --raft-http)
            RAFT_HTTP_PORT="$2"
            shift 2
            ;;
        --raft-mqtt)
            RAFT_MQTT_PORT="$2"
            shift 2
            ;;
        --api-concurrency)
            API_CONCURRENCY="$2"
            shift 2
            ;;
        --api-requests)
            API_REQUESTS="$2"
            shift 2
            ;;
        --mqtt-devices)
            MQTT_DEVICES="$2"
            shift 2
            ;;
        --mqtt-conn-per-dev)
            MQTT_CONNECTIONS_PER_DEVICE="$2"
            shift 2
            ;;
        --pressure-conc)
            PRESSURE_CONCURRENCY="$2"
            shift 2
            ;;
        --pressure-duration)
            PRESSURE_DURATION="$2"
            shift 2
            ;;
        --skip-health)
            SKIP_HEALTH=true
            shift
            ;;
        --skip-api)
            SKIP_API=true
            shift
            ;;
        --skip-mqtt)
            SKIP_MQTT=true
            shift
            ;;
        --skip-pressure)
            SKIP_PRESSURE=true
            shift
            ;;
        *)
            echo "未知选项: $1"
            show_usage
            exit 1
            ;;
    esac
done

# 主函数
main() {
    print_header "Z-Audience 端到端测试"
    print_info "配置:"
    echo "  Z-Audience: ${AUDIENCE_HOST}:${AUDIENCE_PORT}"
    echo "  Raft HTTP:  ${RAFT_HOST}:${RAFT_HTTP_PORT}"
    echo "  Raft MQTT:  ${RAFT_HOST}:${RAFT_MQTT_PORT}"
    echo ""
    
    # 检查依赖
    if ! command -v jq &> /dev/null; then
        print_error "需要安装 jq: brew install jq 或 apt-get install jq"
        exit 1
    fi
    
    if ! command -v bc &> /dev/null; then
        print_error "需要安装 bc"
        exit 1
    fi
    
    # 检查 Z-Audience
    check_audience || exit 1
    
    # 运行测试
    [ "$SKIP_HEALTH" = false ] && test_health_check
    [ "$SKIP_API" = false ] && test_api
    [ "$SKIP_MQTT" = false ] && test_mqtt
    [ "$SKIP_PRESSURE" = false ] && test_pressure
    
    # 生成报告
    generate_report
    
    # 返回状态
    if [ "$OVERALL_SUCCESS" = true ]; then
        exit 0
    else
        exit 1
    fi
}

main "$@"
