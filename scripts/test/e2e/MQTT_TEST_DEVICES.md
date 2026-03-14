# MQTT 测试设备使用指南

## 设备数据文件

已生成 **100个测试设备**，存储在以下文件中：

| 文件 | 格式 | 说明 |
|------|------|------|
| `devices-100.csv` | CSV | 包含所有设备信息，可用Excel或脚本处理 |
| `devices-100.json` | JSON | 结构化数据，便于程序解析 |

## 设备信息格式

每个设备包含以下字段：

```json
{
  "index": 1,
  "username": "mqtt_test_1773427392_1",
  "token": "55E2A465EAABEE272E7E3A5519CB9BBAA680590EF1CF852ED02F21F7FE3BD257",
  "serial": "5F4607A9BB06CC210E3500FEDF110A3A96010F865712FF0E40A4585BC9E441DE",
  "public_key": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...",
  "expire_date": "2027-04-14",
  "mqtt_username": "55E2A465EAABEE272E7E3A5519CB9BBAA680590EF1CF852ED02F21F7FE3BD257",
  "mqtt_password": "55E2A465EAABEE272E7E3A5519CB9BBAA680590EF1CF852ED02F21F7FE3BD257",
  "mqtt_client_id": "5F4607A9BB06CC210E3500FEDF110A3A96010F865712FF0E40A4585BC9E441DE"
}
```

## MQTT 连接参数

| 参数 | 值 |
|------|-----|
| Broker | `localhost` |
| Port | `1883` |
| Username | `{token}` |
| Password | `{token}` |
| Client ID | `{serial}` |
| QoS | `1` |

## 快速测试示例

### 1. 使用 mosquitto_pub 测试单个设备

```bash
# 获取第一个设备的 token
TOKEN=$(cat devices-100.json | jq -r '.devices[0].token')
SERIAL=$(cat devices-100.json | jq -r '.devices[0].serial')

# 发布测试消息
mosquitto_pub -h localhost -p 1883 \
    -u "$TOKEN" -P "$TOKEN" \
    -i "$SERIAL" \
    -t "test/topic" \
    -m "Hello MQTT" \
    -q 1
```

### 2. 批量测试多个设备

```bash
#!/bin/bash

# 读取前10个设备进行测试
for i in $(seq 0 9); do
    DEVICE=$(cat devices-100.json | jq ".devices[$i]")
    TOKEN=$(echo "$DEVICE" | jq -r '.token')
    SERIAL=$(echo "$DEVICE" | jq -r '.serial')
    
    echo "Testing device $((i+1)): $SERIAL"
    
    mosquitto_pub -h localhost -p 1883 \
        -u "$TOKEN" -P "$TOKEN" \
        -i "$SERIAL" \
        -t "test/device_$i" \
        -m "Test message from device $((i+1))" \
        -q 1 &
done

wait
echo "Batch test completed"
```

### 3. 使用 Python 进行并发测试

```python
import paho.mqtt.client as mqtt
import json
import time
import threading

# 加载设备数据
with open('devices-100.json', 'r') as f:
    data = json.load(f)

devices = data['devices']

def test_device(device):
    client = mqtt.Client(client_id=device['mqtt_client_id'])
    client.username_pw_set(
        username=device['mqtt_username'],
        password=device['mqtt_password']
    )
    
    try:
        client.connect("localhost", 1883, 60)
        client.publish(
            f"test/{device['index']}",
            f"Message from device {device['index']}",
            qos=1
        )
        client.disconnect()
        print(f"Device {device['index']}: OK")
    except Exception as e:
        print(f"Device {device['index']}: Failed - {e}")

# 并发测试10个设备
threads = []
for device in devices[:10]:
    t = threading.Thread(target=test_device, args=(device,))
    threads.append(t)
    t.start()

for t in threads:
    t.join()
```

### 4. 提取令牌列表用于批量测试

```bash
# 提取所有 token
cat devices-100.json | jq -r '.devices[].token' > tokens.txt

# 提取所有 serial
cat devices-100.json | jq -r '.devices[].serial' > serials.txt

# 提取 MQTT 连接信息
cat devices-100.json | jq -r '.devices[] | "\(.mqtt_username),\(.mqtt_password),\(.mqtt_client_id)"' > mqtt-credentials.csv
```

## 数据库验证

验证设备是否已正确注册到数据库：

```bash
export PGPASSWORD=chess123
psql -h localhost -p 6543 -U chess -d isahl_9.x -c 'SELECT COUNT(*) FROM "isahl"."zc_id_devi-chess";'
```

预期输出：
```
 count
-------
   100
(1 row)
```

## 压力测试建议

### 分批次测试

建议将100个设备分批次进行测试：

| 批次 | 设备数 | 测试目的 |
|------|--------|----------|
| 1-10 | 10 | 基础功能验证 |
| 11-30 | 20 | 中等并发测试 |
| 31-60 | 30 | 高并发测试 |
| 61-100 | 40 | 极限压力测试 |

### 测试指标

- **连接成功率**: 应 > 99%
- **消息发布成功率**: 应 > 99%
- **平均延迟**: < 100ms
- **P99 延迟**: < 500ms

## 故障排查

### 设备连接失败

```bash
# 检查设备是否存在
curl "http://localhost:8080/device/open/query?token=DEVICE_TOKEN"

# 检查 MQTT 端口
nc -zv localhost 1883
```

### Token 失效

如果 token 过期，需要重新生成设备：

```bash
# 重新生成单个设备
curl -X POST "http://localhost:8080/device/init?docker=true" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "new_device",
        "name": "NewDevice",
        "profile": {
            "wifi_mac": "02:a1:b2:c3:d4:e5",
            "ethernet_mac": "02:f2:a3:b4:c5:d6",
            "bluetooth_mac": "02:e5:d4:c3:b2:a1"
        }
    }'
```

## 生成更多设备

如需生成更多设备，可使用以下脚本：

```bash
./generate-100-devices.sh
```

或自定义数量：

```bash
# 修改脚本中的 DEVICE_COUNT 变量
DEVICE_COUNT=200 ./generate-100-devices.sh
```

## 注意事项

1. **有效期**: 设备有效期至 2027-04-14
2. **唯一性**: 每个设备的 MAC 地址组合是唯一的
3. **安全性**: Token 是 MQTT 登录凭证，请妥善保管
4. **性能**: 大批量测试时建议分批进行，避免压垮服务
