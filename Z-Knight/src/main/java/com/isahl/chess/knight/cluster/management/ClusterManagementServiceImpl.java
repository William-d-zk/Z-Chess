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

package com.isahl.chess.knight.cluster.management;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群管理服务实现
 *
 * @author william.d.zk
 */
public class ClusterManagementServiceImpl implements ClusterManagementService {

  private static final Logger _Logger =
      LoggerFactory.getLogger(
          "cluster.knight." + ClusterManagementServiceImpl.class.getSimpleName());

  private final Map<String, ClusterNode> _nodes = new ConcurrentHashMap<>();
  private final String _localNodeId;

  public ClusterManagementServiceImpl() {
    _localNodeId = generateLocalNodeId();
  }

  private String generateLocalNodeId() {
    return "node-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @Override
  public CompletableFuture<Boolean> addNode(ClusterNode node) {
    return CompletableFuture.supplyAsync(
        () -> {
          if (_nodes.containsKey(node.getNodeId())) {
            _Logger.warn("Node already exists: " + node.getNodeId());
            return false;
          }

          _nodes.put(node.getNodeId(), node);
          _Logger.info(
              "Node added: "
                  + node.getNodeId()
                  + " ("
                  + node.getHost()
                  + ":"
                  + node.getPort()
                  + ")");
          return true;
        });
  }

  @Override
  public CompletableFuture<Boolean> removeNode(String nodeId) {
    return CompletableFuture.supplyAsync(
        () -> {
          ClusterNode removed = _nodes.remove(nodeId);
          if (removed != null) {
            _Logger.info("Node removed: " + nodeId);
            return true;
          }
          _Logger.warn("Node not found: " + nodeId);
          return false;
        });
  }

  @Override
  public List<ClusterNode> getMembers() {
    return new ArrayList<>(_nodes.values());
  }

  @Override
  public ClusterStatus getStatus() {
    ClusterStatus status = new ClusterStatus();
    status.setClusterSize(_nodes.size());
    status.setQuorumSize((_nodes.size() / 2) + 1);

    // 查找 Leader
    _nodes.values().stream()
        .filter(ClusterNode::isLeader)
        .findFirst()
        .ifPresent(
            leader -> {
              status.setLeaderId(leader.getNodeId());
            });

    // 构建节点状态映射
    Map<String, ClusterNode.NodeStatus> nodeStatusMap = new HashMap<>();
    _nodes.forEach(
        (id, node) -> {
          nodeStatusMap.put(id, node.getStatus());
        });
    status.setNodeStatusMap(nodeStatusMap);

    return status;
  }

  @Override
  public ClusterNode getLeader() {
    return _nodes.values().stream().filter(ClusterNode::isLeader).findFirst().orElse(null);
  }

  @Override
  public boolean isLocalNodeLeader() {
    ClusterNode localNode = _nodes.get(_localNodeId);
    return localNode != null && localNode.isLeader();
  }
}
