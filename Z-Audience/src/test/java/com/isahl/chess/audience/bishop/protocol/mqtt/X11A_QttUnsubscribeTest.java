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

import com.isahl.chess.bishop.protocol.mqtt.command.X11A_QttUnsubscribe;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * X11A_QttUnsubscribe 测试类
 */
class X11A_QttUnsubscribeTest {

    @Test
    void testBasicUnsubscribe() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        unsubscribe.msgId(12345);
        unsubscribe.setTopics("test/topic");
        
        assertThat(unsubscribe.msgId()).isEqualTo(12345);
        List<String> topics = unsubscribe.getTopics();
        assertThat(topics).contains("test/topic");
        assertThat(topics).hasSize(1);
    }

    @Test
    void testMultiTopicUnsubscribe() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        unsubscribe.msgId(42);
        unsubscribe.setTopics("topic/a", "topic/b", "topic/c");
        
        List<String> topics = unsubscribe.getTopics();
        assertThat(topics).hasSize(3);
        assertThat(topics).contains("topic/a", "topic/b", "topic/c");
    }

    @Test
    void testWildcardTopics() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        unsubscribe.msgId(1);
        
        unsubscribe.setTopics("test/+", "test/#", "sensor/+/temperature");
        
        assertThat(unsubscribe.getTopics()).hasSize(3);
        assertThat(unsubscribe.getTopics()).contains("test/+", "test/#", "sensor/+/temperature");
    }

    @Test
    void testSharedSubscriptionUnsubscribe() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        unsubscribe.msgId(1);
        
        // 取消共享订阅
        unsubscribe.setTopics("$share/group1/sensors/+");
        
        assertThat(unsubscribe.getTopics()).contains("$share/group1/sensors/+");
    }

    @Test
    void testPriority() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        assertThat(unsubscribe.priority()).isEqualTo(X11A_QttUnsubscribe.QOS_PRIORITY_06_META_CREATE);
    }

    @Test
    void testSerial() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        assertThat(unsubscribe.serial()).isEqualTo(0x11A);
    }

    @Test
    void testToStringFormat() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        unsubscribe.msgId(42);
        unsubscribe.setTopics("topic/a", "topic/b");
        
        String str = unsubscribe.toString();
        assertThat(str).contains("unsubscribe");
        assertThat(str).contains("42");
    }

    @Test
    void testIsMapping() {
        X11A_QttUnsubscribe unsubscribe = new X11A_QttUnsubscribe();
        assertThat(unsubscribe.isMapping()).isTrue();
    }
}
