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

package com.tgx.chess.queen.io.core.inf;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;

/**
 * @author william.d.zk
 */
public interface IAioConnector<C extends IContext<C>>
        extends
        CompletionHandler<Void,
                          AsynchronousSocketChannel>,
        IConnectActivity<C>,
        IConnected<C>,
        IConnectError<C>
{
    @Override
    IOperator<Throwable,
              IAioConnector<C>,
              IAioConnector<C>> getErrorOperator();

    @Override
    default void completed(Void result, AsynchronousSocketChannel channel)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishConnected(getConnectedOperator(), this, channel);
    }

    @Override
    default void failed(Throwable exc, AsynchronousSocketChannel channel)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishConnectingError(getErrorOperator(), exc, this);
    }

    void attach(IConnectFailed<C> func);

    void error();

    void retry();

    interface IConnectFailed<C extends IContext<C>>
    {
        void onFailed(IAioConnector<C> connector);
    }
}
