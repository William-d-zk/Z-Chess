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

import com.isahl.chess.bishop.sort.ZSortHolder;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X106_Identity;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.io.core.features.cluster.IClusterPeer;
import com.isahl.chess.queen.io.core.features.model.channels.IConnectActivity;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.IDismiss;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.net.socket.AioSession;
import com.isahl.chess.queen.io.core.net.socket.BaseAioConnector;
import com.isahl.chess.queen.io.core.net.socket.BaseAioServer;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.client.IAioClient;
import com.isahl.chess.queen.io.core.net.socket.features.server.IAioServer;
import com.isahl.chess.queen.io.core.tasks.features.ILocalPublisher;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.SINGLE;

/**
 * @author william.d.zk
 * @date 2020/4/23
 */
public interface IClusterNode
        extends ILocalPublisher
{
    default IAioConnector buildConnector(final String _Host,
                                         final int _Port,
                                         final ISocketConfig _SocketConfig,
                                         final IAioClient _Client,
                                         final IManager _Manager,
                                         final ZSortHolder _ZSortHolder,
                                         final IClusterPeer _ClusterPeer)
    {
        if(_ZSortHolder.getSort()
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
            public ISession create(AsynchronousSocketChannel socketChannel,
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
            public ITriple response(ISession session)
            {
                if(_ZSortHolder.getSort()
                               .getMode() == ISort.Mode.CLUSTER)
                {
                    return new Triple<>(new X106_Identity(_ClusterPeer.getPeerId(), _ClusterPeer.generateId()),
                                        session,
                                        SINGLE);
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

    default IAioServer buildServer(final String _Host,
                                   final int _Port,
                                   final ISocketConfig _SocketConfig,
                                   final ZSortHolder _ZSortHolder,
                                   final IManager _Manager,
                                   final IDismiss _Dismiss,
                                   final boolean _MultiBind,
                                   final IClusterPeer _ClusterPeer)
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
            public ISession create(AsynchronousSocketChannel socketChannel,
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
            public ITriple response(ISession session)
            {
                if(_ZSortHolder.getSort()
                               .getMode() == ISort.Mode.CLUSTER)
                {
                    return new Triple<>(new X106_Identity(_ClusterPeer.getPeerId(), _ClusterPeer.generateId()),
                                        session,
                                        SINGLE);
                }
                return null;
            }
        };
    }

    void setupPeer(String host, int port) throws IOException;

    void setupGate(String host, int port) throws IOException;

    boolean isOwnedBy(long origin);

    IClusterPeer getPeer();
}
