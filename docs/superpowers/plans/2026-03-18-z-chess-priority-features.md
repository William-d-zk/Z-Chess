# Z-Chess 优先功能实施计划

**日期**: 2026-03-18  
**版本**: 1.0  
**状态**: In Progress

## 实施优先级

根据用户需求确认：
1. **UDP 支持** (Z-Queen)
2. **MQTT 5.0 完整性** (Z-Bishop)
3. **管理控制台** (新模块)
4. **协议处理链 SPI** (Z-Bishop)

---

## Chunk 1: UDP 支持 (Z-Queen)

### Task 1.1: UDP 通道接口定义

**Files:**
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/UdpChannel.java`
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/UdpPacket.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/queen/io/udp/UdpChannelTest.java`

**Steps:**
- [ ] 定义 UdpChannel 接口（非阻塞 UDP 通信）
- [ ] 定义 UdpPacket 数据类
- [ ] 编写单元测试
- [ ] 运行测试验证

### Task 1.2: UDP 服务器实现

**Files:**
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/UdpServer.java`
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/BaseUdpServer.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/queen/io/udp/UdpServerTest.java`

**Steps:**
- [ ] 实现 UDP 服务器核心
- [ ] 支持多播/单播
- [ ] 编写集成测试
- [ ] 运行测试验证

### Task 1.3: UDP 客户端实现

**Files:**
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/UdpClient.java`
- Create: `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/udp/BaseUdpClient.java`

**Steps:**
- [ ] 实现 UDP 客户端
- [ ] 支持连接管理
- [ ] 编写测试
- [ ] 提交代码

---

## Chunk 2: MQTT 5.0 完整性 (Z-Bishop)

### Task 2.1: QoS 2 消息处理

**Files:**
- Modify: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/mqtt/` (扩展)
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/Qos2Handler.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/bishop/mqtt/v5/Qos2HandlerTest.java`

**Steps:**
- [ ] 实现 PUBREC 处理
- [ ] 实现 PUBREL 处理
- [ ] 实现 PUBCOMP 处理
- [ ] 编写测试验证 QoS 2 流程

### Task 2.2: 保留消息支持

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/RetainedMessageStore.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/bishop/mqtt/v5/RetainedMessageStoreTest.java`

**Steps:**
- [ ] 实现保留消息存储
- [ ] 支持主题匹配推送
- [ ] 编写测试

### Task 2.3: 遗嘱消息支持

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/mqtt/v5/LastWillHandler.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/bishop/mqtt/v5/LastWillHandlerTest.java`

**Steps:**
- [ ] 实现遗嘱消息注册
- [ ] 实现客户端断开时发布
- [ ] 编写测试

---

## Chunk 3: 管理控制台 (新模块 Z-Console)

### Task 3.1: 创建 Z-Console 模块

**Files:**
- Create: `Z-Console/pom.xml`
- Create: `Z-Console/src/main/java/...` (基础结构)

**Steps:**
- [ ] 创建模块目录结构
- [ ] 配置 pom.xml (Spring Boot + Vue 集成)
- [ ] 添加父 pom 依赖

### Task 3.2: REST API 后端

**Files:**
- Create: `Z-Console/src/main/java/com/isahl/chess/console/api/ClusterController.java`
- Create: `Z-Console/src/main/java/com/isahl/chess/console/api/MetricsController.java`
- Create: `Z-Console/src/main/java/com/isahl/chess/console/service/ClusterService.java`

**Steps:**
- [ ] 实现集群状态 API
- [ ] 实现监控指标 API
- [ ] 实现节点管理 API

### Task 3.3: Web UI 前端

**Files:**
- Create: `Z-Console/src/main/resources/static/` (Vue 应用)

**Steps:**
- [ ] 创建基础 HTML/JS 框架
- [ ] 实现集群监控页面
- [ ] 实现节点管理页面

---

## Chunk 4: 协议处理链 SPI (Z-Bishop)

### Task 4.1: 协议 SPI 接口定义

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/spi/ProtocolHandler.java`
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/spi/ProtocolProvider.java`

**Steps:**
- [ ] 定义协议处理器接口
- [ ] 定义协议提供者 SPI
- [ ] 编写 SPI 文档

### Task 4.2: 协议加载器实现

**Files:**
- Create: `Z-Bishop/src/main/java/com/isahl/chess/bishop/protocol/spi/ProtocolLoader.java`
- Test: `Z-Audience/src/test/java/com/isahl/chess/audience/bishop/protocol/spi/ProtocolLoaderTest.java`

**Steps:**
- [ ] 实现 SPI 加载机制
- [ ] 支持自定义协议发现
- [ ] 编写测试

### Task 4.3: 示例协议实现

**Files:**
- Create: `Z-Bishop/src/test/java/com/isahl/chess/bishop/protocol/spi/example/ExampleProtocolHandler.java`

**Steps:**
- [ ] 创建示例协议处理器
- [ ] 编写使用文档
- [ ] 完整测试验证

---

## 执行检查点

- [ ] Chunk 1 (UDP) 完成
- [ ] Chunk 2 (MQTT 5.0) 完成
- [ ] Chunk 3 (管理控制台) 完成
- [ ] Chunk 4 (协议 SPI) 完成

## 验收标准

| 功能 | 验收标准 |
|------|----------|
| UDP 支持 | 可收发 UDP 数据包，通过测试 |
| QoS 2 | 完整实现 PUBREC/PUBREL/PUBCOMP 流程 |
| 保留消息 | 支持发布/订阅保留消息 |
| 遗嘱消息 | 客户端断开自动发布遗嘱 |
| 管理控制台 | 可访问 Web UI 查看集群状态 |
| 协议 SPI | 可加载自定义协议处理器 |
