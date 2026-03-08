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

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * X111_QttConnect 测试类
 */
class X111_QttConnectTest {

    @Test
    void testBasicConnect() {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setClientId("test-client-123");
        connect.setVersion((byte) 4);
        
        assertThat(connect.getClientId()).isEqualTo("test-client-123");
        assertThat(connect.getVersion()).isEqualTo(4);
    }

    @Test
    void testCleanSession() {
        X111_QttConnect connect = new X111_QttConnect();
        
        connect.setClean();
        assertThat(connect.isClean()).isTrue();
    }

    @Test
    void testKeepAlive() {
        X111_QttConnect connect = new X111_QttConnect();
        
        connect.setKeepAlive(60);
        assertThat(connect.getKeepAlive()).isEqualTo(60000); // 转换为毫秒
        
        connect.setKeepAlive(300);
        assertThat(connect.getKeepAlive()).isEqualTo(300000);
    }

    @Test
    void testWillMessage() {
        X111_QttConnect connect = new X111_QttConnect();
        
        connect.setWill(X111_QttConnect.Level.AT_LEAST_ONCE, true);
        connect.setWillTopic("will/topic");
        connect.setWillMessage("client disconnected".getBytes());
        
        assertThat(connect.hasWill()).isTrue();
        assertThat(connect.getWillTopic()).isEqualTo("will/topic");
        assertThat(connect.getWillMessage()).containsExactly("client disconnected".getBytes());
        assertThat(connect.isWillRetain()).isTrue();
    }

    @Test
    void testUserNameAndPassword() {
        X111_QttConnect connect = new X111_QttConnect();
        
        connect.setUserName("admin");
        connect.setPassword("password123");
        
        assertThat(connect.getUserName()).isEqualTo("admin");
        assertThat(connect.getPassword()).isEqualTo("password123");
    }

    @Test
    void testV5Properties() {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion((byte) 5);
        
        connect.setSessionExpiryInterval(3600);
        connect.setReceiveMaximum(65535);
        connect.setMaximumPacketSize(1048576);
        connect.setTopicAliasMaximum(256);
        
        assertThat(connect.getSessionExpiryInterval()).isEqualTo(3600);
        assertThat(connect.getReceiveMaximum()).isEqualTo(65535);
        assertThat(connect.getMaximumPacketSize()).isEqualTo(1048576);
        assertThat(connect.getTopicAliasMaximum()).isEqualTo(256);
    }

    @Test
    void testV5Auth() {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion((byte) 5);
        
        connect.setAuthenticationMethod("SCRAM-SHA-256");
        connect.setAuthenticationData(new byte[]{0x01, 0x02, 0x03});
        
        assertThat(connect.getAuthenticationMethod()).isEqualTo("SCRAM-SHA-256");
        assertThat(connect.getAuthenticationData()).containsExactly(0x01, 0x02, 0x03);
    }

    @Test
    void testV5UserProperties() {
        X111_QttConnect connect = new X111_QttConnect();
        connect.setVersion((byte) 5);
        
        connect.getProperties().addUserProperty("device-type", "sensor");
        connect.getProperties().addUserProperty("location", "building-a");
        
        assertThat(connect.getProperties().getUserProperties()).hasSize(2);
    }

    @Test
    void testIsMapping() {
        X111_QttConnect connect = new X111_QttConnect();
        assertThat(connect.isMapping()).isTrue();
    }
}
