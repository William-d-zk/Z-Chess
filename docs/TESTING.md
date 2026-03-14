# 测试指南

## 测试分类

本项目采用以下测试分类规范：

| 类型 | 命名模式 | 运行命令 | 说明 |
|------|----------|----------|------|
| 单元测试 | `*Test.java` | `mvn test` | 纯业务逻辑测试，无外部依赖 |
| 集成测试 | `*IT.java` / `*IntegrationTest.java` | `mvn verify -Pintegration-test` | 需要外部服务（如数据库） |

## 运行测试

### 单元测试
```bash
# 运行所有单元测试
mvn test

# 运行特定模块的测试
mvn test -pl Z-Audience

# 运行特定测试类
mvn test -Dtest=CryptoUtilTest

# 运行特定测试方法
mvn test -Dtest=CryptoUtilTest#testEncrypt
```

### 集成测试
```bash
# 需要先启动 PostgreSQL
# 运行集成测试
mvn verify -Pintegration-test

# 跳过单元测试，仅运行集成测试
mvn verify -Pintegration-test -DskipUnitTests=true
```

### 代码覆盖率
```bash
# 生成覆盖率报告
mvn test jacoco:report

# 查看报告 (在 target/site/jacoco/index.html)
open target/site/jacoco/index.html

# 覆盖率检查 (会验证是否达到最低要求)
mvn test jacoco:check
```

## 测试配置

### JaCoCo 覆盖率要求

- **行覆盖率**: ≥ 30%
- **分支覆盖率**: ≥ 20%

### 测试重试机制

失败的测试会自动重试最多 2 次，以处理偶发的 flaky test。

### 测试并行执行

单元测试默认并行执行，最多 4 个线程。

## 集成测试环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| POSTGRES_HOST | localhost | PostgreSQL 主机 |
| POSTGRES_PORT | 5432 | PostgreSQL 端口 |
| POSTGRES_USER | chess | PostgreSQL 用户名 |
| POSTGRES_PASSWORD | chess | PostgreSQL 密码 |
| POSTGRES_DB | test | PostgreSQL 数据库名 |

## 编写测试

### 单元测试示例

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyServiceTest {
    
    @Test
    void testMethod() {
        // Arrange
        MyService service = new MyService();
        
        // Act
        String result = service.process("input");
        
        // Assert
        assertEquals("expected", result);
    }
}
```

### 集成测试示例

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("integration")
class DatabaseIT {
    
    @Test
    void testDatabaseConnection() {
        // Integration test code
    }
}
```

## CI 集成

GitHub Actions 自动运行：
1. 单元测试 (`mvn test`)
2. 代码格式检查 (`mvn spotless:check`)
3. 覆盖率检查 (`mvn jacoco:check`)
4. 集成测试 (`mvn verify -Pintegration-test`)

## 故障排查

### 测试失败

1. 查看详细错误信息：`mvn test -X`
2. 运行单个测试：`mvn test -Dtest=ClassName#methodName`
3. 跳过覆盖率检查：`mvn test -Djacoco.skip=true`

### 集成测试失败

1. 确认 PostgreSQL 服务正常运行
2. 检查环境变量配置
3. 查看测试日志中的详细错误信息
