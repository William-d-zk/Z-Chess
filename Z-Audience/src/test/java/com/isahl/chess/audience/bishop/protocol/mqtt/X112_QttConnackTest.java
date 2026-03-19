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

package com.isahl.chess.bishop.protocol.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import org.junit.jupiter.api.Test;

/** X112_QttConnack 测试类 */
class X112_QttConnackTest {

  private QttContext createV5Context() {
    MockNetworkOption option = new MockNetworkOption();
    QttContext context = new QttContext(option, ISort.Mode.CLUSTER, ISort.Type.SYMMETRY);
    context.setVersion(QttProtocol.VERSION_V5_0);
    return context;
  }

  @Test
  void testBasicConnack() {
    X112_QttConnack connack = new X112_QttConnack();
    connack.responseOk();

    assertThat(connack.isOk()).isTrue();
    assertThat(connack.isPresent()).isFalse();
  }

  @Test
  void testV3RejectionCodes() {
    X112_QttConnack connack = new X112_QttConnack();
    connack.rejectUnsupportedVersion();
    assertThat(connack.isReject()).isTrue();

    connack = new X112_QttConnack();
    connack.rejectIdentifier();
    assertThat(connack.isReject()).isTrue();

    connack = new X112_QttConnack();
    connack.rejectServerUnavailable();
    assertThat(connack.isReject()).isTrue();

    connack = new X112_QttConnack();
    connack.rejectBadUserOrPassword();
    assertThat(connack.isReject()).isTrue();

    connack = new X112_QttConnack();
    connack.rejectNotAuthorized();
    assertThat(connack.isReject()).isTrue();
  }

  @Test
  void testV5RejectionMethods() {
    // V5 拒绝方法设置后，isOk() 应返回 false
    X112_QttConnack connack = new X112_QttConnack();
    connack.rejectBadAuthenticationMethod();
    assertThat(connack.isOk()).isFalse();

    connack = new X112_QttConnack();
    connack.rejectServerBusy();
    assertThat(connack.isOk()).isFalse();

    connack = new X112_QttConnack();
    connack.rejectBanned();
    assertThat(connack.isOk()).isFalse();
  }

  @Test
  void testV5Properties() {
    X112_QttConnack connack = new X112_QttConnack();
    QttContext context = createV5Context();
    connack.wrap(context);

    connack.responseOk();

    // 设置各种 V5 属性
    connack.setSessionExpiryInterval(3600);
    connack.setReceiveMaximum(65535);
    connack.setMaximumQoS(2);
    connack.setMaximumPacketSize(1048576);
    connack.setAssignedClientIdentifier("auto-gen-client-123");
    connack.setTopicAliasMaximum(256);
    connack.setReasonString("Connection accepted");
    connack.setWildcardSubscriptionAvailable(true);
    connack.setSubscriptionIdentifierAvailable(true);
    connack.setSharedSubscriptionAvailable(true);
    connack.setServerKeepAlive(300);
    connack.setResponseInformation("response-info");
    connack.setServerReference("backup.broker.local");
    connack.setAuthenticationMethod("SCRAM-SHA-256");
    connack.setAuthenticationData(new byte[] {0x01, 0x02, 0x03});

    connack.getProperties().addUserProperty("server-name", "Z-Chess");
    connack.getProperties().addUserProperty("version", "1.0.0");

    assertThat(connack.isOk()).isTrue();
    assertThat(connack.isPresent()).isFalse();
    assertThat(connack.getSessionExpiryInterval()).isEqualTo(3600);
    assertThat(connack.getReceiveMaximum()).isEqualTo(65535);
    assertThat(connack.getMaximumQoS()).isEqualTo(2);
    assertThat(connack.getMaximumPacketSize()).isEqualTo(1048576);
    assertThat(connack.getAssignedClientIdentifier()).isEqualTo("auto-gen-client-123");
    assertThat(connack.getTopicAliasMaximum()).isEqualTo(256);
    assertThat(connack.getReasonString()).isEqualTo("Connection accepted");
    assertThat(connack.isWildcardSubscriptionAvailable()).isTrue();
    assertThat(connack.isSubscriptionIdentifierAvailable()).isTrue();
    assertThat(connack.isSharedSubscriptionAvailable()).isTrue();
    assertThat(connack.getServerKeepAlive()).isEqualTo(300);
    assertThat(connack.getResponseInformation()).isEqualTo("response-info");
    assertThat(connack.getServerReference()).isEqualTo("backup.broker.local");
    assertThat(connack.getAuthenticationMethod()).isEqualTo("SCRAM-SHA-256");
    assertThat(connack.getAuthenticationData()).containsExactly(0x01, 0x02, 0x03);
    assertThat(connack.getProperties().getUserProperties()).hasSize(2);
  }

  @Test
  void testSerial() {
    X112_QttConnack connack = new X112_QttConnack();
    assertThat(connack.serial()).isEqualTo(0x112);
  }

  @Test
  void testToStringFormat() {
    X112_QttConnack connack = new X112_QttConnack();
    connack.setPresent();
    connack.responseOk();

    String str = connack.toString();
    assertThat(str).contains("X112");
  }
}
