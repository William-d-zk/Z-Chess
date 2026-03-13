#!/bin/bash
#
# 设备批量注册脚本
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 默认配置
API_ENDPOINT=${API_ENDPOINT:-"http://localhost:8080"}
DEVICE_COUNT=${DEVICE_COUNT:-100}
BATCH_SIZE=${BATCH_SIZE:-10}
DEVICE_PREFIX=${DEVICE_PREFIX:-"TEST-DEVICE"}
OUTPUT_FILE=${OUTPUT_FILE:-"./data/registered-devices.json"}

# 颜色
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

show_help() {
    cat <<EOF
设备批量注册脚本

用法: $0 [选项]

选项:
    -c, --count N           注册设备数量 (默认: 100)
    -b, --batch-size N      批次大小 (默认: 10)
    -p, --prefix NAME       设备ID前缀 (默认: TEST-DEVICE)
    -o, --output FILE       输出文件 (默认: ./data/registered-devices.json)
    -e, --endpoint URL      API 端点 (默认: http://localhost:8080)
    -d, --dry-run           仅生成数据，不实际注册
    -h, --help              显示帮助

示例:
    # 快速注册 50 个设备
    $0 -c 50

    # 大规模注册 10000 设备
    $0 -c 10000 -b 50 --prefix PROD-DEVICE

    # 仅生成数据
    $0 -c 100 -d -o ./devices.json

EOF
}

# 生成设备数据
generate_devices() {
    local count=$1
    local prefix=$2
    local output=$3
    
    log_info "生成 $count 个设备数据..."
    
    mkdir -p "$(dirname "$output")"
    
    # 使用 jq 生成设备数据
    local devices="[]"
    
    for i in $(seq 1 $count); do
        local device_id="${prefix}-$(printf %08d $i)"
        local device_type=$((RANDOM % 5 + 1))
        local capabilities='["telemetry", "commands", "config"]'
        
        devices=$(echo "$devices" | jq ". += [{\"id\": \"$device_id\", \"type\": $device_type, \"capabilities\": $capabilities, \"status\": \"inactive\"}]")
        
        if [ $((i % 100)) -eq 0 ]; then
            echo -ne "\r生成进度: $i/$count"
        fi
    done
    
    echo "$devices" > "$output"
    echo ""
    log_success "设备数据已生成: $output"
}

# 注册单个设备
register_device() {
    local device=$1
    local endpoint=$2
    
    local device_id=$(echo "$device" | jq -r '.id')
    local device_type=$(echo "$device" | jq -r '.type')
    
    # 调用注册 API
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST "${endpoint}/api/devices/register" \
        -H "Content-Type: application/json" \
        -d "{\"device_id\": \"$device_id\", \"type\": $device_type}" 2>/dev/null || echo -e "\n000")
    
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "200" ] || [ "$http_code" == "201" ]; then
        echo "SUCCESS"
    else
        echo "FAILED:$http_code"
    fi
}

# 批量注册设备
batch_register() {
    local input_file=$1
    local endpoint=$2
    local batch_size=$3
    
    local total=$(jq length "$input_file")
    local success_count=0
    local fail_count=0
    local registered_devices="[]"
    local start_time=$(date +%s)
    
    log_info "开始批量注册设备 (总数: $total, 批次: $batch_size)..."
    
    for i in $(seq 0 $((total - 1))); do
        local device=$(jq ".[$i]" "$input_file")
        local device_id=$(echo "$device" | jq -r '.id')
        
        local result=$(register_device "$device" "$endpoint")
        
        if [ "$result" == "SUCCESS" ]; then
            ((success_count++))
            registered_devices=$(echo "$registered_devices" | jq ". += [{\"id\": \"$device_id\", \"registered_at\": \"$(date -Iseconds)\"}]")
        else
            ((fail_count++))
            log_warn "设备 $device_id 注册失败: $result"
        fi
        
        # 批次进度
        local batch_num=$(((i + 1) / batch_size))
        if [ $((i + 1)) -eq $total ] || [ $(((i + 1) % batch_size)) -eq 0 ]; then
            local current_time=$(date +%s)
            local elapsed=$((current_time - start_time))
            local rate=0
            if [ $elapsed -gt 0 ]; then
                rate=$(( (i + 1) / elapsed ))
            fi
            echo -ne "\r进度: $((i + 1))/$total | 成功: $success_count | 失败: $fail_count | 速率: ${rate} 设备/秒"
        fi
        
        # 速率限制
        sleep 0.05
    done
    
    echo ""
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log_info "注册完成 (耗时: ${duration}s)"
    log_info "  - 成功: $success_count"
    log_info "  - 失败: $fail_count"
    
    # 保存注册结果
    echo "$registered_devices" > "$OUTPUT_FILE"
    log_success "注册结果已保存: $OUTPUT_FILE"
    
    return $fail_count
}

# 主函数
main() {
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--count)
                DEVICE_COUNT="$2"
                shift 2
                ;;
            -b|--batch-size)
                BATCH_SIZE="$2"
                shift 2
                ;;
            -p|--prefix)
                DEVICE_PREFIX="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_FILE="$2"
                shift 2
                ;;
            -e|--endpoint)
                API_ENDPOINT="$2"
                shift 2
                ;;
            -d|--dry-run)
                DRY_RUN=true
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
    
    log_info "设备注册工具"
    log_info "============"
    log_info "目标端点: $API_ENDPOINT"
    log_info "设备数量: $DEVICE_COUNT"
    log_info "批次大小: $BATCH_SIZE"
    log_info "设备前缀: $DEVICE_PREFIX"
    
    # 检查依赖
    if ! command -v jq &> /dev/null; then
        log_error "需要安装 jq"
        exit 1
    fi
    
    # 生成设备数据
    local devices_file="./data/devices-to-register-${TIMESTAMP:-$(date +%Y%m%d_%H%M%S)}.json"
    generate_devices "$DEVICE_COUNT" "$DEVICE_PREFIX" "$devices_file"
    
    # 如果是 dry-run，到此结束
    if [ "${DRY_RUN:-false}" == "true" ]; then
        log_info "Dry-run 模式，跳过实际注册"
        log_success "设备数据已生成: $devices_file"
        exit 0
    fi
    
    # 检查 API 健康状态
    log_info "检查 API 健康状态..."
    if ! curl -s "${API_ENDPOINT}/actuator/health" > /dev/null 2>&1; then
        log_error "API 端点不可达: $API_ENDPOINT"
        exit 1
    fi
    log_success "API 端点正常"
    
    # 批量注册
    batch_register "$devices_file" "$API_ENDPOINT" "$BATCH_SIZE"
    local exit_code=$?
    
    # 清理临时文件
    rm -f "$devices_file"
    
    if [ $exit_code -eq 0 ]; then
        log_success "所有设备注册成功!"
        exit 0
    else
        log_warn "部分设备注册失败"
        exit 1
    fi
}

main "$@"
