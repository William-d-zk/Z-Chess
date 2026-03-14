#!/bin/bash
#
# 设备验证脚本
# 用于验证已注册的设备并获取有效期信息
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_ENDPOINT="${API_ENDPOINT:-http://localhost:8080}"

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

# 显示使用帮助
show_help() {
    cat <<EOF
设备验证脚本

用法: $0 <命令> [选项]

命令:
    verify <token>          验证设备 (v1 版本 - ECC 加密)
    verify-v2 <token>       验证设备 (v2 版本 - SHA256 哈希)
    query <token>           查询设备信息
    renew <serial> <date>   更新设备有效期
    batch <token_file>      批量验证设备

选项:
    -e, --endpoint URL      API 端点 (默认: http://localhost:8080)
    -h, --help              显示帮助信息

示例:
    # 验证单个设备
    $0 verify abc123token

    # 查询设备信息
    $0 query abc123token

    # 更新有效期
    $0 renew SERIAL123 2025-12-31

    # 批量验证
    $0 batch ./data/device-tokens.txt

EOF
}

# 计算 SHA256 哈希
calculate_sha256() {
    local input="$1"
    echo -n "$input" | shasum -a 256 | cut -d' ' -f1
}

# 验证设备 v1 (ECC 加密)
verify_device_v1() {
    local token=$1
    
    log_info "验证设备 (v1): $token"
    
    # 获取设备信息用于构建验证数据
    local query_response=$(curl -s "${API_ENDPOINT}/device/open/query?token=${token}" 2>/dev/null)
    
    # 简化验证：直接发送 MAC 地址信息
    # 实际使用时需要使用设备的公钥加密
    local body='{"ethernet_mac":"02:00:00:00:00:01","wifi_mac":"02:00:00:00:00:02","bluetooth_mac":"02:00:00:00:00:03"}'
    
    local response=$(curl -s -X POST \
        "${API_ENDPOINT}/device/verify?token=${token}" \
        -H "Content-Type: application/json" \
        -d "$body" 2>/dev/null)
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# 验证设备 v2 (SHA256 哈希)
verify_device_v2() {
    local token=$1
    
    log_info "验证设备 (v2): $token"
    
    # 查询设备信息
    local query_response=$(curl -s "${API_ENDPOINT}/device/open/query?token=${token}" 2>/dev/null)
    local profile=$(echo "$query_response" | jq -r '.data.profile // empty' 2>/dev/null)
    
    if [ -z "$profile" ]; then
        log_error "无法获取设备信息"
        return 1
    fi
    
    # 提取 MAC 地址和公钥
    local ethernet_mac=$(echo "$profile" | jq -r '.ethernet_mac // empty' 2>/dev/null)
    local wifi_mac=$(echo "$profile" | jq -r '.wifi_mac // empty' 2>/dev/null)
    local bluetooth_mac=$(echo "$profile" | jq -r '.bluetooth_mac // empty' 2>/dev/null)
    local public_key=$(echo "$profile" | jq -r '.key_pair_profile.public_key // empty' 2>/dev/null)
    
    if [ -z "$ethernet_mac" ] || [ -z "$wifi_mac" ] || [ -z "$bluetooth_mac" ] || [ -z "$public_key" ]; then
        log_error "设备信息不完整"
        return 1
    fi
    
    # 计算 SHA256 哈希
    local hash_input="${ethernet_mac}|${wifi_mac}|${bluetooth_mac}|${public_key}"
    local hash=$(calculate_sha256 "$hash_input")
    
    log_info "计算哈希: $hash"
    
    # 发送验证请求
    local response=$(curl -s -X POST \
        "${API_ENDPOINT}/device/verify-v2?token=${token}" \
        -H "Content-Type: text/plain" \
        -d "$hash" 2>/dev/null)
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# 查询设备信息
query_device() {
    local token=$1
    
    log_info "查询设备: $token"
    
    local response=$(curl -s "${API_ENDPOINT}/device/open/query?token=${token}" 2>/dev/null)
    
    local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        log_success "查询成功"
        echo "$response" | jq '.data' 2>/dev/null || echo "$response"
        
        # 显示关键信息
        local serial=$(echo "$response" | jq -r '.data.notice // empty' 2>/dev/null)
        local username=$(echo "$response" | jq -r '.data.username // empty' 2>/dev/null)
        local expire_date=$(echo "$response" | jq -r '.data.profile.expiration_profile.expire_at // empty' 2>/dev/null)
        
        echo ""
        echo "设备信息:"
        echo "  序列号: $serial"
        echo "  用户名: $username"
        echo "  过期时间: $expire_date"
    else
        log_error "查询失败"
        echo "$response" | jq '.' 2>/dev/null || echo "$response"
    fi
}

# 更新设备有效期
renew_device() {
    local serial=$1
    local expire_date=$2
    
    log_info "更新设备有效期: $serial -> $expire_date"
    
    local response=$(curl -s -X GET \
        "${API_ENDPOINT}/device/renew?serial=${serial}&expireAt=${expire_date}" \
        2>/dev/null)
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# 批量验证
batch_verify() {
    local token_file=$1
    
    if [ ! -f "$token_file" ]; then
        log_error "文件不存在: $token_file"
        return 1
    fi
    
    log_info "批量验证设备..."
    
    local total=0
    local success=0
    local failed=0
    
    while IFS= read -r token; do
        [ -z "$token" ] && continue
        
        ((total++))
        printf "\r  进度: %d (成功: %d, 失败: %d)" "$total" "$success" "$failed"
        
        local response=$(curl -s -X POST \
            "${API_ENDPOINT}/device/verify-v2?token=${token}" \
            -H "Content-Type: text/plain" \
            -d "test" 2>/dev/null)
        
        local code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
        if [ "$code" == "0" ] || [ "$code" == "200" ]; then
            ((success++))
        else
            ((failed++))
        fi
        
        # 避免请求过快
        sleep 0.1
    done < "$token_file"
    
    printf "\n"
    echo ""
    echo "========== 批量验证结果 =========="
    echo "总计: $total"
    echo "成功: $success"
    echo "失败: $failed"
    echo "=================================="
}

# 主流程
main() {
    local command="${1:-}"
    shift || true
    
    # 解析全局选项
    while [[ $# -gt 0 ]]; do
        case $1 in
            -e|--endpoint)
                API_ENDPOINT="$2"
                shift 2
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                break
                ;;
        esac
    done
    
    case $command in
        verify)
            local token="${1:-}"
            if [ -z "$token" ]; then
                log_error "请提供设备 token"
                exit 1
            fi
            verify_device_v1 "$token"
            ;;
        verify-v2)
            local token="${1:-}"
            if [ -z "$token" ]; then
                log_error "请提供设备 token"
                exit 1
            fi
            verify_device_v2 "$token"
            ;;
        query)
            local token="${1:-}"
            if [ -z "$token" ]; then
                log_error "请提供设备 token"
                exit 1
            fi
            query_device "$token"
            ;;
        renew)
            local serial="${1:-}"
            local expire_date="${2:-}"
            if [ -z "$serial" ] || [ -z "$expire_date" ]; then
                log_error "请提供序列号和过期日期"
                exit 1
            fi
            renew_device "$serial" "$expire_date"
            ;;
        batch)
            local token_file="${1:-}"
            if [ -z "$token_file" ]; then
                log_error "请提供令牌文件路径"
                exit 1
            fi
            batch_verify "$token_file"
            ;;
        *)
            show_help
            exit 1
            ;;
    esac
}

main "$@"
