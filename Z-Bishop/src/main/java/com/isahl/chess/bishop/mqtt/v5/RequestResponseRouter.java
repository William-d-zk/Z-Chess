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

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.king.base.log.Logger;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * MQTT v5.0 请求/响应路由器
 *
 * <p>实现 MQTT 5.0 的请求/响应模式，支持：
 *
 * <ul>
 *   <li>发送请求并等待响应
 *   <li>通过 Correlation Data 匹配请求和响应
 *   <li>超时处理
 *   <li>异步回调支持
 * </ul>
 *
 * @author william.d.zk
 * @since 1.2.0
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Request_Response">MQTT
 *     v5.0 Request/Response</a>
 */
public class RequestResponseRouter {

  private static final Logger _Logger =
      Logger.getLogger("mqtt.broker." + RequestResponseRouter.class.getSimpleName());

  /** 默认请求超时时间（毫秒） */
  public static final long DEFAULT_TIMEOUT_MS = 30000;

  /** 待处理请求映射：Correlation Data -> PendingRequest */
  private final Map<String, PendingRequest> _pendingRequests = new ConcurrentHashMap<>();

  /** 响应主题订阅映射：Response Topic -> Handler */
  private final Map<String, Consumer<X113_QttPublish>> _responseHandlers =
      new ConcurrentHashMap<>();

  /** 超时清理调度器 */
  private final ScheduledExecutorService _scheduler;

  /** 请求超时时间（毫秒） */
  private final long _timeoutMs;

  public RequestResponseRouter() {
    this(DEFAULT_TIMEOUT_MS);
  }

  public RequestResponseRouter(long timeoutMs) {
    _timeoutMs = timeoutMs;
    _scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "req-resp-timeout-cleaner");
              t.setDaemon(true);
              return t;
            });
    startTimeoutCleaner();
  }

  // ==================== 请求发送 ====================

  /**
   * 发送请求（异步）
   *
   * @param request 请求消息
   * @param correlationData 关联数据（用于匹配响应）
   * @param responseTopic 响应主题
   * @param callback 响应回调
   * @return 请求 ID
   */
  public String sendRequest(
      X113_QttPublish request,
      byte[] correlationData,
      String responseTopic,
      ResponseCallback callback) {

    String correlationId = encodeCorrelationData(correlationData);

    PendingRequest pendingRequest =
        new PendingRequest(
            correlationId, request, responseTopic, callback, System.currentTimeMillis());

    _pendingRequests.put(correlationId, pendingRequest);

    _Logger.debug(
        "Request registered: correlationId={}, responseTopic={}", correlationId, responseTopic);

    return correlationId;
  }

  /**
   * 发送请求（返回 CompletableFuture）
   *
   * @param request 请求消息
   * @param correlationData 关联数据
   * @param responseTopic 响应主题
   * @return CompletableFuture，完成后返回响应消息
   */
  public CompletableFuture<X113_QttPublish> sendRequestAsync(
      X113_QttPublish request, byte[] correlationData, String responseTopic) {

    CompletableFuture<X113_QttPublish> future = new CompletableFuture<>();

    ResponseCallback callback =
        new ResponseCallback() {
          @Override
          public void onResponse(X113_QttPublish response) {
            future.complete(response);
          }

          @Override
          public void onTimeout() {
            future.completeExceptionally(new RequestTimeoutException("Request timed out"));
          }

          @Override
          public void onError(Throwable error) {
            future.completeExceptionally(error);
          }
        };

    sendRequest(request, correlationData, responseTopic, callback);

    return future;
  }

  // ==================== 响应处理 ====================

  /**
   * 处理响应消息
   *
   * @param response 响应消息
   * @return true 如果成功匹配到请求
   */
  public boolean handleResponse(X113_QttPublish response) {
    // 从响应中获取关联数据
    byte[] correlationData = response.getCorrelationData();
    if (correlationData == null) {
      _Logger.debug("Response without correlation data, ignoring");
      return false;
    }

    String correlationId = encodeCorrelationData(correlationData);
    PendingRequest pendingRequest = _pendingRequests.remove(correlationId);

    if (pendingRequest == null) {
      _Logger.debug("No pending request found for correlationId: {}", correlationId);
      return false;
    }

    // 通知回调
    if (pendingRequest.getCallback() != null) {
      try {
        pendingRequest.getCallback().onResponse(response);
      } catch (Exception e) {
        _Logger.warning("Error invoking response callback: {}", e.getMessage());
      }
    }

    _Logger.debug("Response matched and delivered: correlationId={}", correlationId);
    return true;
  }

  /**
   * 注册响应主题处理器
   *
   * @param responseTopic 响应主题
   * @param handler 处理函数
   */
  public void registerResponseHandler(String responseTopic, Consumer<X113_QttPublish> handler) {
    _responseHandlers.put(responseTopic, handler);
    _Logger.debug("Response handler registered for topic: {}", responseTopic);
  }

  /** 注销响应主题处理器 */
  public void unregisterResponseHandler(String responseTopic) {
    _responseHandlers.remove(responseTopic);
    _Logger.debug("Response handler unregistered for topic: {}", responseTopic);
  }

  // ==================== 超时处理 ====================

  private void startTimeoutCleaner() {
    _scheduler.scheduleWithFixedDelay(this::cleanExpiredRequests, 5, 5, TimeUnit.SECONDS);
  }

  private void cleanExpiredRequests() {
    long now = System.currentTimeMillis();

    _pendingRequests
        .entrySet()
        .removeIf(
            entry -> {
              PendingRequest request = entry.getValue();
              if (now - request.getStartTime() > _timeoutMs) {
                // 通知超时
                if (request.getCallback() != null) {
                  try {
                    request.getCallback().onTimeout();
                  } catch (Exception e) {
                    _Logger.warning("Error invoking timeout callback: {}", e.getMessage());
                  }
                }
                _Logger.debug("Request timed out: correlationId={}", entry.getKey());
                return true;
              }
              return false;
            });
  }

  // ==================== 取消请求 ====================

  /**
   * 取消待处理的请求
   *
   * @param correlationId 请求关联 ID
   * @return true 如果成功取消
   */
  public boolean cancelRequest(String correlationId) {
    PendingRequest request = _pendingRequests.remove(correlationId);
    if (request != null) {
      _Logger.debug("Request cancelled: correlationId={}", correlationId);
      return true;
    }
    return false;
  }

  // ==================== 统计信息 ====================

  /** 获取待处理请求数 */
  public int getPendingRequestCount() {
    return _pendingRequests.size();
  }

  /** 获取已注册的响应处理器数 */
  public int getResponseHandlerCount() {
    return _responseHandlers.size();
  }

  // ==================== 工具方法 ====================

  private String encodeCorrelationData(byte[] data) {
    if (data == null) {
      return "";
    }
    // 使用 Base64 编码，确保可以作为 Map 的 key
    return java.util.Base64.getEncoder().encodeToString(data);
  }

  /** 关闭路由器 */
  public void shutdown() {
    _scheduler.shutdown();
    _pendingRequests.clear();
    _responseHandlers.clear();
    _Logger.info("RequestResponseRouter shutdown");
  }

  // ==================== 内部类 ====================

  /** 待处理请求 */
  private static class PendingRequest {
    private final String _correlationId;
    private final X113_QttPublish _request;
    private final String _responseTopic;
    private final ResponseCallback _callback;
    private final long _startTime;

    PendingRequest(
        String correlationId,
        X113_QttPublish request,
        String responseTopic,
        ResponseCallback callback,
        long startTime) {
      _correlationId = correlationId;
      _request = request;
      _responseTopic = responseTopic;
      _callback = callback;
      _startTime = startTime;
    }

    String getCorrelationId() {
      return _correlationId;
    }

    X113_QttPublish getRequest() {
      return _request;
    }

    String getResponseTopic() {
      return _responseTopic;
    }

    ResponseCallback getCallback() {
      return _callback;
    }

    long getStartTime() {
      return _startTime;
    }
  }

  /** 请求超时异常 */
  public static class RequestTimeoutException extends Exception {
    public RequestTimeoutException(String message) {
      super(message);
    }
  }
}
