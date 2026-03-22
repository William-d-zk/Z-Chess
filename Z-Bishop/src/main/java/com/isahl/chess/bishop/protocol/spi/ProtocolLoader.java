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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 协议处理器 SPI 加载器
 *
 * <p>通过 Java SPI 机制加载所有 {@link ProtocolHandler} 实现，并按优先级排序。
 *
 * <p>使用方式：
 *
 * <ol>
 *   <li>创建 ProtocolHandler 实现类
 *   <li>在 META-INF/services/com.isahl.chess.bishop.protocol.spi.ProtocolHandler 文件中添加实现类全名
 *   <li>使用 ProtocolLoader 加载处理器
 * </ol>
 *
 * @author william.d.zk
 * @since 1.1.2
 * @see ProtocolHandler
 */
public class ProtocolLoader {

  private static final Logger _Logger =
      Logger.getLogger("protocol.bishop.spi." + ProtocolLoader.class.getSimpleName());

  /** SPI 服务文件路径 */
  public static final String SPI_PATH =
      "META-INF/services/com.isahl.chess.bishop.protocol.spi.ProtocolHandler";

  /** 已加载的处理器列表 */
  private final List<ProtocolHandler> _handlers = new CopyOnWriteArrayList<>();

  /** 是否已加载 */
  private volatile boolean _loaded = false;

  // ==================== 加载方法 ====================

  /**
   * 加载所有协议处理器
   *
   * @return 按优先级排序的处理器列表
   */
  public List<ProtocolHandler> loadHandlers() {
    if (_loaded) {
      return List.copyOf(_handlers);
    }

    synchronized (this) {
      if (_loaded) {
        return List.copyOf(_handlers);
      }

      doLoadHandlers();
      _loaded = true;
      return List.copyOf(_handlers);
    }
  }

  /** 重新加载处理器 */
  public synchronized void reloadHandlers() {
    _handlers.clear();
    _loaded = false;
    loadHandlers();
    _Logger.info("Reloaded {} protocol handlers", _handlers.size());
  }

  /** 执行加载 */
  private void doLoadHandlers() {
    ServiceLoader<ProtocolHandler> serviceLoader = ServiceLoader.load(ProtocolHandler.class);

    for (ProtocolHandler handler : serviceLoader) {
      try {
        _handlers.add(handler);
        _Logger.debug(
            "Loaded protocol handler: {} (priority={})", handler.getName(), handler.getPriority());
      } catch (Exception e) {
        _Logger.warning("Failed to load protocol handler: {}", e.getMessage());
      }
    }

    // 按优先级排序
    _handlers.sort(Comparator.comparingInt(ProtocolHandler::getPriority));

    _Logger.info("Loaded {} protocol handlers", _handlers.size());
  }

  // ==================== 处理器管理 ====================

  /**
   * 手动添加处理器
   *
   * @param handler 处理器
   */
  public void addHandler(ProtocolHandler handler) {
    Objects.requireNonNull(handler, "Handler cannot be null");

    _handlers.add(handler);
    _handlers.sort(Comparator.comparingInt(ProtocolHandler::getPriority));

    _Logger.debug(
        "Added protocol handler: {} (priority={})", handler.getName(), handler.getPriority());
  }

  /** 移除处理器 */
  public void removeHandler(ProtocolHandler handler) {
    _handlers.remove(handler);
    _Logger.debug("Removed protocol handler: {}", handler.getName());
  }

  /** 获取所有处理器 */
  public List<ProtocolHandler> getHandlers() {
    return List.copyOf(_handlers);
  }

  /** 获取处理器数量 */
  public int getHandlerCount() {
    return _handlers.size();
  }

  /** 清空所有处理器 */
  public void clearHandlers() {
    _handlers.clear();
    _loaded = false;
    _Logger.info("Cleared all protocol handlers");
  }

  // ==================== 查找方法 ====================

  /**
   * 查找支持指定消息类型的处理器
   *
   * @param message 消息
   * @return 支持的处理器列表
   */
  public List<ProtocolHandler> findHandlersFor(Object message) {
    List<ProtocolHandler> result = new ArrayList<>();

    for (ProtocolHandler handler : _handlers) {
      try {
        if (handler.supports(
            (com.isahl.chess.queen.io.core.features.model.content.IProtocol) message)) {
          result.add(handler);
        }
      } catch (ClassCastException e) {
        // 类型不匹配，跳过
      } catch (Exception e) {
        _Logger.warning("Error checking handler support: {}", e.getMessage());
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return String.format("ProtocolLoader{handlers=%d, loaded=%s}", _handlers.size(), _loaded);
  }
}
