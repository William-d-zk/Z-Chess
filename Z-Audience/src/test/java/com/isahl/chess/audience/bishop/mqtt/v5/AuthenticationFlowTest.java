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

package com.isahl.chess.audience.bishop.mqtt.v5;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.mqtt.v5.AuthenticationFlow;
import com.isahl.chess.bishop.mqtt.v5.AuthenticationFlow.AuthState;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthContext;
import com.isahl.chess.bishop.protocol.mqtt.service.IQttAuthProvider.AuthResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 增强认证流程测试
 *
 * @author william.d.zk
 * @since 1.2.0
 */
class AuthenticationFlowTest {

  private AuthenticationFlow authFlow;
  private MockAuthProvider mockProvider;

  @BeforeEach
  void setUp() {
    authFlow = new AuthenticationFlow(5); // 5秒超时
    mockProvider = new MockAuthProvider();
    authFlow.registerProvider(mockProvider);
  }

  @AfterEach
  void tearDown() {
    authFlow.shutdown();
  }

  @Test
  void testRegisterProvider() {
    assertTrue(authFlow.hasProvider("MOCK-AUTH"));
    assertNotNull(authFlow.getProvider("MOCK-AUTH"));
  }

  @Test
  void testUnregisterProvider() {
    authFlow.unregisterProvider("MOCK-AUTH");
    assertFalse(authFlow.hasProvider("MOCK-AUTH"));
  }

  @Test
  void testStartAuthUnknownMethod() {
    AuthResult result = authFlow.startAuth("client1", "UNKNOWN-METHOD", null);

    assertTrue(result.isFailure());
    assertEquals(AuthState.NONE, authFlow.getAuthState("client1"));
  }

  @Test
  void testAuthStateTransitions() {
    String clientId = "client1";

    // 初始状态
    assertEquals(AuthState.NONE, authFlow.getAuthState(clientId));

    // 开始认证 - 多步认证
    mockProvider.setNextResult(AuthResult.Continue(new byte[0]));
    authFlow.startAuth(clientId, "MOCK-AUTH", null);
    assertEquals(AuthState.IN_PROGRESS, authFlow.getAuthState(clientId));

    // 取消认证（取消后会话被移除）
    authFlow.cancelAuth(clientId);
    assertEquals(AuthState.NONE, authFlow.getAuthState(clientId));
  }

  @Test
  void testAuthSuccess() {
    String clientId = "client1";

    // 开始认证 - 直接成功
    mockProvider.setNextResult(AuthResult.success(null));
    AuthResult result = authFlow.startAuth(clientId, "MOCK-AUTH", null);

    assertTrue(result.isSuccess());
    // 成功后会话会被移除，所以状态是 NONE
    assertEquals(AuthState.NONE, authFlow.getAuthState(clientId));
  }

  @Test
  void testContinueAuthWithoutSession() {
    AuthResult result = authFlow.continueAuth("unknown-client", new byte[0]);

    assertTrue(result.isFailure());
    assertEquals("No active authentication session", result.getReason());
  }

  @Test
  void testContinueAuthSuccess() {
    String clientId = "client1";

    // 开始认证
    mockProvider.setNextResult(AuthResult.Continue(new byte[0]));
    authFlow.startAuth(clientId, "MOCK-AUTH", null);
    assertEquals(AuthState.IN_PROGRESS, authFlow.getAuthState(clientId));

    // 继续认证 - 成功
    mockProvider.setNextResult(AuthResult.success(null));
    AuthResult result = authFlow.continueAuth(clientId, new byte[0]);

    assertTrue(result.isSuccess());
  }

  @Test
  void testReauth() {
    // 重新认证
    mockProvider.setNextResult(AuthResult.success(null));
    AuthResult result = authFlow.reauth("client1", "MOCK-AUTH", null, 12345L);

    assertTrue(result.isSuccess());
  }

  @Test
  void testActiveSessionCount() {
    assertEquals(0, authFlow.getActiveSessionCount());

    mockProvider.setNextResult(AuthResult.Continue(new byte[0]));
    authFlow.startAuth("client1", "MOCK-AUTH", null);
    assertEquals(1, authFlow.getActiveSessionCount());

    authFlow.startAuth("client2", "MOCK-AUTH", null);
    assertEquals(2, authFlow.getActiveSessionCount());

    authFlow.cancelAuth("client1");
    assertEquals(1, authFlow.getActiveSessionCount());
  }

  @Test
  void testMultipleProviders() {
    // 注册另一个提供者
    IQttAuthProvider customProvider =
        new IQttAuthProvider() {
          @Override
          public String getAuthMethod() {
            return "CUSTOM-AUTH";
          }

          @Override
          public AuthResult startAuth(X111_QttConnect connect) {
            return AuthResult.success(null);
          }

          @Override
          public AuthResult continueAuth(String authMethod, byte[] authData, AuthContext context) {
            return AuthResult.success(null);
          }

          @Override
          public AuthResult reauth(String authMethod, byte[] authData, long sessionId) {
            return AuthResult.failure("Not implemented");
          }
        };

    authFlow.registerProvider(customProvider);

    assertTrue(authFlow.hasProvider("MOCK-AUTH"));
    assertTrue(authFlow.hasProvider("CUSTOM-AUTH"));
  }

  // ==================== 模拟认证提供者 ====================

  private static class MockAuthProvider implements IQttAuthProvider {
    private AuthResult _nextResult = AuthResult.success(null);

    @Override
    public String getAuthMethod() {
      return "MOCK-AUTH";
    }

    @Override
    public AuthResult startAuth(X111_QttConnect connect) {
      return _nextResult;
    }

    @Override
    public AuthResult continueAuth(String authMethod, byte[] authData, AuthContext context) {
      return _nextResult;
    }

    @Override
    public AuthResult reauth(String authMethod, byte[] authData, long sessionId) {
      return _nextResult;
    }

    public void setNextResult(AuthResult result) {
      _nextResult = result;
    }
  }
}
