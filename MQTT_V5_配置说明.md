# MQTT v5 配置说明

## 配置概述

MQTT v5 配置通过 Spring Boot 的配置文件（`application.yml` 或 `application.properties`）进行管理。

**配置前缀**: `z.chess.mqtt.v5`

---

## 配置示例

### YAML 配置 (application.yml)

```yaml
z:
  chess:
    mqtt:
      v5:
        # 主开关
        enabled: true
        
        # 特性开关
        topic-alias-enabled: true
        message-expiry-enabled: true
        flow-control-enabled: true
        shared-subscription-enabled: true
        enhanced-auth-enabled: false
        
        # 会话配置
        default-session-expiry-interval: 0
        max-session-expiry-interval: 604800
        
        # 流量控制
        server-receive-maximum: 65535
        client-receive-maximum: 0
        
        # 主题别名
        server-topic-alias-maximum: 65535
        client-topic-alias-maximum: 0
        
        # 报文大小
        maximum-packet-size: 268435455
        default-maximum-packet-size: 65536
        
        # 特性支持
        retain-available: true
        maximum-qos: 2
        wildcard-subscription-available: true
        subscription-identifier-available: true
        shared-subscription-available: true
        
        # 遗嘱配置
        max-will-delay-interval: 86400
        default-will-delay-interval: 0
        
        # 增强认证
        supported-auth-methods:
          - SCRAM-SHA-256
          - SCRAM-SHA-1
        default-auth-method: SCRAM-SHA-256
        auth-timeout-seconds: 60
        reauthentication-allowed: true
        
        # 性能调优
        topic-alias-cache-size: 1000
        message-expiry-check-interval: 1000
        max-pending-messages: 1000
```

### Properties 配置 (application.properties)

```properties
# 主开关
z.chess.mqtt.v5.enabled=true

# 特性开关
z.chess.mqtt.v5.topic-alias-enabled=true
z.chess.mqtt.v5.message-expiry-enabled=true
z.chess.mqtt.v5.flow-control-enabled=true
z.chess.mqtt.v5.shared-subscription-enabled=true
z.chess.mqtt.v5.enhanced-auth-enabled=false

# 流量控制
z.chess.mqtt.v5.server-receive-maximum=65535

# 主题别名
z.chess.mqtt.v5.server-topic-alias-maximum=65535

# 特性支持
z.chess.mqtt.v5.maximum-qos=2
z.chess.mqtt.v5.retain-available=true
```

---

## 配置项详解

### 1. 主开关

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | `true` | 是否启用 MQTT v5 支持。设置为 `false` 时，所有 v5 特性将被禁用，仅支持 MQTT v3.1.1。 |

### 2. 特性开关

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `topic-alias-enabled` | boolean | `true` | 是否启用主题别名功能。 |
| `message-expiry-enabled` | boolean | `true` | 是否启用消息过期功能。 |
| `flow-control-enabled` | boolean | `true` | 是否启用流量控制（Receive Maximum）。 |
| `shared-subscription-enabled` | boolean | `true` | 是否启用共享订阅功能。 |
| `enhanced-auth-enabled` | boolean | `false` | 是否启用增强认证。需要额外实现 `IQttAuthProvider` 接口。 |

### 3. 会话配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `default-session-expiry-interval` | long | `0` | 默认会话过期时间（秒）。`0` 表示会话随连接断开而结束。 |
| `max-session-expiry-interval` | long | `604800` | 最大会话过期时间（秒），默认 7 天。 |

### 4. 流量控制

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `server-receive-maximum` | int | `65535` | 服务端接收最大值。限制同时处理的 QoS 1/2 消息数量。 |
| `client-receive-maximum` | int | `0` | 要求客户端的接收最大值。`0` 表示不限制客户端。 |

### 5. 主题别名

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `server-topic-alias-maximum` | int | `65535` | 服务端主题别名最大值。 |
| `client-topic-alias-maximum` | int | `0` | 客户端主题别名最大值。`0` 表示不接受客户端的主题别名。 |

### 6. 报文大小

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `maximum-packet-size` | int | `268435455` | 最大报文大小（字节），约 256MB。`0` 表示不限制。 |
| `default-maximum-packet-size` | int | `65536` | 默认最大报文大小（64KB），当客户端未指定时使用。 |

### 7. 特性支持

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `retain-available` | boolean | `true` | 是否支持保留消息。 |
| `maximum-qos` | int | `2` | 支持的最大 QoS 等级（0/1/2）。 |
| `wildcard-subscription-available` | boolean | `true` | 是否支持通配符订阅（`+`、`#`）。 |
| `subscription-identifier-available` | boolean | `true` | 是否支持订阅标识符。 |
| `shared-subscription-available` | boolean | `true` | 是否支持共享订阅（`$share/{group}/{topic}`）。 |

### 8. 遗嘱配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `max-will-delay-interval` | long | `86400` | 最大遗嘱延迟时间（秒），默认 1 天。 |
| `default-will-delay-interval` | long | `0` | 默认遗嘱延迟时间（秒）。 |

### 9. 增强认证

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `supported-auth-methods` | List | `[]` | 支持的认证方法列表，如 `SCRAM-SHA-256`、`KERBEROS` 等。 |
| `default-auth-method` | String | `null` | 默认认证方法。 |
| `auth-timeout-seconds` | int | `60` | 认证超时时间（秒）。 |
| `reauthentication-allowed` | boolean | `true` | 是否允许重新认证。 |

### 10. 性能调优

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `topic-alias-cache-size` | int | `1000` | 主题别名缓存大小。 |
| `message-expiry-check-interval` | long | `1000` | 消息过期检查间隔（毫秒）。 |
| `max-pending-messages` | int | `1000` | 最大待处理消息数（流量控制）。 |

---

## 使用场景配置

### 场景 1：物联网设备（低带宽）

```yaml
z:
  chess:
    mqtt:
      v5:
        enabled: true
        topic-alias-enabled: true
        server-topic-alias-maximum: 16
        maximum-packet-size: 4096  # 小报文，节省带宽
        flow-control-enabled: true
        server-receive-maximum: 10  # 小流量控制
```

### 场景 2：高并发消息服务

```yaml
z:
  chess:
    mqtt:
      v5:
        enabled: true
        flow-control-enabled: true
        server-receive-maximum: 65535
        shared-subscription-enabled: true
        message-expiry-enabled: true
        max-session-expiry-interval: 3600  # 1小时
```

### 场景 3：仅 MQTT v3.1.1 兼容模式

```yaml
z:
  chess:
    mqtt:
      v5:
        enabled: false  # 完全禁用 v5
```

### 场景 4：增强安全认证

```yaml
z:
  chess:
    mqtt:
      v5:
        enabled: true
        enhanced-auth-enabled: true
        supported-auth-methods:
          - SCRAM-SHA-256
          - SCRAM-SHA-1
        default-auth-method: SCRAM-SHA-256
        auth-timeout-seconds: 120
        reauthentication-allowed: true
```

---

## 配置优先级

配置优先级从低到高：

1. 代码中的默认值
2. `application.properties` / `application.yml`
3. 环境变量（如 `Z_CHESS_MQTT_V5_ENABLED=false`）
4. 命令行参数（如 `--z.chess.mqtt.v5.enabled=false`）

---

## 运行时检查

在业务代码中检查配置：

```java
@Autowired
private MqttV5Config config;

public void handleConnection(X111_QttConnect connect) {
    // 检查 v5 是否启用
    if (!config.isEnabled()) {
        // 仅支持 v3.1.1
        return;
    }
    
    // 检查特定功能是否启用
    if (config.isTopicAliasEnabled()) {
        int maxAlias = config.getServerTopicAliasMaximum();
        // ...
    }
    
    // 检查认证方法是否支持
    if (config.isAuthMethodSupported("SCRAM-SHA-256")) {
        // 启用 SCRAM 认证
    }
}
```

---

## 注意事项

1. **禁用 v5 后**：所有 v5 特性（主题别名、流量控制、共享订阅等）将不可用，即使配置为 `true`。

2. **主题别名**：`client-topic-alias-maximum` 设为 `0` 表示不接受客户端使用主题别名，但服务端仍可使用。

3. **QoS 限制**：当 `maximum-qos` 设为 `1` 时，客户端无法发布/订阅 QoS 2 的消息。

4. **会话过期**：客户端请求的会话过期时间超过 `max-session-expiry-interval` 时，将被限制为最大值。

5. **报文大小**：如果客户端发送的报文超过 `maximum-packet-size`，服务端将断开连接。
