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

package com.isahl.chess.pawn.endpoint.device.spi.plugin;

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttTopicAlias;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.pawn.endpoint.device.config.MqttV5Config;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol.VERSION_V5_0;

/**
 * MQTT v5.0 增强处理器
 * <p>
 * 处理 MQTT v5.0 特有的连接协商逻辑：
 * <ul>
 *   <li>属性协商（Session Expiry、Receive Maximum、Topic Alias 等）</li>
 *   <li>特性标志（Retain、Wildcard、Shared Subscription 可用性）</li>
 *   <li>主题别名管理器初始化</li>
 *   <li>流量控制配额初始化</li>
 * </ul>
 * </p>
 *
 * @author william.d.zk
 * @see MQttPlugin
 * @see MqttV5Config
 */
@Component
public class MQttPluginV5
{
    private final static Logger _Logger = Logger.getLogger("endpoint.pawn." + MQttPluginV5.class.getSimpleName());

    private final MqttV5Config    _Config;
    private final IDeviceService  _DeviceService;

    @Autowired
    public MQttPluginV5(MqttV5Config config, IDeviceService deviceService)
    {
        _Config = config;
        _DeviceService = deviceService;
        _Logger.info("MQTT v5 Plugin initialized: %s", config);
    }

    /**
     * 处理 MQTT v5.0 连接协商
     *
     * @param x111      CONNECT 报文
     * @param x112      CONNACK 报文（需填充 v5 属性）
     * @param session   会话
     * @param deviceId  设备 ID（输出参数）
     * @return true 如果连接可以接受
     */
    public boolean handleV5Connection(X111_QttConnect x111, X112_QttConnack x112, ISession session, long[] deviceId)
    {
        if (x111.getVersion() != VERSION_V5_0) {
            return false; // 不是 v5 连接，让 v3 处理器处理
        }

        _Logger.debug("Handling MQTT v5 connection from: %s", x111.getClientId());

        // 检查 v5 是否启用
        if (!_Config.isEnabled()) {
            x112.rejectUnsupportedVersion();
            return false;
        }

        // 认证检查
        DeviceEntity device = _DeviceService.findByToken(x111.getClientId());
        if (device == null) {
            x112.rejectIdentifier();
            return false;
        }

        if (!device.getUsername().equalsIgnoreCase(x111.getUserName()) ||
            !device.getPassword().equals(x111.getPassword())) {
            x112.rejectNotAuthorized();
            return false;
        }

        deviceId[0] = device.primaryKey();

        // ==================== v5 属性协商 ====================

        // 1. 会话过期时间协商
        long sessionExpiry = x111.getSessionExpiryInterval();
        long effectiveExpiry = _Config.getEffectiveSessionExpiryInterval(sessionExpiry);
        x112.setSessionExpiryInterval(effectiveExpiry);

        // 2. 接收最大值协商
        int clientReceiveMax = x111.getReceiveMaximum();
        int serverReceiveMax = _Config.getServerReceiveMaximum();
        x112.setReceiveMaximum(serverReceiveMax);

        // 3. 主题别名最大值协商
        int clientTopicAliasMax = x111.getTopicAliasMaximum();
        int effectiveTopicAliasMax = _Config.getEffectiveTopicAliasMaximum(clientTopicAliasMax);
        x112.setTopicAliasMaximum(effectiveTopicAliasMax);

        // 4. 最大报文大小
        int clientMaxPacketSize = x111.getMaximumPacketSize();
        int serverMaxPacketSize = _Config.getMaximumPacketSize();
        if (clientMaxPacketSize > 0 && clientMaxPacketSize < serverMaxPacketSize) {
            // 客户端限制更小，使用客户端的值
            x112.setMaximumPacketSize(clientMaxPacketSize);
        }
        else {
            x112.setMaximumPacketSize(serverMaxPacketSize);
        }

        // 5. 最大 QoS
        x112.setMaximumQoS(_Config.getMaximumQoS());

        // 6. 特性标志
        x112.setRetainAvailable(_Config.isRetainAvailable());
        x112.setWildcardSubscriptionAvailable(_Config.isWildcardSubscriptionAvailable());
        x112.setSubscriptionIdentifierAvailable(_Config.isSubscriptionIdentifierAvailable());
        x112.setSharedSubscriptionAvailable(_Config.isSharedSubscriptionAvailable());

        // 7. 增强认证（如果启用且客户端请求了）
        if (_Config.isEnhancedAuthEnabled()) {
            String authMethod = x111.getAuthenticationMethod();
            if (authMethod != null && _Config.isAuthMethodSupported(authMethod)) {
                // TODO: 启动增强认证流程
                _Logger.debug("Enhanced auth requested: %s", authMethod);
            }
        }

        // 8. 响应信息（如果客户端请求了）
        if (x111.isRequestResponseInformation()) {
            x112.setResponseInformation("response_info_available");
        }

        // ==================== 初始化会话上下文 ====================

        QttContext context = session.getContext(QttContext.class);
        if (context != null) {
            // 初始化 v5 上下文
            context.initV5Context(x112.getProperties());

            // 设置客户端接收最大值到入站配额
            if (clientReceiveMax > 0) {
                context.setInboundQuota(clientReceiveMax);
            }

            // 初始化主题别名管理器（如果需要）
            if (effectiveTopicAliasMax > 0) {
                context.setTopicAliasManager(new QttTopicAlias(effectiveTopicAliasMax));
            }
        }

        x112.responseOk();
        _Logger.info("MQTT v5 connection accepted: client=%s, sessionExpiry=%d, recvMax=%d, topicAliasMax=%d",
            x111.getClientId(), effectiveExpiry, serverReceiveMax, effectiveTopicAliasMax);

        return true;
    }

    /**
     * 检查是否应使用 v5 处理
     */
    public boolean isV5Enabled()
    {
        return _Config.isEnabled();
    }

    /**
     * 获取配置
     */
    public MqttV5Config getConfig()
    {
        return _Config;
    }
}
