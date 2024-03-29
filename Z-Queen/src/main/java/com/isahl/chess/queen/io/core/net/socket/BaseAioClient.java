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

package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;
import com.isahl.chess.queen.io.core.net.socket.features.client.IAioClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class BaseAioClient
        implements IAioClient
{
    private final Logger _Logger;

    private final TimeWheel                                            _TimeWheel;
    private final AsynchronousChannelGroup                             _ChannelGroup;
    private final Map<InetSocketAddress, Pair<Integer, IAioConnector>> _TargetManageMap = new HashMap<>();

    public BaseAioClient(TimeWheel timeWheel, AsynchronousChannelGroup channelGroup, String type)
    {
        Objects.requireNonNull(timeWheel);
        Objects.requireNonNull(channelGroup);
        _TimeWheel = timeWheel;
        _ChannelGroup = channelGroup;
        _Logger = Logger.getLogger("io.queen.client." + type);
    }

    private final ReentrantLock _Lock = new ReentrantLock();

    @Override
    public void connect(IAioConnector connector) throws IOException
    {
        _Lock.lock();
        try {
            AsynchronousSocketChannel socketChannel = AsynchronousSocketChannel.open(_ChannelGroup);
            InetSocketAddress remoteAddress = connector.getRemoteAddress();
            Pair<Integer, IAioConnector> pair = _TargetManageMap.putIfAbsent(remoteAddress, Pair.of(0, connector));
            socketChannel.connect(remoteAddress, socketChannel, connector);
            int retryCount = 0;
            if(pair != null) {
                retryCount = pair.getFirst();
                pair.setFirst(++retryCount);
            }
            _Logger.info("client connect to %s,@ %d", remoteAddress, retryCount);
        }
        finally {
            _Lock.unlock();
        }
    }

    @Override
    public void onFailed(IAioConnector connector)
    {
        delayConnect(connector);
    }

    @Override
    public void onCreated(ISession session)
    {
        _Logger.info("connected :%s", session);
    }

    @Override
    public void onDismiss(ISession session)
    {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        IAioConnector connector = _TargetManageMap.get(remoteAddress)
                                                  .getSecond();
        _Logger.info("on dismiss: [shutdown: %s ]", connector.isShutdown());
        delayConnect(connector);
    }

    private void delayConnect(IAioConnector connector)
    {
        if(connector.isShutdown()) {return;}
        _TimeWheel.acquire(connector,
                           new ScheduleHandler<>(connector.getConnectTimeout()
                                                          .multipliedBy(3), c->{
                               try {
                                   _Logger.debug("%s connect",
                                                 Thread.currentThread()
                                                       .getName());
                                   connect(c);
                               }
                               catch(IOException e) {
                                   e.printStackTrace();
                               }
                           }));
    }

    @Override
    public void shutdown(ISession session)
    {
        InetSocketAddress remoteAddress = session.getRemoteAddress();
        if(_TargetManageMap.containsKey(remoteAddress)) {
            IAioConnector connector = _TargetManageMap.get(remoteAddress)
                                                      .getSecond();
            connector.shutdown();
        }
    }
}
