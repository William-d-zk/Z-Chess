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

package com.isahl.chess.knight.cluster;

import com.isahl.chess.bishop.io.sort.ZSortHolder;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X106_Identity;
import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.async.AioSession;
import com.isahl.chess.queen.io.core.async.BaseAioConnector;
import com.isahl.chess.queen.io.core.async.BaseAioServer;
import com.isahl.chess.queen.io.core.async.inf.IAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioConnector;
import com.isahl.chess.queen.io.core.async.inf.IAioServer;
import com.isahl.chess.queen.io.core.executor.IPipeCore;
import com.isahl.chess.queen.io.core.inf.*;
import com.lmax.disruptor.RingBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.locks.ReentrantLock;

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
    default IAioConnector buildConnector(final IPair _Address,
                                         final ISocketConfig _SocketConfig,
                                         final IAioClient _Client,
                                         final ISessionManager _Manager,
                                         final ZSortHolder _ZSortHolder,
                                         final ZUID _ZUid)
    {
        final String _Host = _Address.getFirst();
        final int _Port = _Address.getSecond();
        if (_ZSortHolder.getSort()
                        .getMode() != ISort.Mode.CLUSTER)
        {
            throw new IllegalArgumentException("sort mode is wrong in cluster define");
        }
        return new BaseAioConnector(_Host, _Port, _SocketConfig, _Client)
        {
            @Override
            public ISort.Mode getMode()
            {
                return _ZSortHolder.getSort()
                                   .getMode();
            }

            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel,
                                          IConnectActivity activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, _ZSortHolder.getSort(), activity, _Client, false);
            }

            @Override
            public void onCreated(ISession session)
            {
                super.onCreated(session);
                _Client.onCreated(session);
                _Manager.addSession(session);
            }

            @Override
            public IControl[] onConnectedCommands(ISession session)
            {
                if (_ZSortHolder.getSort()
                                .getMode() == ISort.Mode.CLUSTER)
                {
                    X106_Identity x106 = new X106_Identity(_ZUid.getPeerId(), _ZUid.getId());
                    return new IControl[]{x106};
                }
                return null;
            }

            @Override
            public String getProtocol()
            {
                return _ZSortHolder.getSort()
                                   .getProtocol();
            }
        };
    }

    default IAioServer buildServer(final IPair _Address,
                                   final ISocketConfig _SocketConfig,
                                   final ZSortHolder _ZSortHolder,
                                   final ISessionManager _Manager,
                                   final ISessionDismiss _Dismiss,
                                   final ZUID _ZUid,
                                   final boolean _MultiBind)
    {
        final String _Host = _Address.getFirst();
        final int _Port = _Address.getSecond();
        return buildServer(_Host, _Port, _SocketConfig, _ZSortHolder, _Manager, _Dismiss, _ZUid, _MultiBind);
    }

    default IAioServer buildServer(final String _Host,
                                   final int _Port,
                                   final ISocketConfig _SocketConfig,
                                   final ZSortHolder _ZSortHolder,
                                   final ISessionManager _Manager,
                                   final ISessionDismiss _Dismiss,
                                   final ZUID _ZUid,
                                   final boolean _MultiBind)
    {

        return new BaseAioServer(_Host, _Port, _SocketConfig)
        {
            @Override
            public ISort.Mode getMode()
            {
                return _ZSortHolder.getSort()
                                   .getMode();
            }

            @Override
            public ISession createSession(AsynchronousSocketChannel socketChannel,
                                          IConnectActivity activity) throws IOException
            {
                return new AioSession<>(socketChannel, this, _ZSortHolder.getSort(), activity, _Dismiss, _MultiBind);
            }

            @Override
            public void onCreated(ISession session)
            {
                _Manager.addSession(session);
                session.ready();
            }

            @Override
            public String getProtocol()
            {
                return _ZSortHolder.getSort()
                                   .getProtocol();
            }

            @Override
            public IControl[] onConnectedCommands(ISession session)
            {
                if (_ZSortHolder.getSort()
                                .getMode() == ISort.Mode.CLUSTER)
                {
                    X106_Identity x106 = new X106_Identity(_ZUid.getPeerId(), _ZUid.getId());
                    return new IControl[]{x106};
                }
                return null;
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
                event.produce(OperatorType.CLUSTER_TIMER, new Pair<>(content, null), null);
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
