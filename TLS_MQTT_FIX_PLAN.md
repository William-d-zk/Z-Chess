# Z-Chess TLS MQTT 压力测试修复计划

## 📋 问题摘要

| 问题 | 严重程度 | 状态 |
|------|----------|------|
| TLS 握手超时 | 🔴 高 | 待修复 |
| SSL 配置加载异常 | 🟡 中 | 待验证 |
| 证书路径问题 | 🟡 中 | 待验证 |
| 日志不足 | 🟢 低 | 待优化 |

---

## 🔍 根因分析

### 问题 1: TLS 握手超时

**现象：**
- TCP 连接成功 (端口 1885/8883 可连接)
- TLS 握手阶段卡住 (openssl s_client 超时)
- 服务日志无 TLS 握手相关错误

**可能原因：**
1. `SocketConfig.init()` 中 SSL 上下文初始化失败但未抛出异常
2. 证书从类路径加载失败 (路径或密码错误)
3. `SslSocketConfig` 和 `SocketConfig` 重复配置导致冲突
4. SSL 引擎模式配置不正确 (客户端模式 vs 服务端模式)

**诊断方法：**
```bash
# 1. 检查证书是否正确打包到 jar
jar tf Z-Pawn/target/endpoint.z-pawn-*.jar | grep cert

# 2. 检查容器内日志
docker logs raft00 | grep -i ssl

# 3. 使用 OpenSSL 详细调试
echo | openssl s_client -connect localhost:8883 -CAfile cert/ca-cert.pem -debug
```

---

## 🛠️ 修复计划

### 阶段 1: 诊断与日志增强 (预计 2-4 小时)

#### 任务 1.1: 增强 SocketConfig 日志
**文件：** `Z-Knight/src/main/java/com/isahl/chess/knight/cluster/config/SocketConfig.java`

**修改内容：**
```java
@Override
public void init() {
    _Logger.info("SocketConfig.init() started");
    try {
        if (keyStorePath != null) {
            _Logger.info("Loading keystore from: %s", keyStorePath);
            KeyManager[] kms = getKeyManagers();
            _Logger.info("Loaded %d key managers", kms != null ? kms.length : 0);
        }
        if (trustKeyStorePath != null) {
            _Logger.info("Loading truststore from: %s", trustKeyStorePath);
            TrustManager[] tms = getTrustManagers();
            _Logger.info("Loaded %d trust managers", tms != null ? tms.length : 0);
        }
        
        SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
        sslCtx.init(getKeyManagers(), getTrustManagers(), null);
        _Logger.info("SSLContext initialized successfully");
        
        SSLSession sslSession = sslCtx.createSSLEngine().getSession();
        sslPacketBufferSize = sslSession.getPacketBufferSize();
        sslAppBufferSize = sslSession.getApplicationBufferSize();
        _Logger.info("SSL buffer sizes - packet: %d, app: %d", sslPacketBufferSize, sslAppBufferSize);
    } catch (Exception e) {
        _Logger.error("SSL initialization failed: %s", e.getMessage(), e);
        throw new ZException(e, "ssl static init failed");
    }
}
```

#### 任务 1.2: 验证证书加载
**文件：** `Z-Knight/src/main/java/com/isahl/chess/knight/cluster/config/SocketConfig.java`

**修改内容：**
```java
private KeyStore loadKeyStore(String path, String password) 
        throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
    _Logger.debug("Attempting to load keystore: %s", path);
    
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    InputStream is = getClass().getClassLoader().getResourceAsStream(path);
    
    if (is == null) {
        _Logger.error("Keystore not found in classpath: %s", path);
        throw new IOException("Keystore not found: " + path);
    }
    
    try {
        keyStore.load(is, password.toCharArray());
        _Logger.info("Successfully loaded keystore: %s", path);
        return keyStore;
    } finally {
        is.close();
    }
}
```

#### 任务 1.3: 添加 SSL 握手监听器
**文件：** `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/socket/BaseAioServer.java`

**修改内容：**
在 SSL 握手阶段添加日志记录，追踪握手状态。

---

### 阶段 2: 配置修复 (预计 2-3 小时)

#### 任务 2.1: 统一 SSL 配置
**问题：** `SslSocketConfig` 和 `SocketConfig` 同时存在，可能导致配置冲突

**解决方案：**
1. 优先使用 `SslSocketConfig` (新实现)
2. 移除 `SocketConfig` 的 `@Configuration` 注解，改为普通类
3. 确保 `PawnIoConfig` 使用 `SslSocketConfig.getProvider()`

**文件修改：**
- `Z-Knight/src/main/java/com/isahl/chess/knight/cluster/config/SocketConfig.java`
  - 移除 `@Component` 或 `@Configuration` 注解
  
- `Z-Knight/src/main/java/com/isahl/chess/knight/cluster/config/SslSocketConfig.java`
  - 确保所有 SSL 配置从此类加载

#### 任务 2.2: 修复证书路径
**文件：** `Z-Pawn/src/main/resources/pawn.io.properties`

**当前配置：**
```properties
z.chess.pawn.io.provider.key_store_path=cert/server.p12
z.chess.pawn.io.provider.trust_key_store_path=cert/trust.p12
```

**验证：** 确保证cert目录在Z-Pawn的src/main/resources/下，并且包含：
- server.p12
- trust.p12

#### 任务 2.3: 配置优先级调整
**文件：** `Z-Arena/src/main/resources/application-online.yaml`

**添加环境变量覆盖支持：**
```yaml
z:
  chess:
    pawn:
      io:
        provider:
          ssl:
            enabled: ${SSL_ENABLED:true}
            key-store-path: ${SSL_KEYSTORE_PATH:cert/server.p12}
            key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
            trust-store-path: ${SSL_TRUSTSTORE_PATH:cert/trust.p12}
            trust-store-password: ${SSL_TRUSTSTORE_PASSWORD:changeit}
```

---

### 阶段 3: SSL 引擎修复 (预计 4-6 小时)

#### 任务 3.1: 修复 SSL 引擎模式
**问题：** SSL 引擎可能被错误配置为客户端模式

**文件：** `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/socket/BaseAioServer.java`

**检查点：**
- 确认 `SSLEngine.setUseClientMode(false)` 在服务端正确设置
- 确认 `SSLEngine.setNeedClientAuth(false)` 按需设置

#### 任务 3.2: 添加 SSL 握手超时处理
**文件：** `Z-Queen/src/main/java/com/isahl/chess/queen/io/core/net/socket/BaseAioServer.java`

**修改内容：**
```java
// 在 SSL 握手阶段添加超时处理
sslEngine.beginHandshake();
// 添加握手超时逻辑
```

#### 任务 3.3: 测试证书链完整性
使用 OpenSSL 验证证书链：
```bash
# 验证服务端证书
openssl pkcs12 -in cert/server.p12 -passin pass:changeit -info -noout

# 验证信任库
keytool -list -v -keystore cert/trust.p12 -storepass changeit -storetype PKCS12
```

---

### 阶段 4: 测试验证 (预计 3-4 小时)

#### 任务 4.1: 单元测试
**创建：** `Z-Knight/src/test/java/com/isahl/chess/knight/cluster/config/SslSocketConfigTest.java`

**测试用例：**
1. 测试证书加载
2. 测试 SSL 上下文创建
3. 测试配置属性绑定

#### 任务 4.2: 集成测试
**脚本：** `scripts/test/tls/verify-mqtt-tls.sh`

**测试步骤：**
1. 启动 TLS 服务
2. 使用 OpenSSL 测试 TLS 握手
3. 使用 mosquitto 客户端测试 MQTT over TLS
4. 使用 Java 客户端测试

#### 任务 4.3: 压力测试
**使用 Z-Audience 进行：**
```bash
./scripts/test/docker/test-tls-mqtt.sh start
```

---

## 📅 实施时间表

| 阶段 | 任务 | 预计时间 | 负责人 |
|------|------|----------|--------|
| 阶段 1 | 日志增强 | 2-4 小时 | 开发团队 |
| 阶段 1 | 证书加载验证 | 1-2 小时 | 开发团队 |
| 阶段 2 | 配置统一 | 2-3 小时 | 开发团队 |
| 阶段 3 | SSL 引擎修复 | 4-6 小时 | 开发团队 |
| 阶段 4 | 测试验证 | 3-4 小时 | 测试团队 |
| **总计** | | **12-19 小时** | |

---

## ✅ 验证清单

### 修复前检查
- [ ] 证书文件存在且未损坏
- [ ] 密码正确
- [ ] 证书未过期

### 修复后验证
- [ ] 服务启动无 SSL 相关错误
- [ ] 日志显示 SSL 上下文初始化成功
- [ ] OpenSSL 可以完成 TLS 握手
- [ ] mosquitto_pub/sub 可以连接
- [ ] Java 客户端可以连接
- [ ] Z-Audience 压力测试通过

---

## 🚨 风险与回滚

### 风险
1. **兼容性问题：** 修改 SSL 配置可能影响现有明文连接
2. **性能影响：** SSL 加密会增加 CPU 负载
3. **证书管理：** 测试证书过期后需要重新生成

### 回滚方案
1. 保留原始 `SocketConfig.java` 的备份
2. 提供快速禁用 SSL 的配置开关
3. 保留明文端口 1883 作为 fallback

---

## 📚 相关文档

- [TLS_SETUP_GUIDE.md](./TLS_SETUP_GUIDE.md)
- [scripts/test/TLS_MQTT_TEST_GUIDE.md](./scripts/test/TLS_MQTT_TEST_GUIDE.md)
- [OPENSSL_TLS_TEST.md](./OPENSSL_TLS_TEST.md)

---

## 📝 变更记录

| 日期 | 版本 | 修改内容 | 作者 |
|------|------|----------|------|
| 2026-03-12 | 1.0 | 初始版本 | AI Assistant |

