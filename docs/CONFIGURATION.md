# Z-Chess 功能配置说明

## 1. 配置文件位置

```
Z-Pawn/src/main/resources/
├── application.properties      # 主配置文件
├── application-dev.properties  # 开发环境
├── application-prod.properties # 生产环境
├── db.properties               # 数据库配置
└── logback-spring.xml          # 日志配置
```

## 2. 核心配置项

### 2.1 服务器基础配置

```properties
# 服务端口
server.port=8080

# 应用名称
spring.application.name=z-chess-gateway

# 时区设置
spring.jackson.time-zone=Asia/Shanghai
```

### 2.2 数据库配置 (Z-Rook)

#### 2.2.1 中心数据库 (PostgreSQL)
```properties
# 主数据源
spring.datasource.url=jdbc:postgresql://db-pg.isahl.com:5432/isahl.z-chess
spring.datasource.username=z-chess
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.postgresql.Driver

# 连接池 (HikariCP)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.connection-timeout=20000
```

#### 2.2.2 本地数据库 (SQLite)
```properties
# 本地状态存储
spring.datasource.local.url=jdbc:sqlite:${user.home}/z-chess/local.db
spring.datasource.local.driver-class-name=org.sqlite.JDBC
```

#### 2.2.3 JPA 配置
```properties
# DDL 策略 (生产环境建议 none)
spring.jpa.hibernate.ddl-auto=update

# SQL 日志
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# 方言
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

### 2.3 MQTT 配置 (Z-Pawn)

#### 2.3.1 MQTT v5/v3.1.1 服务端配置
```properties
# MQTT TCP 端口
mqtt.server.port=1883

# MQTT TLS 端口
mqtt.server.tls.port=2883

# WebSocket MQTT 端口
mqtt.server.ws.port=8083

# 最大连接数
mqtt.server.max-connections=100000

# 心跳超时 (秒)
mqtt.server.keep-alive-timeout=60

# 会话过期时间 (秒)
mqtt.server.session-expiry-interval=86400
```

#### 2.3.2 MQTT v5 高级配置
```properties
# 接收最大值 (Receive Maximum)
mqtt.v5.receive-maximum=65535

# 最大 QoS 等级
mqtt.v5.maximum-qos=2

# 保留消息支持
mqtt.v5.retain-available=true

# 通配符订阅支持
mqtt.v5.wildcard-subscription-available=true

# 共享订阅支持
mqtt.v5.shared-subscription-available=true

# 主题别名最大值
mqtt.v5.topic-alias-maximum=65535

# 最大报文大小
mqtt.v5.maximum-packet-size=268435456
```

### 2.4 TLS/SSL 配置 (Z-Bishop)

#### 2.4.1 证书配置
```properties
# 密钥库路径
server.ssl.key-store=${SSL_KEYSTORE_PATH:cert/server.p12}
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12

# 信任库路径
server.ssl.trust-store=${SSL_TRUSTSTORE_PATH:cert/trust.p12}
server.ssl.trust-store-password=${SSL_TRUSTSTORE_PASSWORD}
server.ssl.trust-store-type=PKCS12

# 证书别名
server.ssl.key-alias=server

# TLS 版本 (强制 TLS 1.3)
server.ssl.enabled-protocols=TLSv1.3

# 加密套件
server.ssl.ciphers=TLS_AES_256_GCM_SHA384,TLS_CHACHA20_POLY1305_SHA256
```

#### 2.4.2 证书热重载配置
```properties
# 证书监控启用
ssl.certificate.watch-enabled=true

# 监控间隔 (毫秒)
ssl.certificate.watch-interval=10000

# 去抖时间 (毫秒)
ssl.certificate.reload-debounce=5000
```

#### 2.4.3 SSL Provider 配置
```properties
# SSL Provider 优先级: wolfssl, openssl, jdk
ssl.provider.priority=wolfssl,openssl,jdk

# WolfSSL 库路径 (可选)
ssl.wolfssl.library.path=/usr/local/lib/libwolfssl.so

# OpenSSL 库路径 (可选)
ssl.openssl.library.path=/usr/lib/libssl.so
```

### 2.5 集群配置 (Z-Knight)

#### 2.5.1 RAFT 集群配置
```properties
# 当前节点 ID
raft.node.id=${RAFT_NODE_ID:node1}

# 集群节点列表 (逗号分隔)
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888

# RAFT 端口
raft.port=2888

# 选举超时 (毫秒)
raft.election-timeout-min=300
raft.election-timeout-max=500

# 心跳间隔 (毫秒)
raft.heartbeat-interval=100

# 数据目录
raft.data.dir=${user.home}/z-chess/raft
```

#### 2.5.2 集群网络配置
```properties
# 集群通信端口
cluster.bind.port=2889

# 集群通信线程数
cluster.io.threads=4

# 集群超时时间 (毫秒)
cluster.timeout=5000
```

### 2.6 缓存配置 (Z-Rook)

```properties
# EhCache 配置
spring.cache.type=ehcache
spring.cache.ehcache.config=classpath:ehcache.xml

# 缓存过期时间 (秒)
cache.default-expiry=3600
cache.device-expiry=86400
cache.session-expiry=7200
```

### 2.7 IO 核心配置 (Z-Queen)

```properties
# AIO 线程数
io.aio.threads=8

# 读缓冲区大小
io.buffer.read-size=8192

# 写缓冲区大小
io.buffer.write-size=8192

# 最大帧大小
io.frame.max-size=1048576

# 连接超时 (毫秒)
io.connection.timeout=30000
```

### 2.8 日志配置

```properties
# 日志级别
logging.level.root=INFO
logging.level.com.isahl.chess=DEBUG
logging.level.io.netty=WARN

# 日志文件
logging.file.name=${user.home}/z-chess/logs/z-chess.log
logging.file.max-size=100MB
logging.file.max-history=30

# 控制台日志格式
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

## 3. 环境特定配置

### 3.1 开发环境 (application-dev.properties)

```properties
# 启用开发模式
spring.profiles.active=dev

# H2 内存数据库 (可选)
# spring.datasource.url=jdbc:h2:mem:testdb

# 详细日志
logging.level.com.isahl.chess=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# 禁用 SSL
server.ssl.enabled=false
mqtt.server.tls.port=-1
```

### 3.2 生产环境 (application-prod.properties)

```properties
# 启用生产模式
spring.profiles.active=prod

# 禁用 SQL 日志
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=ERROR

# 启用 SSL
server.ssl.enabled=true

# 连接池优化
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10

# JVM 优化 (启动参数)
# -Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

## 4. 安全配置

### 4.1 设备认证配置

```properties
# 设备认证模式: password, certificate, token
mqtt.auth.mode=password

# 密码加密算法: bcrypt, sha256, plain
mqtt.auth.password.encoder=bcrypt

# 认证失败重试限制
mqtt.auth.max-retries=5

# 认证失败锁定时间 (分钟)
mqtt.auth.lock-time=30
```

### 4.2 访问控制配置

```properties
# 启用 ACL
mqtt.acl.enabled=true

# ACL 数据源: database, file
mqtt.acl.source=database

# 默认权限: allow, deny
mqtt.acl.default=deny
```

## 5. 高级功能配置

### 5.1 消息持久化配置

```properties
# 启用消息持久化
mqtt.persistence.enabled=true

# 离线消息保留时间 (小时)
mqtt.persistence.offline-ttl=168

# 消息存储批量大小
mqtt.persistence.batch-size=1000

# 消息清理间隔 (小时)
mqtt.persistence.clean-interval=24
```

### 5.2 共享订阅配置

```properties
# 启用共享订阅
mqtt.shared-subscription.enabled=true

# 负载均衡策略: round-robin, random, hash
mqtt.shared-subscription.strategy=round-robin
```

### 5.3 WebSocket 配置

```properties
# WebSocket 路径
mqtt.ws.path=/mqtt

# WebSocket 最大帧大小
mqtt.ws.max-frame-size=65536

# WebSocket 压缩
mqtt.ws.compression-enabled=true
```

## 6. 监控与指标

### 6.1 Actuator 配置

```properties
# 启用 Actuator
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# 健康检查详情
management.endpoint.health.show-details=when_authorized

# Prometheus 端点
management.endpoints.web.base-path=/actuator
```

### 6.2 指标采集配置

```properties
# 启用 Micrometer
management.metrics.enabled=true

# MQTT 指标
management.metrics.export.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name}
```

## 7. 配置文件示例

### 7.1 最小化配置 (单机版)

```properties
# application-standalone.properties
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/z-chess
spring.datasource.username=z-chess
spring.datasource.password=your-password

mqtt.server.port=1883
server.ssl.enabled=false
```

### 7.2 集群配置 (3 节点)

```properties
# Node 1
raft.node.id=node1
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
server.port=8080

# Node 2
raft.node.id=node2
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
server.port=8081

# Node 3
raft.node.id=node3
raft.cluster.nodes=node1:172.30.10.10:2888,node2:172.30.10.11:2888,node3:172.30.10.12:2888
server.port=8082
```

## 8. 配置验证

启动时检查配置有效性：

```bash
# 验证配置
java -jar z-arena.jar --spring.profiles.active=dev --debug

# 检查健康状态
curl http://localhost:8080/actuator/health
```
