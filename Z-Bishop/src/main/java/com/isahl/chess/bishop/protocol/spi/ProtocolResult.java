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

/**
 * 协议处理结果
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public enum ProtocolResult {
  /** 继续后续处理 */
  CONTINUE,

  /** 处理完成，跳过后续处理器 */
  COMPLETED,

  /** 跳过当前处理器 */
  SKIP,

  /** 处理失败 */
  FAILED;

  private String reason;

  /** 是否继续后续处理 */
  public boolean shouldContinue() {
    return this == CONTINUE || this == SKIP;
  }

  /** 获取失败原因 */
  public String getReason() {
    return reason;
  }

  /** 是否失败 */
  public boolean isFailed() {
    return this == FAILED;
  }

  /** 创建带原因的失败结果 */
  public static ProtocolResult failed(String reason) {
    ProtocolResult result = FAILED;
    result.reason = reason;
    return result;
  }
}
