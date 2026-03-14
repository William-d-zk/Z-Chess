#!/bin/bash
#
# Z-Audience 快速测试脚本
# 用于快速验证 Z-Audience 功能和集群连通性
#

set -e

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

AUDIENCE_HOST="${AUDIENCE_HOST:-localhost}"
AUDIENCE_PORT="${AUDIENCE_PORT:-8081}"

print_header() {
    echo -e "\n${BLUE}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}\n"
}

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${YELLOW}ℹ $1${NC}"; }

# 测试1: 服务健康检查
test_service_health() {
    print_header "1. Z-Audience 服务健康检查"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/actuator/health" 2>&1) && {
        local status
        status=$(echo "$response" | jq -r '.status')
        if [ "$status" = "UP" ]; then
            print_success "Z-Audience 服务状态: $status"
            echo "$response" | jq '.'
            return 0
        fi
    }
    
    print_error "Z-Audience 服务不可用"
    return 1
}

# 测试2: 设备凭证加载
test_device_credentials() {
    print_header "2. 设备凭证加载测试"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/mqtt/devices/count" 2>&1) && {
        local count
        count=$(echo "$response" | jq -r '.data.totalDevices // 0')
        local has_devices
        has_devices=$(echo "$response" | jq -r '.data.hasDevices')
        
        if [ "$has_devices" = "true" ] && [ "$count" -gt 0 ]; then
            print_success "已加载 $count 个设备凭证"
            echo "$response" | jq '.'
            
            # 显示一个样本设备
            local sample
            sample=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/mqtt/devices/sample" 2>&1)
            print_info "样本设备信息:"
            echo "$sample" | jq '.'
            return 0
        fi
    }
    
    print_error "未找到设备凭证，请检查设备文件是否正确挂载"
    return 1
}

# 测试3: 集群健康检查
test_cluster_health() {
    print_header "3. Z-Chess 集群健康检查"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/test/health" 2>&1) && {
        local healthy_nodes
        healthy_nodes=$(echo "$response" | jq -r '.data.healthyNodes // 0')
        
        if [ "$healthy_nodes" -gt 0 ]; then
            print_success "$healthy_nodes 个节点健康"
            echo "$response" | jq '.data.results'
            return 0
        fi
    }
    
    print_error "集群健康检查失败"
    return 1
}

# 测试4: MQTT 单设备连接
test_mqtt_connection() {
    print_header "4. MQTT 单设备连接测试"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/mqtt/connect" 2>&1) && {
        local connected
        connected=$(echo "$response" | jq -r '.data.connected')
        local time
        time=$(echo "$response" | jq -r '.data.connectTimeMs')
        
        if [ "$connected" = "true" ]; then
            print_success "MQTT 连接成功 (${time}ms)"
            echo "$response" | jq '.data | {test, server, connected, connectTimeMs, status}'
            return 0
        fi
    }
    
    print_error "MQTT 连接失败"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    return 1
}

# 测试5: API 快速测试
test_api_quick() {
    print_header "5. REST API 快速测试"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/test/devices?iterations=1" 2>&1) && {
        local success
        success=$(echo "$response" | jq -r '.data.success // 0')
        
        if [ "$success" -gt 0 ]; then
            print_success "API 测试通过"
            echo "$response" | jq '.data | {test, success, failed, avgLatencyMs}'
            return 0
        fi
    }
    
    print_error "API 测试失败"
    return 1
}

# 测试6: 压测配置检查
test_pressure_config() {
    print_header "6. 压测配置检查"
    
    local response
    response=$(curl -sf "http://${AUDIENCE_HOST}:${AUDIENCE_PORT}/api/stress/config" 2>&1) && {
        print_success "压测配置获取成功"
        echo "$response" | jq '.data'
        return 0
    }
    
    print_error "无法获取压测配置"
    return 1
}

# 主函数
main() {
    print_header "Z-Audience 快速测试"
    
    echo "Z-Audience: http://${AUDIENCE_HOST}:${AUDIENCE_PORT}"
    echo ""
    
    # 检查依赖
    if ! command -v jq &> /dev/null; then
        echo "请安装 jq: brew install jq"
        exit 1
    fi
    
    local failed=0
    
    test_service_health || failed=$((failed + 1))
    test_device_credentials || failed=$((failed + 1))
    test_cluster_health || failed=$((failed + 1))
    test_mqtt_connection || failed=$((failed + 1))
    test_api_quick || failed=$((failed + 1))
    test_pressure_config || failed=$((failed + 1))
    
    echo ""
    print_header "测试结果"
    
    if [ $failed -eq 0 ]; then
        print_success "所有快速测试通过!"
        echo ""
        echo "可以运行完整 E2E 测试:"
        echo "  ./run-z-audience-e2e.sh"
        exit 0
    else
        print_error "$failed 个测试失败"
        exit 1
    fi
}

main "$@"
