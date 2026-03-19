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

import com.isahl.chess.bishop.protocol.mqtt.command.X114_QttPuback;
import org.junit.jupiter.api.Test;

/** X114_QttPuback 测试类 */
class X114_QttPubackTest {

  @Test
  void testBasicPuback() {
    X114_QttPuback puback = new X114_QttPuback();
    puback.msgId(12345);

    assertThat(puback.msgId()).isEqualTo(12345);
  }

  @Test
  void testWithPayload() {
    X114_QttPuback puback = new X114_QttPuback();
    puback.msgId(12345);
    puback.withSub("extra data".getBytes());

    assertThat(puback.msgId()).isEqualTo(12345);
    assertThat(puback.payload()).containsExactly("extra data".getBytes());
  }

  @Test
  void testPriority() {
    X114_QttPuback puback = new X114_QttPuback();
    assertThat(puback.priority()).isEqualTo(X114_QttPuback.QOS_PRIORITY_09_CONFIRM_MESSAGE);
  }

  @Test
  void testSerial() {
    X114_QttPuback puback = new X114_QttPuback();
    assertThat(puback.serial()).isEqualTo(0x114);
  }

  @Test
  void testToStringFormat() {
    X114_QttPuback puback = new X114_QttPuback();
    puback.msgId(42);

    String str = puback.toString();
    assertThat(str).contains("puback");
    assertThat(str).contains("42");
  }
}
