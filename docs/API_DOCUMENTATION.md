# Z-Chess API 接口文档

> 版本: 1.0.0  
> 生成日期: 2026-03-14  
> 适用版本: Z-Chess v3.x

## 目录

1. [概述](#概述)
2. [设备管理接口](#设备管理接口)
3. [消息管理接口](#消息管理接口)
4. [集群管理接口](#集群管理接口)
5. [一致性查询接口](#一致性查询接口)
6. [测试接口](#测试接口)
7. [MQTT 接入](#mqtt-接入)
8. [数据模型](#数据模型)
9. [错误码](#错误码)

---

## 概述

### 基础信息

| 项目 | 说明 |
|------|------|
| 基础 URL | `http://localhost:8080` |
| 内容类型 | `application/json` |
| 编码格式 | UTF-8 |
| 响应格式 | `ZResponse` 统一响应结构 |

### 统一响应结构

```json
{
  "code": 0,
  "message": "成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | 状态码，0 表示成功 |
| `message` | string | 响应消息 |
| `data` | any | 响应数据 |

### 通用状态码

| 状态码 | 说明 |
|--------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 设备管理接口

**基础路径**: `/device`

### 1. 设备初始化

创建设备并获取 Token，用于设备首次注册。

```
POST /device/init?docker={boolean}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `docker` | boolean | 否 | 是否为 Docker 版本初始化，默认 false |

**请求体**:

```json
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

**响应示例**:

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

**响应字段说明**:

| 字段 | 说明 | 用途 |
|------|------|------|
| `token` | 设备令牌 | MQTT 登录用户名/密码 |
| `serial` | 设备序列号 | SHA256(MAC地址组合) |
| `algorithm` | 密钥算法 | 固定为 "EC" |
| `public_key` | ECC 公钥 | 加密通信/验签 |
| `expire_date` | 过期日期 | 设备有效期 |
| `expire_info` | 有效期哈希 | SHA256(serial\|date\|publicKey) |

---

### 2. 设备验证 (v1 - ECC 加密)

使用 ECC 公钥加密数据进行验证。

```
POST /device/verify?token={token}&expireAt={date}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | string | 是 | 设备令牌 |
| `expireAt` | string | 否 | 失效日期，格式 yyyy-MM-dd |

**请求体**: 用 ECC 公钥加密的设备档案数据

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "expire_info": "sha256_hash",
    "expire_date": "2026-03-14"
  }
}
```

---

### 3. 设备验证 (v2 - SHA256 哈希)

使用 SHA256 哈希值进行验证（推荐）。

```
POST /device/verify-v2?token={token}&expireAt={date}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | string | 是 | 设备令牌 |
| `expireAt` | string | 否 | 失效日期，格式 yyyy-MM-dd |

**请求体**: 

SHA256 哈希值，计算方式：
```
SHA256(ethernet_mac|wifi_mac|bluetooth_mac|public_key)
```

**请求体示例**:
```
a1b2c3d4e5f6...
```

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "expire_info": "sha256_hash",
    "expire_date": "2026-03-14"
  }
}
```

---

### 4. 续期设备有效期

更新设备的有效期。

```
GET /device/renew?serial={serial}&expireAt={date}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `serial` | string | 是 | 设备序列号 |
| `expireAt` | string | 是 | 新失效日期，格式 yyyy-MM-dd |

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "renew_info": "serial|2026-03-14",
    "signature": "ecc_signature"
  }
}
```

---

### 5. 通过 Token 查询设备

```
GET /device/open/query?token={token}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | string | 是 | 设备令牌 |

**响应**: 返回完整的 DeviceEntity 数据

---

### 6. 通过序列号查询设备 (管理接口)

```
GET /device/manager/query?number={number}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `number` | string | 是 | 设备序列号 |

---

### 7. 查询在线设备列表

```
GET /device/online/all?page={page}&size={size}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 0 |
| `size` | int | 否 | 每页数量，默认 20，最大 50 |

---

### 8. 查询存储设备列表

```
GET /device/online/stored?page={page}&size={size}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `page` | int | 否 | 页码，默认 0 |
| `size` | int | 否 | 每页数量，默认 20，最大 50 |

---

### 9. 生成验证码

```
GET /device/gvcode?serialNo={serialNo}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `serialNo` | string | 是 | 设备序列号 |

---

### 10. 验证重置出厂初始化

```
POST /device/vreinit?serialNo={serialNo}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `serialNo` | string | 是 | 设备序列号 |

**请求体**: 验证码

---

### 11. 注册设备

```
POST /device/register
```

**请求体**: DeviceDo 对象

```json
{
  "number": "device_serial",
  "username": "device_user",
  "password": "device_pwd",
  "profile": { ... },
  "uid": 12345,
  "name": "Device Name",
  "type": "Device Type"
}
```

---

## 消息管理接口

**基础路径**: `/message`

### 1. 提交消息

```
POST /message/submit?token={token}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | string | 是 | 设备令牌 |

**请求体**:

```json
{
  "origin": 12345,
  "number": "target_device",
  "topic": "test/topic",
  "content": "message content",
  "protocol": "mqtt"
}
```

---

### 2. 过滤 Topic

```
POST /message/filter?token={token}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `token` | string | 是 | 设备令牌 |

**请求体**: 过滤模式字符串

---

## 集群管理接口

**基础路径**: `/cluster`

### 1. 变更集群拓扑

```
POST /cluster/change
```

**请求体**:

```json
{
  "peer_id": 123456789,
  "host": "192.168.1.100",
  "port": 8080
}
```

---

### 2. 关闭集群服务

```
GET /cluster/close
```

**响应**: 返回日志元数据

---

## 一致性查询接口

**基础路径**: `/api`

### 1. 提交一致性查询

```
POST /api/consistent
```

**请求参数 (Form/Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `input` | string | 是 | 查询输入 |

**响应**: 返回一致性提交结果

---

## Echo 测试接口

**基础路径**: `/echo`

### 1. Hook 测试

```
GET /echo/hook?input={input}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `input` | string | 是 | 输入字符串 |

---

## 测试接口

**基础路径**: `/test`

### 1. 获取认证信息

```
GET /test/get-auth-info
```

---

### 2. 获取任务列表

```
GET /test/get-task
```

---

### 3. 更新任务状态

```
GET /test/update-task-status?taskId={taskId}&status={status}
```

---

### 4. 触发招标任务

```
GET /test/trigger-bidding-task?taskId={taskId}
```

---

### 5. 取消招标任务

```
GET /test/cancel-bidding-task?taskId={taskId}
```

---

## 压力测试接口

**基础路径**: `/api/stress`

### 1. 启动压力测试

```
POST /api/stress/start
```

**请求体**:

```json
{
  "concurrency": 3000,
  "durationSeconds": 60,
  "requestsPerSecondPerClient": 10,
  "targetHost": "localhost",
  "targetPort": 8080,
  "protocol": "zchat"
}
```

---

### 2. 停止压力测试

```
POST /api/stress/stop
```

---

### 3. 强制停止压力测试

```
POST /api/stress/force-stop
```

---

### 4. 获取测试状态

```
GET /api/stress/status
```

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "state": "RUNNING",
    "activeClients": 1000,
    "pendingRequests": 500,
    "totalRequests": 10000,
    "successRequests": 9990,
    "failedRequests": 10,
    "qps": 950.5,
    "successRate": 99.9
  }
}
```

---

### 5. 获取性能指标

```
GET /api/stress/metrics
```

---

### 6. 获取性能报告

```
GET /api/stress/report
```

---

### 7. 更新测试配置

```
POST /api/stress/config
```

**请求体**:

```json
{
  "concurrency": 3000,
  "durationSeconds": 60,
  "requestsPerSecondPerClient": 10,
  "targetHost": "localhost",
  "targetPort": 8080,
  "protocol": "zchat"
}
```

---

### 8. 获取当前配置

```
GET /api/stress/config
```

---

## MQTT 设备测试接口

**基础路径**: `/api/mqtt`

### 1. 单设备连接测试

```
GET /api/mqtt/connect?host={host}&port={port}&username={user}&password={pwd}&clientId={clientId}
```

---

### 2. 多设备并发压力测试

```
POST /api/mqtt/stress
```

**请求体**:

```json
{
  "host": "localhost",
  "port": 1883,
  "deviceCount": 10,
  "connectionsPerDevice": 1
}
```

---

### 3. 验证所有设备连接

```
GET /api/mqtt/verify?host={host}&port={port}&limit={limit}
```

---

### 4. 获取可用设备数量

```
GET /api/mqtt/devices/count
```

---

### 5. 获取随机设备信息

```
GET /api/mqtt/devices/sample
```

---

## ZChat 测试接口

**基础路径**: 根路径

### 1. ZChat 连接

```
GET /zchat
```

---

### 2. 发送消息

```
GET /zchat/send?output={message}
```

---

### 3. 发送 Raft 投票消息

```
GET /zchat/raft/x70
```

---

## Mock 测试接口

**基础路径**: `/mock`

### 1. 模拟日志条目输入

用于测试 Raft 日志映射器。

```
GET /mock/mapper
```

**响应示例**:

```json
{
  "code": 0,
  "message": "mapper test",
  "data": "-"
}
```

---

## HTTP API 测试接口

**基础路径**: `/api/test`

### 1. 健康检查测试

测试集群所有节点的健康状态。

```
GET /api/test/health?host={host}&port={port}&nodes={nodes}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `host` | string | 否 | 主机地址，默认 localhost |
| `port` | int | 否 | 起始端口，默认 8080 |
| `nodes` | int | 否 | 节点数量，默认 3 |

**响应示例**:

```json
{
  "code": 0,
  "data": {
    "test": "health",
    "results": [
      {"node": "raft00", "port": 8080, "status": "UP", "latencyMs": 15},
      {"node": "raft01", "port": 8081, "status": "UP", "latencyMs": 12},
      {"node": "raft02", "port": 8082, "status": "UP", "latencyMs": 18}
    ],
    "totalNodes": 3,
    "healthyNodes": 3
  }
}
```

---

### 2. 设备列表查询测试

```
GET /api/test/devices?host={host}&port={port}&iterations={iterations}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `host` | string | 否 | 主机地址，默认 localhost |
| `port` | int | 否 | 端口，默认 8080 |
| `iterations` | int | 否 | 迭代次数，默认 1 |

---

### 3. 并发 API 压力测试

```
POST /api/test/stress
```

**请求体**:

```json
{
  "host": "localhost",
  "port": 8080,
  "endpoint": "/actuator/health",
  "concurrency": 10,
  "requestsPerClient": 10
}
```

---

### 4. 完整 API 测试套件

```
POST /api/test/full
```

**请求体**:

```json
{
  "host": "localhost",
  "startPort": 8080,
  "nodes": 3
}
```

---

## Arena 网关接口

**基础路径**: `/rest/calculate`

### 1. 计算圆面积

```
GET /rest/calculate/areaOfCircle?radius={radius}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `radius` | int | 是 | 圆半径 |

**响应**: 圆面积 (double)

---

### 2. 缓存测试

```
GET /rest/calculate/cache0?radius={radius}
```

**请求参数 (Query)**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `radius` | int | 是 | 圆半径 |

---

## MQTT 接入

### 连接参数

| 参数 | 值 | 说明 |
|------|-----|------|
| Broker | `localhost` | MQTT 服务器地址 |
| Port | `1883` | MQTT 端口 |
| Username | `{token}` | 设备初始化获取的 token |
| Password | `{token}` | 与用户名相同 |
| Client ID | `{serial}` | 设备序列号 |
| QoS | `1` | 服务质量级别 |

### 连接示例 (mosquitto)

```bash
mosquitto_pub -h localhost -p 1883 \
    -u "ABCD1234EFGH5678" \
    -P "ABCD1234EFGH5678" \
    -i "a1b2c3d4e5f6..." \
    -t "device/test" \
    -m "hello" \
    -q 1
```

### 连接示例 (Python paho-mqtt)

```python
import paho.mqtt.client as mqtt

client = mqtt.Client(client_id="a1b2c3d4e5f6...")
client.username_pw_set(
    username="ABCD1234EFGH5678",
    password="ABCD1234EFGH5678"
)
client.connect("localhost", 1883, 60)
```

---

## 数据模型

### DeviceDo (设备数据对象)

| 字段 | 类型 | 说明 |
|------|------|------|
| `number` | string | 设备序列号 |
| `username` | string | 用户名 |
| `password` | string | 密码（敏感） |
| `token` | string | 设备令牌（敏感） |
| `profile` | DeviceProfile | 设备档案 |
| `uid` | long | 用户ID |
| `name` | string | 设备名称 |
| `type` | string | 设备类型 |

### DeviceProfile (设备档案)

| 字段 | 类型 | 说明 |
|------|------|------|
| `wifi_mac` | string | WiFi MAC 地址 |
| `ethernet_mac` | string | 以太网 MAC 地址 |
| `bluetooth_mac` | string | 蓝牙 MAC 地址 |
| `imei` | string | IMEI |
| `imsi` | string | IMSI |
| `key_pair_profile` | KeyPairProfile | 密钥对信息 |
| `expiration_profile` | ExpirationProfile | 有效期信息 |

### KeyPairProfile (密钥对)

| 字段 | 类型 | 说明 |
|------|------|------|
| `key_pair_algorithm` | string | 算法 (EC) |
| `public_key` | string | 公钥 |
| `private_key` | string | 私钥（敏感） |

### ExpirationProfile (有效期)

| 字段 | 类型 | 说明 |
|------|------|------|
| `activation_at` | datetime | 首次激活时间 |
| `expire_at` | datetime | 失效时间 |
| `last_renew_at` | datetime | 最近一次续期时间 |

### DeviceEntity (设备实体)

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | long | 数据库ID |
| `device_id` | long | 设备ID |
| `token` | string | 设备令牌（唯一） |
| `password` | string | 密码 |
| `password_id` | long | 密码版本ID |
| `username` | string | 用户名 |
| `code` | string | 设备代码 |
| `notice` | string | 序列号（与 v_notice 相同） |
| `v_notice` | string | 序列号 |
| `profile` | DeviceProfile | 设备档案 (JSONB) |
| `invalid_at` | datetime | 失效时间 |
| `dk_scene` | long | 场景ID |
| `dk_factor` | long | 因子ID |
| `dk_function` | long | 功能ID |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

### MessageDo (消息对象)

| 字段 | 类型 | 说明 |
|------|------|------|
| `origin` | long | 发送方ID |
| `number` | string | 目标设备号 |
| `topic` | string | MQTT Topic |
| `payload` | byte[] | 消息负载（二进制） |
| `content` | string | 消息内容（文本） |
| `protocol` | string | 协议类型 (mqtt/z-chat) |

### ClusterDo (集群节点)

| 字段 | 类型 | 说明 |
|------|------|------|
| `peer_id` | long | 节点ID |
| `host` | string | 主机地址 |
| `port` | int | 端口号 |

---

## 错误码

### CodeKing (King 模块)

| 错误码 | 说明 |
|--------|------|
| 0 | SUCCESS |
| 1 | FAIL |
| 2 | MISS |
| 3 | ERROR |

### KingCode (King 配置)

| 错误码 | 说明 |
|--------|------|
| 200 | SUCCESS |
| 500 | SERVER_ERROR |
| 400 | BAD_REQUEST |
| 401 | UNAUTHORIZED |
| 403 | FORBIDDEN |
| 404 | NOT_FOUND |

---

## 安全注意事项

### Token 安全
- Token 是 MQTT 登录的唯一凭证，请妥善保管
- Token 在传输过程中应使用 HTTPS
- 建议定期更换 Token

### 密钥安全
- 私钥只在设备端保存，服务器不存储设备私钥
- 公钥用于服务器验证设备身份
- ECC 密钥长度为 256 位

### 有效期管理
- 设备默认有效期为 1 年 + 1 个月
- 可通过 `/device/renew` 接口续期
- 过期设备无法通过验证

---

## 故障排查

### 设备初始化失败

```bash
# 检查 API 服务
curl http://localhost:8080/actuator/health

# 检查 MAC 地址格式
echo "02:a1:b2:c3:d4:e5" | grep -E '^([0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}$'
```

### MQTT 连接失败

```bash
# 检查 MQTT 端口
nc -zv localhost 1883

# 检查 Token 是否正确
curl "http://localhost:8080/device/open/query?token=YOUR_TOKEN"
```

### 验证失败

- 确认 MAC 地址匹配
- 确认 Token 未过期
- 确认哈希计算正确

---

## 相关脚本

| 脚本 | 用途 |
|------|------|
| `scripts/test/e2e/generate-mqtt-test-data.sh` | 生成 MQTT 测试设备和凭证 |
| `scripts/test/e2e/verify-device.sh` | 设备验证和查询 |
| `scripts/test/e2e/mqtt-stress-test.sh` | MQTT 压测 |
| `scripts/test/e2e/api-stress-test.sh` | HTTP API 压测 |

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0.0 | 2026-03-14 | 初始版本 |

---

*文档生成工具: Z-Chess CLI*  
*版权声明: MIT License*
