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

package com.isahl.chess.queen.events.functions;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.AioWorker;

import java.io.EOFException;
import java.nio.channels.CompletionHandler;

import static com.isahl.chess.king.base.features.IError.Type.*;

/**
 * @author william.d.zk
 */
public class AioWriter
        implements CompletionHandler<Integer, ISession>
{
    private final Logger       _Logger        = Logger.getLogger("io.queen.operator." + getClass().getSimpleName());
    private final SessionWrote _WroteOperator = new SessionWrote(this);

    @Override
    public void completed(Integer result, ISession session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        switch(result) {
            case -1 -> worker.publishWroteError(session.getError(), WRITE_EOF, new EOFException("wrote -1!"), session);
            case 0 -> worker.publishWroteError(session.getError(),
                                               WRITE_ZERO,
                                               new IllegalArgumentException("wrote zero!"),
                                               session);
            default -> {
                _Logger.debug("aio wrote %d | %s", result, session);
                worker.publishWrote(_WroteOperator, result, session);
            }
        }
    }

    @Override
    public void failed(Throwable exc, ISession session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishWroteError(session.getError(), WRITE_FAILED, exc, session);
    }
}
