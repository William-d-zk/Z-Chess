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
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X111_QttConnect;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X112_QttConnack;
import com.isahl.chess.bishop.protocol.mqtt.ctrl.X11E_QttDisconnect;
import com.isahl.chess.bishop.protocol.mqtt.model.QttPropertySet;
import com.isahl.chess.king.base.log.Logger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MQTT 重定向处理器
 *
 * <p>处理 MQTT 连接过程中的服务器重定向逻辑：
 *
 * <ul>
 *   <li>CONNECT 处理：检查是否需要重定向新连接
 *   <li>CONNACK 响应：返回重定向信息
 *   <li>DISCONNECT 处理：优雅关闭并通知客户端重定向
 * </ul>
 *
 * @author william.d.zk
 * @since 1.1.1
 */
@Component
public class MqttRedirectHandler {

  private static final Logger _Logger =
      Logger.getLogger("endpoint.pawn." + MqttRedirectHandler.class.getSimpleName());

  /** 重定向计数存储（防止循环重定向） */
  private final Map<String, Integer> _redirectCounts = new ConcurrentHashMap<>();

  /** 客户端最近一次重定向时间（用于限流） */
  private final Map<String, Long> _lastRedirectTime = new ConcurrentHashMap<>();

  /** 重定向最小间隔（毫秒） */
  private static final long REDIRECT_COOLDOWN_MS = 5000;

  @Autowired private ServerRedirectService _redirectService;

  @Autowired private LoadBalancer _loadBalancer;

  // 用于测试注入的 setter 方法
  public void setRedirectService(ServerRedirectService redirectService) {
    this._redirectService = redirectService;
  }

  public void setLoadBalancer(LoadBalancer loadBalancer) {
    this._loadBalancer = loadBalancer;
  }

  // ==================== CONNECT 处理 ====================

  /**
   * 处理 MQTT CONNECT 报文，决策是否需要重定向
   *
   * @param connect CONNECT 报文
   * @return 如果需要重定向返回 CONNACK 报文，否则返回 null
   */
  public X112_QttConnack handleConnect(X111_QttConnect connect) {
    if (!_redirectService.isRedirectEnabled()) {
      return null;
    }

    String clientId = connect.getClientId();
    int redirectCount = getRedirectCount(clientId);

    // 检查重定向冷却时间
    if (isInCooldown(clientId)) {
      _Logger.debug("Client {} is in redirect cooldown, skipping redirect", clientId);
      return null;
    }

    // 决策重定向
    ServerRedirectService.RedirectDecision decision =
        _redirectService.decideRedirect(clientId, redirectCount);

    if (decision == null) {
      // 无需重定向，重置计数
      resetRedirectCount(clientId);
      return null;
    }

    // 创建重定向 CONNACK
    X112_QttConnack connack = createRedirectConnack(decision);

    // 记录重定向
    recordRedirect(clientId, decision);

    _Logger.info(
        "Redirecting client {} to server: {} (reason: {})",
        clientId,
        decision.getTargetServer(),
        decision.getReason());

    return connack;
  }

  /**
   * 处理维护模式下的 CONNECT
   *
   * <p>当服务器进入维护模式时，将所有新连接引导至其他服务器。
   *
   * @param connect CONNECT 报文
   * @return 重定向 CONNACK 报文
   */
  public X112_QttConnack handleMaintenanceConnect(X111_QttConnect connect) {
    String clientId = connect.getClientId();
    int redirectCount = getRedirectCount(clientId);

    ServerRedirectService.RedirectDecision decision =
        _redirectService.decideMaintenanceRedirect(clientId, redirectCount);

    if (decision == null) {
      _Logger.warning("No available server for maintenance redirect of client: {}", clientId);
      // 返回服务器不可用
      X112_QttConnack connack = new X112_QttConnack();
      connack.rejectServerUnavailable();
      return connack;
    }

    X112_QttConnack connack = createRedirectConnack(decision);
    recordRedirect(clientId, decision);

    _Logger.info(
        "Maintenance redirect for client {} to server: {}", clientId, decision.getTargetServer());

    return connack;
  }

  // ==================== DISCONNECT 处理 ====================

  /**
   * 创建优雅迁移的 DISCONNECT 报文
   *
   * <p>当服务器需要下线或迁移时，通知已连接的客户端切换到新服务器。
   *
   * @param reason 重定向原因
   * @param targetServer 目标服务器
   * @return DISCONNECT 报文
   */
  public X11E_QttDisconnect createRedirectDisconnect(RedirectReason reason, String targetServer) {
    X11E_QttDisconnect disconnect = new X11E_QttDisconnect();

    // 设置 MQTT v5 属性
    QttPropertySet properties = new QttPropertySet();
    properties.setServerReference(targetServer);
    properties.setReasonString(
        String.format("Server redirect to %s (%s)", targetServer, reason.getDescription()));

    disconnect.setProperties(properties);
    disconnect.setCode(reason.getCode());

    _Logger.info("Created redirect DISCONNECT to server: {} (reason: {})", targetServer, reason);

    return disconnect;
  }

  /**
   * 处理服务器迁移，向所有已连接客户端发送重定向通知
   *
   * @param newServerRef 新服务器地址
   * @return 需要发送的 DISCONNECT 报文列表
   */
  public Map<String, X11E_QttDisconnect> handleServerMigration(String newServerRef) {
    Map<String, X11E_QttDisconnect> disconnects = new ConcurrentHashMap<>();

    // 这里假设可以获取所有已连接的客户端会话
    // 实际实现需要与 SessionManager 集成

    X11E_QttDisconnect disconnect =
        createRedirectDisconnect(RedirectReason.SERVER_MOVED, newServerRef);

    _Logger.info("Prepared server migration redirects to: {}", newServerRef);

    return disconnects;
  }

  // ==================== 重定向计数管理 ====================

  /** 获取客户端重定向计数 */
  public int getRedirectCount(String clientId) {
    return _redirectCounts.getOrDefault(clientId, 0);
  }

  /** 重置客户端重定向计数 */
  public void resetRedirectCount(String clientId) {
    _redirectCounts.remove(clientId);
    _lastRedirectTime.remove(clientId);
  }

  /** 记录重定向 */
  void recordRedirect(String clientId, ServerRedirectService.RedirectDecision decision) {
    _redirectCounts.put(clientId, decision.getRedirectCount());
    _lastRedirectTime.put(clientId, System.currentTimeMillis());
  }

  /** 检查是否在重定向冷却期内 */
  private boolean isInCooldown(String clientId) {
    Long lastTime = _lastRedirectTime.get(clientId);
    if (lastTime == null) {
      return false;
    }
    return System.currentTimeMillis() - lastTime < REDIRECT_COOLDOWN_MS;
  }

  // ==================== 辅助方法 ====================

  /** 创建重定向 CONNACK 报文 */
  private X112_QttConnack createRedirectConnack(ServerRedirectService.RedirectDecision decision) {
    X112_QttConnack connack = new X112_QttConnack();

    // 设置原因码
    connack.setCode(decision.getReason().getCode());

    // 设置服务器引用
    connack.setServerReference(decision.getTargetServer());

    // 设置原因字符串
    connack.setReasonString(
        String.format(
            "Redirect to %s (%s)",
            decision.getTargetServer(), decision.getReason().getDescription()));

    return connack;
  }

  /**
   * 使用负载均衡器选择服务器
   *
   * @param clientId 客户端 ID
   * @return 选中的服务器
   */
  public String selectServerWithLoadBalancer(String clientId) {
    return _loadBalancer.selectServer(clientId);
  }

  /** 更新负载均衡器服务器列表 */
  public void updateLoadBalancerServers(Map<String, Integer> servers) {
    // 清除现有服务器
    for (String server : _loadBalancer.getServers().keySet()) {
      _loadBalancer.removeServer(server);
    }

    // 添加新服务器
    for (Map.Entry<String, Integer> entry : servers.entrySet()) {
      _loadBalancer.addServer(entry.getKey(), entry.getValue());
    }

    _Logger.info("Updated load balancer servers: {}", servers.keySet());
  }

  /** 设置负载均衡策略 */
  public void setLoadBalancerStrategy(LoadBalancer.Strategy strategy) {
    _loadBalancer.setStrategy(strategy);
  }

  // ==================== 状态查询 ====================

  /** 获取重定向处理器状态 */
  public RedirectHandlerStatus getStatus() {
    return new RedirectHandlerStatus(
        _redirectService.isRedirectEnabled(), _redirectCounts.size(), _loadBalancer.getStats());
  }

  @Override
  public String toString() {
    return String.format(
        "MqttRedirectHandler{enabled=%s, pendingRedirects=%d}",
        _redirectService.isRedirectEnabled(), _redirectCounts.size());
  }

  // ==================== 状态类 ====================

  /** 重定向处理器状态 */
  public static class RedirectHandlerStatus {
    private final boolean redirectEnabled;
    private final int pendingRedirectCount;
    private final LoadBalancer.LoadBalancerStats loadBalancerStats;

    public RedirectHandlerStatus(
        boolean redirectEnabled,
        int pendingRedirectCount,
        LoadBalancer.LoadBalancerStats loadBalancerStats) {
      this.redirectEnabled = redirectEnabled;
      this.pendingRedirectCount = pendingRedirectCount;
      this.loadBalancerStats = loadBalancerStats;
    }

    public boolean isRedirectEnabled() {
      return redirectEnabled;
    }

    public int getPendingRedirectCount() {
      return pendingRedirectCount;
    }

    public LoadBalancer.LoadBalancerStats getLoadBalancerStats() {
      return loadBalancerStats;
    }

    @Override
    public String toString() {
      return String.format(
          "RedirectHandlerStatus{enabled=%s, pending=%d, lb=%s}",
          redirectEnabled, pendingRedirectCount, loadBalancerStats);
    }
  }
}
