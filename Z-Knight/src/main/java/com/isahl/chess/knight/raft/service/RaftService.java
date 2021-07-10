/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.knight.raft.service;

import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.raft.RaftPeer;
import com.isahl.chess.knight.raft.inf.IRaftService;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.io.core.inf.IClusterPeer;
import com.isahl.chess.queen.io.core.inf.IClusterTimer;

import java.util.List;

/**
 * @author william.d.zk
 */
public class RaftService<M extends IClusterPeer & IClusterTimer>
        implements IRaftService
{

    private final IClusterNode _ClusterNode;
    private final RaftPeer<M>  _RaftPeer;

    public RaftService(IClusterNode clusterNode, RaftPeer<M> raftPeer)
    {
        _ClusterNode = clusterNode;
        _RaftPeer = raftPeer;
    }

    @Override
    public RaftNode getLeader()
    {
        return _RaftPeer.getLeader();
    }

    @Override
    public List<RaftNode> getTopology()
    {
        return _RaftPeer.getRaftConfig()
                        .getPeers();
    }

    @Override
    public void changeTopology(RaftNode delta, IStorage.Operation operation)
    {
        _ClusterNode.changeTopology(delta.getHost(),
                                    delta.getPort(),
                                    delta.getState()
                                         .name(),
                                    operation);
    }

    @Override
    public void changeGate(RaftNode delta, IStorage.Operation operation)
    {
        _RaftPeer.getRaftConfig()
                 .changeGate(delta, operation);
    }

}
