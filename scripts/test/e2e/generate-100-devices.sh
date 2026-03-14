#!/bin/bash
#
# 生成100个测试设备并保存登录信息
#

set -e

API_ENDPOINT="${API_ENDPOINT:-http://localhost:8080}"
OUTPUT_DIR="${OUTPUT_DIR:-./data}"
DEVICE_COUNT=100

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok() { echo -e "${GREEN}[OK]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 生成随机MAC
gen_mac() {
    printf '02:%02x:%02x:%02x:%02x:%02x' $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
}

mkdir -p "$OUTPUT_DIR"

log_info "开始生成 $DEVICE_COUNT 个测试设备..."
log_info "API: $API_ENDPOINT"

# CSV文件头
echo "index,username,token,serial,public_key,expire_date,mqtt_username,mqtt_password,mqtt_client_id" > "$OUTPUT_DIR/devices-100.csv"

# JSON文件头
echo '{"devices":[' > "$OUTPUT_DIR/devices-100.json"

success=0
fail=0

for i in $(seq 1 $DEVICE_COUNT); do
    username="mqtt_test_$(date +%s)_$i"
    wifi_mac=$(gen_mac)
    eth_mac=$(gen_mac)
    bt_mac=$(gen_mac)
    
    request=$(cat <<EOF
{
    "username": "$username",
    "name": "device_$i",
    "profile": {
        "wifi_mac": "$wifi_mac",
        "ethernet_mac": "$eth_mac",
        "bluetooth_mac": "$bt_mac"
    }
}
EOF
)
    
    response=$(curl -s -X POST "${API_ENDPOINT}/device/init?docker=true" \
        -H "Content-Type: application/json" \
        -d "$request" 2>/dev/null)
    
    # 解析响应
    code=$(echo "$response" | jq -r '.code // -1' 2>/dev/null)
    
    if [ "$code" == "0" ] || [ "$code" == "200" ]; then
        token=$(echo "$response" | jq -r '.detail.token // empty' 2>/dev/null)
        serial=$(echo "$response" | jq -r '.detail.serial // empty' 2>/dev/null)
        pub_key=$(echo "$response" | jq -r '.detail.publicKey // empty' 2>/dev/null)
        expire=$(echo "$response" | jq -r '.detail.expire_date // empty' 2>/dev/null)
        
        if [ -n "$token" ] && [ -n "$serial" ]; then
            # CSV
            echo "$i,$username,$token,$serial,$pub_key,$expire,$token,$token,$serial" >> "$OUTPUT_DIR/devices-100.csv"
            
            # JSON
            if [ $i -gt 1 ]; then
                echo "," >> "$OUTPUT_DIR/devices-100.json"
            fi
            
            json_obj=$(cat <<EOF
{"index":$i,"username":"$username","token":"$token","serial":"$serial","public_key":"$pub_key","expire_date":"$expire","mqtt_username":"$token","mqtt_password":"$token","mqtt_client_id":"$serial"}
EOF
)
            echo -n "$json_obj" >> "$OUTPUT_DIR/devices-100.json"
            
            ((success++))
            printf "\r  进度: %d/%d (成功: %d, 失败: %d)" "$i" "$DEVICE_COUNT" "$success" "$fail"
        else
            ((fail++))
            log_error "  设备 $i 响应缺少token或serial"
        fi
    else
        ((fail++))
        printf "\r  进度: %d/%d (成功: %d, 失败: %d)" "$i" "$DEVICE_COUNT" "$success" "$fail"
        echo "" >> "$OUTPUT_DIR/failed-requests.log"
        echo "Device $i failed: $response" >> "$OUTPUT_DIR/failed-requests.log"
    fi
    
    # 短暂延迟避免压垮服务
    sleep 0.2
done

# JSON文件尾
echo '],"count":'$success',"generated_at":"'$(date -Iseconds)'"}' >> "$OUTPUT_DIR/devices-100.json"

printf "\n"
echo ""
echo "========================================"
echo "生成完成"
echo "========================================"
echo "成功: $success"
echo "失败: $fail"
echo ""
echo "输出文件:"
echo "  CSV:  $OUTPUT_DIR/devices-100.csv"
echo "  JSON: $OUTPUT_DIR/devices-100.json"
echo ""

if [ -f "$OUTPUT_DIR/failed-requests.log" ]; then
    echo "失败日志: $OUTPUT_DIR/failed-requests.log"
fi

# 显示前5个设备示例
if [ $success -gt 0 ]; then
    echo ""
    echo "前5个设备示例:"
    echo "----------------------------------------"
    head -6 "$OUTPUT_DIR/devices-100.csv" | column -t -s,
fi
