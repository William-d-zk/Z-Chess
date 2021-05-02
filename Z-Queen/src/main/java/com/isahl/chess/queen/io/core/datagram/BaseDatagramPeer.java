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

package com.isahl.chess.queen.io.core.datagram;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.async.inf.IAioClient;
import com.isahl.chess.queen.io.core.async.inf.IAioConnector;
import com.isahl.chess.queen.io.core.inf.ISession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author William.d.zk
 */
public class BaseDatagramPeer
        implements
        IAioClient
{

    private final Logger _Logger = Logger.getLogger("io.queen.client." + getClass().getSimpleName());

    private final TimeWheel                                            _TimeWheel;
    private final Selector                                             _Selector;
    private final Map<InetSocketAddress,
                      Pair<Integer,
                           IAioConnector>>                             _TargetManageMap = new HashMap<>();

    public BaseDatagramPeer(TimeWheel timeWheel) throws IOException
    {
        _TimeWheel = timeWheel;
        _Selector = Selector.open();
    }

    private final ReentrantLock _Lock = new ReentrantLock();

    @Override
    public void connect(IAioConnector connector) throws IOException
    {
        _Lock.lock();
        try {
            InetSocketAddress remoteAddress = connector.getRemoteAddress();
            DatagramChannel _DatagramChannel = DatagramChannel.open();
            _DatagramChannel.bind(connector.getLocalAddress());
            _DatagramChannel.connect(remoteAddress);
            _DatagramChannel.configureBlocking(false);
            _DatagramChannel.register(_Selector, SelectionKey.OP_READ, connector);
            Pair<Integer,
                 IAioConnector> pair = _TargetManageMap.putIfAbsent(remoteAddress, new Pair<>(0, connector));
            int retryCount = 0;
            if (pair != null) {
                retryCount = pair.getFirst();
                pair.setFirst(++retryCount);
            }
            _Logger.info("udp peer connect to %s,@ %d", remoteAddress, retryCount);
        }
        finally {
            _Lock.unlock();
        }
    }

    @Override
    public void shutdown(ISession session) {

    }

    @Override
    public void onFailed(IAioConnector iAioConnector) {

    }

    @Override
    public void onCreated(ISession session) {

    }

    @Override
    public void onDismiss(ISession session) {

    }
}
