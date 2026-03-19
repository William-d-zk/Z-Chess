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

import com.isahl.chess.bishop.protocol.mqtt.command.X118_QttSubscribe;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** X118_QttSubscribe 测试类 */
class X118_QttSubscribeTest {

  @Test
  void testBasicSubscribe() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    subscribe.msgId(12345);
    subscribe.addSubscribe("test/topic", IQoS.Level.AT_LEAST_ONCE);

    assertThat(subscribe.msgId()).isEqualTo(12345);
    assertThat(subscribe.getSubscribes()).containsKey("test/topic");
    assertThat(subscribe.getSubscribes().get("test/topic")).isEqualTo(IQoS.Level.AT_LEAST_ONCE);
  }

  @Test
  void testMultiTopicSubscribe() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    subscribe.msgId(42);
    subscribe.addSubscribe("topic/a", IQoS.Level.ALMOST_ONCE);
    subscribe.addSubscribe("topic/b", IQoS.Level.AT_LEAST_ONCE);
    subscribe.addSubscribe("topic/c", IQoS.Level.EXACTLY_ONCE);

    Map<String, IQoS.Level> subscribes = subscribe.getSubscribes();
    assertThat(subscribes).hasSize(3);
    assertThat(subscribes.get("topic/a")).isEqualTo(IQoS.Level.ALMOST_ONCE);
    assertThat(subscribes.get("topic/b")).isEqualTo(IQoS.Level.AT_LEAST_ONCE);
    assertThat(subscribes.get("topic/c")).isEqualTo(IQoS.Level.EXACTLY_ONCE);
  }

  @Test
  void testWildcardTopics() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    subscribe.msgId(1);

    subscribe.addSubscribe("test/+", IQoS.Level.AT_LEAST_ONCE);
    subscribe.addSubscribe("test/#", IQoS.Level.ALMOST_ONCE);
    subscribe.addSubscribe("sensor/+/temperature", IQoS.Level.EXACTLY_ONCE);

    assertThat(subscribe.getSubscribes()).hasSize(3);
    assertThat(subscribe.getSubscribes()).containsKeys("test/+", "test/#", "sensor/+/temperature");
  }

  @Test
  void testSharedSubscription() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    subscribe.msgId(1);

    // 共享订阅主题
    subscribe.addSubscribe("$share/group1/sensors/+", IQoS.Level.AT_LEAST_ONCE);

    assertThat(subscribe.getSubscribes()).containsKey("$share/group1/sensors/+");
  }

  @Test
  void testPriority() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    assertThat(subscribe.priority()).isEqualTo(X118_QttSubscribe.QOS_PRIORITY_06_META_CREATE);
  }

  @Test
  void testSerial() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    assertThat(subscribe.serial()).isEqualTo(0x118);
  }

  @Test
  void testIsMapping() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    assertThat(subscribe.isMapping()).isTrue();
  }

  @Test
  void testToStringFormat() {
    X118_QttSubscribe subscribe = new X118_QttSubscribe();
    subscribe.msgId(42);
    subscribe.addSubscribe("topic/a", IQoS.Level.AT_LEAST_ONCE);

    String str = subscribe.toString();
    assertThat(str).contains("subscribe");
    assertThat(str).contains("42");
    assertThat(str).contains("topic/a");
  }
}
