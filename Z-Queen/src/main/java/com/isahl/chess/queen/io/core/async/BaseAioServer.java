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

package com.isahl.chess.queen.io.core.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.event.operator.AcceptFailedOperator;
import com.isahl.chess.queen.event.operator.ConnectedOperator;
import com.isahl.chess.queen.io.core.inf.IAioServer;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;

/**
 * @author william.d.zk
 */
public abstract class BaseAioServer
        extends
        AioCreator
        implements
        IAioServer
{
    private final AcceptFailedOperator _AcceptFailedOperator = new AcceptFailedOperator();
    private final ConnectedOperator    _ConnectedOperator    = new ConnectedOperator();
    private final InetSocketAddress    _LocalBind;
    private volatile boolean           vValid;

    protected BaseAioServer(String serverHost,
                            int serverPort,
                            ISocketConfig socketConfig)
    {
        super(socketConfig);
        _LocalBind = new InetSocketAddress(serverHost, serverPort);
    }

    private AsynchronousServerSocketChannel mServerChannel;

    @Override
    public void bindAddress(InetSocketAddress address, AsynchronousChannelGroup channelGroup) throws IOException
    {
        mServerChannel = AsynchronousServerSocketChannel.open(channelGroup);
        mServerChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        mServerChannel.bind(address, 1 << 6);
    }

    @Override
    public void pendingAccept()
    {
        if (mServerChannel.isOpen()) {
            mServerChannel.accept(this, this);
            vValid = true;
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        throw new UnsupportedOperationException(" server hasn't remote address");
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return _LocalBind;
    }

    @Override
    public IOperator<Throwable,
                     IAioServer,
                     Void> getErrorOperator()
    {
        return _AcceptFailedOperator;
    }

    @Override
    public IOperator<IConnectActivity,
                     AsynchronousSocketChannel,
                     ITriple> getConnectedOperator()
    {
        return _ConnectedOperator;
    }

    @Override
    public boolean isShutdown()
    {
        return false;
    }

    @Override
    public boolean isRunning()
    {
        return true;
    }

    @Override
    public boolean isValid()
    {
        return vValid;
    }

    @Override
    public void shutdown()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retry()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void error()
    {
        // ignore
    }
}
