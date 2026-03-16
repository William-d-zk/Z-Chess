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

import java.util.Arrays;
import java.util.List;

/**
 * MQTT 认证配置
 *
 * @author william.d.zk
 */
@Configuration
@ConfigurationProperties(prefix = "z.chess.mqtt.auth")
public class MqttAuthConfig
{
    /**
     * 支持的认证方法列表
     */
    private List<String> supportedAuthMethods = Arrays.asList();

    /**
     * 默认认证方法
     */
    private String defaultAuthMethod = null;

    /**
     * 认证超时时间（秒）
     */
    private int authTimeoutSeconds = 60;

    /**
     * 是否允许重新认证
     */
    private boolean reauthenticationAllowed = true;

    public List<String> getSupportedAuthMethods()
    {
        return supportedAuthMethods;
    }

    public void setSupportedAuthMethods(List<String> supportedAuthMethods)
    {
        this.supportedAuthMethods = supportedAuthMethods;
    }

    public String getDefaultAuthMethod()
    {
        return defaultAuthMethod;
    }

    public void setDefaultAuthMethod(String defaultAuthMethod)
    {
        this.defaultAuthMethod = defaultAuthMethod;
    }

    public int getAuthTimeoutSeconds()
    {
        return authTimeoutSeconds;
    }

    public void setAuthTimeoutSeconds(int authTimeoutSeconds)
    {
        this.authTimeoutSeconds = authTimeoutSeconds;
    }

    public boolean isReauthenticationAllowed()
    {
        return reauthenticationAllowed;
    }

    public void setReauthenticationAllowed(boolean reauthenticationAllowed)
    {
        this.reauthenticationAllowed = reauthenticationAllowed;
    }

    public boolean isAuthMethodSupported(String method)
    {
        return supportedAuthMethods.contains(method);
    }
}
