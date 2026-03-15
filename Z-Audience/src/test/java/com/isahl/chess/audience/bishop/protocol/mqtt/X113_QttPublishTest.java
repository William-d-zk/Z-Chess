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

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * X113_QttPublish 测试类
 */
class X113_QttPublishTest {

    private QttContext createV5Context() {
        MockNetworkOption option = new MockNetworkOption();
        QttContext context = new QttContext(option, ISort.Mode.CLUSTER, ISort.Type.SYMMETRY);
        context.setVersion(QttProtocol.VERSION_V5_0);
        return context;
    }

    @Test
    void testBasicPublish() {
        X113_QttPublish publish = new X113_QttPublish();
        publish.withTopic("test/topic");
        publish.withSub("Hello MQTT".getBytes());
        
        assertThat(publish.topic()).isEqualTo("test/topic");
        assertThat(publish.payload()).containsExactly("Hello MQTT".getBytes());
        assertThat(publish.priority()).isEqualTo(X113_QttPublish.QOS_PRIORITY_07_ROUTE_MESSAGE);
    }

    @Test
    void testV5Properties() {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(createV5Context());
        
        publish.withTopic("v5/topic");
        publish.setMessageExpiryInterval(3600);
        publish.setContentType("application/json");
        publish.setPayloadFormatIndicator(1);
        
        assertThat(publish.isV5()).isTrue();
        assertThat(publish.getMessageExpiryInterval()).isEqualTo(3600);
        assertThat(publish.getContentType()).isEqualTo("application/json");
        assertThat(publish.getPayloadFormatIndicator()).isEqualTo(1);
        assertThat(publish.isPayloadUtf8()).isTrue();
    }

    @Test
    void testTopicAlias() {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(createV5Context());
        
        publish.withTopic("original/topic");
        publish.useTopicAlias(5);
        
        assertThat(publish.getTopicAlias()).isEqualTo(5);
        assertThat(publish.topic()).isNull(); // 主题名被清空
    }

    @Test
    void testMessageExpiry() {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(createV5Context());
        
        // 未设置时默认不过期
        assertThat(publish.isExpired()).isFalse();
        
        // 设置过期时间
        publish.setMessageExpiryInterval(300);
        // 注意：isExpired 的实现可能需要考虑当前时间
    }

    @Test
    void testPayloadFormatIndicator() {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(createV5Context());
        
        // 0 = 未指定字节流
        publish.setPayloadFormatIndicator(0);
        assertThat(publish.isPayloadUtf8()).isFalse();
        
        // 1 = UTF-8 字符串
        publish.setPayloadFormatIndicator(1);
        assertThat(publish.isPayloadUtf8()).isTrue();
    }

    @Test
    void testDuplicate() {
        X113_QttPublish original = new X113_QttPublish();
        QttContext context = createV5Context();
        original.wrap(context);
        
        original.withTopic("test/topic");
        original.withSub("payload".getBytes());
        original.setLevel(X113_QttPublish.Level.AT_LEAST_ONCE);
        original.setContentType("application/json");
        
        X113_QttPublish copy = original.duplicate();
        
        assertThat(copy.topic()).isEqualTo(original.topic());
        assertThat(copy.payload()).containsExactly(original.payload());
        assertThat(copy.level()).isEqualTo(original.level());
        assertThat(copy.getContentType()).isEqualTo(original.getContentType());
    }

    @Test
    void testToStringFormat() {
        X113_QttPublish publish = new X113_QttPublish();
        QttContext context = createV5Context();
        publish.wrap(context);
        
        publish.withTopic("test/topic");
        publish.withSub("test".getBytes());
        publish.setMessageExpiryInterval(3600);
        
        String str = publish.toString();
        assertThat(str).contains("X113 publish");
        assertThat(str).contains("test/topic");
    }

    @Test
    void testUserProperties() {
        X113_QttPublish publish = new X113_QttPublish();
        QttContext context = createV5Context();
        publish.wrap(context);
        
        publish.addUserProperty("key1", "value1");
        publish.addUserProperty("key2", "value2");
        
        assertThat(publish.getProperties().getUserProperties()).hasSize(2);
    }

    @Test
    void testResponseTopicAndCorrelationData() {
        X113_QttPublish publish = new X113_QttPublish();
        QttContext context = createV5Context();
        publish.wrap(context);
        
        publish.setResponseTopic("response/topic");
        publish.setCorrelationData(new byte[]{0x01, 0x02, 0x03});
        
        assertThat(publish.getResponseTopic()).isEqualTo("response/topic");
        assertThat(publish.getCorrelationData()).containsExactly(0x01, 0x02, 0x03);
    }

    @Test
    void testSerial() {
        X113_QttPublish publish = new X113_QttPublish();
        assertThat(publish.serial()).isEqualTo(0x113);
    }
}
