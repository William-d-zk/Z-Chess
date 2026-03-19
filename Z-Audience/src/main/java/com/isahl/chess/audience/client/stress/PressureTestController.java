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

package com.isahl.chess.audience.client.stress;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.king.base.log.Logger;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 压力测试 REST API 控制器
 *
 * <p>提供 HTTP 接口用于控制压力测试和查询测试状态
 *
 * <p>主要接口： - POST /api/stress/start : 启动压力测试 - POST /api/stress/stop : 停止压力测试 - GET
 * /api/stress/status : 获取测试状态 - GET /api/stress/metrics : 获取性能指标 - POST /api/stress/config : 更新测试配置
 */
@RestController
@RequestMapping("/api/stress")
public class PressureTestController {
  private static final Logger _Logger = Logger.getLogger("stress.controller");

  private final PressureTestValidator validator;
  private final PressureTestConfig config;

  @Autowired
  public PressureTestController(PressureTestValidator validator, PressureTestConfig config) {
    this.validator = validator;
    this.config = config;
  }

  /**
   * 启动压力测试
   *
   * <p>示例请求： POST /api/stress/start { "concurrency": 3000, "durationSeconds": 60,
   * "requestsPerSecondPerClient": 10 }
   */
  @PostMapping("/start")
  public ZResponse<?> startTest(@RequestBody(required = false) StartRequest request) {
    try {
      // 应用动态配置（如果提供）
      if (request != null) {
        applyRequestConfig(request);
      }

      _Logger.info("Starting stress test via API: %s", config);

      CompletableFuture<PressureMetrics.Snapshot> future = validator.startTest();

      // 异步返回初始状态
      Map<String, Object> result = new HashMap<>();
      result.put("state", validator.getState().name());
      result.put("config", configToMap());
      result.put("message", "Test started successfully");

      return ZResponse.success(result);

    } catch (Exception e) {
      _Logger.warning("Failed to start test: %s", e.getMessage());
      return ZResponse.error(e.getMessage());
    }
  }

  /** 停止压力测试 */
  @PostMapping("/stop")
  public ZResponse<?> stopTest() {
    try {
      validator.stopTest();
      return ZResponse.success(
          Map.of("state", validator.getState().name(), "message", "Test stopped"));
    } catch (Exception e) {
      return ZResponse.error(e.getMessage());
    }
  }

  /** 强制停止压力测试 */
  @PostMapping("/force-stop")
  public ZResponse<?> forceStopTest() {
    try {
      validator.forceStopTest();
      return ZResponse.success(
          Map.of("state", validator.getState().name(), "message", "Test force stopped"));
    } catch (Exception e) {
      return ZResponse.error(e.getMessage());
    }
  }

  /** 获取当前测试状态 */
  @GetMapping("/status")
  public ZResponse<?> getStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("state", validator.getState().name());
    status.put("activeClients", validator.getActiveClientCount());
    status.put("pendingRequests", validator.getTotalPendingRequests());

    PressureMetrics.Snapshot snapshot = validator.getMetrics().getSnapshot();
    status.put("totalRequests", snapshot.totalRequests);
    status.put("successRequests", snapshot.successRequests);
    status.put("failedRequests", snapshot.failedRequests);
    status.put("qps", Math.round(snapshot.qps * 100) / 100.0);
    status.put("successRate", Math.round(snapshot.successRate * 100) / 100.0);

    return ZResponse.success(status);
  }

  /** 获取详细性能指标 */
  @GetMapping("/metrics")
  public ZResponse<?> getMetrics() {
    PressureMetrics.Snapshot snapshot = validator.getMetrics().getSnapshot();
    return ZResponse.success(snapshot);
  }

  /** 获取性能报告（格式化文本） */
  @GetMapping("/report")
  public ZResponse<String> getReport() {
    PressureMetrics.Snapshot snapshot = validator.getMetrics().getSnapshot();
    return ZResponse.success(snapshot.toReport());
  }

  /** 更新测试配置 */
  @PostMapping("/config")
  public ZResponse<?> updateConfig(@RequestBody ConfigRequest request) {
    try {
      if (request.concurrency != null) {
        config.setConcurrency(request.concurrency);
      }
      if (request.durationSeconds != null) {
        config.setDuration(Duration.ofSeconds(request.durationSeconds));
      }
      if (request.requestsPerSecondPerClient != null) {
        config.setRequestsPerSecondPerClient(request.requestsPerSecondPerClient);
      }
      if (request.targetHost != null) {
        config.getTarget().setHost(request.targetHost);
      }
      if (request.targetPort != null) {
        config.getTarget().setPort(request.targetPort);
      }
      if (request.protocol != null) {
        config.setProtocol(request.protocol);
      }

      return ZResponse.success(Map.of("message", "Config updated", "config", configToMap()));
    } catch (Exception e) {
      return ZResponse.error(e.getMessage());
    }
  }

  /** 获取当前配置 */
  @GetMapping("/config")
  public ZResponse<?> getConfig() {
    return ZResponse.success(configToMap());
  }

  // ==================== 请求/响应类 ====================

  /** 启动请求参数 */
  public static class StartRequest {
    public Integer concurrency;
    public Integer durationSeconds;
    public Integer requestsPerSecondPerClient;
    public String targetHost;
    public Integer targetPort;
    public String protocol;
  }

  /** 配置请求参数 */
  public static class ConfigRequest {
    public Integer concurrency;
    public Integer durationSeconds;
    public Integer requestsPerSecondPerClient;
    public String targetHost;
    public Integer targetPort;
    public String protocol;
  }

  // ==================== 辅助方法 ====================

  private void applyRequestConfig(StartRequest request) {
    if (request.concurrency != null) {
      config.setConcurrency(request.concurrency);
    }
    if (request.durationSeconds != null) {
      config.setDuration(Duration.ofSeconds(request.durationSeconds));
    }
    if (request.requestsPerSecondPerClient != null) {
      config.setRequestsPerSecondPerClient(request.requestsPerSecondPerClient);
    }
    if (request.targetHost != null) {
      config.getTarget().setHost(request.targetHost);
    }
    if (request.targetPort != null) {
      config.getTarget().setPort(request.targetPort);
    }
    if (request.protocol != null) {
      config.setProtocol(request.protocol);
    }
  }

  private Map<String, Object> configToMap() {
    Map<String, Object> map = new HashMap<>();
    map.put(
        "target",
        Map.of(
            "host", config.getTarget().getHost(),
            "port", config.getTarget().getPort()));
    map.put("concurrency", config.getConcurrency());
    map.put("requestsPerSecondPerClient", config.getRequestsPerSecondPerClient());
    map.put("targetTotalQps", config.getTargetTotalQps());
    map.put("durationSeconds", config.getDuration().getSeconds());
    map.put("protocol", config.getProtocol());
    map.put("payloadSize", config.getPayloadSize());
    map.put("connectionRate", config.getConnectionRate());
    return map;
  }
}
