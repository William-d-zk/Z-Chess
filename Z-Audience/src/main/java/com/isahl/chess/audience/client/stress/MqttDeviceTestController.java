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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 基于设备的 MQTT 测试控制器
 *
 * <p>使用已注册的测试设备进行 MQTT 连接和消息测试
 *
 * <p>支持的测试： - /api/mqtt/connect - 单设备连接测试 - /api/mqtt/stress - 多设备并发压力测试 - /api/mqtt/verify -
 * 验证所有设备连接
 */
@RestController
@RequestMapping("/api/mqtt")
@Profile({"test", "stress", "docker"})
public class MqttDeviceTestController {
  private static final Logger _Logger = LoggerFactory.getLogger("stress.mqtt");

  private final DeviceCredentialLoader deviceLoader;
  private final ExecutorService executorService;

  @Autowired
  public MqttDeviceTestController(DeviceCredentialLoader deviceLoader) {
    this.deviceLoader = deviceLoader;
    ThreadFactory threadFactory =
        r -> {
          Thread t = new Thread(r, "mqtt-stress");
          t.setDaemon(true);
          return t;
        };
    this.executorService = Executors.newFixedThreadPool(100, threadFactory);
  }

  /** 单设备 MQTT 连接测试 */
  @GetMapping("/connect")
  public ZResponse<?> testSingleConnection(
      @RequestParam(defaultValue = "localhost") String host,
      @RequestParam(defaultValue = "1883") int port,
      @RequestParam(required = false) String username,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String clientId) {
    // 如果没有提供凭证，使用随机设备
    if (username == null || password == null || clientId == null) {
      DeviceCredentialLoader.DeviceCredential device = deviceLoader.getRandomDevice();
      if (device == null) {
        return ZResponse.error("No device credentials available");
      }
      username = device.mqttUsername;
      password = device.mqttPassword;
      clientId = device.mqttClientId;
    }

    String serverUri = String.format("tcp://%s:%d", host, port);

    MqttClient client = null;
    try {
      long startTime = System.currentTimeMillis();

      client = new MqttClient(serverUri, clientId, new MemoryPersistence());
      MqttConnectOptions options = new MqttConnectOptions();
      options.setUserName(username);
      options.setPassword(password.toCharArray());
      options.setConnectionTimeout(10);
      options.setKeepAliveInterval(60);
      options.setCleanSession(true);

      client.connect(options);

      long connectTime = System.currentTimeMillis() - startTime;

      boolean connected = client.isConnected();

      client.disconnect();

      return ZResponse.success(
          Map.of(
              "test",
              "singleConnect",
              "server",
              serverUri,
              "clientId",
              clientId.substring(0, Math.min(16, clientId.length())) + "...",
              "connected",
              connected,
              "connectTimeMs",
              connectTime,
              "status",
              connected ? "SUCCESS" : "FAILED"));
    } catch (MqttException e) {
      _Logger.warn("MQTT connection failed: %s", e.getMessage());
      return ZResponse.success(
          Map.of(
              "test",
              "singleConnect",
              "server",
              serverUri,
              "error",
              e.getMessage(),
              "reasonCode",
              e.getReasonCode(),
              "status",
              "FAILED"));
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (MqttException e) {
          _Logger.warn("Failed to close MQTT client: %s", e.getMessage());
        }
      }
    }
  }

  /** 多设备并发连接测试 */
  @PostMapping("/stress")
  public ZResponse<?> stressTestConnections(@RequestBody MqttStressRequest request) {
    String host = request.host != null ? request.host : "localhost";
    int port = request.port != null ? request.port : 1883;
    int deviceCount = request.deviceCount != null ? request.deviceCount : 10;
    int connectionsPerDevice =
        request.connectionsPerDevice != null ? request.connectionsPerDevice : 1;

    if (!deviceLoader.hasDevices()) {
      return ZResponse.error("No device credentials available");
    }

    String serverUri = String.format("tcp://%s:%d", host, port);
    List<DeviceCredentialLoader.DeviceCredential> devices = deviceLoader.getDevices(deviceCount);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    AtomicLong totalConnectTime = new AtomicLong(0);
    List<Long> connectTimes = new CopyOnWriteArrayList<>();
    Map<Integer, String> errors = new ConcurrentHashMap<>();

    long testStart = System.currentTimeMillis();

    // 创建并发连接任务
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    int totalConnections = deviceCount * connectionsPerDevice;

    for (int i = 0; i < totalConnections; i++) {
      final int index = i;
      DeviceCredentialLoader.DeviceCredential device = devices.get(i % devices.size());
      String clientId = device.mqttClientId + "_" + (i / devices.size());

      CompletableFuture<Void> future =
          CompletableFuture.runAsync(
              () -> {
                MqttClient client = null;
                try {
                  long startTime = System.currentTimeMillis();

                  client = new MqttClient(serverUri, clientId, new MemoryPersistence());
                  MqttConnectOptions options = new MqttConnectOptions();
                  options.setUserName(device.mqttUsername);
                  options.setPassword(device.mqttPassword.toCharArray());
                  options.setConnectionTimeout(10);
                  options.setKeepAliveInterval(60);
                  options.setCleanSession(true);

                  client.connect(options);

                  long connectTime = System.currentTimeMillis() - startTime;

                  if (client.isConnected()) {
                    successCount.incrementAndGet();
                    totalConnectTime.addAndGet(connectTime);
                    connectTimes.add(connectTime);

                    client.disconnect();
                  } else {
                    failCount.incrementAndGet();
                  }
                } catch (MqttException e) {
                  failCount.incrementAndGet();
                  errors.put(index, e.getMessage());
                } finally {
                  if (client != null) {
                    try {
                      client.close();
                    } catch (MqttException e) {
                      _Logger.warn("Failed to close MQTT client: %s", e.getMessage());
                    }
                  }
                }
              },
              executorService);

      futures.add(future);
    }

    // 等待所有连接完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    long totalTime = System.currentTimeMillis() - testStart;

    double avgConnectTime =
        successCount.get() > 0 ? totalConnectTime.get() * 1.0 / successCount.get() : 0;
    long minConnectTime = connectTimes.stream().mapToLong(Long::longValue).min().orElse(0);
    long maxConnectTime = connectTimes.stream().mapToLong(Long::longValue).max().orElse(0);

    Map<String, Object> result = new HashMap<>();
    result.put("test", "mqttStress");
    result.put("server", serverUri);
    result.put("deviceCount", deviceCount);
    result.put("connectionsPerDevice", connectionsPerDevice);
    result.put("totalConnections", totalConnections);
    result.put("success", successCount.get());
    result.put("failed", failCount.get());
    result.put("successRate", Math.round(successCount.get() * 10000.0 / totalConnections) / 100.0);
    result.put("totalTimeMs", totalTime);
    result.put("avgConnectTimeMs", Math.round(avgConnectTime * 100) / 100.0);
    result.put("minConnectTimeMs", minConnectTime);
    result.put("maxConnectTimeMs", maxConnectTime);
    result.put(
        "connectionsPerSecond", Math.round(totalConnections * 1000.0 / totalTime * 100) / 100.0);

    if (!errors.isEmpty() && errors.size() <= 5) {
      result.put("sampleErrors", errors);
    } else if (!errors.isEmpty()) {
      result.put("errorCount", errors.size());
      result.put(
          "sampleErrors",
          errors.entrySet().stream()
              .limit(5)
              .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll));
    }

    return ZResponse.success(result);
  }

  /** 验证所有设备连接 */
  @GetMapping("/verify")
  public ZResponse<?> verifyAllDevices(
      @RequestParam(defaultValue = "localhost") String host,
      @RequestParam(defaultValue = "1883") int port,
      @RequestParam(defaultValue = "10") int limit) {
    if (!deviceLoader.hasDevices()) {
      return ZResponse.error("No device credentials available");
    }

    List<DeviceCredentialLoader.DeviceCredential> devices =
        deviceLoader.getAllDevices().stream().limit(limit).toList();

    String serverUri = String.format("tcp://%s:%d", host, port);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);
    List<Map<String, Object>> deviceResults = new CopyOnWriteArrayList<>();

    // 串行测试每个设备（避免过载）
    for (DeviceCredentialLoader.DeviceCredential device : devices) {
      MqttClient client = null;
      try {
        client = new MqttClient(serverUri, device.mqttClientId, new MemoryPersistence());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(device.mqttUsername);
        options.setPassword(device.mqttPassword.toCharArray());
        options.setConnectionTimeout(5);
        options.setCleanSession(true);

        client.connect(options);

        boolean connected = client.isConnected();
        if (connected) {
          successCount.incrementAndGet();
        } else {
          failCount.incrementAndGet();
        }

        Map<String, Object> deviceResult = new HashMap<>();
        deviceResult.put("username", device.username);
        deviceResult.put(
            "clientId",
            device.mqttClientId.substring(0, Math.min(16, device.mqttClientId.length())) + "...");
        deviceResult.put("status", connected ? "SUCCESS" : "FAILED");
        deviceResults.add(deviceResult);

        client.disconnect();

        // 短暂延迟避免过载
        Thread.sleep(100);
      } catch (Exception e) {
        failCount.incrementAndGet();
        Map<String, Object> deviceResult = new HashMap<>();
        deviceResult.put("username", device.username);
        deviceResult.put(
            "clientId",
            device.mqttClientId.substring(0, Math.min(16, device.mqttClientId.length())) + "...");
        deviceResult.put("status", "ERROR");
        deviceResult.put("error", e.getMessage());
        deviceResults.add(deviceResult);
      } finally {
        if (client != null) {
          try {
            client.close();
          } catch (MqttException e) {
            _Logger.warn("Failed to close MQTT client: %s", e.getMessage());
          }
        }
      }
    }

    return ZResponse.success(
        Map.of(
            "test",
            "verifyDevices",
            "server",
            serverUri,
            "totalDevices",
            devices.size(),
            "success",
            successCount.get(),
            "failed",
            failCount.get(),
            "successRate",
            Math.round(successCount.get() * 10000.0 / devices.size()) / 100.0,
            "results",
            deviceResults));
  }

  /** 获取可用设备数量 */
  @GetMapping("/devices/count")
  public ZResponse<?> getDeviceCount() {
    return ZResponse.success(
        Map.of(
            "totalDevices", deviceLoader.getDeviceCount(),
            "hasDevices", deviceLoader.hasDevices()));
  }

  /** 获取随机设备信息（脱敏） */
  @GetMapping("/devices/sample")
  public ZResponse<?> getSampleDevice() {
    DeviceCredentialLoader.DeviceCredential device = deviceLoader.getRandomDevice();
    if (device == null) {
      return ZResponse.error("No device credentials available");
    }

    return ZResponse.success(
        Map.of(
            "username",
            device.username,
            "clientId",
            device.mqttClientId.substring(0, Math.min(16, device.mqttClientId.length())) + "...",
            "tokenPrefix",
            device.token.substring(0, Math.min(8, device.token.length())) + "...",
            "expireDate",
            device.expireDate));
  }

  // ==================== 请求类 ====================

  public static class MqttStressRequest {
    public String host;
    public Integer port;
    public Integer deviceCount;
    public Integer connectionsPerDevice;
  }
}
