# 密钥存储安全审计报告

**审计日期**: 2026-03-12  
**审计版本**: 1.0.20  
**审计类型**: 密钥存储安全检查

---

## 审计摘要

本次审计发现了 **6处密钥存储安全问题**，其中 **2处高危**、**3处中危**、**1处低危**。

| 风险等级 | 数量 | 说明 |
|----------|------|------|
| 🔴 高危 | 2 | 硬编码密钥、默认密码 |
| 🟠 中危 | 3 | 测试代码中的明文密码、密码种子 |
| 🟡 低危 | 1 | 密钥文件权限 |

---

## 详细发现

### 🔴 高危问题

#### 1. 本地TLS配置硬编码密码

**位置**: `Z-Arena/src/main/resources/application-local-tls.yaml`

**问题代码**:
```yaml
z:
  chess:
    pawn:
      io:
        provider:
          key-store-password: changeit      # ❌ 硬编码密码
          trust-store-password: changeit    # ❌ 硬编码密码
        cluster:
          key-store-password: changeit      # ❌ 硬编码密码
          trust-store-password: changeit    # ❌ 硬编码密码
```

**风险**: 使用默认密码 `changeit`，攻击者可轻易破解密钥库

**修复方案**: 使用环境变量替代硬编码密码

---

#### 2. 线上配置默认回退密码

**位置**: `Z-Arena/src/main/resources/application-online.yaml`

**问题代码**:
```yaml
provider:
  ssl:
    key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}      # ❌ 默认密码
    trust-store-password: ${SSL_TRUSTSTORE_PASSWORD:changeit}  # ❌ 默认密码
cluster:
  ssl:
    key-store-password: ${CLUSTER_KEYSTORE_PASSWORD:changeit}  # ❌ 默认密码
```

**风险**: 环境变量未设置时使用默认密码 `changeit`

**修复方案**: 移除默认值，强制要求设置环境变量

---

### 🟠 中危问题

#### 3. 密码随机种子硬编码

**位置**: `Z-Pawn/src/main/resources/pawn.mix.properties`

**问题代码**:
```properties
z.chess.pawn.mix.password_random_seed=z-com.tgx.chess.knight  # ❌ 硬编码种子
```

**风险**: 密码生成使用固定种子，可预测生成的密码

**修复方案**: 使用随机生成的种子，通过环境变量配置

---

#### 4. 测试代码硬编码密码

**位置**: `Z-Knight/src/test/java/.../TlsClientTest.java`

**问题代码**:
```java
private static final String PASSWORD = "test123";  # ❌ 测试代码硬编码密码
```

**风险**: 虽然是测试代码，但可能被复制到生产环境

**修复方案**: 使用环境变量或配置文件中读取

---

#### 5. 私钥文件明文存储

**位置**: `cert/` 目录下的 `.pem` 文件

**文件列表**:
- `cert/ca-key.pem` - CA私钥
- `cert/server-key.pem` - 服务器私钥
- `cert/client-key.pem` - 客户端私钥

**当前状态**: 私钥文件使用 PEM 格式明文存储

**风险**: 私钥文件泄露可导致中间人攻击

**修复方案**: 
- 确保私钥文件权限为 600
- 考虑使用硬件安全模块(HSM)或密钥管理系统

---

### 🟡 低危问题

#### 6. 密码字符集定义

**位置**: `Z-King/src/main/java/.../CryptoUtil.java`

**代码**:
```java
private final byte[] _PasswordWords = "qwertyuiopasdfghjklzxcvbnmQAZWSXEDCRFVTGBYHNUJMIKOLP1234567890".getBytes(StandardCharsets.UTF_8);
```

**评估**: 这是密码字符集定义，不是密钥存储问题

**状态**: ✅ 无需修复

---

## 修复方案

### 方案一: 环境变量化所有密码

**优先级**: P0 (立即执行)

**操作步骤**:
1. 修改 `application-local-tls.yaml`，使用环境变量
2. 修改 `application-online.yaml`，移除默认密码
3. 创建 `.env.example` 模板文件
4. 更新启动脚本检查环境变量

**预期效果**: 消除所有硬编码密码

---

### 方案二: 密钥管理系统(KMS)集成

**优先级**: P1 (1周内)

**操作步骤**:
1. 评估 HashiCorp Vault 或 AWS KMS
2. 实现密钥读取接口
3. 迁移现有密钥到KMS
4. 更新应用启动流程

**预期效果**: 集中管理密钥，支持密钥轮换

---

### 方案三: 私钥文件加密

**优先级**: P2 (1个月内)

**操作步骤**:
1. 使用强密码加密私钥文件
2. 私钥密码通过环境变量传入
3. 修改证书加载代码支持解密
4. 更新证书生成脚本

**预期效果**: 即使私钥文件泄露也无法使用

---

## 合规性评估

| 标准 | 要求 | 当前状态 | 修复后状态 |
|------|------|----------|------------|
| PCI DSS | 密钥不得明文存储 | ❌ 不合规 | ✅ 合规 |
| ISO 27001 | 密钥管理流程 | ⚠️ 部分合规 | ✅ 合规 |
| 等保2.0 | 密钥安全存储 | ⚠️ 部分合规 | ✅ 合规 |

---

## 下一步行动

### 立即执行 (今天)
1. 修复 `application-local-tls.yaml` 硬编码密码
2. 修复 `application-online.yaml` 默认密码
3. 修复 `pawn.mix.properties` 密码种子

### 本周内执行
1. 修复测试代码硬编码密码
2. 检查私钥文件权限
3. 创建密钥管理文档

### 本月内执行
1. 评估并集成KMS
2. 私钥文件加密
3. 密钥轮换流程

---

**审计完成时间**: 2026-03-12  
**建议复查时间**: 2026-03-19 (1周后)

---

*本报告由安全审计工具生成，仅供参考。*
