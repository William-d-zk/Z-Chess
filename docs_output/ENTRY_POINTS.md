# Z-Chess 入口点文档

## 应用启动入口

### Z-Arena (网关)
- **启动类**: `ApplicationArena`
- **路径**: `Z-Arena/src/main/java/com/isahl/chess/arena/start/ApplicationArena.java`
- **职责**: 网关应用入口，集成 RookCache 服务

### Z-Audience (测试/观察者)
- **启动类**: `ApplicationAudience`
- **路径**: `Z-Audience/src/main/java/com/isahl/chess/audience/start/ApplicationAudience.java`
- **职责**: 测试客户端、压力测试、API 集成测试

## 核心服务入口

### Z-King (事件处理)
**关键组件**:
- `TimeWheel` - 时间轮调度器
- `ZUID` - 64bit 复杂分段结构 ID 设计
- `ZResponse` / `ZProgress` - 异步处理返回包装
- `Disruptor` 事件处理框架

**入口方法**:
- 事件提交：通过 Disruptor 事件总线
- 定时任务：通过 TimeWheel 调度

### Z-Queen (IO 处理)
**核心服务**:
- `ServerCore` - 带 socket 应答与集群能力的服务核心
- `ClusterCore` - 集群间通信核心
- `AioWorker` / `AioSession` - AIO Socket 处理

**协议支持**:
- TCP/TLS/ZLS
- WebSocket + ZChat
- MQTT 3.1.1 / 5.0.0

### Z-Bishop (通讯协议)
**协议处理器**:
- `ModbusTcpProtocolHandler` / `ModbusRtuProtocolHandler`
- `MqttCodec` - MQTT 协议编解码
- `WsFrameFilter` - WebSocket 过滤器
- `ZCommandFilter` - ZChat 协议过滤器

**加密认证**:
- `SslProviderFactory` - SSL/TLS 配置
- `PasswordAuthProvider` / `ScramSha256AuthProvider` - 认证提供者

### Z-Knight (Raft 集群)
**集群管理**:
- `RaftMachine` - Raft 状态机
- `ClusterManagementServiceImpl` - 集群管理服务
- `DefaultSchedulerEngine` - 调度引擎

**调度 API**:
- `SchedulerController` - REST API 入口
- `DispatchScheduler` / `ClaimScheduler` - 调度器实现

## 业务服务入口

### Z-Pawn (边缘服务)
**设备接入**:
- `DeviceService` - 设备管理服务
- `MessageService` - 消息管理服务
- `NodeService` - 节点服务

**配置类**:
- `PawnIoConfig` - IO 配置
- `MixCoreConfig` - 核心配置
- `MqttConfig` - MQTT 配置

### Z-Player (Open API)
**REST Controller**:
- `DeviceController` - 设备管理 API
- `MessageController` - 消息管理 API
- `ClusterController` - 集群管理 API
- `ConsistencyController` - 一致性 API
- `EchoController` - 健康检查 API

**业务服务**:
- `BusinessScheduler` - 业务调度器
- `PushService` - 推送服务
- `MessageOpenService` - 消息开放服务

### Z-Rook (数据存储)
**存储层**:
- `RookJpaConfig` - JPA 配置
- `EhcacheConfig` - 缓存配置
- `AuditModel` - 审计模型

**图形处理**:
- `BFS` / `DFS` - 图遍历算法
- `GNode` / `GEdge` - 图节点和边

## 工具与注解

### Z-Board (注解处理)
**注解处理器**:
- `ZAnnotationProcessor` - 注解处理器
- `SerialProcessor` - 序列化处理器
- `FactoryTranslator` - 工厂翻译器
- `SwitchBuilderTranslator` - Switch 构建器翻译器

## 主要接口定义

### 协议接口
- `IProtocol` - 基础协议接口
- `InnerProtocol` - 内部协议
- `ProtocolLoader` - 协议加载器 SPI

### 存储接口
- `IDao` - DAO 操作抽象
- `IStorage` - 可存储对象抽象

### 调度接口
- `SchedulerService` - 调度服务接口
- `SchedulingRule` - 调度规则接口
- `Policy` - 策略接口 (重试/负载均衡)

### 集群接口
- `ClusterManagementService` - 集群管理服务
- `ConsistentText` - 一致性文本

## 配置入口

### 数据库配置
- **位置**: `Z-Rook/src/main/resource/db.properties`
- **默认**: PostgreSQL
- **连接**: `jdbc:postgresql://db-pg.isahl.com:5432/isahl.z-chess`

### DNS 配置
- **搜索域**: `isahl.com`
- **主机名**: 
  - DB: `db-pg`
  - Raft 集群：`raft10`, `raft11`, `raft12`

### 集群配置
- **Raft 节点**: 通过 DNS 解析自适应 IP
- **局域网 DNS**: `172.30.10.10 raft10; 172.30.10.11 raft11; 172.30.10.12 raft12`

## 测试入口

### 压力测试
- `PressureTestController` - 压力测试 API
- `StressClient` - 压力测试客户端
- `ApiTestController` - API 测试控制器

### 单元测试基类
- `BaseTest` - 测试基类
- `Mockery` - Mockito 工具
- `TestData` - 测试数据生成器
