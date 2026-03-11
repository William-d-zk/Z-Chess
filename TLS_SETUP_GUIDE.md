# Z-Chess TLS/SSL 配置指南

## 概述

Z-Chess 为 zchat 协议提供完整的 TLS/SSL 加密通信能力，支持：

- **单向 TLS**：服务端证书认证
- **双向 TLS (mTLS)**：客户端和服务端证书互相认证
- **集群内部 TLS**：Raft 节点间加密通信
- **多种密钥库格式**：PKCS12 (推荐)、JKS

## 快速开始

### 1. 生成 TLS 证书

#### 使用 OpenSSL 生成自签名证书（测试环境）

```bash
# 创建证书目录
mkdir -p cert

# 1. 生成 CA 私钥和证书
openssl req -x509 -newkey rsa:4096 -keyout cert/ca-key.pem -out cert/ca-cert.pem \
    -days 365 -nodes -subj "/CN=Z-Chess CA/O=Z-Chess"

# 2. 生成服务端私钥和证书签名请求
openssl req -newkey rsa:2048 -keyout cert/server-key.pem -out cert/server.csr \
    -nodes -subj "/CN=server.z-chess.local/O=Z-Chess"

# 3. 使用 CA 签名服务端证书
openssl x509 -req -in cert/server.csr -CA cert/ca-cert.pem -CAkey cert/ca-key.pem \
    -CAcreateserial -out cert/server-cert.pem -days 365

# 4. 生成 PKCS12 格式的密钥库
openssl pkcs12 -export -in cert/server-cert.pem -inkey cert/server-key.pem \
    -certfile cert/ca-cert.pem -out cert/server.p12 \
    -name server -password pass:changeit

# 5. 生成客户端证书（双向 TLS 需要）
openssl req -newkey rsa:2048 -keyout cert/client-key.pem -out cert/client.csr \
    -nodes -subj "/CN=client.z-chess.local/O=Z-Chess"
openssl x509 -req -in cert/client.csr -CA cert/ca-cert.pem -CAkey cert/ca-key.pem \
    -CAcreateserial -out cert/client-cert.pem -days 365
openssl pkcs12 -export -in cert/client-cert.pem -inkey cert/client-key.pem \
    -certfile cert/ca-cert.pem -out cert/client.p12 \
    -name client -password pass:changeit

# 6. 生成信任库（包含 CA 证书）
keytool -import -alias ca -file cert/ca-cert.pem -keystore cert/trust.p12 \
    -storetype PKCS12 -storepass changeit -noprompt
```

#### 使用 Java keytool（生产环境推荐）

```bash
# 1. 生成服务端密钥库
keytool -genkeypair -alias server -keyalg RSA -keysize 2048 \
    -validity 365 -keystore cert/server.p12 -storetype PKCS12 \
    -storepass changeit -keypass changeit \
    -dname "CN=server.z-chess.local, O=Z-Chess, C=CN"

# 2. 导出服务端证书
keytool -export -alias server -keystore cert/server.p12 \
    -storepass changeit -file cert/server.cer

# 3. 生成客户端密钥库（双向 TLS 需要）
keytool -genkeypair -alias client -keyalg RSA -keysize 2048 \
    -validity 365 -keystore cert/client.p12 -storetype PKCS12 \
    -storepass changeit -keypass changeit \
    -dname "CN=client.z-chess.local, O=Z-Chess, C=CN"

# 4. 导出客户端证书
keytool -export -alias client -keystore cert/client.p12 \
    -storepass changeit -file cert/client.cer

# 5. 创建信任库并导入证书
keytool -import -alias server -file cert/server.cer -keystore cert/trust.p12 \
    -storetype PKCS12 -storepass changeit -noprompt
keytool -import -alias client -file cert/client.cer -keystore cert/trust.p12 \
    -storetype PKCS12 -storepass changeit -noprompt
```

### 2. 配置 TLS

编辑 `Z-Pawn/src/main/resources/pawn.io.ssl.properties`：

#### 服务端 TLS（单向认证）

```properties
# 启用服务端 TLS
z.chess.pawn.io.provider.ssl.enabled=true

# 服务端密钥库配置
z.chess.pawn.io.provider.ssl.key-store-path=cert/server.p12
z.chess.pawn.io.provider.ssl.key-store-password=changeit

# 信任库配置（单向 TLS 可选，用于验证客户端）
z.chess.pawn.io.provider.ssl.trust-store-path=cert/trust.p12
z.chess.pawn.io.provider.ssl.trust-store-password=changeit

# 不要求客户端证书
z.chess.pawn.io.provider.ssl.client-auth=false

# TLS 协议版本
z.chess.pawn.io.provider.ssl.protocol=TLSv1.2
```

#### 服务端 TLS（双向认证 mTLS）

```properties
z.chess.pawn.io.provider.ssl.enabled=true
z.chess.pawn.io.provider.ssl.key-store-path=cert/server.p12
z.chess.pawn.io.provider.ssl.key-store-password=changeit
z.chess.pawn.io.provider.ssl.trust-store-path=cert/trust.p12
z.chess.pawn.io.provider.ssl.trust-store-password=changeit

# 要求客户端证书（双向认证）
z.chess.pawn.io.provider.ssl.client-auth=true
z.chess.pawn.io.provider.ssl.protocol=TLSv1.2
```

#### 客户端 TLS 配置

```properties
# 启用客户端 TLS
z.chess.pawn.io.consumer.ssl.enabled=true

# 客户端密钥库（用于客户端证书认证，可选）
z.chess.pawn.io.consumer.ssl.key-store-path=cert/client.p12
z.chess.pawn.io.consumer.ssl.key-store-password=changeit

# 信任库（用于验证服务端证书）
z.chess.pawn.io.consumer.ssl.trust-store-path=cert/trust.p12
z.chess.pawn.io.consumer.ssl.trust-store-password=changeit

# 验证服务端主机名
z.chess.pawn.io.consumer.ssl.verify-hostname=true
z.chess.pawn.io.consumer.ssl.protocol=TLSv1.2
```

#### 集群内部 TLS（Raft 节点间）

```properties
# 启用集群 TLS
z.chess.pawn.io.cluster.ssl.enabled=true
z.chess.pawn.io.cluster.ssl.key-store-path=cert/cluster.p12
z.chess.pawn.io.cluster.ssl.key-store-password=changeit
z.chess.pawn.io.cluster.ssl.trust-store-path=cert/cluster-trust.p12
z.chess.pawn.io.cluster.ssl.trust-store-password=changeit

# 集群必须启用双向认证
z.chess.pawn.io.cluster.ssl.client-auth=true
z.chess.pawn.io.cluster.ssl.protocol=TLSv1.2
```

### 3. Docker 环境 TLS 配置

在 `application-online.yml` 中添加：

```yaml
# TLS 配置
z:
  chess:
    pawn:
      io:
        provider:
          ssl:
            enabled: true
            key-store-path: /app/cert/server.p12
            key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
            trust-store-path: /app/cert/trust.p12
            trust-store-password: ${SSL_TRUSTSTORE_PASSWORD:changeit}
            client-auth: ${SSL_CLIENT_AUTH:false}
        cluster:
          ssl:
            enabled: ${CLUSTER_SSL_ENABLED:false}
            key-store-path: /app/cert/cluster.p12
            key-store-password: ${CLUSTER_SSL_KEYSTORE_PASSWORD:changeit}
            trust-store-path: /app/cert/cluster-trust.p12
            trust-store-password: ${CLUSTER_SSL_TRUSTSTORE_PASSWORD:changeit}
            client-auth: true
```

Docker 运行命令：

```bash
docker run -d \
  -v $(pwd)/cert:/app/cert:ro \
  -e SSL_KEYSTORE_PASSWORD=secure_password \
  -e SSL_CLIENT_AUTH=true \
  -p 1883:1883 \
  z-chess:latest
```

## 架构说明

### TLS 架构组件

```
┌─────────────────────────────────────────────────────────────┐
│                      应用层 (zchat 协议)                      │
├─────────────────────────────────────────────────────────────┤
│                    SSL 握手过滤器                             │
│              (SslHandShakeFilter - TLS 握手)                 │
├─────────────────────────────────────────────────────────────┤
│                     SSL 过滤器                                │
│              (SSLFilter - 加密/解密)                          │
├─────────────────────────────────────────────────────────────┤
│                    zchat 帧过滤器                             │
│              (ZFrameFilter - 帧解析)                         │
├─────────────────────────────────────────────────────────────┤
│                    TCP 传输层                                 │
└─────────────────────────────────────────────────────────────┘
```

### 核心类说明

| 类名 | 说明 |
|------|------|
| `SslSocketConfig` | TLS 配置类，管理所有 SSL 上下文 |
| `SSLZContext` | TLS 上下文，封装 SSLEngine |
| `SslZSort` | SSL Sort 包装器，为现有 Sort 添加 TLS 能力 |
| `SslHandShakeFilter` | SSL 握手处理过滤器 |
| `SSLFilter` | SSL 数据加密/解密过滤器 |
| `X07_SslHandShake` | SSL 握手控制消息 |

## 安全配置建议

### 1. 生产环境检查清单

- [ ] 使用 CA 签名的证书（而非自签名）
- [ ] 启用双向 TLS (mTLS) 保护敏感接口
- [ ] 使用 TLSv1.2 或 TLSv1.3（禁用 TLSv1.0/1.1）
- [ ] 配置强加密套件
- [ ] 定期轮换证书（建议 90 天）
- [ ] 启用证书吊销检查 (CRL/OCSP)
- [ ] 妥善保管密钥库密码（使用环境变量或密钥管理服务）

### 2. 推荐的加密套件

```properties
# 强加密套件（禁用弱算法）
z.chess.pawn.io.provider.ssl.ciphers=\
  TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,\
  TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,\
  TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,\
  TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
```

### 3. 证书管理脚本

```bash
#!/bin/bash
# cert-manager.sh - 证书自动轮换脚本

CERT_DIR="./cert"
DAYS_BEFORE_EXPIRY=30

# 检查证书有效期
check_cert_expiry() {
    local cert_file=$1
    local expiry_date=$(openssl x509 -enddate -noout -in "$cert_file" | cut -d= -f2)
    local expiry_epoch=$(date -d "$expiry_date" +%s)
    local current_epoch=$(date +%s)
    local days_until_expiry=$(( (expiry_epoch - current_epoch) / 86400 ))
    
    echo "Certificate $cert_file expires in $days_until_expiry days"
    
    if [ $days_until_expiry -le $DAYS_BEFORE_EXPIRY ]; then
        echo "WARNING: Certificate will expire soon!"
        return 1
    fi
    return 0
}

# 轮换证书
rotate_cert() {
    local alias=$1
    local keystore=$2
    local password=$3
    
    echo "Rotating certificate for $alias..."
    # 生成新证书
    keytool -genkeypair -alias ${alias}_new -keyalg RSA -keysize 2048 \
        -validity 90 -keystore $keystore -storepass $password \
        -dname "CN=${alias}.z-chess.local,O=Z-Chess"
    
    echo "New certificate generated. Please update clients."
}

# 主逻辑
check_cert_expiry "$CERT_DIR/server.p12" || rotate_cert "server" "$CERT_DIR/server.p12" "changeit"
```

## 故障排查

### 常见问题

#### 1. SSLHandshakeException: PKIX path building failed

**原因**：信任库中缺少 CA 证书

**解决**：
```bash
# 导入 CA 证书到信任库
keytool -import -alias ca -file ca-cert.pem -keystore trust.p12 -storepass changeit
```

#### 2. SSLException: Received fatal alert: certificate_unknown

**原因**：客户端无法验证服务端证书

**解决**：
- 检查客户端信任库是否包含服务端证书或 CA
- 确认证书未过期
- 验证证书主机名匹配

#### 3. 性能问题

**优化建议**：
- 启用 SSL 会话恢复
- 使用硬件加速（如 Intel QuickAssist）
- 考虑使用 TLS 1.3（更低延迟）

```properties
# 启用会话恢复
z.chess.pawn.io.ssl.enable-session-creation=true
z.chess.pawn.io.ssl.session-timeout=300
```

## 参考资料

- [JSSE Reference Guide](https://docs.oracle.com/en/java/javase/17/security/java-secure-socket-extension-jsse-reference-guide.html)
- [TLS 1.3 RFC 8446](https://tools.ietf.org/html/rfc8446)
- [OWASP Transport Layer Protection](https://owasp.org/www-project-cheat-sheets/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
