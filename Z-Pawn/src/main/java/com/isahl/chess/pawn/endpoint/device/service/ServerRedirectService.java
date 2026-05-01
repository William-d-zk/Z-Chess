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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.bishop.mqtt.v5.RedirectReason;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * MQTT 服务器重定向服务
 *
 * <p>负责决策客户端是否需要重定向到其他服务器，以及重定向到哪个服务器。 支持以下场景：
 *
 * <ul>
 *   <li>负载均衡：将客户端引导至负载较低的服务器
 *   <li>服务器维护：将客户端引导至备用服务器
 *   <li>服务器迁移：永久迁移客户端到新服务器
 * </ul>
 *
 * @author william.d.zk
 * @since 1.1.1
 */
@Service
public class ServerRedirectService {

  private static final Logger _Logger =
      LoggerFactory.getLogger("endpoint.pawn." + ServerRedirectService.class.getSimpleName());

  /** 最大重定向次数，防止循环重定向 */
  public static final int MAX_REDIRECT_HOPS = 3;

  /** 服务器列表 */
  private final List<String> _servers = Collections.synchronizedList(new java.util.ArrayList<>());

  /** 服务器权重映射 */
  private final Map<String, Integer> _serverWeights = new ConcurrentHashMap<>();

  /** 服务器健康状态 */
  private final Map<String, Boolean> _serverHealth = new ConcurrentHashMap<>();

  /** 轮询计数器 */
  private final AtomicInteger _roundRobinCounter = new AtomicInteger(0);

  /** 是否启用重定向 */
  private volatile boolean _redirectEnabled = false;

  /** 当前服务器引用（用于拒绝重定向到自身） */
  private volatile String _currentServerRef = null;

  // ==================== 配置方法 ====================

  /** 启用/禁用重定向 */
  public void setRedirectEnabled(boolean enabled) {
    _redirectEnabled = enabled;
    _Logger.info("Server redirect {}: {}", enabled ? "enabled" : "disabled", this);
  }

  /** 检查是否启用重定向 */
  public boolean isRedirectEnabled() {
    return _redirectEnabled;
  }

  /**
   * 设置当前服务器引用
   *
   * @param serverRef 当前服务器引用（格式：host:port）
   */
  public void setCurrentServerRef(String serverRef) {
    _currentServerRef = serverRef;
    _Logger.debug("Current server reference set to: {}", serverRef);
  }

  /** 获取当前服务器引用 */
  public String getCurrentServerRef() {
    return _currentServerRef;
  }

  /**
   * 添加目标服务器
   *
   * @param serverRef 服务器引用（格式：host:port）
   * @param weight 权重（用于负载均衡）
   */
  public void addServer(String serverRef, int weight) {
    if (serverRef == null || serverRef.isEmpty()) {
      throw new IllegalArgumentException("Server reference cannot be null or empty");
    }
    if (weight <= 0) {
      throw new IllegalArgumentException("Weight must be positive");
    }

    synchronized (_servers) {
      if (!_servers.contains(serverRef)) {
        _servers.add(serverRef);
      }
    }
    _serverWeights.put(serverRef, weight);
    _serverHealth.put(serverRef, true);

    _Logger.info("Added server: {} with weight {}", serverRef, weight);
  }

  /** 移除目标服务器 */
  public void removeServer(String serverRef) {
    synchronized (_servers) {
      _servers.remove(serverRef);
    }
    _serverWeights.remove(serverRef);
    _serverHealth.remove(serverRef);

    _Logger.info("Removed server: {}", serverRef);
  }

  /**
   * 设置服务器健康状态
   *
   * @param serverRef 服务器引用
   * @param healthy 是否健康
   */
  public void setServerHealth(String serverRef, boolean healthy) {
    _serverHealth.put(serverRef, healthy);
    _Logger.debug("Server {} health status: {}", serverRef, healthy ? "healthy" : "unhealthy");
  }

  /** 检查服务器是否健康 */
  public boolean isServerHealthy(String serverRef) {
    return _serverHealth.getOrDefault(serverRef, false);
  }

  // ==================== 重定向决策 ====================

  /**
   * 决策是否需要重定向
   *
   * @param clientId 客户端 ID
   * @param redirectCount 已重定向次数
   * @return 重定向决策结果，不需要重定向返回 null
   */
  public RedirectDecision decideRedirect(String clientId, int redirectCount) {
    if (!_redirectEnabled) {
      return null;
    }

    // 检查重定向次数限制
    if (redirectCount >= MAX_REDIRECT_HOPS) {
      _Logger.warn("Max redirect hops exceeded for client: {}", clientId);
      return null;
    }

    // 获取健康的服务器列表
    List<String> healthyServers = getHealthyServers();
    if (healthyServers.isEmpty()) {
      _Logger.warn("No healthy servers available for redirect");
      return null;
    }

    // 使用轮询策略选择服务器
    String targetServer = selectServerRoundRobin(healthyServers);
    if (targetServer == null || targetServer.equals(_currentServerRef)) {
      return null;
    }

    _Logger.info("Decided redirect for client {} to server: {}", clientId, targetServer);

    return new RedirectDecision(RedirectReason.USE_ANOTHER_SERVER, targetServer, redirectCount + 1);
  }

  /**
   * 决策维护模式重定向
   *
   * <p>当当前服务器需要维护时，将所有客户端引导至备用服务器。
   *
   * @param clientId 客户端 ID
   * @param redirectCount 已重定向次数
   * @return 重定向决策结果
   */
  public RedirectDecision decideMaintenanceRedirect(String clientId, int redirectCount) {
    if (redirectCount >= MAX_REDIRECT_HOPS) {
      return null;
    }

    List<String> healthyServers = getHealthyServers();
    if (healthyServers.isEmpty()) {
      _Logger.warn("No healthy servers available for maintenance redirect");
      return null;
    }

    String targetServer = selectServerRoundRobin(healthyServers);
    if (targetServer == null || targetServer.equals(_currentServerRef)) {
      return null;
    }

    _Logger.info(
        "Decided maintenance redirect for client {} to server: {}", clientId, targetServer);

    return new RedirectDecision(RedirectReason.SERVER_MOVED, targetServer, redirectCount + 1);
  }

  /**
   * 决策服务器迁移重定向
   *
   * <p>当服务器永久迁移时，通知客户端使用新服务器地址。
   *
   * @param newServerRef 新服务器引用
   * @param redirectCount 已重定向次数
   * @return 重定向决策结果
   */
  public RedirectDecision decideMigrationRedirect(String newServerRef, int redirectCount) {
    if (redirectCount >= MAX_REDIRECT_HOPS) {
      return null;
    }

    if (newServerRef == null || newServerRef.equals(_currentServerRef)) {
      return null;
    }

    _Logger.info("Decided migration redirect to new server: {}", newServerRef);

    return new RedirectDecision(RedirectReason.SERVER_MOVED, newServerRef, redirectCount + 1);
  }

  // ==================== 内部方法 ====================

  /** 获取健康的服务器列表 */
  private List<String> getHealthyServers() {
    return _serverHealth.entrySet().stream()
        .filter(Map.Entry::getValue)
        .map(Map.Entry::getKey)
        .filter(server -> !server.equals(_currentServerRef))
        .toList();
  }

  /** 轮询选择服务器 */
  private String selectServerRoundRobin(List<String> servers) {
    if (servers.isEmpty()) {
      return null;
    }
    int index = Math.abs(_roundRobinCounter.getAndIncrement() % servers.size());
    return servers.get(index);
  }

  /** 获取所有服务器列表 */
  public List<String> getServers() {
    synchronized (_servers) {
      return List.copyOf(_servers);
    }
  }

  /** 获取健康服务器数量 */
  public int getHealthyServerCount() {
    return (int) _serverHealth.values().stream().filter(Boolean::booleanValue).count();
  }

  @Override
  public String toString() {
    return String.format(
        "ServerRedirectService{enabled=%s, current=%s, servers=%d, healthy=%d}",
        _redirectEnabled, _currentServerRef, _servers.size(), getHealthyServerCount());
  }

  // ==================== 内部类：重定向决策 ====================

  /** 重定向决策结果 */
  public static class RedirectDecision {
    private final RedirectReason reason;
    private final String targetServer;
    private final int redirectCount;

    public RedirectDecision(RedirectReason reason, String targetServer, int redirectCount) {
      this.reason = reason;
      this.targetServer = targetServer;
      this.redirectCount = redirectCount;
    }

    /** 获取重定向原因 */
    public RedirectReason getReason() {
      return reason;
    }

    /** 获取目标服务器 */
    public String getTargetServer() {
      return targetServer;
    }

    /** 获取重定向次数 */
    public int getRedirectCount() {
      return redirectCount;
    }

    /** 是否为永久重定向 */
    public boolean isPermanent() {
      return reason.isPermanent();
    }

    @Override
    public String toString() {
      return String.format(
          "RedirectDecision{reason=%s, target=%s, count=%d}", reason, targetServer, redirectCount);
    }
  }
}
