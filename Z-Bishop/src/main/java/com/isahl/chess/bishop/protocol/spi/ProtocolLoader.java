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

package com.isahl.chess.bishop.protocol.spi;

import com.isahl.chess.king.base.log.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 协议加载器
 *
 * <p>使用 Java SPI 机制加载协议处理器实现。
 *
 * <p>使用方式: 1. 创建 ProtocolHandler 实现类 2. 在
 * META-INF/services/com.isahl.chess.bishop.protocol.spi.ProtocolHandler 中注册 3. 使用
 * ProtocolLoader.loadAll() 加载所有处理器
 *
 * @author william.d.zk
 */
public class ProtocolLoader {

  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop." + ProtocolLoader.class.getSimpleName());

  private static final ProtocolLoader _Instance = new ProtocolLoader();

  private final ConcurrentMap<String, ProtocolHandler> _handlers = new ConcurrentHashMap<>();

  private ProtocolLoader() {}

  /** 获取单例实例 */
  public static ProtocolLoader getInstance() {
    return _Instance;
  }

  /** 加载所有协议处理器（通过 SPI） */
  public void loadAll() {
    ServiceLoader<ProtocolHandler> loader = ServiceLoader.load(ProtocolHandler.class);

    for (ProtocolHandler handler : loader) {
      try {
        handler.init();
        register(handler);
        _Logger.info(
            "Protocol handler loaded: %s (version %s)",
            handler.getProtocolName(), handler.getProtocolVersion());
      } catch (Exception e) {
        _Logger.error("Failed to initialize protocol handler: %s", e.getMessage());
      }
    }

    // 按优先级排序
    List<ProtocolHandler> sorted = new ArrayList<>(_handlers.values());
    sorted.sort(Comparator.comparingInt(ProtocolHandler::getPriority));

    _Logger.info("Loaded %d protocol handlers", sorted.size());
  }

  /** 注册协议处理器 */
  public void register(ProtocolHandler handler) {
    String name = handler.getProtocolName();
    _handlers.put(name, handler);
    _Logger.debug("Protocol handler registered: %s", name);
  }

  /** 根据名称获取协议处理器 */
  public ProtocolHandler getHandler(String protocolName) {
    return _handlers.get(protocolName);
  }

  /** 获取所有已加载的协议处理器 */
  public Collection<ProtocolHandler> getAllHandlers() {
    return Collections.unmodifiableCollection(_handlers.values());
  }

  /**
   * 根据协议签名识别协议
   *
   * @param signature 协议签名字节
   * @return 匹配的协议处理器，未找到返回 null
   */
  public ProtocolHandler identify(byte[] signature) {
    for (ProtocolHandler handler : _handlers.values()) {
      byte[] expected = handler.getProtocolSignature();
      if (matches(signature, expected)) {
        return handler;
      }
    }
    return null;
  }

  /** 检查签名是否匹配 */
  private boolean matches(byte[] signature, byte[] expected) {
    if (expected == null || signature == null) {
      return false;
    }

    if (expected.length > signature.length) {
      return false;
    }

    for (int i = 0; i < expected.length; i++) {
      if (signature[i] != expected[i]) {
        return false;
      }
    }

    return true;
  }

  /** 卸载协议处理器 */
  public void unregister(String protocolName) {
    ProtocolHandler handler = _handlers.remove(protocolName);
    if (handler != null) {
      try {
        handler.destroy();
      } catch (Exception e) {
        _Logger.error("Error destroying protocol handler %s: %s", protocolName, e.getMessage());
      }
      _Logger.info("Protocol handler unregistered: %s", protocolName);
    }
  }

  /** 清除所有处理器 */
  public void clear() {
    for (ProtocolHandler handler : _handlers.values()) {
      try {
        handler.destroy();
      } catch (Exception e) {
        _Logger.error("Error destroying protocol handler: %s", e.getMessage());
      }
    }
    _handlers.clear();
    _Logger.info("All protocol handlers cleared");
  }
}
