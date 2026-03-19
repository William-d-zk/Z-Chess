/*
 * MIT License
 *
 * Copyright (c) 2022~2022. Z-Chess
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

package com.isahl.chess.audience.start;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import org.junit.jupiter.api.Test;

/** Application 基础测试 简化版本，不依赖完整 Spring 上下文 */
class ApplicationAudienceTest {
  @Test
  void test() {
    // 测试 LogEntry 基本创建
    LogEntry logEntry = new LogEntry(1, 0, 0xc000000000000000L, 0x5f6291e987000L, 0x123, null);

    assertNotNull(logEntry);
    assertEquals(1, logEntry.index());
    assertEquals(0, logEntry.term());
  }
}
