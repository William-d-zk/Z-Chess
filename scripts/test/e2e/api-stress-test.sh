#!/bin/bash
#
# HTTP API 压测测试脚本
# 使用 curl 对 Z-Chess Arena API 进行压力测试
#

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 默认配置
API_ENDPOINT="${API_ENDPOINT:-http://localhost:8080}"
CONCURRENT_REQUESTS="${CONCURRENT_REQUESTS:-100}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-1000}"
TEST_DURATION="${TEST_DURATION:-60}"

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# ==================== 测试函数 ====================

# 检查 API 健康状态
check_api_health() {
    log_info "检查 API 健康状态..."
    
    local response=$(curl -s -o /dev/null -w "%{http_code}" "${API_ENDPOINT}/actuator/health" 2>/dev/null || echo "000")
    
    if [ "$response" == "200" ]; then
        log_success "API 服务运行正常"
        return 0
    else
        log_error "API 服务未就绪 (HTTP $response)"
        return 1
    fi
}

# 并发请求测试
run_concurrent_test() {
    local endpoint="$1"
    local description="$2"
    local method="${3:-GET}"
    local data="${4:-}"
    
    log_info "执行并发测试: $description"
    log_info "  接口: $method $endpoint"
    log_info "  并发数: $CONCURRENT_REQUESTS"
    log_info "  总请求数: $TOTAL_REQUESTS"
    
    local url="${API_ENDPOINT}${endpoint}"
    local success_count=0
    local fail_count=0
    local start_time=$(date +%s%N)
    
    # 创建临时文件存储结果
    local temp_dir=$(mktemp -d)
    local result_file="$temp_dir/results.txt"
    
    # 定义请求函数
    make_request() {
        local idx=$1
        local curl_cmd="curl -s -o /dev/null -w '%{http_code},%{time_total}'"
        
        if [ "$method" == "POST" ] && [ -n "$data" ]; then
            curl_cmd="$curl_cmd -X POST -H 'Content-Type: application/json' -d '$data'"
        fi
        
        local result=$(eval "$curl_cmd '$url'" 2>/dev/null || echo "000,0")
        echo "$result" >> "$result_file"
    }
    
    # 执行并发请求
    log_info "开始发送请求..."
    for i in $(seq 1 $TOTAL_REQUESTS); do
        make_request $i &
        
        # 控制并发数
        if [ $((i % CONCURRENT_REQUESTS)) -eq 0 ]; then
            wait
            printf "\r  进度: %d/%d" "$i" "$TOTAL_REQUESTS"
        fi
    done
    
    # 等待所有请求完成
    wait
    printf "\n"
    
    local end_time=$(date +%s%N)
    
    # 统计结果
    while IFS=',' read -r code time; do
        if [ "$code" == "200" ] || [ "$code" == "201" ]; then
            ((success_count++))
        else
            ((fail_count++))
        fi
    done < "$result_file"
    
    # 计算统计信息
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    local duration_sec=$(( duration_ms / 1000 ))
    local qps=0
    if [ $duration_sec -gt 0 ]; then
        qps=$(( TOTAL_REQUESTS / duration_sec ))
    fi
    local success_rate=0
    if [ $TOTAL_REQUESTS -gt 0 ]; then
        success_rate=$(awk "BEGIN {printf \"%.2f\", ($success_count / $TOTAL_REQUESTS) * 100}")
    fi
    
    # 清理
    rm -rf "$temp_dir"
    
    # 输出结果
    echo ""
    echo "========== 测试结果 =========="
    echo "总请求数:    $TOTAL_REQUESTS"
    echo "成功请求:    $success_count"
    echo "失败请求:    $fail_count"
    echo "成功率:      ${success_rate}%"
    echo "总耗时:      ${duration_ms}ms"
    echo "QPS:         $qps"
    echo "=============================="
    
    if [ $fail_count -eq 0 ]; then
        log_success "并发测试通过"
        return 0
    else
        log_warn "有 $fail_count 个请求失败"
        return 0
    fi
}

# 持续压力测试
run_sustained_test() {
    log_info "执行持续压力测试 (${TEST_DURATION}秒)"
    
    local url="${API_ENDPOINT}/actuator/health"
    local success_count=0
    local fail_count=0
    local start_time=$(date +%s)
    
    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [ $elapsed -ge $TEST_DURATION ]; then
            break
        fi
        
        # 批量发送请求
        for i in $(seq 1 $CONCURRENT_REQUESTS); do
            (
                local code=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
                if [ "$code" == "200" ]; then
                    echo "SUCCESS"
                else
                    echo "FAIL"
                fi
            ) &
        done
        
        wait
        
        # 显示进度
        local remaining=$((TEST_DURATION - elapsed))
        printf "\r  剩余时间: %ds | 成功: %d | 失败: %d" "$remaining" "$success_count" "$fail_count"
        
        # 短暂间隔
        sleep 0.1
    done
    
    printf "\n"
    log_success "持续压力测试完成"
}

# 混合 API 测试
run_mixed_api_test() {
    log_info "执行混合 API 测试"
    
    local apis=(
        "GET:/actuator/health:健康检查"
        "GET:/device/online/all:设备列表"
        "GET:/cluster/close:集群状态"
        "POST:/device/init:设备初始化"
    )
    
    log_info "测试 API 列表:"
    for api in "${apis[@]}"; do
        IFS=':' read -r method endpoint desc <<< "$api"
        echo "  - $desc: $method $endpoint"
    done
    
    echo ""
    
    # 对每个 API 进行测试
    for api in "${apis[@]}"; do
        IFS=':' read -r method endpoint desc <<< "$api"
        
        log_info "测试: $desc"
        
        local data=""
        if [ "$method" == "POST" ]; then
            data='{"username":"test_'$(date +%s)'","profile":{"mac":"02:00:00:00:00:01"}}'
        fi
        
        run_concurrent_test "$endpoint" "$desc" "$method" "$data"
        echo ""
        
        # 短暂间隔
        sleep 2
    done
}

# 显示使用帮助
show_help() {
    cat <<EOF
HTTP API 压测测试脚本

用法: $0 [命令] [选项]

命令:
    health          检查 API 健康状态
    concurrent      并发请求测试
    sustained       持续压力测试
    mixed           混合 API 测试
    all             执行所有测试 (默认)

选项:
    -e, --endpoint URL      API 端点 (默认: http://localhost:8080)
    -c, --concurrent N      并发数 (默认: 100)
    -n, --requests N        总请求数 (默认: 1000)
    -d, --duration SECONDS  测试持续时间 (默认: 60)
    -h, --help              显示帮助信息

示例:
    $0
    $0 concurrent -e http://192.168.1.100:8080 -c 200 -n 2000
    $0 sustained -d 120
    $0 mixed -c 50

EOF
}

# 主流程
main() {
    local command="${1:-all}"
    shift || true
    
    # 解析选项
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--endpoint)
                API_ENDPOINT="$2"
                shift 2
                ;;
            -c|--concurrent)
                CONCURRENT_REQUESTS="$2"
                shift 2
                ;;
            -n|--requests)
                TOTAL_REQUESTS="$2"
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
    
    echo "========================================"
    echo "HTTP API 压测测试"
    echo "========================================"
    echo ""
    echo "配置信息:"
    echo "  API 端点:   $API_ENDPOINT"
    echo "  并发数:     $CONCURRENT_REQUESTS"
    echo "  总请求数:   $TOTAL_REQUESTS (并发测试)"
    echo "  测试时长:   ${TEST_DURATION}秒 (持续测试)"
    echo ""
    
    case $command in
        health)
            check_api_health
            ;;
        concurrent)
            check_api_health && run_concurrent_test "/actuator/health" "API 健康检查"
            ;;
        sustained)
            check_api_health && run_sustained_test
            ;;
        mixed)
            check_api_health && run_mixed_api_test
            ;;
        all)
            check_api_health
            echo ""
            run_concurrent_test "/actuator/health" "API 健康检查"
            echo ""
            run_mixed_api_test
            ;;
        *)
            log_error "未知命令: $command"
            show_help
            exit 1
            ;;
    esac
}

main "$@"
