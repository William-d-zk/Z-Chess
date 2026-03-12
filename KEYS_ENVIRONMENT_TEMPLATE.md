# Z-Chess 密钥环境变量配置模板

**文档版本**: 1.0.20  
**最后更新**: 2026-03-12

---

## 快速开始

复制此模板创建 `.env` 文件，并设置强密码：

```bash
# 生成随机密码示例
export TLS_KEYSTORE_PASSWORD=$(openssl rand -base64 32)
export TLS_TRUSTSTORE_PASSWORD=$(openssl rand -base64 32)
export CLUSTER_KEYSTORE_PASSWORD=$(openssl rand -base64 32)
export CLUSTER_TRUSTSTORE_PASSWORD=$(openssl rand -base64 32)
```

---

## 必需的环境变量

### TLS/SSL 密钥库密码

```bash
# 服务端 TLS 密钥库密码 (必需)
export TLS_KEYSTORE_PASSWORD=your_strong_password_here

# 服务端 TLS 信任库密码 (必需)
export TLS_TRUSTSTORE_PASSWORD=your_strong_password_here

# 集群 TLS 密钥库密码 (必需)
export CLUSTER_KEYSTORE_PASSWORD=your_strong_password_here

# 集群 TLS 信任库密码 (必需)
export CLUSTER_TRUSTSTORE_PASSWORD=your_strong_password_here
```

**使用示例**:
```bash
# 本地开发环境
export TLS_KEYSTORE_PASSWORD=dev_keystore_pass_$(date +%s)
export TLS_TRUSTSTORE_PASSWORD=dev_truststore_pass_$(date +%s)
```

---

### 数据库密码

```bash
# PostgreSQL 数据库密码 (必需)
export POSTGRES_PASSWORD=your_secure_db_password

# 可选: 数据库连接配置
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=zchess_test
export POSTGRES_USER=chess
```

---

### 密码生成种子 (可选)

```bash
# 密码随机生成种子 (可选，如果不设置则使用随机值)
export PASSWORD_RANDOM_SEED=$(openssl rand -hex 16)
```

---

## Docker 环境配置

### Docker Compose 示例

创建 `docker-compose.override.yml`:

```yaml
version: '3.8'

services:
  app:
    environment:
      - TLS_KEYSTORE_PASSWORD=${TLS_KEYSTORE_PASSWORD}
      - TLS_TRUSTSTORE_PASSWORD=${TLS_TRUSTSTORE_PASSWORD}
      - CLUSTER_KEYSTORE_PASSWORD=${CLUSTER_KEYSTORE_PASSWORD}
      - CLUSTER_TRUSTSTORE_PASSWORD=${CLUSTER_TRUSTSTORE_PASSWORD}
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
```

### 使用 Docker Secrets (生产环境推荐)

```yaml
version: '3.8'

services:
  app:
    secrets:
      - tls_keystore_password
      - postgres_password

secrets:
  tls_keystore_password:
    external: true
  postgres_password:
    external: true
```

---

## Kubernetes 配置

### Secret 配置示例

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: z-chess-keys
type: Opaque
stringData:
  tls-keystore-password: "your_strong_password"
  tls-truststore-password: "your_strong_password"
  cluster-keystore-password: "your_strong_password"
  cluster-truststore-password: "your_strong_password"
  postgres-password: "your_secure_db_password"
```

### Pod 中使用

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: z-chess
      env:
        - name: TLS_KEYSTORE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: z-chess-keys
              key: tls-keystore-password
```

---

## 密码强度要求

### 最小要求

- 长度: 至少 16 个字符
- 复杂度: 包含大写、小写、数字、特殊字符
- 熵值: 至少 80 bits

### 生成强密码

```bash
# 方法1: OpenSSL
openssl rand -base64 32

# 方法2: /dev/urandom
tr -dc 'A-Za-z0-9!@#$%^&*' < /dev/urandom | head -c 32

# 方法3: Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 安全最佳实践

### 1. 不要提交密码到代码仓库

```bash
# 将 .env 添加到 .gitignore
echo ".env" >> .gitignore
echo "*.secrets" >> .gitignore
```

### 2. 定期轮换密码

```bash
# 设置提醒每 90 天轮换一次密码
# 使用密钥管理系统自动轮换
```

### 3. 使用密码管理器

推荐的密码管理器:
- HashiCorp Vault
- AWS Secrets Manager
- Azure Key Vault
- 1Password (团队版)

### 4. 生产环境必须使用 KMS

```bash
# 开发环境: 可以使用环境变量
# 测试环境: 可以使用 Docker Secrets
# 生产环境: 必须使用 HashiCorp Vault 或云KMS
```

---

## 故障排除

### 问题1: 应用启动失败，提示密码未设置

**错误信息**:
```
错误: 必须设置 TLS_KEYSTORE_PASSWORD 环境变量
```

**解决方案**:
```bash
export TLS_KEYSTORE_PASSWORD=$(openssl rand -base64 32)
# 然后重新启动应用
```

### 问题2: 密钥库密码错误

**错误信息**:
```
java.security.UnrecoverableKeyException: Password verification failed
```

**解决方案**:
1. 确认环境变量设置正确
2. 确认密钥库文件路径正确
3. 重新生成密钥库

---

## 迁移指南

### 从旧版本迁移

如果你使用的是包含硬编码密码的旧版本，请按以下步骤迁移：

1. **备份现有密钥库**
```bash
cp cert/server.p12 cert/server.p12.backup
cp cert/trust.p12 cert/trust.p12.backup
```

2. **生成新密码**
```bash
export TLS_KEYSTORE_PASSWORD=$(openssl rand -base64 32)
export TLS_TRUSTSTORE_PASSWORD=$(openssl rand -base64 32)
```

3. **重新生成密钥库**
```bash
./scripts/generate-ssl-certs.sh dev "$TLS_KEYSTORE_PASSWORD"
```

4. **更新环境变量**
```bash
# 将密码添加到 ~/.bashrc 或 ~/.zshrc
echo "export TLS_KEYSTORE_PASSWORD=$TLS_KEYSTORE_PASSWORD" >> ~/.bashrc
```

5. **验证配置**
```bash
# 检查环境变量
echo $TLS_KEYSTORE_PASSWORD

# 启动应用验证
java -jar Z-Arena/target/Z-Arena.jar
```

---

## 相关文档

- [TLS_SETUP_GUIDE.md](TLS_SETUP_GUIDE.md) - TLS 配置指南
- [DOCKER_TEST_CLUSTER.md](DOCKER_TEST_CLUSTER.md) - Docker 集群部署
- [SECURITY_AUDIT_REPORT_V2.md](SECURITY_AUDIT_REPORT_V2.md) - 安全审计报告

---

**文档维护**: Z-Chess Security Team  
**最后更新**: 2026-03-12
