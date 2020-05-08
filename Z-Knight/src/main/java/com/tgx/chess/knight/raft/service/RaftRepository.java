/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.knight.raft.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IClusterTimer;

@Component
public class RaftRepository<T extends IActivity<ZContext> & IClusterPeer & IClusterTimer>
        implements
        IRepository<RaftNode<T>>
{
    private final IRaftConfig _RaftConfig;
    private final ZUID        _ZUid;

    public RaftRepository(IRaftConfig raftConfig)
    {
        _RaftConfig = raftConfig;
        _ZUid = _RaftConfig.createZUID();
    }

    @Override
    public RaftNode<T> save(IStorage target)
    {
        return null;
    }

    @Override
    public RaftNode<T> find(IStorage key)
    {
        return null;
    }

    @Override
    public List<RaftNode<T>> findAll(IStorage key)
    {
        return null;
    }

    @Override
    public void saveAll(List<IStorage> targets)
    {

    }

    @Override
    public long getPeerId()
    {
        return _ZUid.getPeerId();
    }

    @Override
    public long getZid()
    {
        return _ZUid.getId();
    }
}