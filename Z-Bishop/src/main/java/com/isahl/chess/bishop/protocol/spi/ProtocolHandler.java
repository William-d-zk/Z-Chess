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

import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * 协议处理器接口
 *
 * <p>实现此接口可以自定义协议处理逻辑。协议处理器通过 SPI 机制加载，并按优先级排序执行。
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * public class MyProtocolHandler implements ProtocolHandler {
 *     @Override
 *     public boolean supports(IProtocol message) {
 *         return message instanceof MyCustomMessage;
 *     }
 *
 *     @Override
 *     public void handle(IProtocol message, ProtocolContext context) {
 *         // 处理消息
 *     }
 *
 *     @Override
 *     public int getPriority() {
 *         return 100; // 优先级（越小越优先）
 *     }
 * }
 * }</pre>
 *
 * @author william.d.zk
 * @since 1.1.2
 * @see ProtocolLoader
 * @see ProtocolChain
 */
@FunctionalInterface
public interface ProtocolHandler {

  /**
   * 检查是否支持处理此消息
   *
   * @param message 待处理的消息
   * @return true 如果此处理器可以处理该消息
   */
  boolean supports(IProtocol message);

  /**
   * 处理消息
   *
   * @param message 待处理的消息
   * @param context 处理上下文
   */
  default void handle(IProtocol message, ProtocolContext context) {
    // 默认实现：子类可以覆盖
  }

  /**
   * 处理消息前钩子
   *
   * @param message 待处理的消息
   * @param context 处理上下文
   * @return true 如果继续处理，false 如果跳过此处理器
   */
  default boolean beforeHandle(IProtocol message, ProtocolContext context) {
    return true;
  }

  /**
   * 处理消息后钩子
   *
   * @param message 已处理的消息
   * @param context 处理上下文
   * @param result 处理结果
   */
  default void afterHandle(IProtocol message, ProtocolContext context, ProtocolResult result) {
    // 默认实现：子类可以覆盖
  }

  /**
   * 处理器优先级
   *
   * <p>值越小优先级越高，优先执行。
   *
   * @return 优先级值，默认 100
   */
  default int getPriority() {
    return 100;
  }

  /**
   * 处理器名称
   *
   * @return 处理器名称，默认类名
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * 处理器描述
   *
   * @return 处理器描述
   */
  default String getDescription() {
    return "";
  }
}
