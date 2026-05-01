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

import com.isahl.chess.king.base.exception.ZException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicStampedReference;
import javax.net.ssl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 可热更新的 SSLContext 包装器
 *
 * <p>支持在不重启服务的情况下更新证书，实现零停机证书轮换。
 *
 * <p>特性： - 原子性更新：使用 AtomicStampedReference 确保更新过程线程安全且避免 ABA 问题 - 无缝切换：新连接使用新证书，旧连接继续使用旧证书直至关闭 -
 * 自动回滚：新证书加载失败时保留旧证书 - 读多写少优化：getSSLContext 无锁，reload 使用轻量级同步
 *
 * @author william.d.zk
 */
public class ReloadableSSLContext {

  private static final Logger _Logger =
      LoggerFactory.getLogger("io.bishop.ssl.ReloadableSSLContext");

  // 使用 AtomicStampedReference 避免 ABA 问题
  // stamp 仅用于 CAS 操作序列，不存储业务版本号
  private final AtomicStampedReference<SSLContextHolder> _ContextRef;

  // 配置信息（final，线程安全）
  private final String _Protocol;
  private final String _KeyStorePath;
  private final String _KeyStorePassword;
  private final String _TrustStorePath;
  private final String _TrustStorePassword;
  private final String _KeyStoreType;

  // 版本号生成器，原子递增
  private final AtomicLong _VersionGenerator = new AtomicLong(0);

  // CAS 操作序列号，用于 ABA 保护
  private final AtomicLong _StampGenerator = new AtomicLong(0);

  // 证书监视器
  private CertificateWatcher _CertificateWatcher;
  private final boolean _EnableHotReload;
  private final long _ReloadDebounceMs;

  /** SSLContext 持有者 - 不可变对象，线程安全 */
  private static final class SSLContextHolder {
    final SSLContext context;
    final long version;
    final long createTime;

    SSLContextHolder(SSLContext context, long version) {
      this.context = context;
      this.version = version;
      this.createTime = System.currentTimeMillis();
    }
  }

  /**
   * 创建可热更新的 SSLContext
   *
   * @param protocol TLS 协议版本 (TLS, TLSv1.2, TLSv1.3)
   * @param keyStorePath KeyStore 路径
   * @param keyStorePassword KeyStore 密码
   * @param trustStorePath TrustStore 路径（可为 null）
   * @param trustStorePassword TrustStore 密码（可为 null）
   * @throws Exception 初始化失败
   */
  public ReloadableSSLContext(
      String protocol,
      String keyStorePath,
      String keyStorePassword,
      String trustStorePath,
      String trustStorePassword)
      throws Exception {
    this(
        protocol,
        keyStorePath,
        keyStorePassword,
        trustStorePath,
        trustStorePassword,
        "PKCS12",
        false,
        5000);
  }

  /**
   * 创建可热更新的 SSLContext（完整配置）
   *
   * @param protocol TLS 协议版本
   * @param keyStorePath KeyStore 路径
   * @param keyStorePassword KeyStore 密码
   * @param trustStorePath TrustStore 路径
   * @param trustStorePassword TrustStore 密码
   * @param keyStoreType KeyStore 类型 (PKCS12, JKS)
   * @param enableHotReload 是否启用热更新
   * @param reloadDebounceMs 热更新防抖时间（毫秒）
   * @throws Exception 初始化失败
   */
  public ReloadableSSLContext(
      String protocol,
      String keyStorePath,
      String keyStorePassword,
      String trustStorePath,
      String trustStorePassword,
      String keyStoreType,
      boolean enableHotReload,
      long reloadDebounceMs)
      throws Exception {

    _Protocol = protocol != null ? protocol : "TLS";
    _KeyStorePath = keyStorePath;
    _KeyStorePassword = keyStorePassword;
    _TrustStorePath = trustStorePath;
    _TrustStorePassword = trustStorePassword;
    _KeyStoreType = keyStoreType != null ? keyStoreType : "PKCS12";
    _EnableHotReload = enableHotReload;
    _ReloadDebounceMs = reloadDebounceMs;

    // 初始加载
    SSLContextHolder initialHolder = createContextHolder();
    _ContextRef =
        new AtomicStampedReference<>(initialHolder, (int) _StampGenerator.incrementAndGet());

    // 如果启用热更新，启动证书监视器
    if (_EnableHotReload && _KeyStorePath != null) {
      startCertificateWatcher();
    }
  }

  /** 启动证书监视器 */
  private void startCertificateWatcher() {
    try {
      _CertificateWatcher =
          new CertificateWatcher(_KeyStorePath, _TrustStorePath, v -> reload(), _ReloadDebounceMs);
      _CertificateWatcher.start();
      _Logger.info("CertificateWatcher started for hot reload");
    } catch (IOException e) {
      _Logger.error("Failed to start CertificateWatcher: %s", e.getMessage());
      throw new RuntimeException("Failed to start certificate watcher", e);
    }
  }

  /** 创建 SSLContextHolder */
  private SSLContextHolder createContextHolder() throws Exception {
    // 使用 SslProviderFactory 获取 SSLContext
    if (!SslProviderFactory.isInitialized()) {
      SslProviderFactory.initialize();
    }

    SSLContext sslContext = SslProviderFactory.getSSLContext(_Protocol);

    // 加载 KeyManager
    KeyManager[] keyManagers = null;
    if (_KeyStorePath != null && _KeyStorePassword != null) {
      keyManagers = loadKeyManagers();
    }

    // 加载 TrustManager
    TrustManager[] trustManagers;
    if (_TrustStorePath != null && _TrustStorePassword != null) {
      trustManagers = loadTrustManagers();
    } else {
      // 使用系统默认 TrustManager（更安全）
      TrustManagerFactory tmf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init((KeyStore) null);
      trustManagers = tmf.getTrustManagers();
      _Logger.debug("Using system default TrustManager");
    }

    // 初始化 SSLContext
    sslContext.init(keyManagers, trustManagers, null);

    long version = _VersionGenerator.incrementAndGet();
    _Logger.info("SSLContext created [version=%d, protocol=%s]", version, _Protocol);
    return new SSLContextHolder(sslContext, version);
  }

  /** 加载 KeyManager */
  private KeyManager[] loadKeyManagers() throws Exception {
    Path keyStorePath = Paths.get(_KeyStorePath);
    if (!Files.exists(keyStorePath)) {
      throw new ZException("KeyStore file not found: %s", _KeyStorePath);
    }

    KeyStore keyStore = KeyStore.getInstance(_KeyStoreType);
    try (InputStream is = Files.newInputStream(keyStorePath)) {
      keyStore.load(is, _KeyStorePassword.toCharArray());
    }

    KeyManagerFactory factory =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    factory.init(keyStore, _KeyStorePassword.toCharArray());

    _Logger.debug("KeyManager loaded from %s", _KeyStorePath);
    return factory.getKeyManagers();
  }

  /** 加载 TrustManager */
  private TrustManager[] loadTrustManagers() throws Exception {
    Path trustStorePath = Paths.get(_TrustStorePath);
    if (!Files.exists(trustStorePath)) {
      throw new ZException("TrustStore file not found: %s", _TrustStorePath);
    }

    KeyStore trustStore = KeyStore.getInstance(_KeyStoreType);
    try (InputStream is = Files.newInputStream(trustStorePath)) {
      trustStore.load(is, _TrustStorePassword.toCharArray());
    }

    TrustManagerFactory factory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(trustStore);

    _Logger.debug("TrustManager loaded from %s", _TrustStorePath);
    return factory.getTrustManagers();
  }

  /**
   * 重新加载证书
   *
   * @return 是否成功
   */
  public boolean reload() {
    try {
      _Logger.info("Reloading SSLContext...");

      // 创建新的 ContextHolder
      SSLContextHolder newHolder = createContextHolder();

      // 获取当前 holder 和 stamp
      int[] stampHolder = new int[1];
      SSLContextHolder oldHolder = _ContextRef.get(stampHolder);
      int currentStamp = stampHolder[0];
      int newStamp = (int) _StampGenerator.incrementAndGet();

      // 原子性更新（使用 CAS）
      if (_ContextRef.compareAndSet(oldHolder, newHolder, currentStamp, newStamp)) {
        _Logger.info(
            "SSLContext reloaded successfully [oldVersion=%d, newVersion=%d]",
            oldHolder != null ? oldHolder.version : -1, newHolder.version);

        // 旧连接会继续使用 oldHolder 的 SSLEngine，直到连接关闭
        // 新连接会使用 newHolder 创建新的 SSLEngine

        return true;
      } else {
        _Logger.warn("SSLContext reload failed: concurrent modification detected");
        return false;
      }
    } catch (Exception e) {
      _Logger.error("Failed to reload SSLContext: %s", e.getMessage());
      return false;
    }
  }

  /**
   * 获取当前的 SSLContext（无锁，高性能）
   *
   * @return SSLContext 实例
   */
  public SSLContext getSSLContext() {
    SSLContextHolder holder = _ContextRef.getReference();
    return holder != null ? holder.context : null;
  }

  /**
   * 获取当前的 SSLEngine
   *
   * @return SSLEngine 实例
   */
  public SSLEngine createSSLEngine() {
    SSLContextHolder holder = _ContextRef.getReference();
    if (holder == null || holder.context == null) {
      throw new IllegalStateException("SSLContext not initialized");
    }
    return holder.context.createSSLEngine();
  }

  /** 获取当前的 SSLEngine（指定主机和端口） */
  public SSLEngine createSSLEngine(String host, int port) {
    SSLContextHolder holder = _ContextRef.getReference();
    if (holder == null || holder.context == null) {
      throw new IllegalStateException("SSLContext not initialized");
    }
    return holder.context.createSSLEngine(host, port);
  }

  /** 获取当前版本号 */
  public long getVersion() {
    SSLContextHolder holder = _ContextRef.getReference();
    return holder != null ? holder.version : -1;
  }

  /** 获取当前 SSLContext 的创建时间 */
  public long getCreateTime() {
    SSLContextHolder holder = _ContextRef.getReference();
    return holder != null ? holder.createTime : 0;
  }

  /** 是否启用了热更新 */
  public boolean isHotReloadEnabled() {
    return _EnableHotReload;
  }

  /** 停止证书监视器 */
  public void stop() {
    if (_CertificateWatcher != null) {
      _CertificateWatcher.stop();
      _Logger.info("CertificateWatcher stopped");
    }
  }
}
