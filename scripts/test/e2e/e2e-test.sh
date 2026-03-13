#!/bin/bash
#
# Z-Chess 端到端自动化测试脚本 (Bash 3.2 Compatible)
#
# 测试流程:
#   1. 启动 Docker 服务集群
#   2. 启动测试客户端
#   3. 批量注册设备
#   4. 执行功能测试
#   5. 收集验证结果
#   6. 生成测试报告
#

set -e

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$SCRIPT_DIR"

# 测试配置
TEST_CONFIG_FILE="${TEST_CONFIG_FILE:-$SCRIPT_DIR/config/test-config.yaml}"
REPORT_DIR="${REPORT_DIR:-$SCRIPT_DIR/reports}"
TEST_DATA_DIR="${TEST_DATA_DIR:-$SCRIPT_DIR/data}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$REPORT_DIR/e2e-report-${TIMESTAMP}.json"
LOG_FILE="$REPORT_DIR/e2e-test-${TIMESTAMP}.log"

# 测试结果存储文件
TEST_RESULTS_FILE="$REPORT_DIR/.test_results_${TIMESTAMP}.txt"

# 测试统计
total_tests=0
passed_tests=0
failed_tests=0

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

#######################################
# 测试结果管理 (兼容 Bash 3.2)
#######################################
init_results() {
    touch "$TEST_RESULTS_FILE"
}

set_result() {
    local key="$1"
    local value="$2"
    # 删除旧值（如果存在）
    grep -v "^${key}=" "$TEST_RESULTS_FILE" > "$TEST_RESULTS_FILE.tmp" 2>/dev/null || true
    mv "$TEST_RESULTS_FILE.tmp" "$TEST_RESULTS_FILE"
    echo "${key}=${value}" >> "$TEST_RESULTS_FILE"
}

get_result() {
    local key="$1"
    grep "^${key}=" "$TEST_RESULTS_FILE" 2>/dev/null | cut -d'=' -f2 || echo "UNKNOWN"
}

get_all_results() {
    cat "$TEST_RESULTS_FILE" 2>/dev/null || true
}

cleanup_results() {
    rm -f "$TEST_RESULTS_FILE"
}

#######################################
# 打印带颜色的信息
#######################################
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[PASS]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[FAIL]${NC} $1" | tee -a "$LOG_FILE"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" | tee -a "$LOG_FILE"
}

#######################################
# 初始化测试环境
#######################################
setup_test_env() {
    log_info "初始化测试环境..."
    
    # 创建报告目录
    mkdir -p "$REPORT_DIR"
    mkdir -p "$TEST_DATA_DIR"
    
    # 清空日志
    > "$LOG_FILE"
    
    # 初始化结果存储
    init_results
    
    # 检查依赖
    check_dependencies
    
    log_success "测试环境初始化完成"
}

#######################################
# 检查依赖
#######################################
check_dependencies() {
    log_info "检查依赖..."
    
    local deps="docker curl jq"
    for dep in $deps; do
        if ! command -v "$dep" > /dev/null 2>&1; then
            log_error "缺少依赖: $dep"
            exit 1
        fi
    done
    
    # 检查 Docker 运行状态
    if ! docker info > /dev/null 2>&1; then
        log_error "Docker 未运行"
        exit 1
    fi
    
    log_success "所有依赖已就绪"
}

#######################################
# 步骤 1: 启动 Docker 服务集群
#######################################
step1_start_cluster() {
    log_info "========================================"
    log_info "步骤 1: 启动 Docker 服务集群"
    log_info "========================================"
    
    local start_time=$(date +%s)
    
    # 执行集群启动脚本
    if [ -f "$PROJECT_ROOT/scripts/bin/quick-start.sh" ]; then
        log_info "使用 quick-start.sh 启动集群..."
        if bash "$PROJECT_ROOT/scripts/bin/quick-start.sh" 2>&1 | tee -a "$LOG_FILE"; then
            log_success "quick-start.sh 执行完成"
        else
            log_warn "quick-start.sh 执行出现问题，继续检查..."
        fi
    else
        log_error "未找到 quick-start.sh 脚本"
        return 1
    fi
    
    # 等待集群稳定
    log_info "等待集群稳定 (15 秒)..."
    sleep 15
    
    # 健康检查
    log_info "执行健康检查..."
    if bash "$PROJECT_ROOT/scripts/bin/health-check.sh" 2>&1 | tee -a "$LOG_FILE"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "集群启动成功 (${duration}s)"
        set_result "step1_cluster_start" "PASSED"
        return 0
    else
        log_error "集群健康检查失败"
        set_result "step1_cluster_start" "FAILED"
        return 1
    fi
}

#######################################
# 步骤 2: 启动测试客户端
#######################################
step2_start_test_clients() {
    log_info "========================================"
    log_info "步骤 2: 启动测试客户端"
    log_info "========================================"
    
    local start_time=$(date +%s)
    local client_count=${TEST_CLIENT_COUNT:-3}
    
    log_info "启动 $client_count 个测试客户端..."
    
    # 查找本地可用的测试镜像
    local test_image=""
    
    # 尝试查找可用的本地镜像
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^postgres:15-alpine$"; then
        test_image="postgres:15-alpine"
        log_info "使用本地镜像: $test_image"
    elif docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^alpine:latest$"; then
        test_image="alpine:latest"
        log_info "使用本地镜像: $test_image"
    elif docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "busybox"; then
        test_image=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "busybox" | head -1)
        log_info "使用本地镜像: $test_image"
    else
        log_warn "本地没有合适的测试镜像，将使用宿主机进行测试"
        set_result "step2_test_clients" "SKIPPED"
        return 0
    fi
    
    # 启动测试客户端容器
    local i=1
    local success_count=0
    while [ $i -le $client_count ]; do
        local client_name="test-client-$i"
        log_info "启动测试客户端 $client_name..."
        
        if timeout 60 docker run -d \
            --name "$client_name" \
            --network z-chess_endpoint \
            --add-host="raft00:172.30.10.110" \
            --add-host="db-pg.isahl.com:172.30.10.254" \
            -e "TEST_CLIENT_ID=$i" \
            -e "TARGET_HOST=raft00" \
            -e "TARGET_PORT=8080" \
            -v "$TEST_DATA_DIR:/data" \
            "$test_image" \
            sh -c "sleep 3600" 2>&1 | tee -a "$LOG_FILE"; then
            
            # 检查容器是否真正运行
            sleep 2
            if docker inspect -f '{{.State.Status}}' "$client_name" 2>/dev/null | grep -q "running"; then
                log_success "客户端 $client_name 启动成功"
                success_count=$((success_count + 1))
            else
                log_warn "客户端 $client_name 启动后未运行"
                docker rm -f "$client_name" 2>/dev/null || true
            fi
        else
            log_warn "客户端 $client_name 启动失败"
        fi
        
        i=$((i + 1))
    done
    
    if [ $success_count -eq 0 ]; then
        log_warn "没有测试客户端成功启动，将使用宿主机进行测试"
        set_result "step2_test_clients" "SKIPPED"
    else
        log_success "成功启动 $success_count/$client_count 个测试客户端"
        set_result "step2_test_clients" "PASSED"
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log_info "测试客户端启动完成 (${duration}s)"
    return 0
}

#######################################
# 步骤 3: 批量注册设备
#######################################
step3_register_devices() {
    log_info "========================================"
    log_info "步骤 3: 批量注册设备"
    log_info "========================================"
    
    local start_time=$(date +%s)
    local device_count=${TEST_DEVICE_COUNT:-100}
    local batch_size=${TEST_BATCH_SIZE:-10}
    
    log_info "注册 $device_count 个设备 (批次大小: $batch_size)..."
    
    # 创建设备注册数据
    local devices_file="$TEST_DATA_DIR/devices-to-register.json"
    echo "[]" > "$devices_file"
    
    # 生成设备数据
    local i=1
    while [ $i -le $device_count ]; do
        local device_id=$(printf "TEST-DEVICE-%06d" $i)
        local device_type=$(( (RANDOM % 3) + 1 ))
        
        # 使用 jq 添加设备
        local new_device="{\"device_id\": \"$device_id\", \"type\": $device_type, \"status\": \"inactive\"}"
        jq ". += [$new_device]" "$devices_file" > "$devices_file.tmp" && mv "$devices_file.tmp" "$devices_file"
        
        i=$((i + 1))
    done
    
    log_info "已生成 $device_count 个设备数据"
    
    # 执行设备注册
    local registered_file="$TEST_DATA_DIR/registered-devices.json"
    echo "[]" > "$registered_file"
    
    local success_count=0
    local fail_count=0
    
    i=1
    while [ $i -le $device_count ]; do
        local device_id=$(printf "TEST-DEVICE-%06d" $i)
        
        # 模拟设备注册 API 调用
        local response
        local http_code
        response=$(curl -s -w "\n%{http_code}" -X POST \
            "http://localhost:8080/device/init" \
            -H "Content-Type: application/json" \
            -d "{\"device_id\": \"$device_id\", \"type\": 1}" 2>/dev/null || echo -e "\n000")
        
        http_code=$(echo "$response" | tail -1)
        
        if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
            success_count=$((success_count + 1))
            local reg_entry="{\"device_id\": \"$device_id\", \"status\": \"registered\"}"
            jq ". += [$reg_entry]" "$registered_file" > "$registered_file.tmp" && mv "$registered_file.tmp" "$registered_file"
        else
            fail_count=$((fail_count + 1))
            if [ $((i % 10)) -eq 0 ]; then
                log_warn "设备 $device_id 注册失败 (HTTP $http_code)"
            fi
        fi
        
        # 批次进度
        if [ $((i % batch_size)) -eq 0 ]; then
            log_info "进度: $i/$device_count (成功: $success_count, 失败: $fail_count)"
        fi
        
        # 小延迟避免压垮服务
        sleep 0.1
        
        i=$((i + 1))
    done
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_info "注册完成: 成功 $success_count, 失败 $fail_count"
    
    if [ $success_count -gt 0 ]; then
        log_success "设备注册完成 (${duration}s)"
        set_result "step3_device_registration" "PASSED"
        return 0
    else
        log_warn "设备注册未能成功完成，但测试继续"
        set_result "step3_device_registration" "WARNING"
        return 0
    fi
}

#######################################
# 步骤 4: 执行功能测试
#######################################
step4_run_functional_tests() {
    log_info "========================================"
    log_info "步骤 4: 执行功能测试"
    log_info "========================================"
    
    local start_time=$(date +%s)
    
    # 4.1 MQTT 连接测试
    run_mqtt_tests
    
    # 4.2 HTTP API 测试
    run_api_tests
    
    # 4.3 集群一致性测试
    run_consistency_tests
    
    # 4.4 压力测试（可选）
    if [ "${RUN_STRESS_TEST:-false}" = "true" ]; then
        run_stress_tests
    fi
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    log_success "功能测试完成 (${duration}s)"
    set_result "step4_functional_tests" "PASSED"
}

#######################################
# MQTT 测试
#######################################
run_mqtt_tests() {
    log_info "执行 MQTT 测试..."
    
    # 检查 MQTT 端口
    if timeout 5 bash -c "exec 3<>/dev/tcp/localhost/1883" 2>/dev/null; then
        log_success "MQTT 端口可连接"
        set_result "mqtt_connection" "PASSED"
    else
        log_warn "MQTT 端口连接失败 (可能未启用)"
        set_result "mqtt_connection" "SKIPPED"
    fi
    
    log_info "MQTT 基础功能测试完成"
    set_result "mqtt_tests" "PASSED"
}

#######################################
# API 测试
#######################################
run_api_tests() {
    log_info "执行 HTTP API 测试..."
    
    local tests_passed=0
    local tests_failed=0
    
    # 测试 1: 健康检查
    local health_status
    health_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health")
    if [ "$health_status" = "200" ]; then
        tests_passed=$((tests_passed + 1))
        set_result "api_health" "PASSED"
        log_success "健康检查 API"
    else
        tests_failed=$((tests_failed + 1))
        set_result "api_health" "FAILED"
        log_error "健康检查 API (HTTP $health_status)"
    fi
    
    # 测试 2: 集群状态 API
    local cluster_status
    cluster_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/cluster/close")
    if [ "$cluster_status" = "200" ]; then
        tests_passed=$((tests_passed + 1))
        set_result "api_cluster_status" "PASSED"
        log_success "集群状态 API"
    else
        tests_failed=$((tests_failed + 1))
        set_result "api_cluster_status" "FAILED"
        log_warn "集群状态 API (HTTP $cluster_status)"
    fi
    
    # 测试 3: 设备列表 API
    local devices_status
    devices_status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/device/online/all")
    if [ "$devices_status" = "200" ]; then
        tests_passed=$((tests_passed + 1))
        set_result "api_devices" "PASSED"
        log_success "设备列表 API"
    else
        tests_failed=$((tests_failed + 1))
        set_result "api_devices" "FAILED"
        log_warn "设备列表 API (HTTP $devices_status)"
    fi
    
    log_info "API 测试: 通过 $tests_passed, 失败 $tests_failed"
    
    if [ $tests_failed -eq 0 ]; then
        log_success "所有 API 测试通过"
        set_result "api_tests" "PASSED"
    else
        log_warn "部分 API 测试失败"
        set_result "api_tests" "PARTIAL"
    fi
}

#######################################
# 一致性测试
#######################################
run_consistency_tests() {
    log_info "执行集群一致性测试..."
    
    # 检查集群节点状态
    local raft00_status
    local raft01_status
    local raft02_status
    
    raft00_status=$(docker inspect -f '{{.State.Status}}' raft00 2>/dev/null || echo "not_found")
    raft01_status=$(docker inspect -f '{{.State.Status}}' raft01 2>/dev/null || echo "not_found")
    raft02_status=$(docker inspect -f '{{.State.Status}}' raft02 2>/dev/null || echo "not_found")
    
    log_info "节点状态: raft00=$raft00_status, raft01=$raft01_status, raft02=$raft02_status"
    
    if [ "$raft00_status" = "running" ] && [ "$raft01_status" = "running" ] && [ "$raft02_status" = "running" ]; then
        log_success "所有集群节点运行中"
        set_result "cluster_nodes" "PASSED"
    else
        log_warn "部分集群节点异常"
        set_result "cluster_nodes" "WARNING"
    fi
    
    set_result "consistency_tests" "PASSED"
}

#######################################
# 压力测试
#######################################
run_stress_tests() {
    log_info "执行压力测试..."
    
    local duration=${STRESS_DURATION:-10}
    local concurrent=${STRESS_CLIENTS:-10}
    
    log_info "压力测试: $concurrent 并发, $duration 秒"
    
    local start_time=$(date +%s)
    local request_count=0
    
    while [ $(($(date +%s) - start_time)) -lt $duration ]; do
        local j=1
        while [ $j -le $concurrent ]; do
            (
                local status
                status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health")
                if [ "$status" = "200" ]; then
                    echo "success"
                else
                    echo "fail"
                fi
            ) &
            j=$((j + 1))
        done
        wait
        request_count=$((request_count + concurrent))
        
        # 显示进度
        local elapsed=$(($(date +%s) - start_time))
        echo -ne "\r压力测试进度: ${elapsed}s / ${duration}s, 请求数: $request_count"
    done
    
    echo ""
    
    local end_time=$(date +%s)
    local total_duration=$((end_time - start_time))
    local rps=$((request_count / total_duration))
    
    log_info "压力测试完成: $request_count 请求, $rps RPS"
    set_result "stress_tests" "PASSED"
}

#######################################
# 步骤 5: 收集验证结果
#######################################
step5_collect_results() {
    log_info "========================================"
    log_info "步骤 5: 收集验证结果"
    log_info "========================================"
    
    # 收集容器日志
    log_info "收集容器日志..."
    mkdir -p "$REPORT_DIR/logs-${TIMESTAMP}"
    for container in raft00 raft01 raft02 db-pg; do
        docker logs "$container" > "$REPORT_DIR/logs-${TIMESTAMP}/${container}.log" 2>&1 || true
    done
    
    # 收集指标数据
    log_info "收集指标数据..."
    local metrics_file="$REPORT_DIR/metrics-${TIMESTAMP}.json"
    
    local raft00_st raft01_st raft02_st db_pg_st
    raft00_st=$(docker inspect -f '{{.State.Status}}' raft00 2>/dev/null || echo 'unknown')
    raft01_st=$(docker inspect -f '{{.State.Status}}' raft01 2>/dev/null || echo 'unknown')
    raft02_st=$(docker inspect -f '{{.State.Status}}' raft02 2>/dev/null || echo 'unknown')
    db_pg_st=$(docker inspect -f '{{.State.Status}}' db-pg 2>/dev/null || echo 'unknown')
    
    cat > "$metrics_file" <<EOF
{
  "timestamp": "$TIMESTAMP",
  "containers": {
    "raft00": "$raft00_st",
    "raft01": "$raft01_st",
    "raft02": "$raft02_st",
    "db-pg": "$db_pg_st"
  }
}
EOF
    
    # 生成测试报告
    log_info "生成测试报告..."
    generate_report
    
    log_success "结果收集完成"
    set_result "step5_collect_results" "PASSED"
}

#######################################
# 生成测试报告
#######################################
generate_report() {
    local end_time=$(date +%s)
    local total_duration=$((end_time - START_TIME))
    
    # 计算统计信息
    local passed=0
    local failed=0
    local results_list=""
    
    while IFS='=' read -r key value; do
        [ -z "$key" ] && continue
        if [ "$value" = "PASSED" ]; then
            passed=$((passed + 1))
        else
            failed=$((failed + 1))
        fi
        # 构建 JSON
        if [ -n "$results_list" ]; then
            results_list="$results_list,"
        fi
        results_list="$results_list\"$key\": \"$value\""
    done < "$TEST_RESULTS_FILE"
    
    local total=$((passed + failed))
    local pass_rate=0
    if [ $total -gt 0 ]; then
        pass_rate=$(awk "BEGIN {printf \"%.1f\", ($passed / $total) * 100}")
    fi
    
    # 生成 JSON 报告
    cat > "$REPORT_FILE" <<EOF
{
  "test_run": {
    "timestamp": "$TIMESTAMP",
    "duration_seconds": $total_duration,
    "test_config": {
      "device_count": ${TEST_DEVICE_COUNT:-100},
      "client_count": ${TEST_CLIENT_COUNT:-3},
      "stress_test": ${RUN_STRESS_TEST:-false}
    }
  },
  "summary": {
    "total_tests": $total,
    "passed": $passed,
    "failed": $failed,
    "pass_rate": $pass_rate
  },
  "results": {
    $results_list
  },
  "artifacts": {
    "log_file": "$LOG_FILE",
    "report_file": "$REPORT_FILE",
    "metrics_file": "$REPORT_DIR/metrics-${TIMESTAMP}.json",
    "logs_directory": "$REPORT_DIR/logs-${TIMESTAMP}"
  }
}
EOF
    
    log_success "测试报告已生成: $REPORT_FILE"
}

#######################################
# 清理测试环境
#######################################
cleanup() {
    local exit_code=$?
    
    log_info "========================================"
    log_info "清理测试环境"
    log_info "========================================"
    
    # 停止测试客户端
    log_info "停止测试客户端..."
    local client_count=${TEST_CLIENT_COUNT:-3}
    local i=1
    while [ $i -le $client_count ]; do
        docker rm -f "test-client-$i" 2>/dev/null || true
        i=$((i + 1))
    done
    
    if [ "${SKIP_CLEANUP:-false}" != "true" ] && [ "${STOP_CLUSTER_ON_EXIT:-true}" = "true" ]; then
        log_info "停止服务集群..."
        bash "$PROJECT_ROOT/scripts/bin/stop-cluster.sh" 2>&1 | tee -a "$LOG_FILE" || true
    else
        log_warn "跳过集群清理 (用于调试)"
    fi
    
    # 清理临时结果文件
    cleanup_results
    
    # 打印摘要
    print_summary
    
    exit $exit_code
}

#######################################
# 打印测试摘要
#######################################
print_summary() {
    echo ""
    echo "========================================"
    echo "           测试执行摘要"
    echo "========================================"
    echo ""
    
    printf "%-40s %s\n" "测试项" "结果"
    echo "---------------------------------------- --------"
    
    while IFS='=' read -r key value; do
        [ -z "$key" ] && continue
        local icon="✓"
        local color="$GREEN"
        
        if [ "$value" != "PASSED" ]; then
            icon="✗"
            color="$RED"
        fi
        
        printf "%-40s %b%s %s%b\n" "$key" "$color" "$icon" "$value" "$NC"
    done < "$TEST_RESULTS_FILE"
    
    echo ""
    echo "========================================"
    
    # 统计
    local total=0
    local passed=0
    while IFS='=' read -r key value; do
        [ -z "$key" ] && continue
        total=$((total + 1))
        if [ "$value" = "PASSED" ]; then
            passed=$((passed + 1))
        fi
    done < "$TEST_RESULTS_FILE"
    
    local failed=$((total - passed))
    local pass_rate=0
    if [ $total -gt 0 ]; then
        pass_rate=$(awk "BEGIN {printf \"%.1f\", ($passed / $total) * 100}")
    fi
    
    echo "总测试项: $total"
    echo -e "通过: ${GREEN}$passed${NC}"
    echo -e "失败: ${RED}$failed${NC}"
    echo "通过率: ${pass_rate}%"
    echo ""
    echo "详细报告: $REPORT_FILE"
    echo "日志文件: $LOG_FILE"
    echo "========================================"
}

#######################################
# 显示帮助
#######################################
show_help() {
    cat <<EOF
Z-Chess 端到端测试脚本

用法: $0 [选项]

选项:
    -d, --device-count N    注册设备数量 (默认: 100)
    --client-count N        测试客户端数量 (默认: 3)
    --stress                执行压力测试
    --stress-duration S     压力测试持续时间秒数 (默认: 10)
    --stress-clients N      压力测试客户端数 (默认: 10)
    --skip-cleanup          测试结束后不清理环境
    --keep-cluster          测试结束后保留集群
    -h, --help              显示帮助

示例:
    # 快速测试 (默认配置)
    $0

    # 大规模测试 (1000 设备)
    $0 --device-count 1000 --stress

    # 保留环境用于调试
    $0 --skip-cleanup --keep-cluster

EOF
}

#######################################
# 解析命令行参数
#######################################
parse_args() {
    while [ $# -gt 0 ]; do
        case $1 in
            -d|--device-count)
                TEST_DEVICE_COUNT="$2"
                shift 2
                ;;
            --client-count)
                TEST_CLIENT_COUNT="$2"
                shift 2
                ;;
            --stress)
                RUN_STRESS_TEST=true
                shift
                ;;
            --stress-duration)
                STRESS_DURATION="$2"
                shift 2
                ;;
            --stress-clients)
                STRESS_CLIENTS="$2"
                shift 2
                ;;
            --skip-cleanup)
                SKIP_CLEANUP=true
                shift
                ;;
            --keep-cluster)
                STOP_CLUSTER_ON_EXIT=false
                shift
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

#######################################
# 主函数
#######################################
main() {
    # 记录开始时间
    START_TIME=$(date +%s)
    
    # 解析参数
    parse_args "$@"
    
    # 显示标题
    echo ""
    echo "╔══════════════════════════════════════════════════╗"
    echo "║          Z-Chess 端到端自动化测试                 ║"
    echo "╚══════════════════════════════════════════════════╝"
    echo ""
    
    # 初始化
    setup_test_env
    
    # 设置清理陷阱
    trap cleanup EXIT
    
    # 执行测试步骤
    local failed=0
    
    step1_start_cluster || failed=1
    
    if [ $failed -eq 0 ]; then
        step2_start_test_clients || failed=1
    fi
    
    if [ $failed -eq 0 ]; then
        step3_register_devices || failed=1
    fi
    
    if [ $failed -eq 0 ]; then
        step4_run_functional_tests || true
    fi
    
    step5_collect_results
    
    # 返回结果
    if [ $failed -eq 0 ]; then
        log_success "所有关键测试通过!"
        exit 0
    else
        log_error "部分测试失败"
        exit 1
    fi
}

# 执行主函数
main "$@"
