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

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.operator.ConnectFailedOperator;
import com.tgx.chess.queen.event.operator.ConnectedOperator;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;

/**
 * @author william.d.zk
 */
public abstract class BaseAioConnector<C extends IContext>
        implements
        IAioConnector<C>
{
    protected BaseAioConnector(String host,
                               int port,
                               IPipeEncoder<C> encoder,
                               IPipeDecoder<C> decoder)
    {
        _RemoteAddress = new InetSocketAddress(host, port);
        _ConnectedOperator = new ConnectedOperator<>(encoder, decoder);
    }

    private final ConnectFailedOperator<C> _ConnectFailedOperator = new ConnectFailedOperator<>();
    private final ConnectedOperator<C>     _ConnectedOperator;
    private final InetSocketAddress        _RemoteAddress;
    private InetSocketAddress              mLocalBind;

    @Override
    public InetSocketAddress getRemoteAddress()
    {
        return _RemoteAddress;
    }

    @Override
    public InetSocketAddress getLocalAddress()
    {
        return mLocalBind;
    }

    @Override
    public void setLocalAddress(InetSocketAddress address)
    {
        mLocalBind = address;
    }

    @Override
    public IOperator<IConnectionContext<C>,
                     AsynchronousSocketChannel,
                     ITriple> getConnectedOperator()
    {
        return _ConnectedOperator;
    }

    @Override
    public IOperator<Throwable,
                     IAioConnector<C>,
                     IAioConnector<C>> getErrorOperator()
    {
        return _ConnectFailedOperator;
    }
}
