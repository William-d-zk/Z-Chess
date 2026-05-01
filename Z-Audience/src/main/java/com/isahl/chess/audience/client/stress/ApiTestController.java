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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP API 测试控制器
 *
 * <p>用于对 Z-Chess 集群的 REST API 进行压力测试
 *
 * <p>支持的测试端点： - /api/test/health - 集群健康检查 - /api/test/device - 设备相关API测试 - /api/test/full -
 * 完整API测试套件
 */
@RestController
@RequestMapping("/api/test")
@Profile({"test", "stress", "docker"})
public class ApiTestController {
  private static final Logger _Logger = LoggerFactory.getLogger("stress.api");

  private final HttpClient httpClient;
  private final ExecutorService executorService;

  @Autowired
  public ApiTestController() {
    ThreadFactory httpThreadFactory =
        r -> {
          Thread t = new Thread(r, "stress-http");
          t.setDaemon(true);
          return t;
        };
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .executor(Executors.newFixedThreadPool(50, httpThreadFactory))
            .build();
    ThreadFactory taskThreadFactory =
        r -> {
          Thread t = new Thread(r, "stress-task");
          t.setDaemon(true);
          return t;
        };
    this.executorService = Executors.newFixedThreadPool(50, taskThreadFactory);
  }

  /**
   * 健康检查测试
   *
   * <p>测试集群所有节点的健康状态
   */
  @GetMapping("/health")
  public ZResponse<?> testHealth(
      @RequestParam(defaultValue = "localhost") String host,
      @RequestParam(defaultValue = "8080") int port,
      @RequestParam(defaultValue = "3") int nodes) {
    List<Map<String, Object>> results = new ArrayList<>();

    for (int i = 0; i < nodes; i++) {
      int nodePort = port + i;
      String url = String.format("http://%s:%d/actuator/health", host, nodePort);

      try {
        long startTime = System.currentTimeMillis();
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        long latency = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("node", "raft0" + i);
        result.put("port", nodePort);
        result.put("status", response.statusCode() == 200 ? "UP" : "DOWN");
        result.put("statusCode", response.statusCode());
        result.put("latencyMs", latency);
        results.add(result);
      } catch (Exception e) {
        Map<String, Object> result = new HashMap<>();
        result.put("node", "raft0" + i);
        result.put("port", nodePort);
        result.put("status", "ERROR");
        result.put("error", e.getMessage());
        results.add(result);
      }
    }

    return ZResponse.success(
        Map.of(
            "test",
            "health",
            "results",
            results,
            "totalNodes",
            nodes,
            "healthyNodes",
            results.stream().filter(r -> "UP".equals(r.get("status"))).count()));
  }

  /** 设备列表查询测试 */
  @GetMapping("/devices")
  public ZResponse<?> testListDevices(
      @RequestParam(defaultValue = "localhost") String host,
      @RequestParam(defaultValue = "8080") int port,
      @RequestParam(defaultValue = "1") int iterations) {
    String url = String.format("http://%s:%d/api/v1/devices?page=0&size=20", host, port);

    List<Long> latencies = new ArrayList<>();
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    long testStart = System.currentTimeMillis();

    for (int i = 0; i < iterations; i++) {
      try {
        long startTime = System.currentTimeMillis();
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        long latency = System.currentTimeMillis() - startTime;
        latencies.add(latency);

        if (response.statusCode() == 200) {
          successCount.incrementAndGet();
        } else {
          failCount.incrementAndGet();
        }
      } catch (Exception e) {
        failCount.incrementAndGet();
        _Logger.warn("Request failed: %s", e.getMessage());
      }
    }

    long totalTime = System.currentTimeMillis() - testStart;
    double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
    long minLatency = latencies.stream().mapToLong(Long::longValue).min().orElse(0);
    long maxLatency = latencies.stream().mapToLong(Long::longValue).max().orElse(0);

    return ZResponse.success(
        Map.of(
            "test",
            "listDevices",
            "iterations",
            iterations,
            "success",
            successCount.get(),
            "failed",
            failCount.get(),
            "totalTimeMs",
            totalTime,
            "avgLatencyMs",
            Math.round(avgLatency * 100) / 100.0,
            "minLatencyMs",
            minLatency,
            "maxLatencyMs",
            maxLatency,
            "qps",
            Math.round(iterations * 1000.0 / totalTime * 100) / 100.0));
  }

  /** 并发API压力测试 */
  @PostMapping("/stress")
  public ZResponse<?> stressTestApi(@RequestBody StressApiRequest request) {
    int concurrency = request.concurrency != null ? request.concurrency : 10;
    int requestsPerClient = request.requestsPerClient != null ? request.requestsPerClient : 10;
    String host = request.host != null ? request.host : "localhost";
    int port = request.port != null ? request.port : 8080;
    String endpoint = request.endpoint != null ? request.endpoint : "/actuator/health";

    String url = String.format("http://%s:%d%s", host, port, endpoint);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    AtomicLong totalLatency = new AtomicLong(0);
    List<Long> latencies = java.util.Collections.synchronizedList(new ArrayList<>());

    long testStart = System.currentTimeMillis();

    // 创建并发请求
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int c = 0; c < concurrency; c++) {
      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                for (int r = 0; r < requestsPerClient; r++) {
                  try {
                    long startTime = System.currentTimeMillis();
                    HttpRequest httpRequest =
                        HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .GET()
                            .timeout(Duration.ofSeconds(10))
                            .build();

                    HttpResponse<String> response =
                        httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                    long latency = System.currentTimeMillis() - startTime;
                    totalLatency.addAndGet(latency);
                    latencies.add(latency);

                    if (response.statusCode() == 200) {
                      successCount.incrementAndGet();
                    } else {
                      failCount.incrementAndGet();
                    }
                  } catch (Exception e) {
                    failCount.incrementAndGet();
                  }
                }
              },
              executorService);

      futures.add(future);
    }

    // 等待所有请求完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    long totalTime = System.currentTimeMillis() - testStart;
    int totalRequests = concurrency * requestsPerClient;
    double avgLatency =
        latencies.isEmpty()
            ? 0
            : latencies.stream().mapToLong(Long::longValue).average().getAsDouble();

    Map<String, Object> result = new HashMap<>();
    result.put("test", "stressApi");
    result.put("url", url);
    result.put("concurrency", concurrency);
    result.put("requestsPerClient", requestsPerClient);
    result.put("totalRequests", totalRequests);
    result.put("success", successCount.get());
    result.put("failed", failCount.get());
    result.put("successRate", Math.round(successCount.get() * 10000.0 / totalRequests) / 100.0);
    result.put("totalTimeMs", totalTime);
    result.put("avgLatencyMs", Math.round(avgLatency * 100) / 100.0);
    result.put("qps", Math.round(totalRequests * 1000.0 / totalTime * 100) / 100.0);
    return ZResponse.success(result);
  }

  /** 完整API测试套件 */
  @PostMapping("/full")
  public ZResponse<?> runFullApiTest(@RequestBody FullTestRequest request) {
    String host = request.host != null ? request.host : "localhost";
    int startPort = request.startPort != null ? request.startPort : 8080;
    int nodes = request.nodes != null ? request.nodes : 3;

    Map<String, Object> results = new HashMap<>();

    // 1. 健康检查
    _Logger.info("Running health check test...");
    ZResponse<?> healthResponse = testHealth(host, startPort, nodes);
    results.put("health", healthResponse.getDetail());

    // 2. 设备列表查询
    _Logger.info("Running device list test...");
    ZResponse<?> deviceResponse = testListDevices(host, startPort, 10);
    results.put("devices", deviceResponse.getDetail());

    // 3. 压力测试（轻量级）
    _Logger.info("Running light stress test...");
    StressApiRequest stressRequest = new StressApiRequest();
    stressRequest.host = host;
    stressRequest.port = startPort;
    stressRequest.concurrency = 10;
    stressRequest.requestsPerClient = 10;
    ZResponse<?> stressResponse = stressTestApi(stressRequest);
    results.put("stress", stressResponse.getDetail());

    return ZResponse.success(
        Map.of(
            "testSuite", "fullApi",
            "timestamp", System.currentTimeMillis(),
            "targetHost", host,
            "targetPort", startPort,
            "results", results));
  }

  // ==================== 请求类 ====================

  public static class StressApiRequest {
    public String host;
    public Integer port;
    public String endpoint;
    public Integer concurrency;
    public Integer requestsPerClient;
  }

  public static class FullTestRequest {
    public String host;
    public Integer startPort;
    public Integer nodes;
  }
}
