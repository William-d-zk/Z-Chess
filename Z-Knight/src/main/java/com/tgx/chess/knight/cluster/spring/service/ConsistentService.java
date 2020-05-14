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
package com.tgx.chess.knight.cluster.spring.service;

import static com.tgx.chess.queen.event.inf.IOperator.Type.CONSENSUS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.bishop.io.zhandler.ZClusterMappingCustom;
import com.tgx.chess.king.base.inf.IValid;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.schedule.inf.ICancelable;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.knight.cluster.ClusterNode;
import com.tgx.chess.knight.cluster.spring.model.ConsistentProtocol;
import com.tgx.chess.knight.raft.IRaftDao;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.service.ClusterCustom;
import com.tgx.chess.queen.config.IAioConfig;
import com.tgx.chess.queen.config.IClusterConfig;
import com.tgx.chess.queen.event.handler.cluster.IConsistentCustom;
import com.tgx.chess.queen.event.processor.QEvent;

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
        _ClusterCustom = new ClusterCustom<>(_TimeWheel, _RaftNode, _ConsistentCustom = consistentCustom);
    }

    @PostConstruct
    private void start() throws IOException
    {
        _ClusterNode.start(new ZClusterMappingCustom<>(_ClusterCustom),
                           _ConsistentCustom,
                           new ClusterLogic(_ClusterNode));
        _RaftNode.init();
    }

    public void consistentPut(String content, boolean pub, long origin)
    {
        if (IoUtil.isBlank(content)) return;
        final ReentrantLock _Lock = _ClusterNode.getCore()
                                                .getLock(CONSENSUS);
        final RingBuffer<QEvent> _Publish = _ClusterNode.getCore()
                                                        .getPublisher(CONSENSUS);
        if (_Lock.tryLock()) {
            try {
                long sequence = _Publish.next();
                try {
                    QEvent event = _Publish.get(sequence);
                    event.produce(CONSENSUS,
                                  new Pair<>(new ConsistentProtocol(content.getBytes(StandardCharsets.UTF_8),
                                                                    pub,
                                                                    _ClusterNode.getZuid(),
                                                                    origin),
                                             origin),
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
        _Logger.debug("consistent put %s", content);
    }

    public <A extends IValid> ICancelable acquire(A attach, ScheduleHandler<A> scheduleHandler)
    {
        return _TimeWheel.acquire(attach, scheduleHandler);
    }

    public long getClusterZuid()
    {
        return _ClusterNode.getZuid();
    }
}
