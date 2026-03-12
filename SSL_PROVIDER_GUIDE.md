# SSL Provider 配置指南

本项目现已支持多层级 SSL Provider 自动降级机制，优先级为：
**WolfSSL → OpenSSL → JDK Default**

## 特性

- **WolfSSL 优先**: 最高性能的原生 SSL/TLS 实现
- **OpenSSL 后备**: 当 WolfSSL 不可用时自动降级到 OpenSSL (via Netty TCNative)
- **JDK 默认**: 当原生库都不可用时，使用 JDK 内置的 SSL 实现
- **灵活配置**: 支持通过系统属性强制指定 Provider 或禁用某些选项

## 依赖

已在 `Z-Bishop/pom.xml` 中添加以下依赖：

```xml
<!-- WolfSSL Java Provider -->
<dependency>
    <groupId>com.wolfssl</groupId>
    <artifactId>wolfssl-jsse</artifactId>
    <version>${wolfssl.version}</version>
</dependency>
<dependency>
    <groupId>com.wolfssl</groupId>
    <artifactId>wolfssl-jni</artifactId>
    <version>${wolfssl.version}</version>
</dependency>

<!-- OpenSSL fallback via Netty TCNative -->
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-tcnative-boringssl-static</artifactId>
    <version>${netty-tcnative.version}</version>
</dependency>
```

## 配置选项

### 系统属性

| 属性名 | 说明 | 示例值 |
|--------|------|--------|
| `ssl.provider.force` | 强制使用指定 Provider | `wolfssl`, `openssl`, `jdk` |
| `ssl.provider.disableWolfSSL` | 禁用 WolfSSL | `true`, `false` |
| `ssl.provider.disableOpenSSL` | 禁用 OpenSSL | `true`, `false` |
| `ssl.debug` | 开启 SSL 调试信息 | `true`, `false` |
| `wolfssl.debug` | 开启 WolfSSL 调试 | `true`, `false` |

### 使用示例

#### 1. 强制使用 WolfSSL

```bash
java -Dssl.provider.force=wolfssl -jar your-app.jar
```

#### 2. 禁用 WolfSSL，使用 OpenSSL

```bash
java -Dssl.provider.disableWolfSSL=true -jar your-app.jar
```

#### 3. 禁用所有原生库，使用 JDK 默认

```bash
java -Dssl.provider.disableWolfSSL=true -Dssl.provider.disableOpenSSL=true -jar your-app.jar
```

#### 4. 开启 SSL 调试模式

```bash
java -Dssl.debug=true -Dwolfssl.debug=true -jar your-app.jar
```

## WolfSSL 安装

### Linux (Ubuntu/Debian)

```bash
# 安装 wolfssl 开发库
sudo apt-get update
sudo apt-get install libwolfssl-dev

# 或者从源码编译安装
git clone https://github.com/wolfSSL/wolfssl.git
cd wolfssl
./autogen.sh
./configure --enable-jni --enable-opensslextra
make
sudo make install
sudo ldconfig
```

### macOS

```bash
# 使用 Homebrew 安装
brew install wolfssl

# 或者从源码编译安装
git clone https://github.com/wolfSSL/wolfssl.git
cd wolfssl
./autogen.sh
./configure --enable-jni --enable-opensslextra --prefix=/usr/local
make
sudo make install
```

### Docker

在 Dockerfile 中添加：

```dockerfile
# 安装 wolfssl
RUN apt-get update && apt-get install -y libwolfssl-dev \
    || (git clone https://github.com/wolfSSL/wolfssl.git /tmp/wolfssl \
    && cd /tmp/wolfssl \
    && ./autogen.sh \
    && ./configure --enable-jni --enable-opensslextra \
    && make && make install && ldconfig)
```

## 验证 SSL Provider

启动应用后，查看日志输出：

```
[INFO] SSL Provider initialized: WolfSSL
```

或者在代码中检查：

```java
import com.isahl.chess.bishop.io.ssl.SslProviderFactory;

// 获取当前 Provider
SslProviderFactory.SslProviderType provider = SslProviderFactory.getCurrentProvider();
System.out.println("Current SSL Provider: " + provider.getDisplayName());

// 检查特定 Provider 是否可用
boolean wolfsslAvailable = SslProviderFactory.isWolfSSLAvailable();
boolean opensslAvailable = SslProviderFactory.isOpenSSLAvailable();
boolean nativeAvailable = SslProviderFactory.isNativeSslAvailable();

// 打印详细状态
SslProviderFactory.printStatus();
```

## 性能对比

| Provider | 性能 | CPU 占用 | 内存占用 | 适用场景 |
|----------|------|----------|----------|----------|
| WolfSSL | 最高 | 最低 | 最低 | 高性能生产环境 |
| OpenSSL | 高 | 低 | 低 | 生产环境 |
| JDK | 中等 | 中等 | 中等 | 开发/测试环境 |

## 故障排除

### WolfSSL 加载失败

**症状**: 日志显示 `WolfSSL native library not found`

**解决方案**:
1. 确认 wolfssl 库已安装: `ldconfig -p | grep wolfssl`
2. 检查 `LD_LIBRARY_PATH` 包含 wolfssl 库路径
3. 在 macOS 上检查 `DYLD_LIBRARY_PATH`

### OpenSSL 加载失败

**症状**: 日志显示 `Netty TCNative OpenSSL classes not found`

**解决方案**:
1. 确认 `netty-tcnative-boringssl-static` 依赖已正确引入
2. 检查是否有依赖冲突

### 回退到 JDK Default

**症状**: 日志显示 `SSL Provider initialized: JDK Default`

**原因**:
- WolfSSL 和 OpenSSL 都不可用
- 或者被系统属性禁用

**解决方案**:
- 安装 WolfSSL 或 OpenSSL 库
- 检查系统属性配置

## 注意事项

1. **线程安全**: `SslProviderFactory` 是线程安全的，初始化只执行一次
2. **不可变更**: 一旦初始化完成，Provider 类型不可更改
3. **顺序加载**: 按 WolfSSL → OpenSSL → JDK 顺序尝试加载
4. **异常处理**: 如果强制指定的 Provider 加载失败，会自动降级

## 相关类

- `com.isahl.chess.bishop.io.ssl.SslProviderFactory`: SSL Provider 工厂
- `com.isahl.chess.bishop.io.ssl.SslConfiguration`: SSL 配置类
- `com.isahl.chess.bishop.io.ssl.SSLZContext`: SSL 上下文实现
