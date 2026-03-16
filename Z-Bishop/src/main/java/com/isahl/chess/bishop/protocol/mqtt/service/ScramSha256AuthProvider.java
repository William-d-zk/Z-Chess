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
import com.isahl.chess.king.base.log.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MQTT v5.0 SCRAM-SHA-256 认证提供者
 * <p>
 * 实现 SCRAM-SHA-256 认证机制，这是 MQTT v5.0 推荐的认证方式。
 * 支持多步认证流程：
 * 1. 客户端发送初始认证数据 (client-first)
 * 2. 服务器返回服务器-first 消息
 * 3. 客户端发送最终认证数据 (client-final)
 * 4. 服务器验证并返回成功
 * </p>
 *
 * @author william.d.zk
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Enhanced_authentication">MQTT v5.0 Enhanced Authentication</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5802">SCRAM</a>
 */
public class ScramSha256AuthProvider
        implements IQttAuthProvider
{
    private final static Logger _Logger = Logger.getLogger("mqtt.auth." + ScramSha256AuthProvider.class.getSimpleName());

    public static final String AUTH_METHOD = "SCRAM-SHA-256";

    private static final String HMAC_NAME = "HmacSHA256";
    private static final int DEFAULT_ITERATIONS = 4096;
    private static final int SALT_LENGTH = 16;

    private final Map<String, StoredCredentials> _UserCredentials;
    private final int _Iterations;
    private final SecureRandom _Random;

    public ScramSha256AuthProvider()
    {
        this(DEFAULT_ITERATIONS);
    }

    public ScramSha256AuthProvider(int iterations)
    {
        _UserCredentials = new ConcurrentHashMap<>();
        _Iterations = iterations;
        _Random = new SecureRandom();
    }

    public void addUser(String username, String password)
    {
        _UserCredentials.put(username, createStoredCredentials(password));
    }

    public void addUserWithSalt(String username, String salt, int iterations, String storedKey, String serverKey)
    {
        _UserCredentials.put(username, new StoredCredentials(salt, iterations, storedKey, serverKey));
    }

    public void removeUser(String username)
    {
        _UserCredentials.remove(username);
    }

    @Override
    public String getAuthMethod()
    {
        return AUTH_METHOD;
    }

    @Override
    public AuthResult startAuth(X111_QttConnect connect)
    {
        String username = connect.getUserName();
        byte[] authData = connect.getAuthenticationData();

        if(username == null) {
            _Logger.warning("Authentication failed: missing username");
            return AuthResult.failure("Missing username");
        }

        StoredCredentials credentials = _UserCredentials.get(username);
        if(credentials == null) {
            _Logger.warning("Authentication failed: user not found: %s", username);
            return AuthResult.failure("User not found");
        }

        if(authData == null || authData.length == 0) {
            return AuthResult.Continue(createServerFirstMessage(username, credentials));
        }

        try {
            String clientFinalMessage = new String(authData, StandardCharsets.UTF_8);
            return verifyClientFinalMessage(username, credentials, clientFinalMessage);
        }
        catch(Exception e) {
            _Logger.warning("Authentication error for user: %s", username, e);
            return AuthResult.failure("Authentication error: " + e.getMessage());
        }
    }

    @Override
    public AuthResult continueAuth(String authMethod, byte[] authData, AuthContext context)
    {
        if(!AUTH_METHOD.equals(authMethod)) {
            return AuthResult.failure("Unsupported authentication method");
        }

        String username = context.getClientId();
        StoredCredentials credentials = _UserCredentials.get(username);

        if(credentials == null) {
            return AuthResult.failure("User not found");
        }

        try {
            String clientFinalMessage = new String(authData, StandardCharsets.UTF_8);
            return verifyClientFinalMessage(username, credentials, clientFinalMessage);
        }
        catch(Exception e) {
            _Logger.warning("Continue auth error for user: %s", username, e);
            return AuthResult.failure("Authentication error: " + e.getMessage());
        }
    }

    @Override
    public AuthResult reauth(String authMethod, byte[] authData, long sessionId)
    {
        _Logger.info("Re-authentication request for session: %d", sessionId);
        return AuthResult.failure("Re-authentication not implemented");
    }

    private StoredCredentials createStoredCredentials(String password)
    {
        byte[] salt = new byte[SALT_LENGTH];
        _Random.nextBytes(salt);

        try {
            byte[] saltedPassword = hi(password, salt, _Iterations);
            byte[] storedKey = H(saltedPassword);
            byte[] serverKey = HMAC(saltedPassword, "Server Key".getBytes(StandardCharsets.UTF_8));

            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String storedKeyBase64 = Base64.getEncoder().encodeToString(storedKey);
            String serverKeyBase64 = Base64.getEncoder().encodeToString(serverKey);

            return new StoredCredentials(saltBase64, _Iterations, storedKeyBase64, serverKeyBase64);
        }
        catch(Exception e) {
            _Logger.warning("Error creating stored credentials", e);
            throw new RuntimeException("Failed to create credentials", e);
        }
    }

    private byte[] createServerFirstMessage(String username, StoredCredentials credentials)
    {
        String serverFirst = String.format("r=%s,i=%d",
                                           generateNonce(),
                                           credentials.iterations);
        return serverFirst.getBytes(StandardCharsets.UTF_8);
    }

    private AuthResult verifyClientFinalMessage(String username, StoredCredentials credentials, String clientFinalMessage)
    {
        String serverFirst = credentials.serverFirstMessage;
        if(serverFirst == null) {
            return AuthResult.failure("Invalid authentication flow");
        }

        String[] clientFinalParts = clientFinalMessage.split(",");
        if(clientFinalParts.length < 2) {
            return AuthResult.failure("Invalid client final message");
        }

        String channelBinding = null;
        String clientProof = null;

        for(String part : clientFinalParts) {
            if(part.startsWith("c=")) {
                channelBinding = part.substring(2);
            }
            else if(part.startsWith("p=")) {
                clientProof = part.substring(2);
            }
        }

        if(clientProof == null) {
            return AuthResult.failure("Missing client proof");
        }

        try {
            byte[] storedKey = Base64.getDecoder().decode(credentials.storedKey);
            byte[] clientSignature = HMAC(storedKey, (serverFirst + clientFinalMessage).getBytes(StandardCharsets.UTF_8));
            byte[] clientKey = XOR(storedKey, clientSignature);

            String computedProof = Base64.getEncoder().encodeToString(clientKey);

            if(!computedProof.equals(clientProof)) {
                _Logger.warning("Authentication failed: invalid proof for user: %s", username);
                return AuthResult.failure("Invalid credentials");
            }

            _Logger.info("Authentication success for user: %s", username);
            return AuthResult.success(null);
        }
        catch(Exception e) {
            _Logger.warning("Verification error for user: %s", username, e);
            return AuthResult.failure("Verification error");
        }
    }

    private String generateNonce()
    {
        byte[] nonce = new byte[16];
        _Random.nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    private byte[] hi(String password, byte[] salt, int iterations) throws Exception
    {
        SecretKeySpec key = new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), HMAC_NAME);
        Mac mac = Mac.getInstance(HMAC_NAME);
        mac.init(key);

        byte[] result = mac.doFinal(salt);

        for(int i = 1; i < iterations; i++) {
            mac.init(key);
            result = XOR(result, mac.doFinal(salt));
        }

        return result;
    }

    private byte[] H(byte[] data) throws Exception
    {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }

    private byte[] HMAC(byte[] key, byte[] data) throws Exception
    {
        SecretKeySpec keySpec = new SecretKeySpec(key, HMAC_NAME);
        Mac mac = Mac.getInstance(HMAC_NAME);
        mac.init(keySpec);
        return mac.doFinal(data);
    }

    private byte[] XOR(byte[] a, byte[] b)
    {
        byte[] result = new byte[a.length];
        for(int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    private static class StoredCredentials
    {
        final String salt;
        final int iterations;
        final String storedKey;
        final String serverKey;
        String serverFirstMessage;

        StoredCredentials(String salt, int iterations, String storedKey, String serverKey)
        {
            this.salt = salt;
            this.iterations = iterations;
            this.storedKey = storedKey;
            this.serverKey = serverKey;
        }
    }
}
