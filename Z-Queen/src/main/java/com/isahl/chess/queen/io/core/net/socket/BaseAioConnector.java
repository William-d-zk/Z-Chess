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

import com.isahl.chess.king.base.cron.features.ITask;
import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.features.IFailed;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.queen.config.ISocketConfig;
import com.isahl.chess.queen.events.functions.SocketConnectFailed;
import com.isahl.chess.queen.events.functions.SocketConnected;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnector;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static com.isahl.chess.king.base.cron.Status.RUNNING;
import static com.isahl.chess.king.base.cron.Status.STOP;

/**
 * @author william.d.zk
 */
public abstract class BaseAioConnector
        extends AioCreator
        implements IAioConnector
{

    protected BaseAioConnector(String host, int port, ISocketConfig socketConfig, IFailed<IAioConnector> failed)
    {
        super(socketConfig);
        _RemoteAddress = new InetSocketAddress(host, port);
        _FailedHandler = failed;
    }

    private final SocketConnectFailed    _SocketConnectFailed = new SocketConnectFailed();
    private final SocketConnected        _SocketConnected     = new SocketConnected();
    private final InetSocketAddress      _RemoteAddress;
    private final AtomicInteger          _State               = new AtomicInteger(ITask.ctlOf(RUNNING.getCode(), 0));
    private final IFailed<IAioConnector> _FailedHandler;

    private InetSocketAddress mLocalBind;

    @Override
    public void error()
    {
        _FailedHandler.onFailed(this);
    }

    /**
     * 在执行retry操作之前需要先切换到stop状态
     */
    @Override
    public boolean retry()
    {
        OUTSIDE:
        for(; ; ) {
            int c = _State.get();
            int rs = ITask.stateOf(c, RETRY_LIMIT);
            if(rs >= STOP.getCode()) {return false;}
            for(; ; ) {
                int rc = ITask.countOf(c, RETRY_LIMIT);
                if(rc >= RETRY_LIMIT) {return false;}
                if(ITask.compareAndIncrementRetry(_State, rc)) {return true;}
                c = _State.get();// reload
                if(ITask.stateOf(c, RETRY_LIMIT) != rs) {
                    continue OUTSIDE;
                }
            }
        }
    }



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
    public IBinaryOperator<IAioConnection, AsynchronousSocketChannel, ITriple> getConnectedOperator()
    {
        return _SocketConnected;
    }

    @Override
    public IBinaryOperator<Throwable, IAioConnection, Void> getErrorOperator()
    {
        return _SocketConnectFailed;
    }

    @Override
    public void shutdown()
    {
        ITask.advanceState(_State, STOP.getCode(), RETRY_LIMIT);
    }

    @Override
    public boolean isShutdown()
    {
        return ITask.stateOf(_State.get(), RETRY_LIMIT) >= STOP.getCode();
    }

    @Override
    public boolean isRunning()
    {
        return ITask.stateOf(_State.get(), RETRY_LIMIT) < STOP.getCode();
    }

    @Override
    public boolean isValid()
    {
        return !isShutdown();
    }

    @Override
    public boolean isInvalid()
    {
        return isShutdown();
    }

    @Override
    public void onCreated(ISession session)
    {
        session.ready();
    }
}
