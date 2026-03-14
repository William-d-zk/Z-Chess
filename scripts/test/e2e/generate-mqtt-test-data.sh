#!/bin/bash
#
# 生成 MQTT 测试数据脚本
# 用于创建设备并生成 MQTT 登录所需的凭证
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_ENDPOINT="${API_ENDPOINT:-http://localhost:8080}"
OUTPUT_DIR="${OUTPUT_DIR:-$SCRIPT_DIR/data}"
DEVICE_COUNT="${DEVICE_COUNT:-10}"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 生成随机 MAC 地址
generate_mac() {
    printf '02:%02x:%02x:%02x:%02x:%02x' $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
}

# 生成设备初始化请求体
generate_init_request() {
    local index=$1
    local username="${DEVICE_USERNAME_PREFIX:-test_device}_$(date +%s)_${index}"
    local wifi_mac=$(generate_mac)
    local ethernet_mac=$(generate_mac)
    local bluetooth_mac=$(generate_mac)
    
    cat <<EOF
{
    "username": "${username}",
    "profile": {
        "wifi_mac": "${wifi_mac}",
        "ethernet_mac": "${ethernet_mac}",
        "bluetooth_mac": "${bluetooth_mac}",
        "imei": "IMEI$(date +%s)${index}",
        "imsi": "IMSI$(date +%s)${index}"
    }
}
EOF
}

# 初始化单个设备
init_device() {
    local index=$1
    local request_body=$(generate_init_request $index)
    
    log_info "初始化设备 $index..."
    
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST "${API_ENDPOINT}/device/init?docker=true" \
        -H "Content-Type: application/json" \
        -d "$request_body" 2>/dev/null || echo -e "\n000")
    
    local http_code=$(echo "$response" | tail -1)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "200" ]; then
        local code=$(echo "$body" | jq -r '.code // -1' 2>/dev/null)
        if [ "$code" == "0" ] || [ "$code" == "200" ]; then
            echo "$body"
            return 0
        fi
    fi
    
    log_error "设备 $index 初始化失败: HTTP $http_code"
    return 1
}

# 解析设备响应并提取 MQTT 登录信息
parse_device_response() {
    local response=$1
    local index=$2
    
    local token=$(echo "$response" | jq -r '.data.token // empty' 2>/dev/null)
    local serial=$(echo "$response" | jq -r '.data.serial // empty' 2>/dev/null)
    local public_key=$(echo "$response" | jq -r '.data.public_key // empty' 2>/dev/null)
    local algorithm=$(echo "$response" | jq -r '.data.algorithm // empty' 2>/dev/null)
    local expire_date=$(echo "$response" | jq -r '.data.expire_date // empty' 2>/dev/null)
    
    if [ -z "$token" ] || [ -z "$serial" ]; then
        log_error "无法解析设备 $index 的响应"
        return 1
    fi
    
    # 输出设备信息
    cat <<EOF
{
    "index": $index,
    "token": "${token}",
    "serial": "${serial}",
    "public_key": "${public_key}",
    "algorithm": "${algorithm}",
    "expire_date": "${expire_date}",
    "mqtt_username": "${token}",
    "mqtt_password": "${token}"
}
EOF
}

# 生成 MQTT 连接配置文件
generate_mqtt_config() {
    local devices_file=$1
    local output_file=$2
    
    log_info "生成 MQTT 配置文件..."
    
    cat > "$output_file" <<'EOF'
# MQTT 测试设备配置文件
# 生成时间: $(date)
# 服务器地址: localhost:1883

mqtt:
  broker: localhost
  port: 1883
  qos: 1
  keep_alive: 60
  
devices:
EOF

    # 添加设备列表
    local count=$(jq '.devices | length' "$devices_file" 2>/dev/null || echo "0")
    
    for i in $(seq 0 $((count - 1))); do
        local device=$(jq ".devices[$i]" "$devices_file" 2>/dev/null)
        local token=$(echo "$device" | jq -r '.token // empty' 2>/dev/null)
        local serial=$(echo "$device" | jq -r '.serial // empty' 2>/dev/null)
        
        if [ -n "$token" ]; then
            cat >> "$output_file" <<EOF
  - name: "device_$i"
    username: "${token}"
    password: "${token}"
    client_id: "${serial}"
    topics:
      subscribe:
        - "device/${serial}/command"
        - "device/${serial}/config"
      publish:
        - "device/${serial}/telemetry"
        - "device/${serial}/status"
EOF
        fi
    done
    
    log_success "MQTT 配置文件已生成: $output_file"
}

# 生成 JSON 格式的设备列表
generate_json_device_list() {
    local devices=($@)
    local json_array="["
    
    for device_json in "${devices[@]}"; do
        if [ -n "$device_json" ]; then
            if [ "$json_array" != "[" ]; then
                json_array="${json_array},"
            fi
            json_array="${json_array}${device_json}"
        fi
    done
    
    json_array="${json_array}]"
    echo "{\"devices\": $json_array, \"generated_at\": \"$(date -Iseconds)\"}"
}

# 显示使用帮助
show_help() {
    cat <<EOF
生成 MQTT 测试数据脚本

用法: $0 [选项]

选项:
    -e, --endpoint URL      API 端点 (默认: http://localhost:8080)
    -n, --count N           生成设备数量 (默认: 10)
    -o, --output DIR        输出目录 (默认: ./data)
    -p, --prefix PREFIX     用户名前缀 (默认: test_device)
    -h, --help              显示帮助信息

示例:
    # 生成 10 个测试设备
    $0

    # 生成 100 个设备到指定目录
    $0 -n 100 -o /tmp/mqtt_devices

    # 指定目标服务器
    $0 -e http://192.168.1.100:8080 -n 50

输出文件:
    - devices.json: 设备列表 (JSON 格式)
    - mqtt-config.yaml: MQTT 配置文件
    - device-tokens.txt: 设备令牌列表 (用于批量测试)

EOF
}

# 主流程
main() {
    # 解析参数
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--endpoint)
                API_ENDPOINT="$2"
                shift 2
                ;;
            -n|--count)
                DEVICE_COUNT="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            -p|--prefix)
                DEVICE_USERNAME_PREFIX="$2"
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
    echo "MQTT 测试数据生成"
    echo "========================================"
    echo ""
    echo "配置信息:"
    echo "  API 端点:   $API_ENDPOINT"
    echo "  设备数量:   $DEVICE_COUNT"
    echo "  输出目录:   $OUTPUT_DIR"
    echo ""
    
    # 创建输出目录
    mkdir -p "$OUTPUT_DIR"
    
    # 检查 API 可用性
    log_info "检查 API 服务..."
    if ! curl -sf "${API_ENDPOINT}/actuator/health" > /dev/null 2>&1; then
        log_error "API 服务不可用: $API_ENDPOINT"
        exit 1
    fi
    log_success "API 服务正常"
    
    # 初始化设备
    log_info "开始初始化 $DEVICE_COUNT 个设备..."
    
    local devices=()
    local success_count=0
    local fail_count=0
    
    for i in $(seq 1 $DEVICE_COUNT); do
        local response=$(init_device $i)
        
        if [ $? -eq 0 ]; then
            local device_json=$(parse_device_response "$response" $i)
            if [ $? -eq 0 ]; then
                devices+=("$device_json")
                ((success_count++))
                printf "\r  进度: %d/%d (成功: %d, 失败: %d)" "$i" "$DEVICE_COUNT" "$success_count" "$fail_count"
            else
                ((fail_count++))
            fi
        else
            ((fail_count++))
        fi
    done
    
    printf "\n"
    echo ""
    
    # 生成输出文件
    if [ ${#devices[@]} -gt 0 ]; then
        local json_output=$(generate_json_device_list "${devices[@]}")
        
        # 保存 JSON 文件
        echo "$json_output" | jq '.' > "$OUTPUT_DIR/devices.json" 2>/dev/null || echo "$json_output" > "$OUTPUT_DIR/devices.json"
        log_success "设备列表已保存: $OUTPUT_DIR/devices.json"
        
        # 生成 MQTT 配置文件
        generate_mqtt_config "$OUTPUT_DIR/devices.json" "$OUTPUT_DIR/mqtt-config.yaml"
        
        # 生成令牌列表文件
        echo "$json_output" | jq -r '.devices[].token' > "$OUTPUT_DIR/device-tokens.txt" 2>/dev/null
        log_success "令牌列表已保存: $OUTPUT_DIR/device-tokens.txt"
        
        # 显示摘要
        echo ""
        echo "========== 生成摘要 =========="
        echo "成功: $success_count"
        echo "失败: $fail_count"
        echo ""
        echo "输出文件:"
        echo "  - $OUTPUT_DIR/devices.json"
        echo "  - $OUTPUT_DIR/mqtt-config.yaml"
        echo "  - $OUTPUT_DIR/device-tokens.txt"
        echo ""
        echo "MQTT 连接信息:"
        echo "  Broker: localhost:1883"
        echo "  用户名/密码: 使用设备 token"
        echo "=============================="
    else
        log_error "没有成功生成任何设备"
        exit 1
    fi
}

main "$@"
