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

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** 压力测试配置类 支持配置并发客户端数、请求速率、测试时长等参数 */
@Configuration("pressure_test_config")
@ConfigurationProperties(prefix = "z.chess.pressure")
@PropertySource("classpath:pressure-test.properties")
public class PressureTestConfig {
  /** 目标服务器配置 */
  private Target target = new Target();

  /** 并发连接数，默认 1000，最大支持 5000 */
  private int concurrency = 1000;

  /** 每个连接每秒请求数，默认 10 */
  private int requestsPerSecondPerClient = 10;

  /** 测试持续时间，默认 60 秒 */
  private Duration duration = Duration.ofSeconds(60);

  /** 连接建立超时时间，默认 10 秒 */
  private Duration connectTimeout = Duration.ofSeconds(10);

  /** 请求超时时间，默认 5 秒 */
  private Duration requestTimeout = Duration.ofSeconds(5);

  /** 连接建立速率（每秒新建连接数），默认 100 */
  private int connectionRate = 100;

  /** 是否使用心跳保持连接，默认 true */
  private boolean keepAlive = true;

  /** 心跳间隔（秒），默认 30 */
  private int heartbeatInterval = 30;

  /** 协议类型：mqtt, websocket, zchat */
  private String protocol = "mqtt";

  /** 消息 payload 大小（字节），默认 256 */
  private int payloadSize = 256;

  /** 是否打印详细日志，默认 false */
  private boolean verbose = false;

  /** 统计采样间隔（秒），默认 5 */
  private int statsInterval = 5;

  /** 预热时间（秒），默认 5 */
  private int warmUpSeconds = 5;

  public static class Target {
    private String host = "127.0.0.1";
    private int port = 1883;
    private String path = "/"; // WebSocket 路径

    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }
  }

  // ==================== Getters & Setters ====================

  public Target getTarget() {
    return target;
  }

  public void setTarget(Target target) {
    this.target = target;
  }

  public int getConcurrency() {
    return Math.min(concurrency, 5000);
  }

  public void setConcurrency(int concurrency) {
    this.concurrency = Math.min(concurrency, 5000);
  }

  public int getRequestsPerSecondPerClient() {
    return requestsPerSecondPerClient;
  }

  public void setRequestsPerSecondPerClient(int requestsPerSecondPerClient) {
    this.requestsPerSecondPerClient = requestsPerSecondPerClient;
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public int getConnectionRate() {
    return connectionRate;
  }

  public void setConnectionRate(int connectionRate) {
    this.connectionRate = connectionRate;
  }

  public boolean isKeepAlive() {
    return keepAlive;
  }

  public void setKeepAlive(boolean keepAlive) {
    this.keepAlive = keepAlive;
  }

  public int getHeartbeatInterval() {
    return heartbeatInterval;
  }

  public void setHeartbeatInterval(int heartbeatInterval) {
    this.heartbeatInterval = heartbeatInterval;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public int getPayloadSize() {
    return payloadSize;
  }

  public void setPayloadSize(int payloadSize) {
    this.payloadSize = payloadSize;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public int getStatsInterval() {
    return statsInterval;
  }

  public void setStatsInterval(int statsInterval) {
    this.statsInterval = statsInterval;
  }

  public int getWarmUpSeconds() {
    return warmUpSeconds;
  }

  public void setWarmUpSeconds(int warmUpSeconds) {
    this.warmUpSeconds = warmUpSeconds;
  }

  /** 获取总目标 QPS */
  public int getTargetTotalQps() {
    return concurrency * requestsPerSecondPerClient;
  }

  @Override
  public String toString() {
    return String.format(
        "PressureTestConfig{target=%s:%d, concurrency=%d, qps=%d, duration=%ds, protocol=%s}",
        target.getHost(),
        target.getPort(),
        concurrency,
        getTargetTotalQps(),
        duration.getSeconds(),
        protocol);
  }
}
