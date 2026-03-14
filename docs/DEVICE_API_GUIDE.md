# 设备注册接口设计与 MQTT 测试指南

## 一、设备注册接口设计

### 1.1 接口概览

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 设备初始化 | POST | `/device/init` | 创建设备并获取 Token |
| 设备验证(v1) | POST | `/device/verify` | ECC 加密验证 |
| 设备验证(v2) | POST | `/device/verify-v2` | SHA256 哈希验证 |
| 设备查询 | GET | `/device/open/query` | 通过 Token 查询设备 |
| 续期 | GET | `/device/renew` | 更新设备有效期 |

### 1.2 设备初始化接口

**请求：**
```bash
POST /device/init?docker=true
Content-Type: application/json

{
    "username": "test_device_001",
    "profile": {
        "wifi_mac": "02:a1:b2:c3:d4:e5",
        "ethernet_mac": "02:f2:a3:b4:c5:d6",
        "bluetooth_mac": "02:e5:d4:c3:b2:a1",
        "imei": "IMEI123456789",
        "imsi": "IMSI987654321"
    }
}
```

**响应：**
```json
{
    "code": 0,
    "message": "成功",
    "data": {
        "token": "ABCD1234EFGH5678",
        "serial": "a1b2c3d4e5f6...",
        "algorithm": "EC",
        "public_key": "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAE...",
        "expire_date": "2026-03-14",
        "expire_info": "sha256_hash_value"
    }
}
```

**关键字段说明：**

| 字段 | 说明 | 用途 |
|------|------|------|
| `token` | 设备令牌 | MQTT 登录用户名/密码 |
| `serial` | 设备序列号 | SHA256(MAC地址组合) |
| `public_key` | ECC 公钥 | 加密通信/验签 |
| `expire_date` | 过期日期 | 设备有效期 |

### 1.3 设备验证接口 (v2)

**验证流程：**
1. 获取设备 MAC 地址和公钥
2. 计算 SHA256: `ethernet_mac|wifi_mac|bluetooth_mac|public_key`
3. 发送验证请求

**请求：**
```bash
POST /device/verify-v2?token=ABCD1234EFGH5678
Content-Type: text/plain

a1b2c3d4e5f6...  # SHA256 哈希值
```

**响应：**
```json
{
    "code": 0,
    "data": {
        "expire_info": "sha256_hash",
        "expire_date": "2026-03-14"
    }
}
```

## 二、MQTT 登录流程

### 2.1 连接参数

| 参数 | 值 | 说明 |
|------|-----|------|
| Broker | `localhost` | MQTT 服务器地址 |
| Port | `1883` | MQTT 端口 |
| Username | `{token}` | 设备初始化获取的 token |
| Password | `{token}` | 与用户名相同 |
| Client ID | `{serial}` | 设备序列号 |
| QoS | `1` | 服务质量级别 |

### 2.2 连接示例

```bash
# 使用 mosquitto_pub 连接
mosquitto_pub -h localhost -p 1883 \
    -u "ABCD1234EFGH5678" \
    -P "ABCD1234EFGH5678" \
    -i "a1b2c3d4e5f6..." \
    -t "device/test" \
    -m "hello" \
    -q 1
```

```python
# Python paho-mqtt 示例
import paho.mqtt.client as mqtt

client = mqtt.Client(client_id="a1b2c3d4e5f6...")
client.username_pw_set(
    username="ABCD1234EFGH5678",
    password="ABCD1234EFGH5678"
)
client.connect("localhost", 1883, 60)
```

## 三、快速测试

### 3.1 生成测试设备

```bash
# 生成 10 个测试设备
./scripts/test/e2e/generate-mqtt-test-data.sh

# 生成 100 个设备到指定目录
./scripts/test/e2e/generate-mqtt-test-data.sh -n 100 -o /tmp/devices

# 指定目标服务器
./scripts/test/e2e/generate-mqtt-test-data.sh -e http://192.168.1.100:8080 -n 50
```

**输出文件：**
- `devices.json` - 设备详细信息
- `mqtt-config.yaml` - MQTT 配置文件
- `device-tokens.txt` - 设备令牌列表

### 3.2 验证设备

```bash
# 查询设备信息
./scripts/test/e2e/verify-device.sh query ABCD1234EFGH5678

# 验证设备 (v2)
./scripts/test/e2e/verify-device.sh verify-v2 ABCD1234EFGH5678

# 批量验证
./scripts/test/e2e/verify-device.sh batch ./data/device-tokens.txt

# 更新有效期
./scripts/test/e2e/verify-device.sh renew SERIAL123 2025-12-31
```

### 3.3 MQTT 连接测试

```bash
# 测试连接
./scripts/test/e2e/mqtt-stress-test.sh connection

# 吞吐量测试
./scripts/test/e2e/mqtt-stress-test.sh throughput -n 1000

# 并发客户端测试
./scripts/test/e2e/mqtt-stress-test.sh concurrent -c 100
```

## 四、设备数据结构

### 4.1 DeviceDo (请求体)

```java
public class DeviceDo {
    private String number;        // 设备序列号
    private String username;      // 用户名
    private String password;      // 密码
    private String token;         // 设备令牌
    private DeviceProfile profile; // 设备档案
    private Long uid;             // 用户ID
    private String name;          // 设备名称
    private String type;          // 设备类型
}
```

### 4.2 DeviceProfile (设备档案)

```java
public class DeviceProfile {
    private String wifiMac;           // WiFi MAC
    private String ethernetMac;       // 以太网 MAC
    private String bluetoothMac;      // 蓝牙 MAC
    private String imei;              // IMEI
    private String imsi;              // IMSI
    private KeyPairProfile keyPairProfile;      // 密钥对
    private ExpirationProfile expirationProfile; // 有效期
}
```

### 4.3 KeyPairProfile (密钥对)

```java
public class KeyPairProfile {
    private String keyPairAlgorithm;  // 算法 (EC)
    private String publicKey;         // 公钥
    private String privateKey;        // 私钥
}
```

## 五、安全注意事项

### 5.1 Token 安全
- Token 是 MQTT 登录的唯一凭证，请妥善保管
- Token 在传输过程中应使用 HTTPS
- 建议定期更换 Token

### 5.2 密钥安全
- 私钥只在设备端保存，服务器不存储设备私钥
- 公钥用于服务器验证设备身份
- ECC 密钥长度为 256 位

### 5.3 有效期管理
- 设备默认有效期为 1 年 + 1 个月
- 可通过 `/device/renew` 接口续期
- 过期设备无法通过验证

## 六、故障排查

### 6.1 设备初始化失败

```bash
# 检查 API 服务
curl http://localhost:8080/actuator/health

# 检查 MAC 地址格式
echo "02:a1:b2:c3:d4:e5" | grep -E '^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$'
```

### 6.2 MQTT 连接失败

```bash
# 检查 MQTT 端口
nc -zv localhost 1883

# 检查 Token 是否正确
curl "http://localhost:8080/device/open/query?token=YOUR_TOKEN"
```

### 6.3 验证失败

```bash
# 确认 MAC 地址匹配
# 确认 Token 未过期
# 确认哈希计算正确
```

## 七、API 测试示例

### 7.1 完整流程测试

```bash
#!/bin/bash

API="http://localhost:8080"

# 1. 初始化设备
echo "1. 初始化设备..."
RESPONSE=$(curl -s -X POST "${API}/device/init?docker=true" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "test_device",
        "profile": {
            "wifi_mac": "02:a1:b2:c3:d4:e5",
            "ethernet_mac": "02:f2:a3:b4:c5:d6",
            "bluetooth_mac": "02:e5:d4:c3:b2:a1"
        }
    }')

TOKEN=$(echo $RESPONSE | jq -r '.data.token')
echo "Token: $TOKEN"

# 2. 查询设备
echo "2. 查询设备..."
curl -s "${API}/device/open/query?token=${TOKEN}" | jq '.data.username'

# 3. MQTT 连接测试
echo "3. MQTT 连接测试..."
mosquitto_pub -h localhost -p 1883 \
    -u "${TOKEN}" -P "${TOKEN}" \
    -t "test/topic" -m "hello" -q 1

echo "完成!"
```

## 八、相关脚本

| 脚本 | 用途 |
|------|------|
| `generate-mqtt-test-data.sh` | 生成 MQTT 测试设备和凭证 |
| `verify-device.sh` | 设备验证和查询 |
| `mqtt-stress-test.sh` | MQTT 压测 |
| `api-stress-test.sh` | HTTP API 压测 |
