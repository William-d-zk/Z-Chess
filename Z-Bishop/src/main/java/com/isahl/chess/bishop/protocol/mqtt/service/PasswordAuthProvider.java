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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MQTT v5.0 密码挑战认证提供者
 *
 * <p>支持简单的用户名/密码认证，通过挑战-响应机制验证客户端身份。 这是一个基础的认证实现，生产环境建议使用 SCRAM 或其他更安全的认证方式。
 *
 * @author william.d.zk
 */
public class PasswordAuthProvider implements IQttAuthProvider {
  private static final Logger _Logger =
      LoggerFactory.getLogger("mqtt.auth." + PasswordAuthProvider.class.getSimpleName());

  public static final String AUTH_METHOD = "PASSWORD";

  private final Map<String, String> _UserCredentials;

  public PasswordAuthProvider() {
    _UserCredentials = new ConcurrentHashMap<>();
  }

  public PasswordAuthProvider(Map<String, String> userCredentials) {
    _UserCredentials = new ConcurrentHashMap<>(userCredentials);
  }

  public void addUser(String username, String password) {
    _UserCredentials.put(username, password);
  }

  public void removeUser(String username) {
    _UserCredentials.remove(username);
  }

  @Override
  public String getAuthMethod() {
    return AUTH_METHOD;
  }

  @Override
  public AuthResult startAuth(X111_QttConnect connect) {
    String username = connect.getUserName();
    String password = connect.getPassword();

    if (username == null || password == null) {
      _Logger.warn("Authentication failed: missing credentials");
      return AuthResult.failure("Missing username or password");
    }

    String storedPassword = _UserCredentials.get(username);
    if (storedPassword == null) {
      _Logger.warn("Authentication failed: user not found: %s", username);
      return AuthResult.failure("Invalid username or password");
    }

    if (verifyPassword(username, password, storedPassword)) {
      _Logger.info("Authentication success for user: %s", username);
      return AuthResult.success(null);
    } else {
      _Logger.warn("Authentication failed: invalid password for user: %s", username);
      return AuthResult.failure("Invalid username or password");
    }
  }

  @Override
  public AuthResult continueAuth(String authMethod, byte[] authData, AuthContext context) {
    _Logger.warn("Continue auth not supported for method: %s", authMethod);
    return AuthResult.failure("Authentication method does not support multi-step authentication");
  }

  @Override
  public AuthResult reauth(String authMethod, byte[] authData, long sessionId) {
    _Logger.info("Re-authentication request for session: %d", sessionId);
    return AuthResult.failure("Re-authentication not supported");
  }

  private boolean verifyPassword(String username, String providedPassword, String storedPassword) {
    if (storedPassword.startsWith("SHA256:")) {
      String storedHash = storedPassword.substring(7);
      try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(providedPassword.getBytes(StandardCharsets.UTF_8));
        String providedHash = Base64.getEncoder().encodeToString(hash);
        return storedHash.equals(providedHash);
      } catch (Exception e) {
        _Logger.warn("Password verification error for user: %s", username, e);
        return false;
      }
    }
    return storedPassword.equals(providedPassword);
  }
}
