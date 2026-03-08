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

import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider;
import com.isahl.bishop.protocol.mqtt.v5.ctrl.X11F_QttAuth;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.pawn.endpoint.device.service.QttSharedSubscriptionManager;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT v5.0 高级特性测试
 * - 增强认证
 * - 共享订阅
 *
 * @author william.d.zk
 */
public class MqttV5AdvancedFeaturesTest
{

    // ==================== 增强认证测试 ====================

    @Test
    public void testAuthPacketCreation()
    {
        X11F_QttAuth auth = new X11F_QttAuth();

        // 默认成功状态
        assertTrue(auth.isSuccess());
        assertEquals(0x00, auth.getReasonCode());

        // 设置继续认证
        auth.setContinueAuthentication();
        assertTrue(auth.isContinueAuthentication());
        assertEquals(0x18, auth.getReasonCode());

        // 设置重新认证
        auth.setReauthenticate();
        assertTrue(auth.isReauthenticate());
        assertEquals(0x19, auth.getReasonCode());
    }

    @Test
    public void testAuthPacketProperties()
    {
        X11F_QttAuth auth = new X11F_QttAuth();

        auth.setAuthMethod("SCRAM-SHA-256");
        auth.setAuthData(new byte[]{0x01, 0x02, 0x03, 0x04});
        auth.setReasonString("Authentication in progress");
        auth.addUserProperty("step", "challenge");

        assertEquals("SCRAM-SHA-256", auth.getAuthMethod());
        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x04}, auth.getAuthData());
        assertEquals("Authentication in progress", auth.getReasonString());
        assertEquals(1, auth.getProperties().getUserProperties().size());
    }

    @Test
    public void testAuthResult()
    {
        // 成功结果
        IQttAuthProvider.AuthResult success = IQttAuthProvider.AuthResult.success(
            new byte[]{0x01, 0x02}
        );
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertArrayEquals(new byte[]{0x01, 0x02}, success.getAuthData());

        // 继续结果
        IQttAuthProvider.AuthResult cont = IQttAuthProvider.AuthResult.Continue(
            new byte[]{0x03, 0x04}
        );
        assertTrue(cont.isContinue());
        assertFalse(cont.isSuccess());

        // 失败结果
        IQttAuthProvider.AuthResult failure = IQttAuthProvider.AuthResult.failure("Invalid credentials");
        assertTrue(failure.isFailure());
        assertEquals("Invalid credentials", failure.getReason());
    }

    @Test
    public void testAuthContext()
    {
        IQttAuthProvider.AuthContext context = new IQttAuthProvider.AuthContext(
            0x12345678L, "client-001", "SCRAM-SHA-256"
        );

        assertEquals(0x12345678L, context.getSessionId());
        assertEquals("client-001", context.getClientId());
        assertEquals("SCRAM-SHA-256", context.getAuthMethod());
        assertEquals(0, context.getStep());

        context.nextStep();
        assertEquals(1, context.getStep());

        // 保存状态
        context.setState("challenge-data");
        assertEquals("challenge-data", context.getState());
    }

    // ==================== 共享订阅测试 ====================

    @Test
    public void testSharedSubscriptionParsing()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        // 有效格式
        assertTrue(manager.isSharedSubscription("$share/group1/topic/a"));
        String[] parsed = manager.parseSharedSubscription("$share/group1/topic/a");
        assertNotNull(parsed);
        assertEquals("group1", parsed[0]);
        assertEquals("topic/a", parsed[1]);

        // 多级主题
        String[] parsed2 = manager.parseSharedSubscription("$share/my-group/sensor/+/temperature");
        assertEquals("my-group", parsed2[0]);
        assertEquals("sensor/+/temperature", parsed2[1]);

        // 无效格式
        assertFalse(manager.isSharedSubscription("topic/a"));
        assertNull(manager.parseSharedSubscription("$share/")); // 缺少主题
        assertNull(manager.parseSharedSubscription("$share/group-only")); // 缺少主题
    }

    @Test
    public void testSharedSubscriptionSubscribeUnsubscribe()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        // 订阅
        boolean result = manager.subscribe(
            "$share/group1/topic/a",
            0x100L,
            IQoS.Level.AT_LEAST_ONCE
        );
        assertTrue(result);
        assertEquals(1, manager.getGroupCount());
        assertEquals(1, manager.getSharedSubscriptionCount());

        // 同一组再次订阅（不同会话）
        manager.subscribe("$share/group1/topic/a", 0x101L, IQoS.Level.EXACTLY_ONCE);
        assertEquals(1, manager.getGroupCount()); // 组数量不变
        assertEquals(2, manager.getSharedSubscriptionCount());

        // 不同组
        manager.subscribe("$share/group2/topic/a", 0x102L, IQoS.Level.ALMOST_ONCE);
        assertEquals(2, manager.getGroupCount());
        assertEquals(3, manager.getSharedSubscriptionCount());

        // 取消订阅
        assertTrue(manager.unsubscribe("$share/group1/topic/a", 0x100L));
        assertEquals(2, manager.getSharedSubscriptionCount());
    }

    @Test
    public void testSharedSubscriptionRoundRobin()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        // 创建共享组，3 个成员
        manager.subscribe("$share/rr-group/topic/data", 0x100L, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/rr-group/topic/data", 0x101L, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/rr-group/topic/data", 0x102L, IQoS.Level.AT_LEAST_ONCE);

        // 轮询选择
        List<QttSharedSubscriptionManager.SelectedSubscriber> subscribers1 =
            manager.selectSubscribers("topic/data");
        assertEquals(1, subscribers1.size());
        long first = subscribers1.get(0).getSessionId();

        List<QttSharedSubscriptionManager.SelectedSubscriber> subscribers2 =
            manager.selectSubscribers("topic/data");
        assertEquals(1, subscribers2.size());
        long second = subscribers2.get(0).getSessionId();

        // 应该轮询到不同成员
        assertNotEquals(first, second);

        // 第三次应该轮到第三个
        List<QttSharedSubscriptionManager.SelectedSubscriber> subscribers3 =
            manager.selectSubscribers("topic/data");
        assertEquals(1, subscribers3.size());

        // 第四次应该回到第一个
        List<QttSharedSubscriptionManager.SelectedSubscriber> subscribers4 =
            manager.selectSubscribers("topic/data");
        assertEquals(first, subscribers4.get(0).getSessionId());
    }

    @Test
    public void testSharedSubscriptionTopicMatching()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        // 带通配符的共享订阅
        manager.subscribe("$share/wild-group/sensor/+/temp", 0x100L, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/wild-group/sensor/+/humidity", 0x101L, IQoS.Level.AT_LEAST_ONCE);

        // 匹配 temp
        List<QttSharedSubscriptionManager.SelectedSubscriber> subs1 =
            manager.selectSubscribers("sensor/room1/temp");
        assertEquals(1, subs1.size());

        // 匹配 humidity
        List<QttSharedSubscriptionManager.SelectedSubscriber> subs2 =
            manager.selectSubscribers("sensor/room2/humidity");
        assertEquals(1, subs2.size());

        // 不匹配
        List<QttSharedSubscriptionManager.SelectedSubscriber> subs3 =
            manager.selectSubscribers("sensor/room1/pressure");
        assertEquals(0, subs3.size());
    }

    @Test
    public void testSharedSubscriptionSessionCleanup()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        long sessionId = 0x100L;
        manager.subscribe("$share/g1/topic/a", sessionId, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/g2/topic/b", sessionId, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/g3/topic/c", sessionId, IQoS.Level.AT_LEAST_ONCE);

        assertEquals(3, manager.getSessionGroups(sessionId).size());

        // 清理会话
        manager.clearSessionSubscriptions(sessionId);

        assertEquals(0, manager.getSessionGroups(sessionId).size());
        assertEquals(0, manager.getGroupCount()); // 所有组都已空被删除
    }

    @Test
    public void testSharedSubscriptionStatistics()
    {
        QttSharedSubscriptionManager manager = new QttSharedSubscriptionManager();

        manager.subscribe("$share/g1/topic/a", 0x100L, IQoS.Level.AT_LEAST_ONCE);
        manager.subscribe("$share/g2/topic/b", 0x101L, IQoS.Level.EXACTLY_ONCE);

        String stats = manager.getStatistics();
        assertTrue(stats.contains("groups=2"));
        assertTrue(stats.contains("subscriptions=2"));
    }
}
