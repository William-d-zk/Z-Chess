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

import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.events.functions.AcceptFailed;
import com.isahl.chess.queen.events.functions.SocketConnected;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;
import com.isahl.chess.queen.io.core.net.socket.features.server.IAioServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author william.d.zk
 */
public abstract class BaseAioServer
        extends AioCreator
        implements IAioServer
{
    private final    AcceptFailed      _AcceptFailed    = new AcceptFailed();
    private final    SocketConnected   _SocketConnected = new SocketConnected();
    private final    InetSocketAddress _LocalBind;
    private volatile boolean           vValid;

    protected BaseAioServer(String serverHost, int serverPort, ISocketConfig socketConfig)
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
        if(mServerChannel != null && mServerChannel.isOpen()) {
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
    public IBinaryOperator<Throwable, IAioConnection, Void> getErrorOperator()
    {
        return _AcceptFailed;
    }

    @Override
    public IBinaryOperator<IAioConnection, AsynchronousSocketChannel, ITriple> getConnectedOperator()
    {
        return _SocketConnected;
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
    public boolean isInvalid()
    {
        return !vValid;
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
