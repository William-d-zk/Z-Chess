/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.tgx.chess.queen.event.operator;

import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_EOF;
import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_FAILED;
import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_ZERO;

import java.io.EOFException;
import java.nio.channels.CompletionHandler;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class AioWriter<C extends IContext<C>>
        implements
        CompletionHandler<Integer,
                          ISession<C>>
{
    private final Logger           _Logger        = Logger.getLogger("io.queen.operator." + getClass().getSimpleName());
    private final WroteOperator<C> _WroteOperator = new WroteOperator<>(this);

    @Override
    public void completed(Integer result, ISession<C> session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        switch (result)
        {
            case -1:
                worker.publishWroteError(session.getContext()
                                                .getSort()
                                                .getError(),
                                         WRITE_EOF,
                                         new EOFException("wrote -1!"),
                                         session);
                break;
            case 0:
                worker.publishWroteError(session.getContext()
                                                .getSort()
                                                .getError(),
                                         WRITE_ZERO,
                                         new IllegalArgumentException("wrote zero!"),
                                         session);
                break;
            default:
                _Logger.debug("aio wrote %d | %s", result, session);
                worker.publishWrote(_WroteOperator, result, session);
                break;
        }
    }

    @Override
    public void failed(Throwable exc, ISession<C> session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishWroteError(session.getContext()
                                        .getSort()
                                        .getError(),
                                 WRITE_FAILED,
                                 exc,
                                 session);
    }
}
