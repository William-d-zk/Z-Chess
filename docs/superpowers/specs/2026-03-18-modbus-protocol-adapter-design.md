# Modbus 协议适配器设计文档

**日期**: 2026-03-18  
**版本**: 1.0.0  
**状态**: 待审核

---

## 1. 概述

### 1.1 目标

将 Modbus 协议完整集成到 Z-Chess 的协议处理链 SPI 中，提供高性能、高可用的 Modbus TCP/RTU协议支持，支持主站/从站双模式。

### 1.2 范围

#### 1.2.1 首期实现 (Phase 1)
- **协议支持**: Modbus TCP、Modbus RTU over TCP
- **功能码**: 常用功能码 (0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x0F, 0x10)
- **模式**: 主站 (Master) 和从站 (Slave) 双模式
- **性能**: 1000+ 并发连接，10000+ TPS
- **监控**: 完整的指标采集和监控

#### 1.2.2 后续迭代 (Phase 2)
- **功能码扩展**: 0x07, 0x08, 0x11, 0x14, 0x15, 0x16, 0x17, 0x18, 0x2B
- **RTU over Serial**: 串行端口支持 (需引入 jSerialComm 库)
- **Modbus UDP**: 实验性支持

### 1.3 安全设计

#### 1.3.1 TLS 加密传输
- 支持 Modbus over TLS (端口 802)
- 复用 Z-Bishop 的 SSLContext 管理机制
- 证书热重载支持

#### 1.3.2 访问控制
- IP 白名单机制
- 从站地址访问控制
- 功能码访问权限控制

#### 1.3.3 安全配置
```java
ModbusSecurityConfig config = ModbusSecurityConfig.builder()
    .enableTls(true)
    .ipWhitelist(List.of("192.168.1.0/24", "10.0.0.0/8"))
    .allowedUnitIds(Set.of(1, 2, 3))
    .allowedFunctions(Set.of(0x03, 0x04, 0x06, 0x10))
    .build();
```

### 1.4 术语定义

| 术语 | 定义 |
|------|------|
| Master | 主站，主动发起请求的客户端 |
| Slave | 从站，被动响应请求的服务端 |
| Unit ID | 从站地址，1 字节 (0-255)，0 为广播地址 |
| Transaction ID | 事务 ID，Modbus TCP 用于请求响应匹配，16 位 (0-65535) |
| Coil | 线圈，1 位布尔量输出 |
| Discrete Input | 离散输入，1 位布尔量输入 |
| Holding Register | 保持寄存器，16 位可读写寄存器 |
| Input Register | 输入寄存器，16 位只读寄存器 |
| RTU over TCP | RTU 帧封装在 TCP 中传输 (无 MBAP 头) |
| Modbus TCP | Modbus TCP 协议 (带 MBAP 头) |

---

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Modbus 应用层 (Application)                │
│  - ModbusMaster / ModbusSlave                               │
│  - TagManager / DataModel                                   │
│  - RetryPolicy / TimeoutConfig                              │
├─────────────────────────────────────────────────────────────┤
│                    Modbus 协议层 (Protocol)                   │
│  - ModbusProtocolHandler                                    │
│  - ModbusTcpCodec / ModbusRtuCodec / ModbusUdpCodec         │
│  - ModbusProtocolDetector                                   │
├─────────────────────────────────────────────────────────────┤
│                    传输层 (Transport)                         │
│  - Z-Queen AIO (TCP/TLS/UDP)                                │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖

```
Z-Bishop (协议层)
    └── Z-Queen (IO 层)
        └── Z-King (基础组件)
```

---

## 3. 核心组件设计

### 3.1 协议编解码器

#### 3.1.1 Modbus TCP 编解码器

**位置**: `com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec`

**MBAP 报文头结构** (7 字节):
```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Transaction │  Protocol   │    Length   │   Unit ID   │
│     ID      │     ID      │             │             │
│  (2 bytes)  │  (2 bytes)  │  (2 bytes)  │  (1 byte)   │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

**功能**:
- `encode(ModbusMessage message, int transactionId)` - 编码请求/响应
- `encodeException(...)` - 编码异常响应
- `decode(ByteBuf buffer)` - 解码请求/响应
- `getFrameLength(ByteBuf buffer)` - 获取帧长度

#### 3.1.2 Modbus RTU 编解码器 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec`

**RTU 帧结构**:
```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│  Address    │  Function   │    Data     │     CRC     │
│  (1 byte)   │   (1 byte)  │  (N bytes)  │  (2 bytes)  │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

**功能**:
- `encode(ModbusMessage message)` - 编码 RTU 帧
- `decode(ByteBuf buffer)` - 解码 RTU 帧
- `getCrc(ByteBuf buffer)` - 计算/验证 CRC16 (复用 CryptoUtil.crc16_modbus)
- `getFrameLength(ByteBuf buffer)` - 获取帧长度（基于功能码和数据长度）

**3.5 字符静默间隔**:
- **计算公式**: `silentIntervalMs = 3.5 * 11 * 1000 / baudRate`
  - 11 = 1 起始位 + 8 数据位 + 1 校验位 + 1 停止位
- **仅 Serial 模式需要**: TCP/UDP 传输不需要静默间隔
- **实现方案**: 使用 Z-King TimeWheel 实现定时控制
- **默认波特率**: 9600 (可配置)

#### 3.1.3 Modbus UDP 编解码器 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.udp.ModbusUdpCodec`

UDP 模式下使用与 TCP 相同的 MBAP 结构，但无连接状态管理。

#### 3.1.4 协议自动检测器 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.ModbusProtocolDetector`

**检测策略**:
- **TCP 检测**: 检查前 2 字节是否为 0x0000 (MBAP Protocol ID)
- **RTU 检测**: 非 TCP 流量默认为 RTU
- **可配置**: 支持强制指定协议类型

### 3.2 协议处理器 SPI

#### 3.2.1 ProtocolHandler 接口集成

**接口定义** (复用现有 `com.isahl.chess.bishop.protocol.spi.ProtocolHandler`):
```java
public interface ProtocolHandler {
    String getProtocolName();
    String getProtocolVersion();
    byte[] getProtocolSignature();
    Object decode(ByteBuf buffer);
    byte[] encode(Object message);
    int getPriority();
}
```

#### 3.2.2 ModbusTcpProtocolHandler

**位置**: `com.isahl.chess.bishop.protocol.modbus.spi.ModbusTcpProtocolHandler` (已有，需扩展)

**扩展内容**:
- 完整功能码支持 (Phase 1: 0x01-0x06, 0x0F, 0x10)
- 主站/从站模式切换
- 请求 - 响应匹配管理 (事务 ID 跟踪)
- 超时处理
- 指标采集
- **协议签名**: `{0x00, 0x00}` (MBAP Protocol ID，用于协议检测)
- **优先级**: 500 (可配置)

**事务 ID 回绕处理**:
- 事务 ID 范围：0-65535 (16 位)
- 达到最大值后回绕到 0
- 确保旧事务已超时 (> requestTimeoutMs) 后才重用
- 使用循环缓冲区跟踪未决事务

#### 3.2.3 ModbusRtuProtocolHandler (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.spi.ModbusRtuProtocolHandler`

**特性**:
- 3.5 字符静默间隔管理 (仅 Serial 模式)
- CRC 校验 (复用 CryptoUtil.crc16_modbus)
- 广播地址 (0) 支持
- **协议签名**: 无固定签名 (通过排除法检测：非 TCP 即为 RTU)
- **优先级**: 400 (低于 TCP)

#### 3.2.4 ModbusUdpProtocolHandler (新建，实验性)

**位置**: `com.isahl.chess.bishop.protocol.modbus.spi.ModbusUdpProtocolHandler`

**特性**:
- 无连接状态管理
- 支持广播
- **协议签名**: 同 TCP `{0x00, 0x00}`
- **优先级**: 300 (实验性)

### 3.3 功能码扩展

**位置**: `com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction`

#### 3.3.1 首期实现功能码 (Phase 1)

| 功能码 | 名称 | 描述 | 优先级 |
|--------|------|------|--------|
| 0x01 | Read Coils | 读线圈状态 | P0 |
| 0x02 | Read Discrete Inputs | 读离散输入 | P0 |
| 0x03 | Read Holding Registers | 读保持寄存器 | P0 |
| 0x04 | Read Input Registers | 读输入寄存器 | P0 |
| 0x05 | Write Single Coil | 写单个线圈 | P0 |
| 0x06 | Write Single Register | 写单个寄存器 | P0 |
| 0x0F | Write Multiple Coils | 写多个线圈 | P1 |
| 0x10 | Write Multiple Registers | 写多个寄存器 | P1 |

#### 3.3.2 后续迭代功能码 (Phase 2)

| 功能码 | 名称 | 描述 | 复杂度 |
|--------|------|------|--------|
| 0x07 | Read Exception Status | 读异常状态 | 低 |
| 0x08 | Diagnostics | 诊断 (20+ 种子功能) | 高 |
| 0x0B | Get Comm Event Counter | 获取通信事件计数器 | 中 |
| 0x0C | Get Comm Event Log | 获取通信事件日志 | 中 |
| 0x11 | Report Server ID | 报告服务器 ID | 低 |
| 0x14 | Read File Record | 读文件记录 | 高 |
| 0x15 | Write File Record | 写文件记录 | 高 |
| 0x16 | Mask Write Register | 掩码写寄存器 | 中 |
| 0x17 | Read/Write Multiple Registers | 读/写多个寄存器 | 中 |
| 0x18 | FIFO Queue Command | FIFO 队列命令 | 高 |
| 0x2B | Encapsulated Interface Transport | 封装接口传输 | 中 |

### 3.4 异常码扩展

**位置**: `com.isahl.chess.bishop.protocol.modbus.exception.ModbusExceptionCode`

**完整异常码列表**:

| 异常码 | 名称 | 描述 |
|--------|------|------|
| 0x01 | Illegal Function | 非法功能 |
| 0x02 | Illegal Data Address | 非法数据地址 |
| 0x03 | Illegal Data Value | 非法数据值 |
| 0x04 | Server Device Failure | 从站设备故障 |
| 0x05 | Acknowledge | 确认 |
| 0x06 | Server Device Busy | 从属设备忙 |
| 0x08 | Memory Parity Error | 存储奇偶校验错误 |
| 0x0A | Gateway Path Unavailable | 网关不可用 |
| 0x0B | Gateway Target Device Failed | 网关目标设备响应失败 |

### 3.5 主站实现 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.master.ModbusMaster`

**核心功能**:
- 连接管理 (TCP/UDP/Serial)
- 请求发送与响应接收
- 事务 ID 管理
- 超时控制
- 重试策略
- 从站地址管理

**API 示例**:
```java
ModbusMaster master = ModbusMaster.builder()
    .host("192.168.1.100")
    .port(502)
    .protocol(Protocol.TCP)
    .timeout(3000)
    .retryPolicy(new ExponentialBackoffRetry(3, 1000))
    .build();

// 读保持寄存器
int[] registers = master.readHoldingRegisters(1, 0, 10);

// 写单个寄存器
master.writeSingleRegister(1, 100, 1234);

// 写多个寄存器
master.writeMultipleRegisters(1, 0, new int[]{1, 2, 3, 4});

// 读线圈
boolean[] coils = master.readCoils(1, 0, 16);

// 关闭连接
master.close();
```

### 3.6 从站实现 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.slave.ModbusSlave`

**核心功能**:
- 服务端监听 (TCP/UDP/Serial)
- 请求处理与响应发送
- 数据模型管理 (线圈/寄存器)
- 异常处理
- 并发控制

**API 示例**:
```java
ModbusSlave slave = ModbusSlave.builder()
    .port(502)
    .protocol(Protocol.TCP)
    .unitId(1)
    .build();

// 注册数据模型
slave.registerModel(new DataModel(1, 100, 200, 300));

// 注册功能码处理器
slave.registerHandler(ModbusFunction.READ_HOLDING_REGISTERS, 
    (request) -> {
        // 自定义处理逻辑
        return response;
    });

// 启动服务
slave.start();

// 停止服务
slave.stop();
```

### 3.7 标签系统 (新建)

#### 3.7.1 标签定义

**位置**: `com.isahl.chess.bishop.protocol.modbus.tag.Tag`

**字段**:
```java
public class Tag {
    private String name;           // 标签名称
    private String description;    // 描述
    private int unitId;            // 从站地址
    private int address;           // 寄存器/线圈地址
    private DataType dataType;     // 数据类型
    private DataArea dataArea;     // 数据区 (线圈/离散/保持寄存器/输入寄存器)
    private double scale;          // 缩放比例
    private double offset;         // 偏移量
    private String unit;           // 单位
    private int precision;         // 精度 (小数位数)
    private AccessMode accessMode; // 访问模式 (只读/只写/读写)
    private String defaultValue;   // 默认值
    private Map<String, String> metadata; // 元数据
}
```

**数据类型**:
```java
public enum DataType {
    BOOLEAN,        // 1 位 (Coil/Discrete Input)
    INT8,           // 8 位有符号 (占用 1 寄存器高 8 位，低 8 位填充 0)
    UINT8,          // 8 位无符号 (占用 1 寄存器高 8 位，低 8 位填充 0)
    INT16,          // 16 位有符号 (1 寄存器)
    UINT16,         // 16 位无符号 (1 寄存器)
    INT32,          // 32 位有符号 (2 寄存器，字节序可配置)
    UINT32,         // 32 位无符号 (2 寄存器，字节序可配置)
    INT64,          // 64 位有符号 (4 寄存器，字节序可配置)
    FLOAT32,        // 32 位浮点 IEEE754 (2 寄存器，字节序可配置)
    FLOAT64,        // 64 位浮点 IEEE754 (4 寄存器，字节序可配置)
    STRING          // 字符串 (N 寄存器，ASCII/UTF-16 可配置)
}
```

**字节序配置**:
```java
public enum ByteOrder {
    ABCD,  // Big Endian (默认): 寄存器 0[高 16 位], 寄存器 1[低 16 位]
    CDAB,  // Little Endian: 寄存器 0[低 16 位], 寄存器 1[高 16 位]
    BADC,  // Byte Swap: 寄存器 0[字节交换高 16 位], 寄存器 1[字节交换低 16 位]
    DCBA   // Full Swap: 完全字节交换
}
```

**数据类型与寄存器映射**:
| 数据类型 | 寄存器数 | 字节序 | 说明 |
|----------|----------|--------|------|
| BOOLEAN | 0 (Coil) | N/A | 使用 Coil 地址空间 |
| INT8/UINT8 | 1 | N/A | 高 8 位有效，低 8 位填充 0 |
| INT16/UINT16 | 1 | N/A | 直接映射 |
| INT32/UINT32/FLOAT32 | 2 | 可配置 | 高位寄存器在前 |
| INT64/FLOAT64 | 4 | 可配置 | 高位寄存器在前 |
| STRING | N | UTF-16 | 每寄存器 1 字符 (UTF-16BE) |

**数据区**:
```java
public enum DataArea {
    COIL,              // 线圈 (0xxxx)
    DISCRETE_INPUT,    // 离散输入 (1xxxx)
    HOLDING_REGISTER,  // 保持寄存器 (4xxxx)
    INPUT_REGISTER     // 输入寄存器 (3xxxx)
}
```

#### 3.7.2 标签管理器

**位置**: `com.isahl.chess.bishop.protocol.modbus.tag.TagManager`

**功能**:
- 标签配置加载 (JSON/YAML/数据库)
- 标签注册与注销
- 标签地址映射
- 标签值缓存
- 标签分组管理
- **配置热重载**: 监听配置文件变化，动态更新标签
- **配置验证**: 地址范围检查、数据类型兼容性检查
- **冲突处理**: 重复标签名检测、地址冲突检测

**配置示例** (YAML):
```yaml
tags:
  - name: "Temperature"
    description: "环境温度"
    unitId: 1
    address: 0
    dataType: FLOAT32
    dataArea: HOLDING_REGISTER
    byteOrder: ABCD
    scale: 0.1
    offset: -40.0
    unit: "°C"
    precision: 2
    accessMode: READ_ONLY
    
  - name: "Valve_Control"
    description: "阀门控制"
    unitId: 1
    address: 100
    dataType: BOOLEAN
    dataArea: COIL
    accessMode: READ_WRITE
```

**配置示例** (JSON):
```json
{
  "tags": [
    {
      "name": "Temperature",
      "description": "环境温度",
      "unitId": 1,
      "address": 0,
      "dataType": "FLOAT32",
      "dataArea": "HOLDING_REGISTER",
      "scale": 0.1,
      "offset": -40.0,
      "unit": "°C",
      "precision": 2,
      "accessMode": "READ_ONLY"
    },
    {
      "name": "Valve_Control",
      "description": "阀门控制",
      "unitId": 1,
      "address": 100,
      "dataType": "BOOLEAN",
      "dataArea": "COIL",
      "accessMode": "READ_WRITE"
    }
  ]
}
```

#### 3.7.3 数据类型转换器

**位置**: `com.isahl.chess.bishop.protocol.modbus.tag.DataTypeConverter`

**功能**:
- 寄存器数组 ↔ Java 类型转换
- 字节序处理 (Big-Endian / Little-Endian / ABCD / CDAB 等)
- 缩放和偏移计算
- 字符串编解码

### 3.8 重试策略 (新建)

#### 3.8.1 重试策略接口

**位置**: `com.isahl.chess.bishop.protocol.modbus.retry.RetryPolicy`

```java
public interface RetryPolicy {
    boolean shouldRetry(int attempt, Throwable exception);
    long getDelay(int attempt);
}
```

#### 3.8.2 指数退避实现

**位置**: `com.isahl.chess.bishop.protocol.modbus.retry.ExponentialBackoffRetry`

**参数**:
- `maxRetries` - 最大重试次数
- `initialDelayMs` - 初始延迟 (毫秒)
- `maxDelayMs` - 最大延迟 (毫秒)
- `multiplier` - 延迟乘数 (默认 2.0)

**计算**: `delay = min(initialDelay * (multiplier ^ attempt), maxDelay)`

### 3.9 超时配置 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.TimeoutConfig`

**参数**:
- `connectionTimeoutMs` - 连接超时
- `requestTimeoutMs` - 请求响应超时
- `betweenRequestsDelayMs` - 请求间隔延迟
- `recoveryDelayMs` - 错误后恢复延迟

### 3.10 监控指标 (新建)

**位置**: `com.isahl.chess.bishop.protocol.modbus.metrics.ModbusMetrics`

**指标分类**:

#### 连接指标
- `modbus_connections_active` - 活跃连接数
- `modbus_connections_total` - 总连接数
- `modbus_connection_errors` - 连接错误数

#### 请求指标
- `modbus_requests_total` - 总请求数 (按功能码分组)
- `modbus_requests_success` - 成功请求数
- `modbus_requests_failed` - 失败请求数
- `modbus_request_duration_seconds` - 请求耗时 (直方图)

#### 从站指标
- `modbus_slave_responses` - 从站响应数 (按从站地址分组)
- `modbus_slave_timeouts` - 从站超时数
- `modbus_slave_errors` - 从站错误数

#### 协议指标
- `modbus_protocol_errors` - 协议解析错误数
- `modbus_crc_errors` - CRC 校验错误数 (RTU)
- `modbus_exception_responses` - 异常响应数 (按异常码分组)

#### 重试指标
- `modbus_retries_total` - 总重试次数
- `modbus_retries_success` - 重试成功次数

**集成 Prometheus**:
```java
ModbusMetrics metrics = new ModbusMetrics(registry);
metrics.recordRequest(functionCode, duration, success);
metrics.recordConnection();
metrics.recordError(errorType);
```

---

## 4. 数据流设计

### 4.1 主站请求流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Application│────▶│ ModbusMaster│────▶│  Protocol   │
│   (Caller)  │     │             │     │   Handler   │
└─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           │                   ▼
                           │          ┌─────────────┐
                           │          │   Codec     │
                           │          │  (Encode)   │
                           │          └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │ RetryPolicy │     │  Transport  │
                    │             │     │  (TCP/UDP)  │
                    └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │   Device    │
                                        │   (Slave)   │
                                        └─────────────┘
```

### 4.2 从站响应流程

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Device    │────▶│  Transport  │────▶│   Codec     │
│   (Master)  │     │  (TCP/UDP)  │     │  (Decode)   │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                               ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Response   │◀────│  Protocol   │◀────│ DataModel   │
│   (Encode)  │     │   Handler   │     │  (Lookup)   │
└─────────────┘     └─────────────┘     └─────────────┘
       │
       ▼
┌─────────────┐
│   Device    │
│   (Master)  │
└─────────────┘
```

---

## 5. 错误处理

### 5.1 异常分类

| 异常类型 | 处理策略 | 异常码 |
|----------|----------|--------|
| 连接异常 | 触发重连，记录指标 | N/A |
| 超时异常 | 触发重试，记录指标 | 0x04 (Server Device Failure) |
| 协议异常 | 返回异常响应，记录指标 | 0x01 (Illegal Function) |
| CRC 错误 | 丢弃帧，记录指标 | N/A (RTU 专用) |
| 非法地址 | 返回异常响应 | 0x02 (Illegal Data Address) |
| 非法数据值 | 返回异常响应 | 0x03 (Illegal Data Value) |
| 从站设备故障 | 返回异常响应 | 0x04 (Server Device Failure) |
| 从站忙 | 返回异常响应，触发重试 | 0x06 (Server Device Busy) |

### 5.2 广播地址处理

**Unit ID 0 特殊处理**:
- 广播请求不返回响应
- 主站发送广播后不等待响应
- 从站处理广播请求但不发送响应
- 广播功能码限制：仅支持写操作 (0x05, 0x06, 0x0F, 0x10)

### 5.3 异常响应格式

```java
// 异常响应 = 功能码 | 0x80 + 异常码
byte exceptionResponse = (byte) (functionCode | 0x80);
byte exceptionCode = 0x02; // Illegal Data Address
```

**异常响应处理流程**:
1. 检测功能码最高位是否为 1 (`functionCode & 0x80 != 0`)
2. 解析异常码 (PDU 第二个字节)
3. 映射到 `ModbusExceptionCode` 枚举
4. 抛出 `ModbusException` 包含功能码和异常码
5. 记录指标 (按异常码分组)

---

## 6. 性能优化

### 6.1 连接池

- TCP 长连接复用
- 连接池大小可配置
- 空闲连接检测与回收

### 6.2 零拷贝

- 使用 `ByteBuf` 直接缓冲区
- 减少内存拷贝

### 6.3 批量操作

- 支持多寄存器批量读写
- 减少请求次数

### 6.4 异步处理

- 基于 Z-Queen AIO 异步 IO
- 非阻塞请求响应

### 6.5 连接池配置

```java
ModbusConnectionPoolConfig poolConfig = ModbusConnectionPoolConfig.builder()
    .maxConnections(100)        // 最大连接数
    .minIdle(10)                // 最小空闲连接
    .maxIdle(50)                // 最大空闲连接
    .idleTimeout(300000)        // 空闲超时 (5 分钟)
    .connectionTimeout(5000)    // 连接超时 (5 秒)
    .testOnBorrow(true)         // 借出时检测连接
    .build();
```

---

## 7. 测试策略

### 7.1 单元测试

- 编解码器测试
- 功能码测试
- 异常码测试
- 标签系统测试
- 重试策略测试

### 7.2 集成测试

- 主站 - 从站通信测试
- 协议处理链集成测试
- 超时重试测试

### 7.3 性能测试

**测试环境**:
- CPU: 8 核 16 线程
- 内存：16GB
- JVM: OpenJDK 17
- 网络：千兆以太网

**测试指标**:
- 并发连接数：1000+
- TPS: 10000+
- 平均响应时间：< 10ms
- P99 响应时间：< 50ms
- 长时间稳定性：72 小时无内存泄漏

**基准测试工具**:
- JMH (Java Microbenchmark Harness)
- 自定义压力测试工具

### 7.4 模拟从站

使用 `ModbusSlave` 构建模拟从站用于测试。

---

## 8. 文件结构

```
Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/modbus/
├── tcp/
│   └── ModbusTcpCodec.java (已有，扩展)
├── rtu/
│   ├── ModbusRtuCodec.java (新建)
│   └── ModbusRtuProtocol.java (已有)
├── udp/
│   └── ModbusUdpCodec.java (新建)
├── spi/
│   ├── ModbusTcpProtocolHandler.java (已有，扩展)
│   ├── ModbusRtuProtocolHandler.java (新建)
│   └── ModbusUdpProtocolHandler.java (新建)
├── function/
│   └── ModbusFunction.java (已有，扩展)
├── exception/
│   ├── ModbusExceptionCode.java (新建)
│   └── ModbusException.java (新建)
├── master/
│   ├── ModbusMaster.java (新建)
│   ├── ModbusRequest.java (新建)
│   └── ModbusResponse.java (新建)
├── slave/
│   ├── ModbusSlave.java (新建)
│   ├── ModbusSlaveConfig.java (新建)
│   └── FunctionHandler.java (新建)
├── tag/
│   ├── Tag.java (新建)
│   ├── TagManager.java (新建)
│   ├── TagConfig.java (新建)
│   ├── DataType.java (新建)
│   ├── DataArea.java (新建)
│   └── DataTypeConverter.java (新建)
├── retry/
│   ├── RetryPolicy.java (新建)
│   └── ExponentialBackoffRetry.java (新建)
├── metrics/
│   ├── ModbusMetrics.java (新建)
│   └── ModbusMetricsCollector.java (新建)
├── ModbusProtocolDetector.java (新建)
└── ModbusConstants.java (新建)
```

**测试文件** (符合 TESTING.md 规范，与被测类相同包路径):
```
Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/modbus/
├── tcp/
│   └── ModbusTcpCodecTest.java (已有，扩展)
├── rtu/
│   └── ModbusRtuCodecTest.java (新建)
├── spi/
│   └── ModbusProtocolHandlerTest.java (新建)
├── master/
│   └── ModbusMasterTest.java (新建)
├── slave/
│   └── ModbusSlaveTest.java (新建)
├── tag/
│   └── TagManagerTest.java (新建)
├── retry/
│   └── ExponentialBackoffRetryTest.java (新建)
└── integration/
    └── ModbusIntegrationTest.java (新建)
```

**集成测试** (Z-Audience 模块):
```
Z-Audience/src/test/java/com/isahl/chess/audience/bishop/protocol/modbus/
└── ModbusProtocolIntegrationTest.java (新建)
```

---

## 9. 验收标准

### 9.1 Phase 1 验收标准

- [ ] Phase 1 功能码 (0x01-0x06, 0x0F, 0x10) 实现并通过测试
- [ ] Modbus TCP 协议支持
- [ ] Modbus RTU over TCP 支持
- [ ] 主站/从站双模式正常工作
- [ ] 标签系统支持常用数据类型 (BOOLEAN, INT16, UINT16, INT32, UINT32, FLOAT32)
- [ ] 重试策略和超时配置生效
- [ ] 监控指标正确采集
- [ ] 安全功能：TLS 加密、IP 白名单
- [ ] 性能达到 1000+ 连接，10000+ TPS
- [ ] 单元测试覆盖率：行覆盖率≥30%，分支覆盖率≥20% (符合项目规范)
- [ ] 集成测试通过
- [ ] 文档完整

### 9.2 Phase 2 验收标准

- [ ] Phase 2 功能码实现并通过测试
- [ ] RTU over Serial 支持 (如实现)
- [ ] Modbus UDP 实验性支持 (如实现)
- [ ] 标签系统支持所有数据类型
- [ ] 文档更新

---

## 10. 参考文档

- [Modbus 规范](https://modbus.org/specs.php)
- [Modbus TCP 规范](https://modbus.org/docs/Modbus-TCP-IP.pdf)
- [Modbus RTU 规范](https://modbus.org/docs/Modbus-RTU-IA.pdf)
