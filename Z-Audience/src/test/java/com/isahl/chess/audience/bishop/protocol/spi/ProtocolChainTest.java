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

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.protocol.spi.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 协议处理链测试
 *
 * @author william.d.zk
 * @since 1.1.2
 */
class ProtocolChainTest {

  private ProtocolChain chain;

  @BeforeEach
  void setUp() {
    chain = new ProtocolChain();
  }

  @Test
  void testEmptyChain() {
    // 创建空链
    ProtocolLoader emptyLoader = new ProtocolLoader();
    emptyLoader.clearHandlers();
    ProtocolChain emptyChain = new ProtocolChain(emptyLoader);

    // 处理消息
    X113_QttPublish message = createSimpleMessage();
    ProtocolResult result = emptyChain.process(message, null);

    assertNotNull(result);
  }

  @Test
  void testProcessOrder() {
    AtomicInteger counter = new AtomicInteger(0);
    AtomicInteger firstOrder = new AtomicInteger(-1);
    AtomicInteger secondOrder = new AtomicInteger(-1);

    // 添加两个处理器，测试执行顺序
    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            firstOrder.set(counter.incrementAndGet());
          }

          @Override
          public int getPriority() {
            return 10; // 较低优先级，后执行
          }
        });

    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            secondOrder.set(counter.incrementAndGet());
          }

          @Override
          public int getPriority() {
            return 1; // 较高优先级，先执行
          }
        });

    // 处理消息
    chain.process(createSimpleMessage(), null);

    // 验证执行顺序
    assertEquals(1, secondOrder.get(), "Higher priority handler should execute first");
    assertEquals(2, firstOrder.get(), "Lower priority handler should execute second");
  }

  @Test
  void testHandlerNotSupportingMessage() {
    AtomicBoolean executed = new AtomicBoolean(false);

    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return false; // 不支持任何消息
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            executed.set(true);
          }
        });

    chain.process(createSimpleMessage(), null);

    assertFalse(executed.get(), "Handler should not be executed for unsupported message");
  }

  @Test
  void testBeforeHandleHook() {
    AtomicBoolean beforeExecuted = new AtomicBoolean(false);
    AtomicBoolean handleExecuted = new AtomicBoolean(false);

    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public boolean beforeHandle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            beforeExecuted.set(true);
            return true;
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            handleExecuted.set(true);
          }
        });

    chain.process(createSimpleMessage(), null);

    assertTrue(beforeExecuted.get());
    assertTrue(handleExecuted.get());
  }

  @Test
  void testBeforeHandleSkip() {
    AtomicBoolean handleExecuted = new AtomicBoolean(false);

    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public boolean beforeHandle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            return false; // 返回 false 跳过处理
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            handleExecuted.set(true);
          }
        });

    chain.process(createSimpleMessage(), null);

    assertFalse(handleExecuted.get(), "Handler should be skipped when beforeHandle returns false");
  }

  @Test
  void testAfterHandleHook() {
    AtomicBoolean afterExecuted = new AtomicBoolean(false);

    chain.addHandler(
        new ProtocolHandler() {
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
          public void afterHandle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context,
              ProtocolResult result) {
            afterExecuted.set(true);
          }
        });

    chain.process(createSimpleMessage(), null);

    assertTrue(afterExecuted.get());
  }

  @Test
  void testContextAttributes() {
    chain.addHandler(
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {
            context.setAttribute("testKey", "testValue");
          }
        });

    X113_QttPublish message = createSimpleMessage();
    ProtocolResult result = chain.process(message, null);

    // 注意：process 方法不返回 context，但可以通过其他方式验证
    // 这里主要验证不抛出异常
    assertNotNull(result);
  }

  @Test
  void testStatistics() {
    // 处理多条消息
    for (int i = 0; i < 5; i++) {
      chain.process(createSimpleMessage(), null);
    }

    ProtocolChain.ChainStatistics stats = chain.getStatistics();

    assertEquals(5, stats.getTotalCount());
    assertEquals(5, stats.getSuccessCount());
    assertEquals(0, stats.getFailureCount());
  }

  @Test
  void testAddAndRemoveHandler() {
    ProtocolHandler handler =
        new ProtocolHandler() {
          @Override
          public boolean supports(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message) {
            return true;
          }

          @Override
          public void handle(
              com.isahl.chess.queen.io.core.features.model.content.IProtocol message,
              ProtocolContext context) {}
        };

    int initialCount = chain.getHandlerCount();

    chain.addHandler(handler);
    assertEquals(initialCount + 1, chain.getHandlerCount());

    chain.removeHandler(handler);
    assertEquals(initialCount, chain.getHandlerCount());
  }

  // ==================== 辅助方法 ====================

  private X113_QttPublish createSimpleMessage() {
    X113_QttPublish publish = new X113_QttPublish();
    publish.setResponseTopic("test/topic");
    publish.setCorrelationData("test payload".getBytes());
    return publish;
  }
}
