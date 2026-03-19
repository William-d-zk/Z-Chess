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

import com.isahl.chess.bishop.protocol.ProtocolContext;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.ssl.ISslOption;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.*;

/**
 * SSL/TLS Context 实现类 使用 SslProviderFactory 获取 SSLContext，支持 WolfSSL/OpenSSL/JDK 自动降级
 *
 * <p>优化点： - 减少日志输出，使用 debug/trace 级别代替 info - TLS 1.3 加密套件缓存，避免重复计算 - 更精确的异常分类
 *
 * @author william.d.zk
 */
public class SSLZContext<A extends IPContext> extends ProtocolContext<IPacket>
    implements IProxyContext<A> {
  private static final Logger _Logger = Logger.getLogger("io.bishop.ssl.SSLZContext");

  // TLS 1.3 推荐加密套件（按优先级排序）
  private static final List<String> TLS13_CIPHER_SUITES =
      Collections.unmodifiableList(
          Arrays.asList(
              "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256", "TLS_AES_128_GCM_SHA256"));

  // 缓存支持的 TLS 1.3 套件，避免每次创建时重新计算
  // 使用 SoftReference 允许 GC 在内存紧张时回收
  private static volatile java.lang.ref.SoftReference<String[]> _CachedTls13Suites = null;

  private final SSLEngine _SslEngine;
  private final SSLContext _SslContext;
  private final SSLSession _SslSession;
  private final A _ActingContext;
  private final int _AppInBufferSize;

  // 热更新支持
  private final ReloadableSSLContext _ReloadableContext;
  private final boolean _HotReloadEnabled;
  private final String _Protocol;

  /** 创建 SSLZContext（基础版本，使用 TLSv1.2） */
  public SSLZContext(ISslOption option, ISort.Mode mode, ISort.Type type, A acting) {
    this(option, mode, type, acting, "TLSv1.2", false);
  }

  /**
   * 创建 SSLZContext（支持 TLS 1.3 和热更新）
   *
   * @param option SSL 选项
   * @param mode 模式
   * @param type 类型
   * @param acting 代理上下文
   * @param protocol 协议版本 (TLS, TLSv1.2, TLSv1.3)
   * @param hotReloadEnabled 是否启用热更新
   */
  public SSLZContext(
      ISslOption option,
      ISort.Mode mode,
      ISort.Type type,
      A acting,
      String protocol,
      boolean hotReloadEnabled) {
    super(option, mode, type);
    _ActingContext = acting;
    _Protocol = protocol != null ? protocol : "TLSv1.2";
    _HotReloadEnabled = hotReloadEnabled;

    // 确保 SSL Provider 已初始化
    SslProviderFactory.initialize();

    try {
      if (_HotReloadEnabled && option.getKeyStorePath() != null) {
        // 使用可热更新的 SSLContext
        _ReloadableContext =
            new ReloadableSSLContext(
                _Protocol,
                option.getKeyStorePath(),
                option.getKeyStorePassword(),
                option.getTrustStorePath(),
                option.getTrustStorePassword(),
                "PKCS12",
                true,
                5000);
        _SslContext = _ReloadableContext.getSSLContext();
        _Logger.debug("Using ReloadableSSLContext with hot reload enabled");
      } else {
        // 使用普通 SSLContext
        _ReloadableContext = null;
        _SslContext = SslProviderFactory.getSSLContext(_Protocol);
        _SslContext.init(option.getKeyManagers(), option.getTrustManagers(), null);
      }

      // 创建 SSLEngine
      _SslEngine = _SslContext.createSSLEngine();

      // 配置协议版本
      configureProtocols(_SslEngine, _Protocol);

      // 配置加密套件
      configureCipherSuites(_SslEngine, _Protocol);

      _SslEngine.setUseClientMode(type == ISort.Type.CLIENT);
      _SslEngine.setNeedClientAuth(type == ISort.Type.SERVER && option.isSslClientAuth());
      _SslSession = _SslEngine.getSession();
      _AppInBufferSize = option.getSslAppSize();

      // 记录当前使用的 SSL Provider 和协议（仅 debug 级别）
      SslProviderFactory.SslProviderType providerType = SslProviderFactory.getCurrentProvider();
      _Logger.debug(
          "SSLZContext created [provider=%s, protocol=%s, hotReload=%s]",
          providerType.getDisplayName(), _Protocol, _HotReloadEnabled);
    } catch (SSLException e) {
      throw new ZException(e, "SSL context init failed: SSL error - " + e.getMessage());
    } catch (Exception e) {
      throw new ZException(e, "SSL context init failed: " + e.getMessage());
    }
  }

  /** 配置协议版本 */
  private void configureProtocols(SSLEngine engine, String protocol) {
    switch (protocol) {
      case "TLSv1.3" -> engine.setEnabledProtocols(new String[] {"TLSv1.3"});
      case "TLSv1.2" -> engine.setEnabledProtocols(new String[] {"TLSv1.2"});
      default -> engine.setEnabledProtocols(new String[] {"TLSv1.3", "TLSv1.2"});
    }
  }

  /** 配置加密套件 */
  private void configureCipherSuites(SSLEngine engine, String protocol) {
    if ("TLSv1.3".equals(protocol)) {
      String[] suites = getTls13CipherSuites(engine);
      if (suites.length > 0) {
        engine.setEnabledCipherSuites(suites);
        _Logger.debug("TLS 1.3 cipher suites configured: %s", Arrays.toString(suites));
      } else {
        _Logger.warning("No TLS 1.3 cipher suites available, using defaults");
      }
    } else {
      // TLS 1.2 启用所有支持的加密套件（但过滤掉弱加密套件）
      String[] supported = engine.getSupportedCipherSuites();
      String[] enabled =
          Arrays.stream(supported).filter(this::isStrongCipherSuite).toArray(String[]::new);
      engine.setEnabledCipherSuites(enabled);
      _Logger.debug("Enabled %d strong cipher suites for TLS 1.2", enabled.length);
    }
  }

  /** 获取 TLS 1.3 加密套件（带缓存） */
  private String[] getTls13CipherSuites(SSLEngine engine) {
    // 检查缓存
    String[] cached = _CachedTls13Suites != null ? _CachedTls13Suites.get() : null;
    if (cached != null) {
      return cached;
    }

    // 计算并缓存
    List<String> supported = Arrays.asList(engine.getSupportedCipherSuites());
    List<String> enabled = TLS13_CIPHER_SUITES.stream().filter(supported::contains).toList();

    String[] result = enabled.toArray(new String[0]);
    _CachedTls13Suites = new java.lang.ref.SoftReference<>(result);
    return result;
  }

  /** 检查是否是强加密套件 */
  private boolean isStrongCipherSuite(String suite) {
    // 排除已知的弱加密套件
    String upper = suite.toUpperCase();
    return !upper.contains("NULL")
        && !upper.contains("EXPORT")
        && !upper.contains("DES")
        && !upper.contains("MD5")
        && !upper.contains("SHA1")
        && !upper.contains("RC4");
  }

  @Override
  public A getActingContext() {
    return _ActingContext;
  }

  public SSLEngine getSSLEngine() {
    return _SslEngine;
  }

  public SSLContext getSSLContext() {
    // 如果启用了热更新，从 ReloadableSSLContext 获取最新的 Context
    if (_HotReloadEnabled && _ReloadableContext != null) {
      return _ReloadableContext.getSSLContext();
    }
    return _SslContext;
  }

  /**
   * 手动触发证书重新加载（热更新）
   *
   * @return 是否成功
   */
  public boolean reloadCertificates() {
    if (!_HotReloadEnabled || _ReloadableContext == null) {
      _Logger.warning("Hot reload is not enabled, cannot reload certificates");
      return false;
    }
    return _ReloadableContext.reload();
  }

  /** 检查是否启用了热更新 */
  public boolean isHotReloadEnabled() {
    return _HotReloadEnabled;
  }

  /** 获取当前协议版本 */
  public String getProtocol() {
    return _Protocol;
  }

  /** 检查是否启用了 TLS 1.3 */
  public boolean isTls13Enabled() {
    return "TLSv1.3".equals(_Protocol) || "TLS".equals(_Protocol);
  }

  public void close() {
    try {
      // 先关闭 outbound 发送 close_notify，再关闭 inbound 接收 close_notify
      // 这是 SSL/TLS 标准关闭顺序
      _SslEngine.closeOutbound();
      _SslEngine.closeInbound();
    } catch (SSLException e) {
      // closeInbound 可能抛出异常如果对等端没有发送 close_notify
      // 这是正常的，特别是在连接异常断开时
      _Logger.debug(
          "SSL close error (normal if peer didn't send close_notify): %s", e.getMessage());
    }

    // 停止证书监视器
    if (_ReloadableContext != null) {
      _ReloadableContext.stop();
    }
  }

  @Override
  public boolean isProxy() {
    return true;
  }

  @Override
  public void ready() {
    // Use ENCODE_FRAME for handshake phase so SslHandShakeFilter.pipeSeek can be invoked
    advanceOutState(ENCODE_FRAME);
    advanceInState(DECODE_FRAME);
    _ActingContext.ready();
    try {
      _Logger.debug(
          "Starting SSL handshake [protocol=%s, clientMode=%s, needClientAuth=%s]",
          _Protocol, _SslEngine.getUseClientMode(), _SslEngine.getNeedClientAuth());
      _SslEngine.beginHandshake();
      _Logger.trace("SSL handshake started, status=%s", _SslEngine.getHandshakeStatus());
    } catch (SSLException e) {
      _Logger.fetal("SSL handshake initialization failed: %s", e.getMessage());
    }
  }

  public SSLEngineResult.HandshakeStatus getHandShakeStatus() {
    return _SslEngine.getHandshakeStatus();
  }

  public SSLEngineResult.HandshakeStatus doTask() {
    Runnable delegatedTask;
    while ((delegatedTask = _SslEngine.getDelegatedTask()) != null) {
      delegatedTask.run();
    }
    return _SslEngine.getHandshakeStatus();
  }

  public ByteBuf doWrap(ByteBuf output) {
    try {
      ByteBuf netOutBuffer = ByteBuf.allocate(_SslSession.getPacketBufferSize());
      _Logger.trace(
          "SSL wrap: input=%d bytes, handshakeStatus=%s",
          output.readableBytes(), _SslEngine.getHandshakeStatus());

      SSLEngineResult result = _SslEngine.wrap(output.toReadBuffer(), netOutBuffer.toWriteBuffer());
      int produced = result.bytesProduced();

      _Logger.trace(
          "SSL wrap result: status=%s, produced=%d, handshakeStatus=%s",
          result.getStatus(), produced, result.getHandshakeStatus());

      return switch (result.getStatus()) {
        case OK, BUFFER_UNDERFLOW -> {
          doTask();
          yield netOutBuffer.seek(produced);
        }
        case CLOSED -> throw new ZException("SSL connection closed during wrap");
        case BUFFER_OVERFLOW -> {
          _Logger.warning("SSL wrap BUFFER_OVERFLOW, packet buffer may be too small");
          throw new ZException("SSL wrap buffer overflow");
        }
      };
    } catch (SSLException e) {
      _Logger.fetal("SSL wrap error: %s", e.getMessage());
      throw new ZException(e, "ssl wrap error");
    }
  }

  public ByteBuf doUnwrap(ByteBuf netInBuffer) {
    try {
      ByteBuf appInBuffer = ByteBuf.allocate(_AppInBufferSize);
      ByteBuffer inputBuffer = netInBuffer.toReadBuffer();

      _Logger.trace(
          "SSL unwrap: input=%d bytes, handshakeStatus=%s",
          inputBuffer.remaining(), _SslEngine.getHandshakeStatus());

      SSLEngineResult result = _SslEngine.unwrap(inputBuffer, appInBuffer.toWriteBuffer());
      int consumed = result.bytesConsumed();
      int produced = result.bytesProduced();

      _Logger.trace(
          "SSL unwrap result: status=%s, consumed=%d, produced=%d, handshakeStatus=%s",
          result.getStatus(), consumed, produced, result.getHandshakeStatus());

      switch (result.getStatus()) {
        case OK -> doTask();
        case BUFFER_UNDERFLOW -> {
          _Logger.trace("SSL unwrap BUFFER_UNDERFLOW, need more data");
          if (inputBuffer.hasRemaining()) {
            throw new ZException(
                new IllegalStateException(), "state error, unwrap underflow & input has remain");
          }
          return null;
        }
        case CLOSED -> throw new ZException("SSL connection closed during unwrap");
        case BUFFER_OVERFLOW -> {
          _Logger.warning("SSL unwrap BUFFER_OVERFLOW, app buffer may be too small");
          throw new ZException("SSL unwrap buffer overflow");
        }
      }

      netInBuffer.skip(consumed);
      return produced > 0 ? appInBuffer.seek(produced) : null;
    } catch (SSLException e) {
      _Logger.fetal("SSL unwrap error: %s", e.getMessage());
      throw new ZException(e, "ssl unwrap error");
    }
  }

  /**
   * 获取当前使用的 SSL Provider 类型
   *
   * @return SSL Provider 类型
   */
  public SslProviderFactory.SslProviderType getSslProviderType() {
    return SslProviderFactory.getCurrentProvider();
  }
}
