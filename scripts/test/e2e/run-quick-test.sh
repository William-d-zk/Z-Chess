#!/bin/bash
#
# 快速功能测试脚本
# 用于 CI/CD 或开发验证
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
source "$SCRIPT_DIR/lib/test-utils.sh"

API_ENDPOINT="http://localhost:8080"
MQTT_HOST="localhost"
MQTT_PORT=1883

# 测试结果
TESTS_PASSED=0
TESTS_FAILED=0

#######################################
# 测试: 服务健康检查
#######################################
test_health_check() {
    log_info "测试: 服务健康检查"
    
    local response=$(http_get "${API_ENDPOINT}/actuator/health")
    local code=$(get_http_code "$response")
    
    if assert_http_ok "$code" "健康检查"; then
        ((TESTS_PASSED++))
        return 0
    else
        ((TESTS_FAILED++))
        return 1
    fi
}

#######################################
# 测试: 集群状态
#######################################
test_cluster_status() {
    log_info "测试: 集群状态"
    
    local response=$(http_get "${API_ENDPOINT}/cluster/close")
    local code=$(get_http_code "$response")
    local body=$(get_response_body "$response")
    
    if [ "$code" == "200" ]; then
        local code_val=$(echo "$body" | jq -r '.code // -1')
        if [ "$code_val" == "0" ]; then
            log_info "集群状态: 正常"
            log_success "集群状态 API"
            ((TESTS_PASSED++))
            return 0
        fi
    fi
    
    log_error "集群状态 API (HTTP $code)"
    ((TESTS_FAILED++))
    return 1
}

#######################################
# 测试: 设备查询
#######################################
test_device_query() {
    log_info "测试: 设备查询"
    
    local response=$(http_get "${API_ENDPOINT}/device/online/all")
    local code=$(get_http_code "$response")
    local body=$(get_response_body "$response")
    
    if [ "$code" == "200" ]; then
        local code_val=$(echo "$body" | jq -r '.code // -1')
        if [ "$code_val" == "0" ]; then
            log_success "设备查询 API"
            ((TESTS_PASSED++))
            return 0
        fi
    fi
    
    log_error "设备查询 API (HTTP $code)"
    ((TESTS_FAILED++))
    return 1
}

#######################################
# 测试: MQTT 连接
#######################################
test_mqtt_connection() {
    log_info "测试: MQTT 连接"
    
    # 检查端口 (使用 nc 或 bash 内置)
    local port_open=false
    if command -v nc > /dev/null 2>&1; then
        if nc -z "${MQTT_HOST}" "${MQTT_PORT}" 2>/dev/null; then
            port_open=true
        fi
    elif command -v bash > /dev/null 2>&1; then
        if bash -c "exec 3<>/dev/tcp/${MQTT_HOST}/${MQTT_PORT}" 2>/dev/null; then
            port_open=true
        fi
    fi
    
    if [ "$port_open" == "true" ]; then
        log_success "MQTT 端口可连接"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "MQTT 端口不可连接"
        ((TESTS_FAILED++))
        return 1
    fi
}

#######################################
# 测试: 数据库连接
#######################################
test_database_connection() {
    log_info "测试: 数据库连接"
    
    # 通过健康检查间接验证
    local response=$(http_get "${API_ENDPOINT}/actuator/health")
    local code=$(get_http_code "$response")
    local body=$(get_response_body "$response")
    
    if [ "$code" == "200" ]; then
        log_success "数据库连接"
        ((TESTS_PASSED++))
        return 0
    else
        log_error "数据库连接检查失败"
        ((TESTS_FAILED++))
        return 1
    fi
}

#######################################
# 运行所有测试
#######################################
run_all_tests() {
    log_info "========================================"
    log_info "     Z-Chess 快速功能测试"
    log_info "========================================"
    echo ""
    
    local start_time=$(date +%s)
    
    # 等待服务就绪
    log_info "等待服务就绪..."
    if ! wait_for_service "$API_ENDPOINT/actuator/health" 60; then
        log_error "服务未就绪，终止测试"
        return 1
    fi
    
    # 执行测试
    test_health_check || true
    test_cluster_status || true
    test_database_connection || true
    test_mqtt_connection || true
    test_device_query || true
    
    # 统计
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo ""
    log_info "========================================"
    log_info "测试完成"
    log_info "========================================"
    log_info "通过: $TESTS_PASSED"
    log_info "失败: $TESTS_FAILED"
    log_info "耗时: ${duration}s"
    
    if [ $TESTS_FAILED -eq 0 ]; then
        log_success "所有测试通过!"
        return 0
    else
        log_warn "部分测试失败"
        return 1
    fi
}

# 帮助信息
show_help() {
    cat <<EOF
快速功能测试脚本

用法: $0 [选项]

选项:
    -e, --endpoint URL      API 端点 (默认: http://localhost:8080)
    --mqtt-host HOST        MQTT 主机 (默认: localhost)
    --mqtt-port PORT        MQTT 端口 (默认: 1883)
    -h, --help              显示帮助

EOF
}

# 解析参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--endpoint)
            API_ENDPOINT="$2"
            shift 2
            ;;
        --mqtt-host)
            MQTT_HOST="$2"
            shift 2
            ;;
        --mqtt-port)
            MQTT_PORT="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "未知选项: $1"
            exit 1
            ;;
    esac
done

# 运行测试
run_all_tests
exit $?
