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
import java.io.File;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;

/**
 * SSL Provider 工厂类 优先级: WolfSSL -> JDK Default
 *
 * <p>可通过系统属性配置: - ssl.provider.force=wolfssl 强制使用指定 provider - ssl.provider.disableWolfSSL=true 禁用
 * WolfSSL
 *
 * <p>线程安全：使用双重检查锁定（DCL）模式确保线程安全初始化
 *
 * @author william.d.zk
 */
public class SslProviderFactory {

  private static final Logger _Logger = Logger.getLogger(SslProviderFactory.class.getSimpleName());

  /** SSL Provider 类型枚举 */
  public enum SslProviderType {
    WOLFSSL("wolfJSSE", "WolfSSL"),
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

  private static volatile SslProviderType _CurrentProvider = null;
  private static volatile boolean _Initialized = false;
  private static final Object INIT_LOCK = new Object();

  private static final Map<String, SSLContext> _ContextCache = new ConcurrentHashMap<>();

  private static final String[] NATIVE_LIBRARY_SEARCH_PATHS = {
    System.getProperty("java.library.path", ""),
    System.getProperty("user.home") + "/.local/lib",
    "/usr/local/lib",
    "/usr/lib",
    "/opt/wolfssl/lib"
  };

  public static void initialize() {
    if (_Initialized) {
      return;
    }

    synchronized (INIT_LOCK) {
      if (_Initialized) {
        return;
      }

      SslConfiguration.applyDebugSettings();
      SslConfiguration.printConfiguration();

      SslProviderType forcedProvider = SslConfiguration.getForcedProvider();
      if (forcedProvider != null) {
        _Logger.info("SSL Provider forced to: %s", forcedProvider.getDisplayName());
        if (tryLoadProvider(forcedProvider)) {
          _CurrentProvider = forcedProvider;
          _Initialized = true;
          _Logger.info("SSL Provider initialized (forced): %s", forcedProvider.getDisplayName());
          return;
        } else {
          _Logger.warning(
              "Failed to load forced provider %s, falling back to auto-detect",
              forcedProvider.getDisplayName());
        }
      }

      if (!SslConfiguration.isWolfSSLDisabled() && tryLoadWolfSSL()) {
        _CurrentProvider = SslProviderType.WOLFSSL;
        _Logger.info("SSL Provider initialized: WolfSSL");
        _Initialized = true;
        return;
      }

      _CurrentProvider = SslProviderType.JDK;
      _Logger.info("SSL Provider initialized: JDK Default (WolfSSL not available or disabled)");
      _Initialized = true;
    }
  }

  /** 重置 SSL Provider 状态（主要用于测试） */
  public static void reset() {
    synchronized (INIT_LOCK) {
      _Initialized = false;
      _CurrentProvider = null;
      _ContextCache.clear();

      Security.removeProvider("wolfJSSE");

      _Logger.debug("SSL Provider reset completed");
    }
  }

  private static boolean tryLoadProvider(SslProviderType providerType) {
    return switch (providerType) {
      case WOLFSSL -> tryLoadWolfSSL();
      case JDK -> true;
    };
  }

  private static boolean tryLoadWolfSSL() {
    try {
      Class<?> wolfSSLProviderClass;
      try {
        wolfSSLProviderClass = Class.forName("com.wolfssl.provider.jsse.WolfSSLProvider");
      } catch (ClassNotFoundException e) {
        _Logger.debug("WolfSSL JSSE Provider class not found in classpath");
        return false;
      }

      if (!loadWolfSSLNativeLibrary()) {
        return false;
      }

      Provider wolfSSLProvider =
          (Provider) wolfSSLProviderClass.getDeclaredConstructor().newInstance();

      int existingWolfSSL = findProviderIndex("wolfJSSE");
      if (existingWolfSSL > 0) {
        Security.removeProvider("wolfJSSE");
        _Logger.debug("Removed existing WolfSSL provider at position %d", existingWolfSSL);
      }

      int insertPos = findInsertPosition();
      Security.insertProviderAt(wolfSSLProvider, insertPos);

      SSLContext ctx = SSLContext.getInstance("TLS", wolfSSLProvider);
      ctx.init(null, null, null);

      _Logger.info(
          "WolfSSL provider registered at position %d and verified successfully", insertPos);
      return true;
    } catch (UnsatisfiedLinkError e) {
      _Logger.debug("WolfSSL native library not found: " + e.getMessage());
    } catch (Exception e) {
      _Logger.debug("Failed to initialize WolfSSL: " + e.getMessage());
    }
    return false;
  }

  private static boolean loadWolfSSLNativeLibrary() {
    String osName = System.getProperty("os.name", "").toLowerCase();
    String jniLibName = osName.contains("mac") ? "libwolfssljni.dylib" : "libwolfssljni.so";

    try {
      System.loadLibrary("wolfssljni");
      _Logger.debug("wolfssljni native library loaded via System.loadLibrary");
      return true;
    } catch (UnsatisfiedLinkError e) {
      _Logger.debug("System.loadLibrary failed, searching in known paths...");
    }

    for (String path : NATIVE_LIBRARY_SEARCH_PATHS) {
      if (path == null || path.isEmpty()) {
        continue;
      }

      String[] subPaths = path.split(File.pathSeparator);
      for (String subPath : subPaths) {
        File jniLib = new File(subPath, jniLibName);
        if (jniLib.exists() && jniLib.canRead()) {
          try {
            System.load(jniLib.getAbsolutePath());
            _Logger.info("wolfssljni native library loaded from: %s", jniLib.getAbsolutePath());
            return true;
          } catch (UnsatisfiedLinkError e) {
            _Logger.debug("Failed to load from %s: %s", jniLib.getAbsolutePath(), e.getMessage());
          }
        }
      }
    }

    _Logger.debug("WolfSSL native library not found in any known path");
    return false;
  }

  private static int findProviderIndex(String providerName) {
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      if (providers[i].getName().equals(providerName)) {
        return i + 1;
      }
    }
    return -1;
  }

  private static int findInsertPosition() {
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      String name = providers[i].getName();
      if (name.startsWith("SUN") || name.startsWith("Sun") || name.equals("SunJSSE")) {
        return i + 2;
      }
    }
    return 2;
  }

  public static SslProviderType getCurrentProvider() {
    if (!_Initialized) {
      initialize();
    }
    return _CurrentProvider;
  }

  public static SSLContext getSSLContext(String protocol) throws Exception {
    if (!_Initialized) {
      initialize();
    }

    return _ContextCache.computeIfAbsent(
        protocol,
        p -> {
          try {
            return createSSLContext(p);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create SSLContext for protocol: " + p, e);
          }
        });
  }

  private static SSLContext createSSLContext(String protocol) throws Exception {
    if (!isProtocolSupported(protocol)) {
      _Logger.warning(
          "Protocol %s may not be fully supported by current provider, attempting anyway",
          protocol);
    }

    return switch (_CurrentProvider) {
      case WOLFSSL -> {
        try {
          SSLContext ctx =
              SSLContext.getInstance(protocol, SslProviderType.WOLFSSL.getProviderName());
          _Logger.debug("Created WolfSSL context with protocol: %s", protocol);
          yield ctx;
        } catch (Exception e) {
          _Logger.warning(
              "Failed to create WolfSSL context with protocol %s, falling back to JDK: %s",
              protocol, e.getMessage());
          yield SSLContext.getInstance(protocol);
        }
      }
      case JDK -> {
        if ("TLSv1.3".equals(protocol)) {
          _Logger.debug("Using %s for TLS 1.3", _CurrentProvider.getDisplayName());
        }
        yield SSLContext.getInstance(protocol);
      }
    };
  }

  public static boolean isProtocolSupported(String protocol) {
    if (protocol == null || protocol.isEmpty()) {
      return false;
    }

    return switch (protocol) {
      case "TLS", "TLSv1.2" -> true;
      case "TLSv1.3" -> {
        if (_CurrentProvider == null) {
          yield false;
        }
        yield switch (_CurrentProvider) {
          case WOLFSSL -> true;
          case JDK -> Runtime.version().feature() >= 11;
        };
      }
      default -> false;
    };
  }

  public static String getBestProtocol() {
    if (isProtocolSupported("TLSv1.3")) {
      return "TLSv1.3";
    }
    return "TLSv1.2";
  }

  public static boolean isInitialized() {
    return _Initialized;
  }

  public static SSLContext getSSLContext() throws Exception {
    return getSSLContext("TLS");
  }

  public static boolean isWolfSSLAvailable() {
    return getCurrentProvider() == SslProviderType.WOLFSSL;
  }

  public static boolean isNativeSslAvailable() {
    return getCurrentProvider() == SslProviderType.WOLFSSL;
  }

  public static void printStatus() {
    if (!_Initialized) {
      initialize();
    }

    _Logger.info("===== SSL Provider Status =====");
    _Logger.info("Current Provider: %s", _CurrentProvider.getDisplayName());
    _Logger.info("Provider Name: %s", _CurrentProvider.getProviderName());

    _Logger.debug("Registered Security Providers:");
    Provider[] providers = Security.getProviders();
    for (int i = 0; i < providers.length; i++) {
      _Logger.debug(
          "  [%d] %s version %s", i + 1, providers[i].getName(), providers[i].getVersion());
    }
    _Logger.info("=============================");
  }
}
