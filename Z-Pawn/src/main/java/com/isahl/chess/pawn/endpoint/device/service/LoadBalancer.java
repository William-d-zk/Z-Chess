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

import com.isahl.chess.king.base.log.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MQTT 服务器负载均衡器
 *
 * <p>提供多种负载均衡策略：
 *
 * <ul>
 *   <li>轮询 (Round Robin)：按顺序分配客户端到不同服务器
 *   <li>加权轮询 (Weighted Round Robin)：根据服务器权重分配
 *   <li>最少连接 (Least Connections)：选择当前连接数最少的服务器
 *   <li>一致性哈希 (Consistent Hashing)：根据客户端 ID 哈希选择服务器
 * </ul>
 *
 * @author william.d.zk
 * @since 1.1.1
 */
public class LoadBalancer {

  private static final Logger _Logger =
      Logger.getLogger("endpoint.pawn." + LoadBalancer.class.getSimpleName());

  /** 负载均衡策略枚举 */
  public enum Strategy {
    ROUND_ROBIN,
    WEIGHTED_ROUND_ROBIN,
    LEAST_CONNECTIONS,
    CONSISTENT_HASHING
  }

  private Strategy _strategy = Strategy.ROUND_ROBIN;

  /** 服务器列表及其权重 */
  private final Map<String, Integer> _servers = new ConcurrentHashMap<>();

  /** 服务器连接数统计 */
  private final Map<String, AtomicInteger> _connectionCounts = new ConcurrentHashMap<>();

  /** 轮询计数器 */
  private final AtomicInteger _roundRobinCounter = new AtomicInteger(0);

  /** 加权轮询当前权重 */
  private final AtomicInteger _currentWeight = new AtomicInteger(0);

  /** 加权轮询最大权重 */
  private volatile int _maxWeight = 0;

  /** 加权轮询 GCD */
  private volatile int _gcdWeight = 1;

  // ==================== 策略设置 ====================

  /** 设置负载均衡策略 */
  public void setStrategy(Strategy strategy) {
    this._strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
    _Logger.info("Load balancer strategy set to: {}", strategy);
  }

  /** 获取当前策略 */
  public Strategy getStrategy() {
    return _strategy;
  }

  // ==================== 服务器管理 ====================

  /**
   * 添加服务器
   *
   * @param serverRef 服务器引用（格式：host:port）
   * @param weight 权重（用于加权轮询策略）
   */
  public void addServer(String serverRef, int weight) {
    if (serverRef == null || serverRef.isEmpty()) {
      throw new IllegalArgumentException("Server reference cannot be null or empty");
    }
    if (weight <= 0) {
      weight = 1;
    }

    _servers.put(serverRef, weight);
    _connectionCounts.putIfAbsent(serverRef, new AtomicInteger(0));

    recalculateWeights();

    _Logger.info("Added server: {} with weight {}", serverRef, weight);
  }

  /** 移除服务器 */
  public void removeServer(String serverRef) {
    _servers.remove(serverRef);
    _connectionCounts.remove(serverRef);

    recalculateWeights();

    _Logger.info("Removed server: {}", serverRef);
  }

  /** 更新服务器权重 */
  public void updateWeight(String serverRef, int weight) {
    if (!_servers.containsKey(serverRef)) {
      throw new IllegalArgumentException("Server not found: " + serverRef);
    }
    if (weight <= 0) {
      weight = 1;
    }

    _servers.put(serverRef, weight);
    recalculateWeights();

    _Logger.info("Updated server {} weight to {}", serverRef, weight);
  }

  /** 获取所有服务器 */
  public Map<String, Integer> getServers() {
    return Map.copyOf(_servers);
  }

  // ==================== 连接数统计 ====================

  /** 增加服务器连接数 */
  public void incrementConnections(String serverRef) {
    _connectionCounts.computeIfAbsent(serverRef, k -> new AtomicInteger(0)).incrementAndGet();
  }

  /** 减少服务器连接数 */
  public void decrementConnections(String serverRef) {
    AtomicInteger count = _connectionCounts.get(serverRef);
    if (count != null) {
      count.decrementAndGet();
    }
  }

  /** 获取服务器当前连接数 */
  public int getConnectionCount(String serverRef) {
    AtomicInteger count = _connectionCounts.get(serverRef);
    return count != null ? count.get() : 0;
  }

  // ==================== 服务器选择 ====================

  /**
   * 选择服务器
   *
   * @param clientId 客户端 ID（用于一致性哈希策略）
   * @return 选中的服务器，无可用服务器返回 null
   */
  public String selectServer(String clientId) {
    if (_servers.isEmpty()) {
      return null;
    }

    return switch (_strategy) {
      case ROUND_ROBIN -> selectRoundRobin();
      case WEIGHTED_ROUND_ROBIN -> selectWeightedRoundRobin();
      case LEAST_CONNECTIONS -> selectLeastConnections();
      case CONSISTENT_HASHING -> selectConsistentHashing(clientId);
    };
  }

  /** 轮询选择 */
  private String selectRoundRobin() {
    List<String> serverList = new ArrayList<>(_servers.keySet());
    if (serverList.isEmpty()) {
      return null;
    }
    int index = Math.abs(_roundRobinCounter.getAndIncrement() % serverList.size());
    return serverList.get(index);
  }

  /** 加权轮询选择（平滑加权轮询算法） */
  private String selectWeightedRoundRobin() {
    List<String> serverList = new ArrayList<>(_servers.keySet());
    if (serverList.isEmpty()) {
      return null;
    }

    // 简单加权轮询实现
    int totalWeight = _servers.values().stream().mapToInt(Integer::intValue).sum();
    int sequence = Math.abs(_roundRobinCounter.getAndIncrement() % totalWeight);

    int currentSum = 0;
    for (Map.Entry<String, Integer> entry : _servers.entrySet()) {
      currentSum += entry.getValue();
      if (sequence < currentSum) {
        return entry.getKey();
      }
    }

    return serverList.get(0);
  }

  /** 最少连接选择 */
  private String selectLeastConnections() {
    String selected = null;
    int minConnections = Integer.MAX_VALUE;

    for (String server : _servers.keySet()) {
      int connections = getConnectionCount(server);
      if (connections < minConnections) {
        minConnections = connections;
        selected = server;
      }
    }

    return selected;
  }

  /** 一致性哈希选择 */
  private String selectConsistentHashing(String clientId) {
    if (clientId == null || clientId.isEmpty()) {
      return selectRoundRobin();
    }

    List<String> serverList = new ArrayList<>(_servers.keySet());
    if (serverList.isEmpty()) {
      return null;
    }

    // 使用客户端 ID 的哈希值选择服务器
    int hash = clientId.hashCode();
    int index = Math.abs(hash % serverList.size());
    return serverList.get(index);
  }

  // ==================== 内部方法 ====================

  /** 重新计算权重参数 */
  private void recalculateWeights() {
    if (_servers.isEmpty()) {
      _maxWeight = 0;
      _gcdWeight = 1;
      return;
    }

    _maxWeight = _servers.values().stream().mapToInt(Integer::intValue).max().orElse(1);
    _gcdWeight = calculateGcd(_servers.values());
  }

  /** 计算最大公约数 */
  private int calculateGcd(Collection<Integer> numbers) {
    return numbers.stream().reduce(this::gcd).orElse(1);
  }

  /** 计算两个数的最大公约数 */
  private int gcd(int a, int b) {
    while (b != 0) {
      int temp = b;
      b = a % b;
      a = temp;
    }
    return a;
  }

  // ==================== 统计信息 ====================

  /** 获取负载均衡统计信息 */
  public LoadBalancerStats getStats() {
    Map<String, Integer> connections = new HashMap<>();
    for (String server : _servers.keySet()) {
      connections.put(server, getConnectionCount(server));
    }

    return new LoadBalancerStats(_strategy, _servers.size(), connections);
  }

  @Override
  public String toString() {
    return String.format("LoadBalancer{strategy=%s, servers=%d}", _strategy, _servers.size());
  }

  // ==================== 统计信息类 ====================

  /** 负载均衡统计信息 */
  public static class LoadBalancerStats {
    private final Strategy strategy;
    private final int serverCount;
    private final Map<String, Integer> connectionCounts;

    public LoadBalancerStats(
        Strategy strategy, int serverCount, Map<String, Integer> connectionCounts) {
      this.strategy = strategy;
      this.serverCount = serverCount;
      this.connectionCounts = Map.copyOf(connectionCounts);
    }

    public Strategy getStrategy() {
      return strategy;
    }

    public int getServerCount() {
      return serverCount;
    }

    public Map<String, Integer> getConnectionCounts() {
      return connectionCounts;
    }

    @Override
    public String toString() {
      return String.format(
          "LoadBalancerStats{strategy=%s, servers=%d, connections=%s}",
          strategy, serverCount, connectionCounts);
    }
  }
}
