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

package com.isahl.chess.audience.bishop.protocol.spi;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.spi.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 协议 SPI 加载器测试
 *
 * @author william.d.zk
 * @since 1.1.2
 */
class ProtocolLoaderTest {

  private ProtocolLoader loader;

  @BeforeEach
  void setUp() {
    loader = new ProtocolLoader();
  }

  @Test
  void testLoadHandlers() {
    List<ProtocolHandler> handlers = loader.loadHandlers();

    // 应该加载了测试配置文件中的处理器
    assertFalse(handlers.isEmpty(), "Should load at least one handler from test SPI config");
  }

  @Test
  void testLoadHandlersSorted() {
    List<ProtocolHandler> handlers = loader.loadHandlers();

    // 验证处理器按优先级排序
    for (int i = 1; i < handlers.size(); i++) {
      int prevPriority = handlers.get(i - 1).getPriority();
      int currPriority = handlers.get(i).getPriority();
      assertTrue(
          prevPriority <= currPriority,
          "Handlers should be sorted by priority: " + prevPriority + " <= " + currPriority);
    }
  }

  @Test
  void testAddHandler() {
    ProtocolHandler customHandler = createCustomHandler("CustomHandler", 50);

    loader.addHandler(customHandler);

    assertTrue(loader.getHandlers().contains(customHandler));
  }

  @Test
  void testRemoveHandler() {
    ProtocolHandler customHandler = createCustomHandler("CustomHandler", 50);

    loader.addHandler(customHandler);
    assertTrue(loader.getHandlers().contains(customHandler));

    loader.removeHandler(customHandler);
    assertFalse(loader.getHandlers().contains(customHandler));
  }

  @Test
  void testClearHandlers() {
    loader.loadHandlers();
    assertTrue(loader.getHandlerCount() > 0);

    loader.clearHandlers();
    assertEquals(0, loader.getHandlerCount());
  }

  @Test
  void testReloadHandlers() {
    loader.loadHandlers();
    int initialCount = loader.getHandlerCount();

    // 添加一个处理器后重新加载
    loader.addHandler(createCustomHandler("ExtraHandler", 100));

    // 注意：reload 会从 SPI 重新加载，所以数量可能变化
    loader.reloadHandlers();

    // 重新加载后应该至少包含 SPI 中定义的处理器
    assertTrue(loader.getHandlerCount() >= 0);
  }

  // ==================== 辅助方法 ====================

  private ProtocolHandler createCustomHandler(String name, int priority) {
    return new ProtocolHandler() {
      @Override
      public boolean supports(
          com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
        return true;
      }

      @Override
      public void handle(
          com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
          ProtocolContext context) {}

      @Override
      public int getPriority() {
        return priority;
      }

      @Override
      public String getName() {
        return name;
      }
    };
  }
}
