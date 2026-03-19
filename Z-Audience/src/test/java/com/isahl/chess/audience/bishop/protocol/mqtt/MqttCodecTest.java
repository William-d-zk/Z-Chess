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

package com.isahl.chess.bishop.protocol.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.protocol.mqtt.command.X114_QttPuback;
import com.isahl.chess.bishop.protocol.mqtt.command.X115_QttPubrec;
import com.isahl.chess.bishop.protocol.mqtt.command.X118_QttSubscribe;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** MQTT 编解码测试类 - 测试 suffix/prefix 方法 */
class MqttCodecTest {

  private QttContext createV5Context() {
    MockNetworkOption option = new MockNetworkOption();
    QttContext context = new QttContext(option, ISort.Mode.CLUSTER, ISort.Type.SYMMETRY);
    context.setVersion(QttProtocol.VERSION_V5_0);
    return context;
  }

  private QttContext createV3Context() {
    MockNetworkOption option = new MockNetworkOption();
    QttContext context = new QttContext(option, ISort.Mode.CLUSTER, ISort.Type.SYMMETRY);
    context.setVersion(QttProtocol.VERSION_V3_1_1);
    return context;
  }

  /** 从 suffix 写入后的 buffer 准备读取 */
  private ByteBuf prepareForRead(ByteBuf buffer) {
    // 获取写入的数据长度
    int writerIdx = buffer.writerIdx();
    // 获取底层数组并复制实际写入的数据
    byte[] data = buffer.array();
    return ByteBuf.wrap(Arrays.copyOfRange(data, 0, writerIdx));
  }

  @Test
  void testX114_QttPuback_Codec() {
    X114_QttPuback original = new X114_QttPuback();
    original.msgId(12345);

    // 编码
    ByteBuf buffer = ByteBuf.allocate(64);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X114_QttPuback decoded = new X114_QttPuback();
    decoded.prefix(readBuffer);

    // 验证
    assertThat(decoded.msgId()).isEqualTo(12345);
  }

  @Test
  void testX115_QttPubrec_Codec() {
    X115_QttPubrec original = new X115_QttPubrec();
    original.msgId(42);

    // 编码
    ByteBuf buffer = ByteBuf.allocate(64);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X115_QttPubrec decoded = new X115_QttPubrec();
    decoded.prefix(readBuffer);

    // 验证
    assertThat(decoded.msgId()).isEqualTo(42);
  }

  @Test
  void testX118_QttSubscribe_Codec() {
    X118_QttSubscribe original = new X118_QttSubscribe();
    original.msgId(100);
    original.addSubscribe("topic/a", IQoS.Level.AT_LEAST_ONCE);
    original.addSubscribe("topic/b", IQoS.Level.EXACTLY_ONCE);
    original.addSubscribe("topic/c", IQoS.Level.ALMOST_ONCE);

    // 编码
    ByteBuf buffer = ByteBuf.allocate(256);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X118_QttSubscribe decoded = new X118_QttSubscribe();
    decoded.prefix(readBuffer);

    // 验证
    assertThat(decoded.msgId()).isEqualTo(100);
    assertThat(decoded.getSubscribes()).hasSize(3);
    assertThat(decoded.getSubscribes().get("topic/a")).isEqualTo(IQoS.Level.AT_LEAST_ONCE);
    assertThat(decoded.getSubscribes().get("topic/b")).isEqualTo(IQoS.Level.EXACTLY_ONCE);
    assertThat(decoded.getSubscribes().get("topic/c")).isEqualTo(IQoS.Level.ALMOST_ONCE);
  }

  @Test
  void testX118_QttSubscribe_WildcardTopics_Codec() {
    X118_QttSubscribe original = new X118_QttSubscribe();
    original.msgId(200);
    original.addSubscribe("sensors/+/temperature", IQoS.Level.AT_LEAST_ONCE);
    original.addSubscribe("sensors/#", IQoS.Level.ALMOST_ONCE);

    // 编码
    ByteBuf buffer = ByteBuf.allocate(256);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X118_QttSubscribe decoded = new X118_QttSubscribe();
    decoded.prefix(readBuffer);

    // 验证
    assertThat(decoded.msgId()).isEqualTo(200);
    assertThat(decoded.getSubscribes()).hasSize(2);
    assertThat(decoded.getSubscribes().get("sensors/+/temperature"))
        .isEqualTo(IQoS.Level.AT_LEAST_ONCE);
    assertThat(decoded.getSubscribes().get("sensors/#")).isEqualTo(IQoS.Level.ALMOST_ONCE);
  }

  @Test
  void testRoundTrip_MultiplePuback() {
    // 测试多个消息的往返编解码
    for (int i = 0; i < 5; i++) {
      X114_QttPuback original = new X114_QttPuback();
      original.msgId(1000 + i);

      ByteBuf buffer = ByteBuf.allocate(64);
      original.suffix(buffer);
      ByteBuf readBuffer = prepareForRead(buffer);

      X114_QttPuback decoded = new X114_QttPuback();
      decoded.prefix(readBuffer);

      assertThat(decoded.msgId()).isEqualTo(1000 + i);
    }
  }

  @Test
  void testX113_QttPublish_V5_Codec() {
    // V5 测试 - 注意：setContentType 会创建 _Properties
    X113_QttPublish original = new X113_QttPublish();
    original.wrap(createV5Context());
    original.withTopic("v5/topic");
    original.withSub("v5 payload".getBytes());
    original.msgId(42);
    original.setLevel(IQoS.Level.AT_LEAST_ONCE);

    // 设置 V5 属性会创建 _Properties
    original.setContentType("application/json");

    // 编码
    ByteBuf buffer = ByteBuf.allocate(512);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X113_QttPublish decoded = new X113_QttPublish();
    decoded.wrap(createV5Context());
    decoded.setLevel(IQoS.Level.AT_LEAST_ONCE); // 设置相同的 QoS
    decoded.prefix(readBuffer);
    decoded.fold(readBuffer, readBuffer.readableBytes());

    // 验证基本字段
    assertThat(decoded.topic()).isEqualTo("v5/topic");
    assertThat(decoded.msgId()).isEqualTo(42);
    // V5 属性验证
    assertThat(decoded.getContentType()).isEqualTo("application/json");
  }

  @Test
  void testX113_QttPublish_V3_Basic() {
    // V3 基本测试 - 不设置任何可能创建 _Properties 的属性
    X113_QttPublish original = new X113_QttPublish();
    original.wrap(createV3Context());
    original.withTopic("test/topic");
    original.withSub("test payload".getBytes());
    original.msgId(123);
    original.setLevel(IQoS.Level.AT_LEAST_ONCE);
    // 不调用任何 setXxx 方法，避免创建 _Properties

    // 编码
    ByteBuf buffer = ByteBuf.allocate(512);
    original.suffix(buffer);

    // 准备读取
    ByteBuf readBuffer = prepareForRead(buffer);

    // 解码
    X113_QttPublish decoded = new X113_QttPublish();
    decoded.wrap(createV3Context());
    decoded.setLevel(IQoS.Level.AT_LEAST_ONCE); // 设置相同的 QoS
    decoded.prefix(readBuffer);
    decoded.fold(readBuffer, readBuffer.readableBytes());

    // 验证
    assertThat(decoded.topic()).isEqualTo("test/topic");
    assertThat(decoded.msgId()).isEqualTo(123);
    assertThat(decoded.payload()).containsExactly("test payload".getBytes());
  }
}
