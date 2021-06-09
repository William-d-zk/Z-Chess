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
package com.isahl.chess.knight.cluster.service;

import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZClusterMappingCustom;
import com.isahl.chess.king.base.inf.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.schedule.inf.ICancelable;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.knight.cluster.ClusterNode;
import com.isahl.chess.knight.cluster.model.ConsistentProtocol;
import com.isahl.chess.knight.raft.IRaftDao;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.service.IConsistentService;
import com.isahl.chess.knight.raft.service.RaftCustom;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IClusterConfig;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConsistentService
        implements
        IConsistentService
{

    private final Logger                  _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
    private final ClusterNode             _ClusterNode;
    private final RaftCustom<ClusterNode> _RaftCustom;
    private final IConsistentCustom       _ConsistentCustom;
    private final RaftNode<ClusterNode>   _RaftNode;
    private final TimeWheel               _TimeWheel;

    public ConsistentService(IAioConfig ioConfig,
                             IClusterConfig clusterConfig,
                             IConsistentCustom consistentCustom,
                             IRaftConfig raftConfig,
                             IRaftDao raftDao) throws IOException
    {
        _TimeWheel = new TimeWheel();
        _ClusterNode = new ClusterNode(ioConfig, clusterConfig, raftConfig, _TimeWheel);
        _RaftNode = new RaftNode<>(_TimeWheel, raftConfig, raftDao, _ClusterNode);
        _ConsistentCustom = consistentCustom;
        _RaftCustom = new RaftCustom<>(_RaftNode);
        start();
    }

    private void start() throws IOException
    {
        _ClusterNode.start(new ZClusterMappingCustom<>(_RaftCustom), _ConsistentCustom, new ClusterLogic(_ClusterNode));
        _RaftNode.init();
        _RaftNode.start();
    }

    public void submit(String content, boolean pub, long origin)
    {
        if (IoUtil.isBlank(content)) return;
        ConsistentProtocol consensus = new ConsistentProtocol(content.getBytes(StandardCharsets.UTF_8),
                                                              pub,
                                                              _RaftNode.getRaftZUid(),
                                                              origin);
        submit(consensus, _ClusterNode, _ConsistentCustom);
        _Logger.debug("consistent submit %s", consensus);
    }

    public <A extends IValid> ICancelable acquire(A attach, ScheduleHandler<A> scheduleHandler)
    {
        return _TimeWheel.acquire(attach, scheduleHandler);
    }
}
