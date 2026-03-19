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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 集群管理服务接口
 *
 * @author william.d.zk
 */
public interface ClusterManagementService {

  /** 添加集群节点 */
  CompletableFuture<Boolean> addNode(ClusterNode node);

  /** 移除集群节点 */
  CompletableFuture<Boolean> removeNode(String nodeId);

  /** 获取集群成员列表 */
  List<ClusterNode> getMembers();

  /** 获取集群状态 */
  ClusterStatus getStatus();

  /** 获取当前 Leader 节点 */
  ClusterNode getLeader();

  /** 检查当前节点是否为 Leader */
  boolean isLocalNodeLeader();
}
