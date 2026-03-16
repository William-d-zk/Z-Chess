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

package com.isahl.chess.pawn.endpoint.device.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 会话配置
 *
 * @author william.d.zk
 */
@Configuration
@ConfigurationProperties(prefix = "z.chess.mqtt.session")
public class MqttSessionConfig
{
    /**
     * 默认会话过期时间（秒）
     * <p>0 表示会话随连接断开而结束</p>
     */
    private long defaultSessionExpiryInterval = 0;

    /**
     * 最大会话过期时间（秒）
     */
    private long maxSessionExpiryInterval = 604800; // 7 天

    /**
     * 最大遗嘱延迟时间（秒）
     */
    private long maxWillDelayInterval = 86400; // 1 天

    /**
     * 默认遗嘱延迟时间（秒）
     */
    private long defaultWillDelayInterval = 0;

    /**
     * 服务端接收最大值（Receive Maximum）
     * <p>限制同时处理的 QoS 1/2 消息数量</p>
     */
    private int serverReceiveMaximum = 65535;

    /**
     * 要求客户端的接收最大值
     * <p>0 表示不限制客户端</p>
     */
    private int clientReceiveMaximum = 0;

    public long getDefaultSessionExpiryInterval()
    {
        return defaultSessionExpiryInterval;
    }

    public void setDefaultSessionExpiryInterval(long defaultSessionExpiryInterval)
    {
        this.defaultSessionExpiryInterval = defaultSessionExpiryInterval;
    }

    public long getMaxSessionExpiryInterval()
    {
        return maxSessionExpiryInterval;
    }

    public void setMaxSessionExpiryInterval(long maxSessionExpiryInterval)
    {
        this.maxSessionExpiryInterval = maxSessionExpiryInterval;
    }

    public long getMaxWillDelayInterval()
    {
        return maxWillDelayInterval;
    }

    public void setMaxWillDelayInterval(long maxWillDelayInterval)
    {
        this.maxWillDelayInterval = maxWillDelayInterval;
    }

    public long getDefaultWillDelayInterval()
    {
        return defaultWillDelayInterval;
    }

    public void setDefaultWillDelayInterval(long defaultWillDelayInterval)
    {
        this.defaultWillDelayInterval = defaultWillDelayInterval;
    }

    public int getServerReceiveMaximum()
    {
        return serverReceiveMaximum;
    }

    public void setServerReceiveMaximum(int serverReceiveMaximum)
    {
        this.serverReceiveMaximum = serverReceiveMaximum;
    }

    public int getClientReceiveMaximum()
    {
        return clientReceiveMaximum;
    }

    public void setClientReceiveMaximum(int clientReceiveMaximum)
    {
        this.clientReceiveMaximum = clientReceiveMaximum;
    }

    public long getEffectiveSessionExpiryInterval(long clientInterval)
    {
        if(clientInterval == 0) {
            return defaultSessionExpiryInterval;
        }
        if(clientInterval > maxSessionExpiryInterval) {
            return maxSessionExpiryInterval;
        }
        return clientInterval;
    }

    public int getEffectiveReceiveMaximum(int clientReceiveMax)
    {
        if(clientReceiveMax == 0) {
            return serverReceiveMaximum;
        }
        return Math.min(clientReceiveMax, serverReceiveMaximum);
    }
}
