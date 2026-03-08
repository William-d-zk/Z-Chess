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

import com.isahl.chess.bishop.protocol.mqtt.command.X115_QttPubrec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * X115_QttPubrec 测试类
 */
class X115_QttPubrecTest {

    @Test
    void testBasicPubrec() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        pubrec.msgId(12345);
        
        assertThat(pubrec.msgId()).isEqualTo(12345);
    }

    @Test
    void testTarget() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        pubrec.target(123456789L);
        
        assertThat(pubrec.target()).isEqualTo(123456789L);
    }

    @Test
    void testTopic() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        assertThat(pubrec.topic()).isNull(); // PUBREC 没有主题
    }

    @Test
    void testWithPayload() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        pubrec.msgId(12345);
        pubrec.withSub("extra data".getBytes());
        
        assertThat(pubrec.msgId()).isEqualTo(12345);
        assertThat(pubrec.payload()).containsExactly("extra data".getBytes());
    }

    @Test
    void testPriority() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        assertThat(pubrec.priority()).isEqualTo(X115_QttPubrec.QOS_PRIORITY_09_CONFIRM_MESSAGE);
    }

    @Test
    void testSerial() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        assertThat(pubrec.serial()).isEqualTo(0x115);
    }

    @Test
    void testToStringFormat() {
        X115_QttPubrec pubrec = new X115_QttPubrec();
        pubrec.msgId(42);
        
        String str = pubrec.toString();
        assertThat(str).contains("pubrec");
        assertThat(str).contains("42");
    }
}
