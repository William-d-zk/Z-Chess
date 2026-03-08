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

package com.isahl.chess.audience.protocol.mqtt;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.bishop.protocol.mqtt.model.QttTopicAlias;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.config.SocketConfig;
import org.junit.jupiter.api.Test;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT v5.0 核心特性测试
 * - 主题别名
 * - PUBLISH 报文 v5
 * - 流量控制
 * - 消息过期
 *
 * @author william.d.zk
 */
public class MqttV5CoreFeaturesTest
{

    // ==================== 主题别名管理器测试 ====================

    @Test
    public void testTopicAliasManagerBasics()
    {
        QttTopicAlias aliasManager = new QttTopicAlias(16);

        assertTrue(aliasManager.isEnabled());
        assertEquals(16, aliasManager.getMaxAliasId());

        // 测试 C2S 注册和解析
        aliasManager.registerClientToServer(1, "sensor/temperature");
        assertTrue(aliasManager.hasClientToServerAlias(1));
        assertEquals("sensor/temperature", aliasManager.resolveClientToServer(1));

        // 测试别名超出范围
        assertThrows(IllegalArgumentException.class, () -> {
            aliasManager.registerClientToServer(17, "sensor/humidity");
        });

        // 测试无效别名
        assertThrows(IllegalArgumentException.class, () -> {
            aliasManager.registerClientToServer(0, "sensor/pressure");
        });
    }

    @Test
    public void testTopicAliasManagerS2C()
    {
        QttTopicAlias aliasManager = new QttTopicAlias(10);

        // 第一次获取应创建新别名
        int alias1 = aliasManager.getOrCreateServerToClient("device/001/status");
        assertTrue(alias1 > 0);
        assertEquals(1, alias1);

        // 第二次获取应返回相同别名
        int alias2 = aliasManager.getOrCreateServerToClient("device/001/status");
        assertEquals(alias1, alias2);

        // 新主题应分配新别名
        int alias3 = aliasManager.getOrCreateServerToClient("device/002/status");
        assertEquals(2, alias3);

        // 解析别名
        assertEquals("device/001/status", aliasManager.resolveServerToClient(alias1));
        assertEquals("device/002/status", aliasManager.resolveServerToClient(alias3));
    }

    @Test
    public void testTopicAliasManagerMaxLimit()
    {
        QttTopicAlias aliasManager = new QttTopicAlias(3);

        // 创建最大数量的别名
        aliasManager.getOrCreateServerToClient("topic/1");
        aliasManager.getOrCreateServerToClient("topic/2");
        aliasManager.getOrCreateServerToClient("topic/3");

        // 超出限制应返回 0
        int alias4 = aliasManager.getOrCreateServerToClient("topic/4");
        assertEquals(0, alias4);
    }

    @Test
    public void testTopicAliasManagerDisabled()
    {
        QttTopicAlias aliasManager = new QttTopicAlias(0);

        assertFalse(aliasManager.isEnabled());
        assertEquals(0, aliasManager.getOrCreateServerToClient("any/topic"));
        assertNull(aliasManager.resolveClientToServer(1));
    }

    @Test
    public void testTopicAliasStatistics()
    {
        QttTopicAlias aliasManager = new QttTopicAlias(10);

        aliasManager.registerClientToServer(1, "test/topic");
        aliasManager.resolveClientToServer(1);
        aliasManager.resolveClientToServer(1);

        assertEquals(1, aliasManager.getAliasCreatedCount());
        assertEquals(2, aliasManager.getAliasHitCount());
        assertTrue(aliasManager.getStatistics().contains("hits=2"));
    }

    // ==================== PUBLISH v5 测试 ====================

    @Test
    public void testPublishV5Properties()
    {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        publish.context().setVersion(VERSION_V5_0);

        publish.withTopic("test/topic");
        publish.setLevel(com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.AT_LEAST_ONCE);

        // 设置 v5 属性
        publish.setMessageExpiryInterval(3600);
        publish.setContentType("application/json");
        publish.setPayloadFormatIndicator(1);
        publish.setResponseTopic("test/response");
        publish.setCorrelationData(new byte[]{0x01, 0x02, 0x03});
        publish.addUserProperty("env", "test");
        publish.addUserProperty("version", "1.0");

        // 验证
        assertEquals(3600L, publish.getMessageExpiryInterval());
        assertEquals("application/json", publish.getContentType());
        assertEquals(1, publish.getPayloadFormatIndicator());
        assertTrue(publish.isPayloadUtf8());
        assertEquals("test/response", publish.getResponseTopic());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, publish.getCorrelationData());
        assertEquals(2, publish.getProperties().getUserProperties().size());
    }

    @Test
    public void testPublishV5TopicAlias()
    {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        publish.context().setVersion(VERSION_V5_0);

        // 初始状态
        publish.withTopic("sensor/temperature");
        assertEquals("sensor/temperature", publish.topic());
        assertNull(publish.getTopicAlias());

        // 使用主题别名
        publish.useTopicAlias(1);
        assertEquals(1, publish.getTopicAlias());
        assertNull(publish.topic()); // 主题名被清空
    }

    @Test
    public void testPublishV5MessageExpiry()
    {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        publish.context().setVersion(VERSION_V5_0);

        // 未设置过期时间
        assertNull(publish.getMessageExpiryInterval());
        assertFalse(publish.isExpired());

        // 设置过期时间
        publish.setMessageExpiryInterval(300);
        assertEquals(300L, publish.getMessageExpiryInterval());
        assertFalse(publish.isExpired());

        // 设置为 0 表示立即过期
        publish.setMessageExpiryInterval(0);
        assertTrue(publish.isExpired());
    }

    @Test
    public void testPublishV5EncodeDecode()
    {
        X113_QttPublish original = new X113_QttPublish();
        original.wrap(new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        original.context().setVersion(VERSION_V5_0);

        original.withTopic("device/data");
        original.setLevel(com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.AT_LEAST_ONCE);
        original.msgId(12345);
        original.withSub("{\"temp\":25.5}".getBytes());
        original.setMessageExpiryInterval(600);
        original.setContentType("application/json");
        original.setPayloadFormatIndicator(1);

        // 编码
        ByteBuf buffer = original.encode();

        // 解码
        buffer.flip();
        X113_QttPublish decoded = new X113_QttPublish();
        decoded.wrap(original.context());
        decoded.prefix(buffer);
        decoded.fold(buffer, buffer.readableBytes());

        // 验证
        assertEquals("device/data", decoded.topic());
        assertEquals(12345, decoded.msgId());
        assertEquals(600L, decoded.getMessageExpiryInterval());
        assertEquals("application/json", decoded.getContentType());
        assertEquals(1, decoded.getPayloadFormatIndicator());
        assertArrayEquals("{\"temp\":25.5}".getBytes(), decoded.payload());
    }

    @Test
    public void testPublishV3Compatibility()
    {
        X113_QttPublish publish = new X113_QttPublish();
        publish.wrap(new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        publish.context().setVersion(4); // v3.1.1

        publish.withTopic("test/topic");
        publish.setLevel(com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE);
        publish.withSub("payload".getBytes());

        // 编码（不应包含 v5 属性）
        ByteBuf buffer = publish.encode();

        // 解码
        buffer.flip();
        X113_QttPublish decoded = new X113_QttPublish();
        decoded.wrap(publish.context());
        decoded.prefix(buffer);
        decoded.fold(buffer, buffer.readableBytes());

        assertEquals("test/topic", decoded.topic());
        assertFalse(decoded.isV5());
    }

    // ==================== 流量控制测试 ====================

    @Test
    public void testFlowControlBasics()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(VERSION_V5_0);
        context.setReceiveMaximum(5);

        // 初始状态应可以发送
        assertTrue(context.canSendQoSMessage());
        assertEquals(5, context.getRemainingSendQuota());

        // 获取配额
        assertTrue(context.acquireSendQuota());
        assertEquals(4, context.getRemainingSendQuota());

        // 获取所有配额
        for (int i = 0; i < 4; i++) {
            assertTrue(context.acquireSendQuota());
        }
        assertEquals(0, context.getRemainingSendQuota());

        // 配额耗尽
        assertFalse(context.canSendQoSMessage());
        assertFalse(context.acquireSendQuota());
    }

    @Test
    public void testFlowControlRelease()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(VERSION_V5_0);
        context.setReceiveMaximum(3);

        // 使用配额
        context.acquireSendQuota();
        context.acquireSendQuota();
        assertEquals(1, context.getRemainingSendQuota());

        // 释放配额
        context.releaseSendQuota();
        assertEquals(2, context.getRemainingSendQuota());

        // 释放超过最大值的配额（不应溢出）
        context.releaseSendQuota();
        context.releaseSendQuota();
        context.releaseSendQuota();
        assertEquals(3, context.getRemainingSendQuota()); // 应限制为最大值
    }

    @Test
    public void testFlowControlV3Compatibility()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(4); // v3.1.1

        // v3.1.1 应无限制
        assertTrue(context.canSendQoSMessage());
        assertTrue(context.acquireSendQuota());
        assertEquals(-1, context.getRemainingSendQuota()); // -1 表示无限制
    }

    @Test
    public void testMaxPacketSize()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(VERSION_V5_0);
        context.setMaximumPacketSize(1024);

        assertTrue(context.isPacketSizeValid(512));
        assertTrue(context.isPacketSizeValid(1024));
        assertFalse(context.isPacketSizeValid(1025));
        assertEquals(1024, context.getAllowedMaxPacketSize());

        // 未设置限制
        context.setMaximumPacketSize(0);
        assertTrue(context.isPacketSizeValid(999999));
    }

    // ==================== QttContext v5 初始化测试 ====================

    @Test
    public void testContextV5Init()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(VERSION_V5_0);

        QttPropertySet connackProps = new QttPropertySet();
        connackProps.setReceiveMaximum(100);
        connackProps.setTopicAliasMaximum(32);
        connackProps.setMaximumPacketSize(65535);
        connackProps.setSessionExpiryInterval(3600);
        connackProps.setMaximumQoS(1);
        connackProps.setRetainAvailable(false);
        connackProps.setSharedSubscriptionAvailable(false);

        context.initV5Context(connackProps);

        assertEquals(100, context.getReceiveMaximum());
        assertTrue(context.isTopicAliasEnabled());
        assertEquals(32, context.getTopicAliasManager().getMaxAliasId());
        assertEquals(65535, context.getMaximumPacketSize());
        assertEquals(3600, context.getSessionExpiryInterval());
        assertEquals(1, context.getMaximumQoS());
        assertFalse(context.isRetainAvailable());
        assertFalse(context.isSharedSubscriptionAvailable());
    }

    @Test
    public void testContextInboundQuota()
    {
        QttContext context = new QttContext(
            new SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        );
        context.setVersion(VERSION_V5_0);
        context.setInboundQuota(10);

        assertEquals(10, context.getInboundQuota());
        assertTrue(context.canReceiveQoSMessage());
    }
}
