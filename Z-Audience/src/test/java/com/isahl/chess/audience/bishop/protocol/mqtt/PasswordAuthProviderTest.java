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

package com.isahl.chess.audience.bishop.protocol.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.service.PasswordAuthProvider;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** PasswordAuthProvider 测试类 */
@DisplayName("Password Authentication Provider Tests")
class PasswordAuthProviderTest {
  private PasswordAuthProvider authProvider;

  @BeforeEach
  void setUp() {
    Map<String, String> credentials = new HashMap<>();
    credentials.put("testuser", "testpassword123");
    credentials.put("admin", "adminpass456");
    authProvider = new PasswordAuthProvider(credentials);
  }

  @Test
  @DisplayName("Should return correct auth method name")
  void testGetAuthMethod() {
    assertThat(authProvider.getAuthMethod()).isEqualTo("PASSWORD");
  }

  @Test
  @DisplayName("Should authenticate with valid credentials")
  void testValidAuthentication() {
    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("testuser");
    connect.setPassword("testpassword123");

    var result = authProvider.startAuth(connect);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @DisplayName("Should reject invalid password")
  void testInvalidPassword() {
    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("testuser");
    connect.setPassword("wrongpassword");

    var result = authProvider.startAuth(connect);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should reject unknown user")
  void testUnknownUser() {
    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("unknownuser");
    connect.setPassword("anypassword");

    var result = authProvider.startAuth(connect);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should reject missing username")
  void testMissingUsername() {
    X111_QttConnect connect = new X111_QttConnect();
    connect.setPassword("testpassword123");

    var result = authProvider.startAuth(connect);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should reject missing password")
  void testMissingPassword() {
    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("testuser");

    var result = authProvider.startAuth(connect);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should not support continue authentication")
  void testContinueAuthNotSupported() {
    var result = authProvider.continueAuth("PASSWORD", new byte[0], null);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should not support reauthentication")
  void testReauthNotSupported() {
    var result = authProvider.reauth("PASSWORD", new byte[0], 12345L);

    assertThat(result.isFailure()).isTrue();
  }

  @Test
  @DisplayName("Should add user at runtime")
  void testAddUser() {
    authProvider.addUser("newuser", "newpassword");

    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("newuser");
    connect.setPassword("newpassword");

    var result = authProvider.startAuth(connect);

    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  @DisplayName("Should remove user at runtime")
  void testRemoveUser() {
    authProvider.removeUser("testuser");

    X111_QttConnect connect = new X111_QttConnect();
    connect.setUserName("testuser");
    connect.setPassword("testpassword123");

    var result = authProvider.startAuth(connect);

    assertThat(result.isFailure()).isTrue();
  }
}
