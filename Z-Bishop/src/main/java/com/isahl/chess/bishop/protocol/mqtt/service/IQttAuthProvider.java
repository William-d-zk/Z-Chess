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

package com.isahl.chess.bishop.protocol.mqtt.service;

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;

/**
 * MQTT v5.0 增强认证提供者接口
 * <p>
 * 支持 MQTT v5.0 的增强认证流程（Re-authentication 和 Auth Method）。
 * 实现类需要处理多步认证流程。
 * </p>
 *
 * @author william.d.zk
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Enhanced_authentication">MQTT v5.0 Enhanced Authentication</a>
 */
public interface IQttAuthProvider
{
    /**
     * 获取认证方法名称
     * <p>例如：SCRAM-SHA-256, SCRAM-SHA-1, KERBEROS 等</p>
     *
     * @return 认证方法名称
     */
    String getAuthMethod();

    /**
     * 开始认证流程
     * <p>在收到 CONNECT 报文后调用</p>
     *
     * @param connect CONNECT 报文
     * @return 认证响应数据，null 表示认证失败
     */
    AuthResult startAuth(X111_QttConnect connect);

    /**
     * 继续认证流程
     * <p>在收到 AUTH 报文（Reason Code = 0x18 Continue Authentication）后调用</p>
     *
     * @param authMethod   认证方法
     * @param authData     认证数据
     * @param context      认证上下文（保存中间状态）
     * @return 认证结果
     */
    AuthResult continueAuth(String authMethod, byte[] authData, AuthContext context);

    /**
     * 重新认证（Re-authenticate）
     * <p>在连接建立后，客户端可以通过 AUTH 报文请求重新认证</p>
     *
     * @param authMethod   认证方法
     * @param authData     认证数据
     * @param sessionId    会话标识
     * @return 认证结果
     */
    AuthResult reauth(String authMethod, byte[] authData, long sessionId);

    /**
     * 认证结果
     */
    class AuthResult
    {
        /**
         * 认证状态
         */
        public enum Status {
            /**
             * 认证成功
             */
            SUCCESS,
            /**
             * 需要继续认证（多步认证）
             */
            CONTINUE,
            /**
             * 认证失败
             */
            FAILURE
        }

        private final Status  _Status;
        private final byte[]  _AuthData;
        private final String  _Reason;
        private final QttPropertySet _Properties;

        private AuthResult(Status status, byte[] authData, String reason, QttPropertySet properties)
        {
            _Status = status;
            _AuthData = authData;
            _Reason = reason;
            _Properties = properties;
        }

        public static AuthResult success(byte[] authData)
        {
            return new AuthResult(Status.SUCCESS, authData, null, null);
        }

        public static AuthResult success(byte[] authData, QttPropertySet properties)
        {
            return new AuthResult(Status.SUCCESS, authData, null, properties);
        }

        public static AuthResult Continue(byte[] authData)
        {
            return new AuthResult(Status.CONTINUE, authData, null, null);
        }

        public static AuthResult failure(String reason)
        {
            return new AuthResult(Status.FAILURE, null, reason, null);
        }

        public Status getStatus()
        {
            return _Status;
        }

        public byte[] getAuthData()
        {
            return _AuthData;
        }

        public String getReason()
        {
            return _Reason;
        }

        public QttPropertySet getProperties()
        {
            return _Properties;
        }

        public boolean isSuccess()
        {
            return _Status == Status.SUCCESS;
        }

        public boolean isContinue()
        {
            return _Status == Status.CONTINUE;
        }

        public boolean isFailure()
        {
            return _Status == Status.FAILURE;
        }
    }

    /**
     * 认证上下文
     * <p>用于保存多步认证过程中的中间状态</p>
     */
    class AuthContext
    {
        private final long       _SessionId;
        private final String     _ClientId;
        private final String     _AuthMethod;
        private       int        _Step;
        private       Object     _State;

        public AuthContext(long sessionId, String clientId, String authMethod)
        {
            _SessionId = sessionId;
            _ClientId = clientId;
            _AuthMethod = authMethod;
            _Step = 0;
        }

        public long getSessionId()
        {
            return _SessionId;
        }

        public String getClientId()
        {
            return _ClientId;
        }

        public String getAuthMethod()
        {
            return _AuthMethod;
        }

        public int getStep()
        {
            return _Step;
        }

        public void nextStep()
        {
            _Step++;
        }

        @SuppressWarnings("unchecked")
        public <T> T getState()
        {
            return (T) _State;
        }

        public void setState(Object state)
        {
            _State = state;
        }
    }
}
