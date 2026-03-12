/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.king.base.log.Logger;

import javax.net.ssl.SSLContext;
import java.security.Provider;
import java.security.Security;

/**
 * SSL Provider 工厂类
 * 优先级: WolfSSL -> OpenSSL (via Netty TCNative) -> JDK Default
 * 
 * 可通过系统属性配置:
 * - ssl.provider.force=openssl 强制使用指定 provider
 * - ssl.provider.disableWolfSSL=true 禁用 WolfSSL
 * - ssl.provider.disableOpenSSL=true 禁用 OpenSSL
 * 
 * 线程安全：使用双重检查锁定（DCL）模式确保线程安全初始化
 * 
 * @author william.d.zk
 */
public class SslProviderFactory {

    private static final Logger _Logger = Logger.getLogger(SslProviderFactory.class.getSimpleName());

    /**
     * SSL Provider 类型枚举
     */
    public enum SslProviderType {
        WOLFSSL("wolfJSSE", "WolfSSL"),
        OPENSSL("OpenSSL", "OpenSSL (via Netty TCNative)"),
        JDK("SunJSSE", "JDK Default");

        private final String providerName;
        private final String displayName;

        SslProviderType(String providerName, String displayName) {
            this.providerName = providerName;
            this.displayName = displayName;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 使用 volatile 确保可见性
    private static volatile SslProviderType _CurrentProvider = null;
    private static volatile boolean _Initialized = false;
    
    // 用于同步的锁对象
    private static final Object INIT_LOCK = new Object();

    /**
     * 初始化 SSL Provider
     * 按优先级尝试: WolfSSL -> OpenSSL -> JDK Default
     * 支持通过 SslConfiguration 进行配置
     * 
     * 线程安全：使用 DCL 模式，确保只初始化一次
     */
    public static void initialize() {
        // 第一次检查（无锁）
        if (_Initialized) {
            return;
        }
        
        // 同步块
        synchronized (INIT_LOCK) {
            // 第二次检查（有锁）
            if (_Initialized) {
                return;
            }

            // 应用调试配置
            SslConfiguration.applyDebugSettings();
            SslConfiguration.printConfiguration();

            // 检查是否有强制指定的 Provider
            SslProviderType forcedProvider = SslConfiguration.getForcedProvider();
            if (forcedProvider != null) {
                _Logger.info("SSL Provider forced to: %s", forcedProvider.getDisplayName());
                if (tryLoadProvider(forcedProvider)) {
                    _CurrentProvider = forcedProvider;
                    _Initialized = true;
                    _Logger.info("SSL Provider initialized (forced): %s", forcedProvider.getDisplayName());
                    return;
                } else {
                    _Logger.warning("Failed to load forced provider %s, falling back to auto-detect", 
                                  forcedProvider.getDisplayName());
                }
            }

            // 1. 首先尝试 WolfSSL（如果未禁用）
            if (!SslConfiguration.isWolfSSLDisabled() && tryLoadWolfSSL()) {
                _CurrentProvider = SslProviderType.WOLFSSL;
                _Logger.info("SSL Provider initialized: WolfSSL");
                _Initialized = true;
                return;
            }

            // 2. 尝试 OpenSSL（如果未禁用）
            if (!SslConfiguration.isOpenSSLDisabled() && tryLoadOpenSSL()) {
                _CurrentProvider = SslProviderType.OPENSSL;
                _Logger.info("SSL Provider initialized: OpenSSL (via Netty TCNative)");
                _Initialized = true;
                return;
            }

            // 3. 退化为 JDK 默认实现
            _CurrentProvider = SslProviderType.JDK;
            _Logger.info("SSL Provider initialized: JDK Default (WolfSSL and OpenSSL not available or disabled)");
            _Initialized = true;
        }
    }

    /**
     * 尝试加载指定的 Provider
     * @param providerType Provider 类型
     * @return 是否成功
     */
    private static boolean tryLoadProvider(SslProviderType providerType) {
        return switch (providerType) {
            case WOLFSSL -> tryLoadWolfSSL();
            case OPENSSL -> tryLoadOpenSSL();
            case JDK -> true; // JDK 总是可用
        };
    }

    /**
     * 尝试加载 WolfSSL
     * @return 是否成功
     */
    private static boolean tryLoadWolfSSL() {
        try {
            // 加载 wolfssl JNI 库
            System.loadLibrary("wolfssl");
            _Logger.debug("wolfssl native library loaded successfully");

            // 尝试加载 WolfSSL JSSE Provider
            Class<?> wolfSSLProviderClass = Class.forName("com.wolfssl.provider.jsse.WolfSSLProvider");
            Provider wolfSSLProvider = (Provider) wolfSSLProviderClass.getDeclaredConstructor().newInstance();
            
            // 插入到 Provider 列表的最前面（优先级最高）
            Security.insertProviderAt(wolfSSLProvider, 1);
            
            // 验证 SSLContext 可以创建
            SSLContext ctx = SSLContext.getInstance("TLS", wolfSSLProvider);
            ctx.init(null, null, null);
            
            _Logger.info("WolfSSL provider registered and verified successfully");
            return true;
        } catch (UnsatisfiedLinkError e) {
            _Logger.debug("WolfSSL native library not found: %s", e.getMessage());
        } catch (ClassNotFoundException e) {
            _Logger.debug("WolfSSL JSSE Provider class not found: %s", e.getMessage());
        } catch (Exception e) {
            _Logger.debug("Failed to initialize WolfSSL: %s", e.getMessage());
        }
        return false;
    }

    /**
     * 尝试加载 OpenSSL (via Netty TCNative)
     * @return 是否成功
     */
    private static boolean tryLoadOpenSSL() {
        try {
            // 尝试加载 Netty TCNative OpenSSL
            Class<?> opensslClass = Class.forName("io.netty.handler.ssl.OpenSsl");
            
            // 检查 OpenSSL 是否可用
            Boolean available = (Boolean) opensslClass.getMethod("isAvailable").invoke(null);
            if (!available) {
                String error = (String) opensslClass.getMethod("unavailabilityCause").invoke(null);
                _Logger.debug("OpenSSL is not available: %s", error);
                return false;
            }
            
            // 获取版本信息
            String version = (String) opensslClass.getMethod("versionString").invoke(null);
            _Logger.info("OpenSSL (via Netty TCNative) version: %s", version);
            
            return true;
        } catch (ClassNotFoundException e) {
            _Logger.debug("Netty TCNative OpenSSL classes not found: %s", e.getMessage());
        } catch (Exception e) {
            _Logger.debug("Failed to initialize OpenSSL: %s", e.getMessage());
        }
        return false;
    }

    /**
     * 获取当前使用的 SSL Provider 类型
     * @return SSL Provider 类型
     */
    public static SslProviderType getCurrentProvider() {
        // 确保初始化完成（无锁快速路径）
        if (!_Initialized) {
            initialize();
        }
        return _CurrentProvider;
    }

    /**
     * 获取 SSLContext 实例
     * 根据当前 provider 类型返回相应的 SSLContext
     * @param protocol TLS 协议版本 (如 "TLS", "TLSv1.2", "TLSv1.3")
     * @return SSLContext 实例
     * @throws Exception 创建失败时抛出
     */
    public static SSLContext getSSLContext(String protocol) throws Exception {
        if (!_Initialized) {
            initialize();
        }

        // 检查协议版本是否支持
        if (!isProtocolSupported(protocol)) {
            _Logger.warning("Protocol %s may not be fully supported by current provider, attempting anyway", protocol);
        }

        return switch (_CurrentProvider) {
            case WOLFSSL -> {
                try {
                    SSLContext ctx = SSLContext.getInstance(protocol, SslProviderType.WOLFSSL.getProviderName());
                    _Logger.debug("Created WolfSSL context with protocol: %s", protocol);
                    yield ctx;
                } catch (Exception e) {
                    _Logger.warning("Failed to create WolfSSL context with protocol %s, falling back to JDK: %s", 
                                   protocol, e.getMessage());
                    yield SSLContext.getInstance(protocol);
                }
            }
            case OPENSSL, JDK -> {
                // OpenSSL 和 JDK 都使用标准方式创建
                if ("TLSv1.3".equals(protocol)) {
                    _Logger.debug("Using %s for TLS 1.3", _CurrentProvider.getDisplayName());
                }
                yield SSLContext.getInstance(protocol);
            }
        };
    }
    
    /**
     * 检查指定的 TLS 协议版本是否被当前 provider 支持
     * @param protocol 协议版本
     * @return 是否支持
     */
    public static boolean isProtocolSupported(String protocol) {
        if (protocol == null || protocol.isEmpty()) {
            return false;
        }
        
        return switch (protocol) {
            case "TLS", "TLSv1.2" -> true;  // 所有 provider 都支持 TLS 1.2
            case "TLSv1.3" -> {
                // TLS 1.3 支持情况
                if (_CurrentProvider == null) {
                    yield false;
                }
                yield switch (_CurrentProvider) {
                    case WOLFSSL, OPENSSL -> true;
                    case JDK -> Runtime.version().feature() >= 11; // JDK 11+ 支持 TLS 1.3
                };
            }
            default -> false;
        };
    }
    
    /**
     * 获取最佳的 TLS 协议版本
     * 优先返回 TLS 1.3，如果不可用则返回 TLS 1.2
     * @return 最佳协议版本字符串
     */
    public static String getBestProtocol() {
        if (isProtocolSupported("TLSv1.3")) {
            return "TLSv1.3";
        }
        return "TLSv1.2";
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return _Initialized;
    }

    /**
     * 获取 SSLContext 实例 (使用默认 TLS 协议)
     * @return SSLContext 实例
     * @throws Exception 创建失败时抛出
     */
    public static SSLContext getSSLContext() throws Exception {
        return getSSLContext("TLS");
    }

    /**
     * 检查 WolfSSL 是否可用
     * @return 是否可用
     */
    public static boolean isWolfSSLAvailable() {
        return getCurrentProvider() == SslProviderType.WOLFSSL;
    }

    /**
     * 检查 OpenSSL 是否可用
     * @return 是否可用
     */
    public static boolean isOpenSSLAvailable() {
        return getCurrentProvider() == SslProviderType.OPENSSL;
    }

    /**
     * 检查是否使用了原生 SSL 库 (WolfSSL 或 OpenSSL)
     * @return 是否使用了原生库
     */
    public static boolean isNativeSslAvailable() {
        SslProviderType provider = getCurrentProvider();
        return provider == SslProviderType.WOLFSSL || provider == SslProviderType.OPENSSL;
    }

    /**
     * 打印当前 SSL Provider 状态信息
     */
    public static void printStatus() {
        if (!_Initialized) {
            initialize();
        }
        
        _Logger.info("===== SSL Provider Status =====");
        _Logger.info("Current Provider: %s", _CurrentProvider.getDisplayName());
        _Logger.info("Provider Name: %s", _CurrentProvider.getProviderName());
        
        // 打印所有已注册的 Provider
        _Logger.debug("Registered Security Providers:");
        Provider[] providers = Security.getProviders();
        for (int i = 0; i < providers.length; i++) {
            _Logger.debug("  [%d] %s version %s", i + 1, providers[i].getName(), providers[i].getVersion());
        }
        _Logger.info("=============================");
    }
}
