#!/bin/bash
#
# 测试工具库
#

# 颜色定义
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly YELLOW='\033[1;33m'
readonly BLUE='\033[0;34m'
readonly NC='\033[0m'

# 日志级别
LOG_LEVEL=${LOG_LEVEL:-"INFO"}

#######################################
# 日志函数
#######################################
log_debug() { [[ "$LOG_LEVEL" == "DEBUG" ]] && echo -e "${BLUE}[DEBUG]${NC} $1" >&2; }
log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[PASS]${NC} $1"; }
log_error() { echo -e "${RED}[FAIL]${NC} $1" >&2; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }

#######################################
# HTTP 请求工具
#######################################
http_get() {
    local url=$1
    local timeout=${2:-30}
    curl -s -m "$timeout" -w "\n%{http_code}" "$url" 2>/dev/null
}

http_post() {
    local url=$1
    local data=$2
    local timeout=${3:-30}
    curl -s -m "$timeout" -X POST \
        -H "Content-Type: application/json" \
        -d "$data" \
        -w "\n%{http_code}" \
        "$url" 2>/dev/null
}

http_put() {
    local url=$1
    local data=$2
    local timeout=${3:-30}
    curl -s -m "$timeout" -X PUT \
        -H "Content-Type: application/json" \
        -d "$data" \
        -w "\n%{http_code}" \
        "$url" 2>/dev/null
}

http_delete() {
    local url=$1
    local timeout=${2:-30}
    curl -s -m "$timeout" -X DELETE \
        -w "\n%{http_code}" \
        "$url" 2>/dev/null
}

#######################################
# 响应解析
#######################################
get_http_code() {
    local response=$1
    echo "$response" | tail -1
}

get_response_body() {
    local response=$1
    echo "$response" | sed '$d'
}

#######################################
# 断言函数
#######################################
assert_http_ok() {
    local code=$1
    local msg=${2:-"HTTP 请求"}
    
    if [ "$code" == "200" ] || [ "$code" == "201" ]; then
        log_success "$msg 成功 (HTTP $code)"
        return 0
    else
        log_error "$msg 失败 (HTTP $code)"
        return 1
    fi
}

assert_equals() {
    local expected=$1
    local actual=$2
    local msg=${3:-"断言"}
    
    if [ "$expected" == "$actual" ]; then
        log_success "$msg: $actual"
        return 0
    else
        log_error "$msg: 期望 $expected, 实际 $actual"
        return 1
    fi
}

assert_not_empty() {
    local value=$1
    local msg=${2:-"断言非空"}
    
    if [ -n "$value" ]; then
        log_success "$msg: OK"
        return 0
    else
        log_error "$msg: 值为空"
        return 1
    fi
}

#######################################
# 重试机制
#######################################
retry() {
    local max_attempts=$1
    local delay=$2
    local cmd="${@:3}"
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        log_debug "尝试 $attempt/$max_attempts: $cmd"
        
        if eval "$cmd"; then
            return 0
        fi
        
        if [ $attempt -lt $max_attempts ]; then
            log_warn "命令失败，${delay}秒后重试..."
            sleep "$delay"
        fi
        
        ((attempt++))
    done
    
    log_error "命令在 $max_attempts 次尝试后仍然失败"
    return 1
}

#######################################
# 等待服务
#######################################
wait_for_service() {
    local url=$1
    local timeout=${2:-60}
    local interval=${3:-2}
    local start_time=$(date +%s)
    
    log_info "等待服务就绪: $url (超时: ${timeout}s)"
    
    while true; do
        local response=$(http_get "$url" 5)
        local code=$(get_http_code "$response")
        
        if [ "$code" == "200" ]; then
            log_success "服务已就绪"
            return 0
        fi
        
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [ $elapsed -ge $timeout ]; then
            log_error "等待服务超时"
            return 1
        fi
        
        echo -ne "\r等待中... ${elapsed}s"
        sleep "$interval"
    done
}

#######################################
# 等待容器
#######################################
wait_for_container() {
    local container=$1
    local timeout=${2:-60}
    local start_time=$(date +%s)
    
    log_info "等待容器就绪: $container"
    
    while true; do
        if docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null | grep -q "running"; then
            log_success "容器已就绪: $container"
            return 0
        fi
        
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [ $elapsed -ge $timeout ]; then
            log_error "等待容器超时: $container"
            return 1
        fi
        
        sleep 1
    done
}

#######################################
# 性能计时
#######################################
timer_start() {
    echo $(date +%s%N)
}

timer_end() {
    local start_time=$1
    local end_time=$(date +%s%N)
    local duration_ns=$((end_time - start_time))
    local duration_ms=$((duration_ns / 1000000))
    echo "$duration_ms"
}

#######################################
# 生成测试数据
#######################################
generate_uuid() {
    cat /proc/sys/kernel/random/uuid 2>/dev/null || echo "$(date +%s%N)-$$-$RANDOM"
}

generate_timestamp() {
    date -Iseconds
}

generate_random_string() {
    local length=${1:-10}
    tr -dc 'a-zA-Z0-9' < /dev/urandom | head -c "$length"
}

#######################################
# JSON 工具
#######################################
json_get() {
    local json=$1
    local key=$2
    echo "$json" | jq -r "$key" 2>/dev/null || echo "null"
}

json_validate() {
    local json=$1
    echo "$json" | jq empty 2>/dev/null && echo "true" || echo "false"
}

#######################################
# 统计计算
#######################################
calc_average() {
    local numbers=$1
    echo "$numbers" | awk '{sum+=$1; count++} END {if(count>0) printf "%.2f", sum/count; else print 0}'
}

calc_max() {
    local numbers=$1
    echo "$numbers" | awk 'BEGIN{max=0} {if($1>max || NR==1) max=$1} END{print max}'
}

calc_min() {
    local numbers=$1
    echo "$numbers" | awk 'BEGIN{min=0} {if($1<min || NR==1) min=$1} END{print min}'
}

#######################################
# 信号处理
#######################################
setup_cleanup() {
    local cleanup_func=$1
    trap "$cleanup_func" EXIT INT TERM
}

#######################################
# 报告生成
#######################################
generate_json_report() {
    local output_file=$1
    shift
    local results="$@"
    
    cat > "$output_file" <<EOF
{
  "timestamp": "$(generate_timestamp)",
  "results": $results
}
EOF
}
