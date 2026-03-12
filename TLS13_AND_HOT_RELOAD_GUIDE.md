# Z-Chess TLS 1.3 与证书热更新配置指南

## 概述

本文档介绍 Z-Chess 的 TLS 1.3 支持和证书热更新功能。这些功能提供了：

- **TLS 1.3**: 更快的握手速度、更强的安全性
- **证书热更新**: 无需重启服务即可更新证书，实现零停机维护

---

## TLS 1.3 特性

### 相比 TLS 1.2 的优势

| 特性 | TLS 1.2 | TLS 1.3 |
|------|---------|---------|
| 握手延迟 | 2-RTT | 1-RTT (0-RTT 可选) |
| 加密套件 | 复杂多样 | 简化优化 |
| 安全性 | 易受攻击的算法可选 | 移除不安全算法 |
| 性能 | 标准 | 提升 30-50% |

### 支持的加密套件

- `TLS_AES_256_GCM_SHA384`
- `TLS_CHACHA20_POLY1305_SHA256`
- `TLS_AES_128_GCM_SHA256`

---

## 配置方法

### 1. 启用 TLS 1.3

```yaml
z:
  chess:
    pawn:
      io:
        cluster:  # 或其他组件 (provider/consumer/internal)
          enabled: true
          protocol: TLSv1.2  # 基础协议
          tls13-enabled: true  # 启用 TLS 1.3
          zero-rtt-enabled: false  # 是否启用 0-RTT（实验性）
          key-store-path: /path/to/server.p12
          key-store-password: changeit
          trust-store-path: /path/to/trust.p12
          trust-store-password: changeit
```

### 2. 启用证书热更新

```yaml
z:
  chess:
    pawn:
      io:
        cluster:
          enabled: true
          tls13-enabled: true
          hot-reload-enabled: true  # 启用热更新
          hot-reload-debounce-ms: 5000  # 防抖时间（毫秒）
          key-store-path: /path/to/server.p12
          key-store-password: changeit
          trust-store-path: /path/to/trust.p12
          trust-store-password: changeit
```

### 3. 完整配置示例

```yaml
# application-tls13-hotreload.yaml

z:
  chess:
    pawn:
      io:
        provider:
          enabled: true
          protocol: TLSv1.2
          tls13-enabled: true
          hot-reload-enabled: true
          hot-reload-debounce-ms: 10000
          key-store-path: /etc/z-chess/certs/server.p12
          key-store-password: ${KEYSTORE_PASSWORD}
          trust-store-path: /etc/z-chess/certs/trust.p12
          trust-store-password: ${TRUSTSTORE_PASSWORD}
          client-auth: false
          
        cluster:
          enabled: true
          protocol: TLSv1.2
          tls13-enabled: true
          hot-reload-enabled: true
          hot-reload-debounce-ms: 5000
          key-store-path: /etc/z-chess/certs/cluster.p12
          key-store-password: ${KEYSTORE_PASSWORD}
          trust-store-path: /etc/z-chess/certs/trust.p12
          trust-store-password: ${TRUSTSTORE_PASSWORD}

server:
  port: 2808

logging:
  level:
    io.bishop.ssl: DEBUG
```

---

## 使用 Java 系统属性

### SSL Provider 选择

```bash
# 强制使用指定 Provider
java -Dssl.provider.force=openssl -jar z-chess.jar

# 禁用特定 Provider
java -Dssl.provider.disableWolfSSL=true -jar z-chess.jar
java -Dssl.provider.disableOpenSSL=true -jar z-chess.jar

# 开启 SSL 调试
java -Dssl.debug=true -jar z-chess.jar
```

### 环境变量

```bash
# 生产环境推荐设置
export SSL_PROVIDER_FORCE=openssl  # 或 wolfssl, jdk
export SSL_DEBUG=false
```

---

## 证书热更新操作

### 自动热更新

当 `hot-reload-enabled: true` 时，系统会自动监视证书文件变化：

1. 检测到证书文件修改
2. 等待防抖时间（默认 5 秒）
3. 加载新证书
4. 新连接使用新证书
5. 旧连接继续使用旧证书直至关闭

### 手动触发热更新

通过 JMX 或 REST API（如已实现）：

```java
// 获取 SSLZContext 并手动触发
SSLZContext context = ...;
if (context.isHotReloadEnabled()) {
    boolean success = context.reloadCertificates();
    System.out.println("Reload " + (success ? "successful" : "failed"));
}
```

### 使用脚本触发

```bash
#!/bin/bash
# reload-certs.sh - 触发证书重新加载

# 方法1: 通过 JMX
jcmd <pid> JMX.execute com.isahl.chess:name=SSLManager reloadCertificates

# 方法2: 通过 REST API (如果启用)
curl -X POST http://localhost:8080/api/admin/ssl/reload \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 证书更新流程

### 标准更新步骤

```bash
# 1. 准备新证书
cp new-server.p12 /etc/z-chess/certs/server.p12.new

# 2. 备份旧证书
cp /etc/z-chess/certs/server.p12 /etc/z-chess/certs/server.p12.bak.$(date +%Y%m%d)

# 3. 原子性替换
mv /etc/z-chess/certs/server.p12.new /etc/z-chess/certs/server.p12

# 4. 系统自动检测并加载（热更新）
# 或者手动触发
jcmd <pid> JMX.execute ...

# 5. 验证新证书
openssl s_client -connect localhost:2883 -tls1_3
```

---

## 验证 TLS 1.3

### 使用 OpenSSL 客户端

```bash
# 测试 TLS 1.3 连接
echo | openssl s_client -connect localhost:2883 -tls1_3

# 查看详细的协议信息
openssl s_client -connect localhost:2883 -tls1_3 -msg

# 检查支持的加密套件
echo | openssl s_client -connect localhost:2883 -tls1_3 2>/dev/null | grep "Cipher"
```

### 使用 curl

```bash
# HTTP 接口测试
curl -v --tlsv1.3 https://localhost:8080/health
```

### 使用 MQTT 客户端

```bash
# mosquitto 客户端（支持 TLS 1.3）
mosquitto_sub -h localhost -p 2883 --tls-version tlsv1.3 \
  --cafile ca-cert.pem --cert client.pem --key client.key \
  -t test/topic
```

---

## 监控与日志

### 关键日志

```
# TLS 1.3 初始化
INFO  SSLZContext created [provider=OpenSSL, protocol=TLSv1.3, hotReload=true]

# 热更新触发
INFO  CertificateWatcher - KeyStore file changed: /path/to/server.p12
INFO  Scheduled certificate reload in 5000 ms (triggered by KeyStore)

# 重新加载成功
INFO  Executing certificate reload (triggered by KeyStore)...
INFO  SSLContext reloaded successfully [oldVersion=1, newVersion=2]
INFO  Certificate reload completed successfully

# 加密套件协商
DEBUG TLS 1.3 cipher suites configured: [TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256, TLS_AES_128_GCM_SHA256]
```

### JMX 指标

| MBean | 属性 | 说明 |
|-------|------|------|
| `io.bishop.ssl:type=ReloadableSSLContext` | `Version` | 当前证书版本 |
| `io.bishop.ssl:type=ReloadableSSLContext` | `CreateTime` | 证书创建时间 |
| `io.bishop.ssl:type=ReloadableSSLContext` | `HotReloadEnabled` | 是否启用热更新 |
| `io.bishop.ssl:type=CertificateWatcher` | `Running` | 监视器运行状态 |

---

## 故障排除

### 问题: TLS 1.3 协商失败

**症状**: 客户端无法使用 TLS 1.3 连接

**排查**:
```bash
# 检查 JDK 版本
java -version  # 需要 JDK 11+

# 检查 Provider 支持
java -Dssl.debug=true -jar app.jar 2>&1 | grep "TLSv1.3"

# 使用 OpenSSL 测试
openssl s_client -connect localhost:2883 -tls1_3
```

**解决**:
- 升级到 JDK 11+（如果使用 JDK Provider）
- 启用 OpenSSL Provider: `-Dssl.provider.force=openssl`
- 降级到 TLS 1.2: `tls13-enabled: false`

### 问题: 热更新不生效

**症状**: 替换证书后，新连接仍使用旧证书

**排查**:
```bash
# 检查文件系统事件
tail -f /var/log/z-chess/application.log | grep -i "watcher\|reload"

# 检查证书文件权限
ls -la /etc/z-chess/certs/

# 手动测试文件监视
touch /etc/z-chess/certs/server.p12
```

**解决**:
- 确保 `hot-reload-enabled: true`
- 检查文件权限（运行用户需要读取权限）
- 增加防抖时间（避免文件写入过程中的多次触发）

### 问题: 证书加载失败

**症状**: 热更新后新连接失败

**排查**:
```bash
# 验证证书格式
keytool -list -v -keystore /path/to/server.p12 -storepass <password>

# 检查证书有效期
openssl pkcs12 -in server.p12 -nokeys -clcerts | openssl x509 -noout -dates
```

**解决**:
- 确保证书格式正确（PKCS12）
- 验证密码正确性
- 回滚到备份证书

---

## 性能优化

### TLS 1.3 0-RTT（实验性）

```yaml
z:
  chess:
    pawn:
      io:
        cluster:
          tls13-enabled: true
          zero-rtt-enabled: true  # 启用 0-RTT
```

**注意**: 0-RTT 有重放攻击风险，建议仅在幂等操作场景使用。

### 会话恢复

```yaml
# 启用会话票据
z.chess.pawn.io.ssl.session-tickets-enabled: true
```

---

## 安全建议

1. **定期轮换证书**: 即使证书未过期，也建议每 3-6 个月轮换一次
2. **使用强密钥**: RSA 2048+ 或 ECDSA P-256+
3. **启用热更新**: 避免证书过期导致的服务中断
4. **监控证书有效期**: 在证书过期前 30 天发送告警
5. **使用专用 CA**: 生产环境使用私有 CA 或受信任的公共 CA

---

## 参考文档

- [TLS 1.3 RFC 8446](https://tools.ietf.org/html/rfc8446)
- [OpenSSL TLS 1.3](https://www.openssl.org/blog/blog/2018/09/11/release111/)
- [WolfSSL TLS 1.3](https://www.wolfssl.com/tls-1-3/)
- [Z-Chess SSL Provider 指南](./SSL_PROVIDER_GUIDE.md)
