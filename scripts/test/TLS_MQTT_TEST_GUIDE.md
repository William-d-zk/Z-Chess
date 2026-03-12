# TLS MQTT 压力测试验证指南

本文档介绍如何使用 Z-Audience 压力测试验证器测试 TLS 加密的 MQTT 服务链路。

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                     TLS MQTT 压力测试架构                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐                    ┌──────────────┐          │
│  │   Audience   │ ═══════════════════│    Raft00    │          │
│  │  (压力测试)   │    TLS 加密隧道     │  (MQTT Broker)│          │
│  │              │    Port: 8883      │              │          │
│  │  50 并发连接  │◄──────────────────►│  TLS Enabled │          │
│  │  120 秒测试   │                    │              │          │
│  └──────────────┘                    └──────────────┘          │
│         │                                   │                    │
│         │                                   │                    │
│         ▼                                   ▼                    │
│  ┌──────────────┐                    ┌──────────────┐          │
│  │   证书验证    │                    │   证书挂载    │          │
│  │  trust.p12   │                    │  server.p12  │          │
│  └──────────────┘                    └──────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 快速开始

### 1. 一键启动完整测试

```bash
cd scripts/test/docker

# 启动完整的 TLS MQTT 压力测试
./test-tls-mqtt.sh start
```

这个命令会：
1. 生成 TLS 证书（如不存在）
2. 启动 PostgreSQL 和 Z-Chess Arena (启用 TLS)
3. 使用 OpenSSL 验证 TLS 端口
4. 使用 Mosquitto 验证 MQTT TLS 连接
5. 启动 Audience 进行 120 秒压力测试
6. 输出测试报告

### 2. 仅验证 TLS 连接（不跑压力测试）

```bash
./test-tls-mqtt.sh verify-only
```

### 3. 独立验证 TLS MQTT 链路

```bash
# 方式 1: 使用 OpenSSL
echo | openssl s_client -connect localhost:8883 \
    -CAfile cert/ca-cert.pem

# 方式 2: 使用 mosquitto 客户端
mosquitto_pub -h localhost -p 8883 -t test/topic -m "hello" \
    --cafile cert/ca-cert.pem

# 方式 3: 使用验证脚本
cd scripts/test/tls
./verify-mqtt-tls.sh
```

## 详细步骤

### 步骤 1: 准备证书

```bash
cd scripts/test/docker

# 生成证书（如果尚未生成）
../tls/generate-ssl-certs.sh dev changeit

# 验证证书
ls -la cert/
openssl x509 -in cert/ca-cert.pem -noout -text
```

### 步骤 2: 启动 TLS 服务

```bash
# 使用 docker-compose-tls.yaml 启动
docker compose -f docker-compose-tls.yaml up -d db-pg raft00

# 等待服务就绪
sleep 15

# 验证端口
docker compose -f docker-compose-tls.yaml ps
```

### 步骤 3: 验证 TLS 端口

```bash
# 测试 TLS 握手
echo | openssl s_client -connect localhost:8883 \
    -CAfile cert/ca-cert.pem

# 预期输出应包含：
# Verify return code: 0 (ok)
# Protocol: TLSv1.2
# Cipher: ECDHE-RSA-AES256-GCM-SHA384
```

### 步骤 4: 启动压力测试

```bash
# 启动 Audience TLS 压力测试
docker compose -f docker-compose-tls.yaml --profile tls-test up -d audience-tls

# 查看实时日志
docker compose -f docker-compose-tls.yaml logs -f audience-tls
```

### 步骤 5: 监控测试

```bash
# 查看连接统计
watch -n 1 'docker compose -f docker-compose-tls.yaml logs --tail=20 audience-tls'

# 查看系统资源
docker stats raft00 audience-tls
```

## 测试配置

### Audience TLS 配置

```yaml
# docker-compose-tls.yaml 中的配置
environment:
  # 压力测试目标
  - Z_CHESS_PRESSURE_TARGET_HOST=raft00
  - Z_CHESS_PRESSURE_TARGET_PORT=8883  # TLS 端口
  
  # 测试参数
  - Z_CHESS_PRESSURE_CONCURRENCY=50     # 50 并发连接
  - Z_CHESS_PRESSURE_DURATION=PT120S    # 120 秒
  - Z_CHESS_PRESSURE_PROTOCOL=mqtt
  
  # TLS 配置
  - Z_CHESS_PRESSURE_SSL_ENABLED=true
  - Z_CHESS_PRESSURE_SSL_TRUSTSTORE_PATH=/app/cert/trust.p12
  - Z_CHESS_PRESSURE_SSL_TRUSTSTORE_PASSWORD=changeit
```

### Z-Chess TLS 配置

```yaml
# Raft00 的 TLS 配置
environment:
  - SSL_ENABLED=true
  - SSL_KEYSTORE_PATH=/app/cert/server.p12
  - SSL_KEYSTORE_PASSWORD=changeit
  - SSL_TRUSTSTORE_PATH=/app/cert/trust.p12
  - SSL_TRUSTSTORE_PASSWORD=changeit

volumes:
  - ./cert:/app/cert:ro  # 只读挂载证书
```

## 验证清单

### TLS 层验证

- [ ] TLS 端口 8883 可访问
- [ ] TLS 握手成功
- [ ] 证书验证通过
- [ ] 协议版本为 TLSv1.2 或更高
- [ ] 使用强加密套件 (ECDHE-RSA-AES256-GCM-SHA384)

### MQTT 层验证

- [ ] MQTT CONNECT 成功
- [ ] MQTT PUBLISH 成功
- [ ] MQTT SUBSCRIBE 成功
- [ ] 消息加解密正常

### 性能验证

- [ ] 并发连接数达标 (50+)
- [ ] 消息吞吐量正常
- [ ] 延迟在可接受范围
- [ ] 无内存泄漏

## 故障排查

### 问题 1: TLS 握手失败

```bash
# 检查证书
openssl x509 -in cert/ca-cert.pem -noout -text

# 检查端口
nc -zv localhost 8883

# 详细调试
echo | openssl s_client -connect localhost:8883 -debug
```

### 问题 2: 证书验证失败

```bash
# 确保证书正确挂载到容器
docker compose -f docker-compose-tls.yaml exec raft00 ls -la /app/cert/

# 检查证书有效期
openssl x509 -in cert/ca-cert.pem -noout -dates
```

### 问题 3: Audience 连接失败

```bash
# 检查 Audience 日志
docker compose -f docker-compose-tls.yaml logs audience-tls

# 检查网络连通性
docker compose -f docker-compose-tls.yaml exec audience-tls ping raft00
```

## 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| TLS 握手时间 | < 100ms | 单次握手耗时 |
| 连接建立率 | > 95% | 成功连接比例 |
| 消息吞吐量 | > 1000 msg/s | 每秒消息数 |
| 平均延迟 | < 50ms | 端到端延迟 |
| 内存使用 | < 1GB | Audience 内存 |

## 高级用法

### 调整并发数

```bash
# 修改 docker-compose-tls.yaml
environment:
  - Z_CHESS_PRESSURE_CONCURRENCY=100  # 增加到 100 并发
  - Z_CHESS_PRESSURE_DURATION=PT300S  # 增加到 5 分钟
```

### 双向 TLS (mTLS)

```yaml
# 启用客户端证书验证
environment:
  - SSL_CLIENT_AUTH=true
  
volumes:
  # Audience 挂载客户端证书
  - ./cert/client.p12:/app/cert/client.p12:ro
```

### 使用自定义证书

```bash
# 使用生产证书
cp /path/to/production/certs/* cert/
docker compose -f docker-compose-tls.yaml restart
```

## 相关文档

- [TLS 配置指南](../../TLS_SETUP_GUIDE.md)
- [Docker 测试集群](docker/README.md)
- [压力测试配置](../Z-Audience/src/main/resources/application-docker.yml)

## 问题反馈

遇到问题请提交 Issue 或联系开发团队。
