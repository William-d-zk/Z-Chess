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
package com.isahl.chess.knight.cluster.spring.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.isahl.chess.bishop.io.ws.zchat.zhandler.ZClusterMappingCustom;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.schedule.inf.ICancelable;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.knight.cluster.ClusterNode;
import com.isahl.chess.knight.cluster.spring.model.ConsistentProtocol;
import com.isahl.chess.knight.raft.IRaftDao;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.service.ClusterCustom;
import com.isahl.chess.queen.config.IAioConfig;
import com.isahl.chess.queen.config.IClusterConfig;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.io.core.inf.IConsistent;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.lmax.disruptor.RingBuffer;

@Service
public class ConsistentService
{

    private final Logger                     _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
    private final ClusterNode                _ClusterNode;
    private final ClusterCustom<ClusterNode> _ClusterCustom;
    private final IConsistentCustom          _ConsistentCustom;
    private final RaftNode<ClusterNode>      _RaftNode;
    private final TimeWheel                  _TimeWheel;

    @Autowired
    public ConsistentService(@Qualifier("io_cluster_config") IAioConfig ioConfig,
                             @Qualifier("core_cluster_config") IClusterConfig clusterConfig,
                             IConsistentCustom consistentCustom,
                             IRaftConfig raftConfig,
                             IRaftDao raftDao) throws IOException
    {
        _TimeWheel = new TimeWheel();
        _ClusterNode = new ClusterNode(ioConfig, clusterConfig, raftConfig, _TimeWheel);
        _RaftNode = new RaftNode<>(_TimeWheel, raftConfig, raftDao, _ClusterNode);
        _ConsistentCustom = consistentCustom;
        _ClusterCustom = new ClusterCustom<>(_RaftNode);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _ClusterNode.start(new ZClusterMappingCustom<>(_ClusterCustom),
                           _ConsistentCustom,
                           new ClusterLogic(_ClusterNode));
        _RaftNode.init();
        _RaftNode.start();
    }

    public void consistentSubmit(String content, boolean pub, long origin)
    {
        if (IoUtil.isBlank(content)) return;
        consistentSubmit(new ConsistentProtocol(content.getBytes(StandardCharsets.UTF_8),
                                                pub,
                                                _ClusterNode.getZuid(),
                                                origin));
    }

    public <T extends IConsistent & IProtocol> void consistentSubmit(T consensus)
    {
        if (consensus == null) return;
        final ReentrantLock _Lock = _ClusterNode.getCore()
                                                .getLock(OperatorType.CONSENSUS);
        final RingBuffer<QEvent> _Publish = _ClusterNode.getCore()
                                                        .getPublisher(OperatorType.CONSENSUS);
        if (_Lock.tryLock()) {
            try {
                long sequence = _Publish.next();
                try {
                    QEvent event = _Publish.get(sequence);
                    event.produce(OperatorType.CONSENSUS,
                                  new Pair<>(consensus, consensus.getOrigin()),
                                  _ConsistentCustom.getOperator());
                }
                finally {
                    _Publish.publish(sequence);
                }
            }
            finally {
                _Lock.unlock();
            }
        }
        _Logger.debug("consistent submit %s", consensus);
    }

    public <A extends IValid> ICancelable acquire(A attach, ScheduleHandler<A> scheduleHandler)
    {
        return _TimeWheel.acquire(attach, scheduleHandler);
    }
}
