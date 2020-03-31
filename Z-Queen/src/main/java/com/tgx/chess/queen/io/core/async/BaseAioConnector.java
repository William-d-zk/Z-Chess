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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.config.ISocketConfig;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.operator.ConnectFailedOperator;
import com.tgx.chess.queen.event.operator.ConnectedOperator;
import com.tgx.chess.queen.io.core.inf.IAioConnector;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;

/**
 * @author william.d.zk
 */
public abstract class BaseAioConnector<C extends IContext<C>>
        extends
        AioCreator<C>
        implements
        IAioConnector<C>
{

    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    protected BaseAioConnector(String host,
                               int port,
                               ISocketConfig socketConfig,
                               IConnectFailed<C> connectFailed)
    {
        super(socketConfig);
        _RemoteAddress = new InetSocketAddress(host, port);
        attach(connectFailed);
    }

    private static final int COUNT_BITS = Integer.SIZE - 7;
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;
    private static final int RUNNING    = -1 << COUNT_BITS;
    private static final int SHUTDOWN   = 0 << COUNT_BITS;
    private static final int STOP       = 1 << COUNT_BITS;
    private static final int TIDYING    = 2 << COUNT_BITS;
    private static final int TERMINATED = 3 << COUNT_BITS;

    private final ConnectFailedOperator<C> _ConnectFailedOperator = new ConnectFailedOperator<>();
    private final ConnectedOperator<C>     _ConnectedOperator     = new ConnectedOperator<>();
    private final InetSocketAddress        _RemoteAddress;
    private final AtomicInteger            _State                 = new AtomicInteger(ctlOf(RUNNING, 0));

    private InetSocketAddress mLocalBind;
    private IConnectFailed<C> mFailedConsumer;

    private void advanceRunState(int targetState)
    {
        for (;;) {
            int c = _State.get();
            if (c >= targetState || _State.compareAndSet(c, ctlOf(targetState, retryCountOf(c)))) break;
        }
    }

    /*对state进行拆装箱*/
    private static int runStateOf(int state)
    {
        return state & ~CAPACITY;
    }

    private static int retryCountOf(int c)
    {
        return c & CAPACITY;
    }

    private static int ctlOf(int rs, int rc)
    {
        return rs | rc;
    }

    private boolean compareAndIncrementCount(int expect)
    {
        return _State.compareAndSet(expect, expect + 1);
    }

    @Override
    public void error()
    {
        mFailedConsumer.onFailed(this);
    }

    /**
     * 在执行retry操作之前需要先切换到stop状态
     */
    @Override
    public void retry()
    {
        retry:
        for (;;) {
            int c = _State.get();
            int rs = runStateOf(c);
            if (rs >= SHUTDOWN) { return; }
            for (;;) {
                int rc = retryCountOf(c);
                if (rc >= CAPACITY) { return; }
                if (compareAndIncrementCount(rc)) { return; }
                c = _State.get();// reload 
                if (runStateOf(c) != rs) {
                    continue retry;
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
    public IOperator<IConnectActivity<C>,
                     AsynchronousSocketChannel,
                     ITriple> getConnectedOperator()
    {
        return _ConnectedOperator;
    }

    @Override
    public IOperator<Throwable,
                     IAioConnector<C>,
                     Void> getErrorOperator()
    {
        return _ConnectFailedOperator;
    }

    @Override
    public void attach(IConnectFailed<C> func)
    {
        mFailedConsumer = func;
    }

    @Override
    public void shutdown()
    {
        advanceRunState(SHUTDOWN);
    }

    @Override
    public boolean isShutdown()
    {
        return runStateOf(_State.get()) >= SHUTDOWN;
    }

    @Override
    public boolean isRunning()
    {
        return runStateOf(_State.get()) < SHUTDOWN;
    }
}
