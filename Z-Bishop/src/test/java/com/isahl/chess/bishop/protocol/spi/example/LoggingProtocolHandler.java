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

package com.isahl.chess.bishop.protocol.spi.example;

import com.isahl.chess.bishop.protocol.spi.ProtocolContext;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.bishop.protocol.spi.ProtocolResult;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志记录协议处理器示例
 *
 * <p>示例处理器，记录所有经过的消息信息。
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class LoggingProtocolHandler implements ProtocolHandler {

  private static final Logger _Logger =
      LoggerFactory.getLogger(
          "protocol.bishop.spi.example." + LoggingProtocolHandler.class.getSimpleName());

  @Override
  public boolean supports(IProtocol message) {
    // 支持所有消息类型
    return true;
  }

  @Override
  public void handle(IProtocol message, ProtocolContext context) {
    _Logger.debug(
        "[Logging] Message type: {}, Session: {}, Size: {}",
        message.getClass().getSimpleName(),
        context.getSessionId(),
        message.sizeOf());
  }

  @Override
  public void afterHandle(IProtocol message, ProtocolContext context, ProtocolResult result) {
    _Logger.debug(
        "[Logging] Message processed in {} ms, Result: {}", context.getElapsedTime(), result);
  }

  @Override
  public int getPriority() {
    // 最高优先级，优先执行
    return 0;
  }

  @Override
  public String getName() {
    return "LoggingProtocolHandler";
  }

  @Override
  public String getDescription() {
    return "Logs all protocol messages for debugging";
  }
}
