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

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11F_QttAuth;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * X11F_QttAuth 测试类
 */
class X11F_QttAuthTest {

    @Test
    void testDefaultState() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        assertThat(auth.getReasonCode()).isEqualTo(X11F_QttAuth.REASON_SUCCESS);
        assertThat(auth.isSuccess()).isTrue();
        assertThat(auth.isContinueAuthentication()).isFalse();
        assertThat(auth.isReauthenticate()).isFalse();
    }

    @Test
    void testReasonCodes() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        // 继续认证
        auth.setContinueAuthentication();
        assertThat(auth.getReasonCode()).isEqualTo(X11F_QttAuth.REASON_CONTINUE_AUTHENTICATION);
        assertThat(auth.isContinueAuthentication()).isTrue();
        assertThat(auth.isSuccess()).isFalse();
        
        // 重新认证
        auth.setReauthenticate();
        assertThat(auth.getReasonCode()).isEqualTo(X11F_QttAuth.REASON_REAUTHENTICATE);
        assertThat(auth.isReauthenticate()).isTrue();
        
        // 成功
        auth.setSuccess();
        assertThat(auth.getReasonCode()).isEqualTo(X11F_QttAuth.REASON_SUCCESS);
        assertThat(auth.isSuccess()).isTrue();
    }

    @Test
    void testAuthMethod() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        auth.setAuthMethod("SCRAM-SHA-256");
        assertThat(auth.getAuthMethod()).isEqualTo("SCRAM-SHA-256");
        
        auth.setAuthMethod("KERBEROS");
        assertThat(auth.getAuthMethod()).isEqualTo("KERBEROS");
    }

    @Test
    void testAuthData() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        byte[] data = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        auth.setAuthData(data);
        assertThat(auth.getAuthData()).containsExactly(0x01, 0x02, 0x03, 0x04, 0x05);
    }

    @Test
    void testReasonString() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        auth.setReasonString("Authentication in progress");
        assertThat(auth.getReasonString()).isEqualTo("Authentication in progress");
        
        auth.setReasonString("Challenge required");
        assertThat(auth.getReasonString()).isEqualTo("Challenge required");
    }

    @Test
    void testUserProperties() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        auth.addUserProperty("step", "challenge");
        auth.addUserProperty("algorithm", "SCRAM-SHA-256");
        
        assertThat(auth.getProperties().getUserProperties()).hasSize(2);
    }

    @Test
    void testPropertiesCaching() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        // 设置属性
        auth.setAuthMethod("SCRAM-SHA-256");
        auth.setAuthData(new byte[]{0x01, 0x02});
        auth.setReasonString("test");
        
        // 获取属性应该返回缓存值
        assertThat(auth.getAuthMethod()).isEqualTo("SCRAM-SHA-256");
        assertThat(auth.getAuthData()).containsExactly(0x01, 0x02);
        assertThat(auth.getReasonString()).isEqualTo("test");
    }

    @Test
    void testSetProperties() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet props = 
            new com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet();
        props.setAuthenticationMethod("KERBEROS");
        props.setAuthenticationData(new byte[]{0x03, 0x04});
        props.setReasonString("Kerberos auth");
        
        auth.setProperties(props);
        
        assertThat(auth.getAuthMethod()).isEqualTo("KERBEROS");
        assertThat(auth.getAuthData()).containsExactly(0x03, 0x04);
        assertThat(auth.getReasonString()).isEqualTo("Kerberos auth");
    }

    @Test
    void testLength() {
        X11F_QttAuth auth = new X11F_QttAuth();
        auth.setSuccess();
        
        int emptyLength = auth.length();
        assertThat(emptyLength).isEqualTo(2); // reason code + empty properties
        
        // 添加属性后长度应该增加
        auth.setAuthMethod("SCRAM-SHA-256");
        auth.setAuthData(new byte[]{0x01, 0x02});
        
        int withPropsLength = auth.length();
        assertThat(withPropsLength).isGreaterThan(emptyLength);
    }

    @Test
    void testToStringFormat() {
        X11F_QttAuth auth = new X11F_QttAuth();
        auth.setContinueAuthentication();
        auth.setAuthMethod("SCRAM-SHA-256");
        auth.setAuthData(new byte[]{0x01, 0x02});
        
        String str = auth.toString();
        assertThat(str).contains("X11F auth");
        assertThat(str).contains("0x18");
        assertThat(str).contains("Continue Authentication");
        assertThat(str).contains("SCRAM-SHA-256");
        assertThat(str).contains("2bytes");
    }

    @Test
    void testGetPropertiesLazyInitialization() {
        X11F_QttAuth auth = new X11F_QttAuth();
        
        // getProperties 应该延迟初始化
        assertThat(auth.getProperties()).isNotNull();
        
        // 修改返回的属性集合应该影响对象
        auth.getProperties().setAuthenticationMethod("TEST");
        assertThat(auth.getAuthMethod()).isEqualTo("TEST");
    }

    @Test
    void testSerial() {
        X11F_QttAuth auth = new X11F_QttAuth();
        assertThat(auth.serial()).isEqualTo(0x11F);
    }
}
