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

package com.isahl.chess.knight.cluster.spring;

import static com.isahl.chess.queen.event.inf.IOperator.Type.CLUSTER_TIMER;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.locks.ReentrantLock;

import com.isahl.chess.bishop.io.ZSortHolder;
import com.isahl.chess.bishop.io.zprotocol.control.X106_Identity;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.event.processor.QEvent;
import com.isahl.chess.queen.io.core.async.AioSession;
import com.isahl.chess.queen.io.core.async.BaseAioConnector;
import com.isahl.chess.queen.io.core.executor.IPipeCore;
import com.isahl.chess.queen.io.core.inf.IAioClient;
import com.isahl.chess.queen.io.core.inf.IAioConnector;
import com.isahl.chess.queen.io.core.inf.IClusterPeer;
import com.isahl.chess.queen.io.core.inf.IClusterTimer;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.INode;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;
import com.lmax.disruptor.RingBuffer;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/23
 */
public interface IClusterNode<K extends IPipeCore>
        extends
        IClusterPeer,
        IClusterTimer,
        INode
{
    default IAioConnector buildConnector(IPair address,
                                         ISocketConfig socketConfig,
                                         IAioClient client,
                                         final long _Type,
                                         final ISessionManager _Manager,
                                         final ZSortHolder _SortHolder,
                                         final ZUID _ZUID)
    {
        final String _Host = address.getFirst();
        final int _Port = address.getSecond();
        if (_SortHolder.getSort()
                       .getMode() != ISort.Mode.CLUSTER)
        {
            throw new IllegalArgumentException("sort mode is wrong in cluster define");
        }
        return new BaseAioConnector(_Host, _Port, socketConfig, client)
        {
            @Override
            public ISort.Mode getMode()
            {
                return _SortHolder.getSort()
                                  .getMode();
            }

            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel,
                                          IConnectActivity activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, _SortHolder.getSort(), activity, client);
            }

            @Override
            public void onCreate(ISession session)
            {
                session.setIndex(_ZUID.getId(_Type));
                _Manager.addSession(session);
                client.onCreate(session);
            }

            @Override
            public IControl[] createCommands(ISession session)
            {
                X106_Identity x106 = new X106_Identity(_ZUID.getPeerId());
                return new IControl[] { x106
                };
            }

            @Override
            public String getProtocol()
            {
                return _SortHolder.getSort()
                                  .getProtocol();
            }
        };
    }

    K getCore();

    @Override
    default <T extends IStorage> void timerEvent(T content)
    {
        final RingBuffer<QEvent> _ConsensusEvent = getCore().getConsensusEvent();
        final ReentrantLock _ConsensusLock = getCore().getConsensusLock();
        /*
        通过 Schedule thread-pool 进行 timer 执行, 排队执行。
         */
        _ConsensusLock.lock();
        try {
            long sequence = _ConsensusEvent.next();
            try {
                QEvent event = _ConsensusEvent.get(sequence);
                event.produce(CLUSTER_TIMER, new Pair<>(content, null), null);
            }
            finally {
                _ConsensusEvent.publish(sequence);
            }
        }
        finally {
            _ConsensusLock.unlock();
        }
    }

}