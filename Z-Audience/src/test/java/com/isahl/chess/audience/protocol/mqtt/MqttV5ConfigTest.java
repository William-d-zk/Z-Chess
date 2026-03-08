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

import com.isahl.chess.pawn.endpoint.device.config.MqttV5Config;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT v5 配置测试
 *
 * @author william.d.zk
 */
public class MqttV5ConfigTest
{

    @Test
    public void testDefaultConfig()
    {
        MqttV5Config config = new MqttV5Config();

        // 默认启用
        assertTrue(config.isEnabled());
        assertTrue(config.isTopicAliasEnabled());
        assertTrue(config.isMessageExpiryEnabled());
        assertTrue(config.isFlowControlEnabled());
        assertTrue(config.isSharedSubscriptionEnabled());
        assertFalse(config.isEnhancedAuthEnabled()); // 默认关闭

        // 默认值
        assertEquals(0, config.getDefaultSessionExpiryInterval());
        assertEquals(604800, config.getMaxSessionExpiryInterval());
        assertEquals(65535, config.getServerReceiveMaximum());
        assertEquals(0, config.getClientReceiveMaximum());
        assertEquals(65535, config.getServerTopicAliasMaximum());
        assertEquals(0, config.getClientTopicAliasMaximum());
        assertEquals(2, config.getMaximumQoS());
    }

    @Test
    public void testConfigDisabled()
    {
        MqttV5Config config = new MqttV5Config();
        config.setEnabled(false);

        // 禁用后所有特性都返回 false
        assertFalse(config.isTopicAliasEnabled());
        assertFalse(config.isMessageExpiryEnabled());
        assertFalse(config.isFlowControlEnabled());
        assertFalse(config.isSharedSubscriptionEnabled());
        assertFalse(config.isEnhancedAuthEnabled());
        assertFalse(config.isRetainAvailable());
        assertEquals(1, config.getMaximumQoS()); // v3.1.1 只支持 QoS 1
    }

    @Test
    public void testEffectiveValues()
    {
        MqttV5Config config = new MqttV5Config();
        config.setServerTopicAliasMaximum(100);
        config.setClientTopicAliasMaximum(50);

        // 有效的主题别名最大值
        assertEquals(30, config.getEffectiveTopicAliasMaximum(30)); // 客户端请求更小
        assertEquals(50, config.getEffectiveTopicAliasMaximum(100)); // 客户端请求更大，取限制

        // 禁用主题别名
        config.setTopicAliasEnabled(false);
        assertEquals(0, config.getEffectiveTopicAliasMaximum(100));
    }

    @Test
    public void testSessionExpiryConstraints()
    {
        MqttV5Config config = new MqttV5Config();
        config.setMaxSessionExpiryInterval(3600); // 1 小时

        // 在限制范围内
        assertEquals(1800, config.getEffectiveSessionExpiryInterval(1800));

        // 超出限制
        assertEquals(3600, config.getEffectiveSessionExpiryInterval(7200));

        // 0 表示不保留会话
        assertEquals(0, config.getEffectiveSessionExpiryInterval(0));
    }

    @Test
    public void testAuthMethods()
    {
        MqttV5Config config = new MqttV5Config();
        config.setEnhancedAuthEnabled(true);
        config.setSupportedAuthMethods(Arrays.asList("SCRAM-SHA-256", "SCRAM-SHA-1", "KERBEROS"));

        assertTrue(config.isAuthMethodSupported("SCRAM-SHA-256"));
        assertTrue(config.isAuthMethodSupported("KERBEROS"));
        assertFalse(config.isAuthMethodSupported("PLAIN"));
        assertFalse(config.isAuthMethodSupported("OAUTH2"));

        // 禁用增强认证
        config.setEnhancedAuthEnabled(false);
        assertFalse(config.isAuthMethodSupported("SCRAM-SHA-256"));
    }

    @Test
    public void testMaximumQoSConstraints()
    {
        MqttV5Config config = new MqttV5Config();

        // 有效范围
        config.setMaximumQoS(0);
        assertEquals(0, config.getMaximumQoS());

        config.setMaximumQoS(1);
        assertEquals(1, config.getMaximumQoS());

        config.setMaximumQoS(2);
        assertEquals(2, config.getMaximumQoS());

        // 超出范围的值应被限制
        config.setMaximumQoS(3);
        assertEquals(2, config.getMaximumQoS());

        config.setMaximumQoS(-1);
        assertEquals(0, config.getMaximumQoS());
    }

    @Test
    public void testWillDelayConstraints()
    {
        MqttV5Config config = new MqttV5Config();

        assertEquals(86400, config.getMaxWillDelayInterval());
        assertEquals(0, config.getDefaultWillDelayInterval());

        // 设置新值
        config.setMaxWillDelayInterval(3600);
        config.setDefaultWillDelayInterval(300);

        assertEquals(3600, config.getMaxWillDelayInterval());
        assertEquals(300, config.getDefaultWillDelayInterval());
    }

    @Test
    public void testPacketSize()
    {
        MqttV5Config config = new MqttV5Config();

        assertEquals(268435455, config.getMaximumPacketSize()); // 256MB
        assertEquals(65536, config.getDefaultMaximumPacketSize()); // 64KB

        // 设置新值
        config.setMaximumPacketSize(1048576); // 1MB
        assertEquals(1048576, config.getMaximumPacketSize());
    }

    @Test
    public void testPerformanceSettings()
    {
        MqttV5Config config = new MqttV5Config();

        assertEquals(1000, config.getTopicAliasCacheSize());
        assertEquals(1000, config.getMessageExpiryCheckInterval());
        assertEquals(1000, config.getMaxPendingMessages());

        // 设置新值
        config.setTopicAliasCacheSize(500);
        config.setMessageExpiryCheckInterval(500);
        config.setMaxPendingMessages(500);

        assertEquals(500, config.getTopicAliasCacheSize());
        assertEquals(500, config.getMessageExpiryCheckInterval());
        assertEquals(500, config.getMaxPendingMessages());
    }

    @Test
    public void testReauthentication()
    {
        MqttV5Config config = new MqttV5Config();

        assertTrue(config.isReauthenticationAllowed()); // 默认允许

        config.setReauthenticationAllowed(false);
        assertFalse(config.isReauthenticationAllowed());

        // 禁用 v5 后也不允许
        config.setEnabled(false);
        assertFalse(config.isReauthenticationAllowed());
    }

    @Test
    public void testAuthTimeout()
    {
        MqttV5Config config = new MqttV5Config();

        assertEquals(60, config.getAuthTimeoutSeconds());

        config.setAuthTimeoutSeconds(120);
        assertEquals(120, config.getAuthTimeoutSeconds());
    }

    @Test
    public void testToString()
    {
        MqttV5Config config = new MqttV5Config();
        String str = config.toString();

        assertTrue(str.contains("enabled=true"));
        assertTrue(str.contains("recvMax=65535"));
        assertTrue(str.contains("maxQoS=2"));
    }

    @Test
    public void testFeatureAvailability()
    {
        MqttV5Config config = new MqttV5Config();

        // 全部启用
        assertTrue(config.isRetainAvailable());
        assertTrue(config.isWildcardSubscriptionAvailable());
        assertTrue(config.isSubscriptionIdentifierAvailable());
        assertTrue(config.isSharedSubscriptionAvailable());

        // 禁用特定功能
        config.setRetainAvailable(false);
        config.setWildcardSubscriptionAvailable(false);
        config.setSubscriptionIdentifierAvailable(false);
        config.setSharedSubscriptionAvailable(false);

        assertFalse(config.isRetainAvailable());
        assertFalse(config.isWildcardSubscriptionAvailable());
        assertFalse(config.isSubscriptionIdentifierAvailable());
        assertFalse(config.isSharedSubscriptionAvailable());
    }
}
