#!/bin/bash
#
# MQTT 测试工具库
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-utils.sh"

# MQTT 配置
MQTT_HOST=${MQTT_HOST:-"localhost"}
MQTT_PORT=${MQTT_PORT:-1883}
MQTT_USER=${MQTT_USER:-"test"}
MQTT_PASS=${MQTT_PASS:-"test"}
MQTT_TIMEOUT=${MQTT_TIMEOUT:-10}

#######################################
# 检查 MQTT 端口
#######################################
mqtt_check_port() {
    local host=${1:-$MQTT_HOST}
    local port=${2:-$MQTT_PORT}
    
    if timeout "$MQTT_TIMEOUT" bash -c "</dev/tcp/$host/$port" 2>/dev/null; then
        log_success "MQTT 端口 $host:$port 可连接"
        return 0
    else
        log_error "MQTT 端口 $host:$port 不可连接"
        return 1
    fi
}

#######################################
# 发布 MQTT 消息
#######################################
mqtt_publish() {
    local topic=$1
    local payload=$2
    local qos=${3:-0}
    
    if ! command -v mosquitto_pub &> /dev/null; then
        log_error "未找到 mosquitto_pub，请安装 mosquitto-clients"
        return 1
    fi
    
    mosquitto_pub \
        -h "$MQTT_HOST" \
        -p "$MQTT_PORT" \
        -u "$MQTT_USER" \
        -P "$MQTT_PASS" \
        -t "$topic" \
        -m "$payload" \
        -q "$qos" \
        -W "$MQTT_TIMEOUT" 2>/dev/null
    
    local exit_code=$?
    
    if [ $exit_code -eq 0 ]; then
        log_debug "发布成功: $topic"
        return 0
    else
        log_error "发布失败: $topic (exit: $exit_code)"
        return 1
    fi
}

#######################################
# 订阅 MQTT 主题
#######################################
mqtt_subscribe() {
    local topic=$1
    local output_file=$2
    local duration=${3:-5}
    local qos=${4:-0}
    
    if ! command -v mosquitto_sub &> /dev/null; then
        log_error "未找到 mosquitto_sub，请安装 mosquitto-clients"
        return 1
    fi
    
    log_info "订阅主题 $topic (${duration}s)..."
    
    mosquitto_sub \
        -h "$MQTT_HOST" \
        -p "$MQTT_PORT" \
        -u "$MQTT_USER" \
        -P "$MQTT_PASS" \
        -t "$topic" \
        -q "$qos" \
        -W "$((duration + 2))" \
        -C 100 2>/dev/null | head -n 100 > "$output_file" &
    
    local sub_pid=$!
    
    # 等待订阅
    sleep 1
    
    # 运行指定时间
    sleep "$duration"
    
    # 停止订阅
    kill $sub_pid 2>/dev/null || true
    wait $sub_pid 2>/dev/null || true
    
    # 统计接收消息
    local count=$(wc -l < "$output_file" 2>/dev/null || echo 0)
    log_info "接收消息数: $count"
    
    return 0
}

#######################################
# MQTT 延迟测试
#######################################
mqtt_latency_test() {
    local topic=${1:-"test/latency"}
    local count=${2:-10}
    
    log_info "MQTT 延迟测试 (发送 $count 条消息)..."
    
    local latencies=""
    
    for i in $(seq 1 $count); do
        local start_time=$(date +%s%N)
        local payload="{\"seq\": $i, \"ts\": $start_time}"
        
        if mqtt_publish "$topic" "$payload" 1; then
            local end_time=$(date +%s%N)
            local latency_ms=$(((end_time - start_time) / 1000000))
            latencies="$latencies $latency_ms"
            echo -ne "\r进度: $i/$count, 当前延迟: ${latency_ms}ms"
        else
            echo -ne "\r进度: $i/$count, 发送失败"
        fi
        
        sleep 0.1
    done
    
    echo ""
    
    # 计算统计
    if [ -n "$latencies" ]; then
        local avg=$(echo "$latencies" | awk '{sum=0; for(i=1;i<=NF;i++) sum+=$i; print int(sum/NF)}')
        local min=$(echo "$latencies" | awk '{min=$1; for(i=2;i<=NF;i++) if($i<min) min=$i; print min}')
        local max=$(echo "$latencies" | awk '{max=$1; for(i=2;i<=NF;i++) if($i>max) max=$i; print max}')
        
        log_info "延迟统计:"
        log_info "  平均: ${avg}ms"
        log_info "  最小: ${min}ms"
        log_info "  最大: ${max}ms"
    fi
}

#######################################
# MQTT 吞吐量测试
#######################################
mqtt_throughput_test() {
    local topic=${1:-"test/throughput"}
    local duration=${2:-10}
    local parallel=${3:-10}
    
    log_info "MQTT 吞吐量测试 (${duration}s, $parallel 并行)..."
    
    local start_time=$(date +%s)
    local pids=()
    local count_file=$(mktemp)
    echo 0 > "$count_file"
    
    # 启动多个发布进程
    for p in $(seq 1 $parallel); do
        (
            local seq=$p
            while true; do
                local payload="{\"seq\": $seq, \"ts\": $(date +%s%N)}"
                if mqtt_publish "$topic" "$payload" 0; then
                    local current=$(cat "$count_file")
                    echo $((current + 1)) > "$count_file"
                fi
                ((seq += parallel))
            done
        ) &
        pids+=($!)
    done
    
    # 运行指定时间
    sleep "$duration"
    
    # 停止所有进程
    for pid in "${pids[@]}"; do
        kill $pid 2>/dev/null || true
    done
    wait 2>/dev/null || true
    
    local total_count=$(cat "$count_file")
    rm -f "$count_file"
    
    local throughput=$((total_count / duration))
    
    log_info "吞吐量: $total_count 消息 / ${duration}s = $throughput msg/s"
}

#######################################
# MQTT 连接压力测试
#######################################
mqtt_connection_stress() {
    local num_connections=${1:-100}
    local keepalive=${2:-60}
    
    log_info "MQTT 连接压力测试 ($num_connections 连接)..."
    
    local pids=()
    local connected=0
    local failed=0
    
    for i in $(seq 1 $num_connections); do
        (
            local client_id="stress-test-$(date +%s)-$$-$i"
            local sub_file=$(mktemp)
            
            timeout 30 mosquitto_sub \
                -h "$MQTT_HOST" \
                -p "$MQTT_PORT" \
                -u "$MQTT_USER" \
                -P "$MQTT_PASS" \
                -i "$client_id" \
                -t "test/connection" \
                -k "$keepalive" \
                -W 30 2>/dev/null > "$sub_file"
            
            rm -f "$sub_file"
        ) &
        pids+=($!)
        
        if [ $((i % 10)) -eq 0 ]; then
            echo -ne "\r创建连接: $i/$num_connections"
        fi
        
        # 控制创建速率
        sleep 0.05
    done
    
    echo ""
    log_info "等待连接稳定 (5s)..."
    sleep 5
    
    # 统计活跃连接
    log_info "测试完成，正在断开连接..."
    
    for pid in "${pids[@]}"; do
        kill $pid 2>/dev/null || true
    done
    wait 2>/dev/null || true
    
    log_success "连接压力测试完成"
}
