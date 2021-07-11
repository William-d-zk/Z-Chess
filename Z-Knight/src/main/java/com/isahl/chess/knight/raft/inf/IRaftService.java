/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.knight.raft.inf;

import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.db.inf.IStorage.Operation;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/2/20
 */
public interface IRaftService
{
    /**
     * 获取集群的leader信息
     *
     * @return leader peer_id
     */
    RaftNode getLeader();

    /**
     * 获取集群拓扑
     *
     * @return peers
     */
    List<RaftNode> getTopology();

    void changeTopology(RaftNode delta, Operation operation);

    /**
     * ra f
     * 调整多个分区之间通信的端点结构。
     *
     * @param delta     变化网关节点
     * @param operation OP_APPEND,增加网关 节点
     *                  OP_REMOVE,减少网关 节点
     *                  OP_MODIFY,变更gate-listen的端口
     */
    void changeGate(RaftNode delta, Operation operation);
}