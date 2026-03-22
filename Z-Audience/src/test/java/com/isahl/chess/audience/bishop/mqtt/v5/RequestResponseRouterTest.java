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

import com.isahl.chess.bishop.mqtt.v5.RequestResponseRouter;
import com.isahl.chess.bishop.mqtt.v5.ResponseCallback;
import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 请求/响应路由器测试
 *
 * @author william.d.zk
 * @since 1.2.0
 */
class RequestResponseRouterTest {

  private RequestResponseRouter router;

  @BeforeEach
  void setUp() {
    router = new RequestResponseRouter(5000); // 5秒超时
  }

  @AfterEach
  void tearDown() {
    router.shutdown();
  }

  @Test
  void testSendRequest() {
    X113_QttPublish request = createRequest("request/topic", "test data");
    byte[] correlationData = "correlation-1".getBytes();
    String responseTopic = "response/topic";

    AtomicBoolean callbackInvoked = new AtomicBoolean(false);

    ResponseCallback callback =
        new ResponseCallback() {
          @Override
          public void onResponse(X113_QttPublish response) {
            callbackInvoked.set(true);
          }
        };

    String requestId = router.sendRequest(request, correlationData, responseTopic, callback);

    assertNotNull(requestId);
    assertEquals(1, router.getPendingRequestCount());
  }

  @Test
  void testHandleResponse() {
    X113_QttPublish request = createRequest("request/topic", "test data");
    byte[] correlationData = "correlation-2".getBytes();
    String responseTopic = "response/topic";

    AtomicReference<X113_QttPublish> receivedResponse = new AtomicReference<>();

    ResponseCallback callback =
        new ResponseCallback() {
          @Override
          public void onResponse(X113_QttPublish response) {
            receivedResponse.set(response);
          }
        };

    router.sendRequest(request, correlationData, responseTopic, callback);

    // 发送响应
    X113_QttPublish response = createResponse(responseTopic, "response data", correlationData);
    boolean matched = router.handleResponse(response);

    assertTrue(matched);
    assertNotNull(receivedResponse.get());
    assertEquals(0, router.getPendingRequestCount());
  }

  @Test
  void testHandleResponseWithoutCorrelationData() {
    X113_QttPublish response = createResponse("response/topic", "data", null);
    boolean matched = router.handleResponse(response);

    assertFalse(matched);
  }

  @Test
  void testHandleUnknownResponse() {
    byte[] unknownCorrelation = "unknown".getBytes();
    X113_QttPublish response = createResponse("response/topic", "data", unknownCorrelation);
    boolean matched = router.handleResponse(response);

    assertFalse(matched);
  }

  @Test
  void testSendRequestAsync() throws Exception {
    X113_QttPublish request = createRequest("request/topic", "test data");
    byte[] correlationData = "async-correlation".getBytes();
    String responseTopic = "response/topic";

    CompletableFuture<X113_QttPublish> future =
        router.sendRequestAsync(request, correlationData, responseTopic);

    assertEquals(1, router.getPendingRequestCount());

    // 模拟响应
    X113_QttPublish response = createResponse(responseTopic, "async response", correlationData);
    router.handleResponse(response);

    X113_QttPublish result = future.get(1, TimeUnit.SECONDS);
    assertNotNull(result);
    assertEquals(0, router.getPendingRequestCount());
  }

  @Test
  void testCancelRequest() {
    X113_QttPublish request = createRequest("request/topic", "test data");
    byte[] correlationData = "cancel-correlation".getBytes();
    String responseTopic = "response/topic";

    String requestId = router.sendRequest(request, correlationData, responseTopic, null);
    assertEquals(1, router.getPendingRequestCount());

    boolean cancelled = router.cancelRequest(requestId);
    assertTrue(cancelled);
    assertEquals(0, router.getPendingRequestCount());

    // 重复取消应该返回 false
    cancelled = router.cancelRequest(requestId);
    assertFalse(cancelled);
  }

  @Test
  void testResponseHandlerRegistration() {
    String topic = "custom/response/topic";
    AtomicBoolean handlerInvoked = new AtomicBoolean(false);

    router.registerResponseHandler(topic, msg -> handlerInvoked.set(true));
    assertEquals(1, router.getResponseHandlerCount());

    router.unregisterResponseHandler(topic);
    assertEquals(0, router.getResponseHandlerCount());
  }

  @Test
  void testMultipleRequests() {
    for (int i = 0; i < 10; i++) {
      X113_QttPublish request = createRequest("request/topic", "data" + i);
      byte[] correlationData = ("corr" + i).getBytes();
      router.sendRequest(request, correlationData, "response/topic", null);
    }

    assertEquals(10, router.getPendingRequestCount());
  }

  // ==================== 辅助方法 ====================

  private X113_QttPublish createRequest(String topic, String payload) {
    X113_QttPublish publish = new X113_QttPublish();
    publish.setResponseTopic(topic);
    publish.setPayload(payload.getBytes());
    return publish;
  }

  private X113_QttPublish createResponse(String topic, String payload, byte[] correlationData) {
    X113_QttPublish publish = new X113_QttPublish();
    publish.setResponseTopic(topic);
    publish.setPayload(payload.getBytes());
    publish.setCorrelationData(correlationData);
    return publish;
  }
}
