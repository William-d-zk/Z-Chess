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

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProperty;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V3_1_1;
import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT v5.0 属性系统测试
 *
 * @author william.d.zk
 */
public class MqttV5PropertyTest
{

    // ==================== QttProperty 枚举测试 ====================

    @Test
    public void testQttPropertyEnum()
    {
        // 测试属性 ID 正确性
        assertEquals(0x01, QttProperty.PAYLOAD_FORMAT_INDICATOR.getId());
        assertEquals(0x11, QttProperty.SESSION_EXPIRY_INTERVAL.getId());
        assertEquals(0x21, QttProperty.RECEIVE_MAXIMUM.getId());
        assertEquals(0x26, QttProperty.USER_PROPERTY.getId());

        // 测试属性类型
        assertEquals(QttProperty.PropertyType.BYTE, QttProperty.PAYLOAD_FORMAT_INDICATOR.getType());
        assertEquals(QttProperty.PropertyType.FOUR_BYTE_INTEGER, QttProperty.SESSION_EXPIRY_INTERVAL.getType());
        assertEquals(QttProperty.PropertyType.TWO_BYTE_INTEGER, QttProperty.RECEIVE_MAXIMUM.getType());
        assertEquals(QttProperty.PropertyType.UTF_8_STRING_PAIR, QttProperty.USER_PROPERTY.getType());

        // 测试可重复属性
        assertTrue(QttProperty.USER_PROPERTY.isRepeatable());
        assertTrue(QttProperty.SUBSCRIPTION_IDENTIFIER.isRepeatable());
        assertFalse(QttProperty.SESSION_EXPIRY_INTERVAL.isRepeatable());
        assertFalse(QttProperty.RECEIVE_MAXIMUM.isRepeatable());

        // 测试根据 ID 查找属性
        assertEquals(QttProperty.SESSION_EXPIRY_INTERVAL, QttProperty.valueOf(0x11));
        assertEquals(QttProperty.RECEIVE_MAXIMUM, QttProperty.valueOf(0x21));
        assertNull(QttProperty.valueOf(0xFF)); // 未知属性
    }

    // ==================== QttPropertySet 编解码测试 ====================

    @Test
    public void testQttPropertySetEncodeDecode()
    {
        QttPropertySet original = new QttPropertySet();
        original.setSessionExpiryInterval(3600); // 1小时
        original.setReceiveMaximum(100);
        original.setTopicAliasMaximum(16);
        original.setMaximumPacketSize(65535);
        original.addUserProperty("env", "test");
        original.addUserProperty("client", "java");

        // 编码
        ByteBuf buffer = ByteBuf.allocate(256);
        original.encode(buffer, VERSION_V5_0);

        // 解码
        buffer.flip();
        QttPropertySet decoded = new QttPropertySet();
        decoded.decode(buffer, VERSION_V5_0);

        // 验证
        assertEquals(3600, decoded.getSessionExpiryInterval());
        assertEquals(100, decoded.getReceiveMaximum());
        assertEquals(16, decoded.getTopicAliasMaximum());
        assertEquals(65535, decoded.getMaximumPacketSize());
        assertEquals(2, decoded.getUserProperties().size());
    }

    @Test
    public void testQttPropertySetV3Compatibility()
    {
        QttPropertySet props = new QttPropertySet();
        props.setSessionExpiryInterval(3600);

        // v3.1.1 编码应不输出任何内容
        ByteBuf buffer = ByteBuf.allocate(256);
        props.encode(buffer, VERSION_V3_1_1);
        assertEquals(0, buffer.writerIndex());

        // v3.1.1 解码应不解析任何内容
        buffer.put(new byte[]{0x01, 0x02, 0x03}); // 模拟数据
        buffer.flip();
        QttPropertySet decoded = new QttPropertySet();
        decoded.decode(buffer, VERSION_V3_1_1);
        assertTrue(decoded.isEmpty());
    }

    @Test
    public void testQttPropertySetEmpty()
    {
        QttPropertySet props = new QttPropertySet();
        assertTrue(props.isEmpty());

        ByteBuf buffer = ByteBuf.allocate(256);
        props.encode(buffer, VERSION_V5_0);

        // 空属性应编码为单个零字节（属性长度）
        buffer.flip();
        assertEquals(1, buffer.readableBytes());
        assertEquals(0, buffer.get());
    }

    // ==================== X111_QttConnect v5 测试 ====================

    @Test
    public void testConnectV5Properties()
    {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion(VERSION_V5_0);
        connect.setClientId("test-client-v5");
        connect.setUserName("testuser");
        connect.setPassword("testpass123");
        connect.setClean();

        // 设置 v5 属性
        connect.setSessionExpiryInterval(7200);
        connect.setReceiveMaximum(50);
        connect.setTopicAliasMaximum(32);
        connect.setMaximumPacketSize(131072);
        connect.setRequestProblemInformation(true);
        connect.setRequestResponseInformation(false);

        // 验证属性
        assertTrue(connect.isV5());
        assertEquals(7200, connect.getSessionExpiryInterval());
        assertEquals(50, connect.getReceiveMaximum());
        assertEquals(32, connect.getTopicAliasMaximum());
        assertEquals(131072, connect.getMaximumPacketSize());
        assertTrue(connect.isRequestProblemInformation());
        assertFalse(connect.isRequestResponseInformation());
    }

    @Test
    public void testConnectV5EncodeDecode()
    {
        X111_QttConnect original = new X111_QttConnect();
        original.setVersion(VERSION_V5_0);
        original.setClientId("test-client");
        original.setUserName("user");
        original.setPassword("pass");
        original.setClean();
        original.setKeepAlive(60);
        original.setSessionExpiryInterval(3600);
        original.setReceiveMaximum(100);
        original.setTopicAliasMaximum(16);

        // 编码
        ByteBuf buffer = original.encode();

        // 解码
        buffer.flip();
        X111_QttConnect decoded = new X111_QttConnect();
        decoded.prefix(buffer);
        decoded.fold(buffer, buffer.readableBytes());

        // 验证基础字段
        assertEquals(VERSION_V5_0, decoded.getVersion());
        assertEquals("test-client", decoded.getClientId());
        assertEquals("user", decoded.getUserName());
        assertEquals("pass", decoded.getPassword());
        assertTrue(decoded.isClean());

        // 验证 v5 属性
        assertEquals(3600, decoded.getSessionExpiryInterval());
        assertEquals(100, decoded.getReceiveMaximum());
        assertEquals(16, decoded.getTopicAliasMaximum());
    }

    @Test
    public void testConnectV3Compatibility()
    {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion(VERSION_V3_1_1);
        connect.setClientId("test-client-v3");
        connect.setUserName("user");
        connect.setPassword("pass");
        connect.setClean();
        connect.setKeepAlive(30);

        // 编码
        ByteBuf buffer = connect.encode();

        // 解码
        buffer.flip();
        X111_QttConnect decoded = new X111_QttConnect();
        decoded.prefix(buffer);
        decoded.fold(buffer, buffer.readableBytes());

        // 验证
        assertEquals(VERSION_V3_1_1, decoded.getVersion());
        assertEquals("test-client-v3", decoded.getClientId());
        assertFalse(decoded.isV5());
    }

    @Test
    public void testConnectV5WithAuth()
    {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion(VERSION_V5_0);
        connect.setClientId("auth-client");
        connect.setAuthenticationMethod("SCRAM-SHA-256");
        connect.setAuthenticationData(new byte[]{0x01, 0x02, 0x03, 0x04});

        assertEquals("SCRAM-SHA-256", connect.getAuthenticationMethod());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, connect.getAuthenticationData());
    }

    // ==================== X112_QttConnack v5 测试 ====================

    @Test
    public void testConnackV5Properties()
    {
        X112_QttConnack connack = new X112_QttConnack();
        // 模拟 v5 上下文
        connack.wrap(new com.isahl.chess.bishop.protocol.mqtt.model.QttContext(
            new com.isahl.chess.queen.config.SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        connack.context().setVersion(VERSION_V5_0);

        connack.responseOk();
        connack.setPresent();

        // 设置 v5 属性
        connack.setReceiveMaximum(200);
        connack.setTopicAliasMaximum(64);
        connack.setMaximumQoS(1);
        connack.setRetainAvailable(true);
        connack.setWildcardSubscriptionAvailable(true);
        connack.setSharedSubscriptionAvailable(true);
        connack.setSessionExpiryInterval(3600);
        connack.setServerKeepAlive(120);
        connack.setAssignedClientIdentifier("assigned-id-123");
        connack.setReasonString("Connection accepted");

        // 验证
        assertTrue(connack.isV5());
        assertTrue(connack.isOk());
        assertTrue(connack.isPresent());
        assertEquals(200, connack.getReceiveMaximum());
        assertEquals(64, connack.getTopicAliasMaximum());
        assertEquals(1, connack.getMaximumQoS());
        assertTrue(connack.isRetainAvailable());
        assertTrue(connack.isWildcardSubscriptionAvailable());
        assertTrue(connack.isSharedSubscriptionAvailable());
        assertEquals(3600, connack.getSessionExpiryInterval());
        assertEquals(120, connack.getServerKeepAlive());
        assertEquals("assigned-id-123", connack.getAssignedClientIdentifier());
        assertEquals("Connection accepted", connack.getReasonString());
    }

    @Test
    public void testConnackV5RejectWithReason()
    {
        X112_QttConnack connack = new X112_QttConnack();
        connack.wrap(new com.isahl.chess.bishop.protocol.mqtt.model.QttContext(
            new com.isahl.chess.queen.config.SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        connack.context().setVersion(VERSION_V5_0);

        connack.rejectWithReason(
            com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt.REJECT_BAD_USER_OR_PASSWORD,
            "Invalid credentials provided"
        );

        assertTrue(connack.isReject());
        assertEquals("Invalid credentials provided", connack.getReasonString());
    }

    @Test
    public void testConnackV5ServerRedirect()
    {
        X112_QttConnack connack = new X112_QttConnack();
        connack.wrap(new com.isahl.chess.bishop.protocol.mqtt.model.QttContext(
            new com.isahl.chess.queen.config.SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        connack.context().setVersion(VERSION_V5_0);

        connack.rejectWithReason(
            com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt.USE_ANOTHER_SERVER,
            "Redirecting to backup server"
        );
        connack.setServerReference("backup.mqtt.example.com:1883");

        assertEquals("backup.mqtt.example.com:1883", connack.getServerReference());
    }

    @Test
    public void testConnackV5EncodeDecode()
    {
        X112_QttConnack original = new X112_QttConnack();
        original.wrap(new com.isahl.chess.bishop.protocol.mqtt.model.QttContext(
            new com.isahl.chess.queen.config.SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        original.context().setVersion(VERSION_V5_0);

        original.responseOk();
        original.setPresent();
        original.setReceiveMaximum(150);
        original.setTopicAliasMaximum(32);
        original.setMaximumQoS(2);

        // 编码
        ByteBuf buffer = original.encode();

        // 解码
        buffer.flip();
        X112_QttConnack decoded = new X112_QttConnack();
        decoded.wrap(original.context());
        decoded.prefix(buffer);

        // 验证
        assertTrue(decoded.isOk());
        assertTrue(decoded.isPresent());
        assertEquals(150, decoded.getReceiveMaximum());
        assertEquals(32, decoded.getTopicAliasMaximum());
        assertEquals(2, decoded.getMaximumQoS());
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testPropertyValidation()
    {
        QttPropertySet props = new QttPropertySet();

        // 测试非法值类型
        assertThrows(IllegalArgumentException.class, () -> {
            props.setProperty(QttProperty.SESSION_EXPIRY_INTERVAL, "invalid");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            props.setProperty(QttProperty.RECEIVE_MAXIMUM, "invalid");
        });

        // 测试可重复属性不能使用 setProperty
        assertThrows(IllegalArgumentException.class, () -> {
            props.setProperty(QttProperty.USER_PROPERTY, "value");
        });
    }

    @Test
    public void testConnectDefaultValues()
    {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion(VERSION_V5_0);

        // 默认值测试
        assertEquals(65535, connect.getReceiveMaximum()); // 默认最大值
        assertEquals(0, connect.getTopicAliasMaximum());   // 默认 0 表示不支持
        assertEquals(0, connect.getMaximumPacketSize());   // 默认 0 表示不限制
        assertEquals(0, connect.getSessionExpiryInterval()); // 默认 0 表示随连接结束
        assertTrue(connect.isRequestProblemInformation());   // 默认 true
        assertFalse(connect.isRequestResponseInformation()); // 默认 false
    }

    @Test
    public void testConnackDefaultValues()
    {
        X112_QttConnack connack = new X112_QttConnack();
        connack.wrap(new com.isahl.chess.bishop.protocol.mqtt.model.QttContext(
            new com.isahl.chess.queen.config.SocketConfig(),
            com.isahl.chess.queen.io.core.features.model.session.ISort.Mode.CLUSTER,
            com.isahl.chess.queen.io.core.features.model.session.ISort.Type.SYMMETRY
        ));
        connack.context().setVersion(VERSION_V5_0);

        // 默认值测试
        assertEquals(65535, connack.getReceiveMaximum());
        assertEquals(0, connack.getTopicAliasMaximum());
        assertEquals(0, connack.getMaximumPacketSize());
        assertEquals(2, connack.getMaximumQoS()); // 默认支持 QoS 2
        assertTrue(connack.isRetainAvailable());
        assertTrue(connack.isWildcardSubscriptionAvailable());
        assertTrue(connack.isSubscriptionIdentifierAvailable());
        assertTrue(connack.isSharedSubscriptionAvailable());
    }
}
