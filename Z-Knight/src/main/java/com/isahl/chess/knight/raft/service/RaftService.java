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

import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.IRaftService;
import com.isahl.chess.knight.raft.model.RaftGraph;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.io.core.inf.IClusterPeer;
import com.isahl.chess.queen.io.core.inf.IClusterTimer;

/**
 * @author william.d.zk
 */
public class RaftService<M extends IClusterPeer & IClusterTimer>
        implements
        IRaftService
{

    private final RaftNode<M> _RaftNode;

    public RaftService(RaftNode<M> raftNode)
    {
        _RaftNode = raftNode;
    }

    @Override
    public long getLeader()
    {
        return ZUID.INVALID_PEER_ID;
    }

    @Override
    public RaftGraph getTopology()
    {
        return _RaftNode.getRaftGraph();
    }

    @Override
    public boolean addPeer(RaftMachine peer)
    {
        return getTopology().append(peer);
    }

    @Override
    public boolean removePeer(long peerId)
    {
        return getTopology().remove(peerId);
    }

}
