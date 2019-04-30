/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

package com.tgx.chess.queen.io.core.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.operator.AcceptFailedOperator;
import com.tgx.chess.queen.event.operator.ConnectedOperator;
import com.tgx.chess.queen.io.core.inf.IAioServer;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;

/**
 * @author william.d.zk
 */
public abstract class BaseAioServer<C extends IContext>
        implements
        IAioServer<C>
{
    private final AcceptFailedOperator<C> _AcceptFailedOperator = new AcceptFailedOperator<>();
    private final ConnectedOperator<C>    _ConnectedOperator;

    public BaseAioServer(String serverHost, int serverPort, IPipeEncoder<C> encoder, IPipeDecoder<C> decoder) {
        _LocalBind = new InetSocketAddress(serverHost, serverPort);
        _ConnectedOperator = new ConnectedOperator<>(encoder, decoder);
    }

    private AsynchronousServerSocketChannel mServerChannel;

    @Override
    public void bindAddress(InetSocketAddress address, AsynchronousChannelGroup channelGroup) throws IOException {
        mServerChannel = AsynchronousServerSocketChannel.open(channelGroup);
        mServerChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
        mServerChannel.bind(address, 1 << 6);
    }

    @Override
    public void pendingAccept() {
        if (mServerChannel.isOpen()) {
            mServerChannel.accept(this, this);
        }
    }

    private final InetSocketAddress _LocalBind;

    @Override
    public InetSocketAddress getRemoteAddress() {
        throw new UnsupportedOperationException(" server hasn't remote address");
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return _LocalBind;
    }

    @Override
    public IOperator<Throwable, IAioServer<C>, IAioServer<C>> getErrorOperator() {
        return _AcceptFailedOperator;
    }

    @Override
    public IOperator<IConnectionContext<C>, AsynchronousSocketChannel, ITriple> getConnectedOperator() {
        return _ConnectedOperator;
    }
}
