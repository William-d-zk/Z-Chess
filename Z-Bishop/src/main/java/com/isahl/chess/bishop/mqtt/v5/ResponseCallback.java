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

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;

/**
 * MQTT v5.0 请求/响应回调接口
 *
 * <p>用于异步接收响应通知。
 *
 * @author william.d.zk
 * @since 1.2.0
 */
public interface ResponseCallback {

  /**
   * 收到响应时调用
   *
   * @param response 响应消息
   */
  void onResponse(X113_QttPublish response);

  /** 请求超时时调用 */
  default void onTimeout() {
    // 默认实现：子类可以覆盖
  }

  /**
   * 发生错误时调用
   *
   * @param error 错误信息
   */
  default void onError(Throwable error) {
    // 默认实现：子类可以覆盖
  }
}
