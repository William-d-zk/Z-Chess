#!/bin/bash
# 生成测试设备并返回登录信息

# 配置
BASE_URL="${ZCHESS_BASE_URL:-http://localhost:8080}"
API_URL="$BASE_URL/device"

echo "=========================================="
echo "Z-Chess 测试设备生成工具"
echo "API地址: $API_URL"
echo "=========================================="
echo ""

# 生成唯一的测试设备信息
TIMESTAMP=$(date +%s)
RANDOM_SUFFIX=$(openssl rand -hex 4 2>/dev/null || echo "$TIMESTAMP")
TEST_DEVICE_NAME="test_device_$RANDOM_SUFFIX"
TEST_USERNAME="testuser_$RANDOM_SUFFIX"
TEST_PASSWORD="TestPass_$(openssl rand -hex 8 2>/dev/null || echo "12345678")"
TEST_NUMBER="TEST$(date +%Y%m%d%H%M%S)$((RANDOM % 1000))"

# 生成MAC地址（用于init接口）
generate_mac() {
    printf '02:%02x:%02x:%02x:%02x:%02x' $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256)) $((RANDOM%256))
}

ETHERNET_MAC=$(generate_mac)
WIFI_MAC=$(generate_mac)
BLUETOOTH_MAC=$(generate_mac)

echo "【生成的测试设备信息】"
echo "设备名称: $TEST_DEVICE_NAME"
echo "用户名: $TEST_USERNAME"
echo "密码: $TEST_PASSWORD"
echo "设备编号: $TEST_NUMBER"
echo ""

# 方法1: 使用 /device/register 接口 (普通注册)
echo "=========================================="
echo "方法1: 调用 /device/register 接口"
echo "=========================================="

REGISTER_PAYLOAD=$(cat <<EOF
{
    "name": "$TEST_DEVICE_NAME",
    "username": "$TEST_USERNAME",
    "number": "$TEST_NUMBER",
    "profile": {
        "ethernet_mac": "$ETHERNET_MAC",
        "wifi_mac": "$WIFI_MAC",
        "bluetooth_mac": "$BLUETOOTH_MAC"
    }
}
EOF
)

echo "请求数据:"
echo "$REGISTER_PAYLOAD" | jq . 2>/dev/null || echo "$REGISTER_PAYLOAD"
echo ""

REGISTER_RESPONSE=$(curl -s -X POST "$API_URL/register" \
    -H "Content-Type: application/json" \
    -d "$REGISTER_PAYLOAD" 2>/dev/null)

echo "注册响应:"
echo "$REGISTER_RESPONSE" | jq . 2>/dev/null || echo "$REGISTER_RESPONSE"
echo ""

# 提取token
TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
    echo "=========================================="
    echo "【设备登录信息】"
    echo "=========================================="
    echo "Token: $TOKEN"
    echo "用户名: $TEST_USERNAME"
    echo "密码: $TEST_PASSWORD"
    echo "设备编号: $TEST_NUMBER"
    echo ""
    
    # 查询设备详情
    echo "=========================================="
    echo "查询设备详情..."
    echo "=========================================="
    curl -s "$API_URL/open/query?token=$TOKEN" | jq . 2>/dev/null || echo "查询失败"
else
    echo "=========================================="
    echo "【方法1失败，尝试方法2: /device/init】"
    echo "=========================================="
    
    INIT_PAYLOAD=$(cat <<EOF
{
    "username": "$TEST_USERNAME",
    "profile": {
        "ethernet_mac": "$ETHERNET_MAC",
        "wifi_mac": "$WIFI_MAC",
        "bluetooth_mac": "$BLUETOOTH_MAC"
    }
}
EOF
)
    
    echo "请求数据:"
    echo "$INIT_PAYLOAD" | jq . 2>/dev/null || echo "$INIT_PAYLOAD"
    echo ""
    
    INIT_RESPONSE=$(curl -s -X POST "$API_URL/init?docker=false" \
        -H "Content-Type: application/json" \
        -d "$INIT_PAYLOAD" 2>/dev/null)
    
    echo "初始化响应:"
    echo "$INIT_RESPONSE" | jq . 2>/dev/null || echo "$INIT_RESPONSE"
    echo ""
    
    # 提取init返回的token
    TOKEN=$(echo "$INIT_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    SERIAL=$(echo "$INIT_RESPONSE" | grep -o '"serial":"[^"]*"' | cut -d'"' -f4)
    
    if [ -n "$TOKEN" ]; then
        echo "=========================================="
        echo "【设备登录信息】"
        echo "=========================================="
        echo "Token: $TOKEN"
        echo "Serial: $SERIAL"
        echo "用户名: $TEST_USERNAME"
        echo ""
        
        # 查询设备详情
        echo "=========================================="
        echo "查询设备详情..."
        echo "=========================================="
        curl -s "$API_URL/open/query?token=$TOKEN" | jq . 2>/dev/null || echo "查询失败"
    else
        echo "设备注册失败，请检查服务是否正常运行"
        echo ""
        echo "检查服务状态:"
        curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "无法连接到服务"
    fi
fi

echo ""
echo "=========================================="
echo "测试设备生成完成"
echo "=========================================="
