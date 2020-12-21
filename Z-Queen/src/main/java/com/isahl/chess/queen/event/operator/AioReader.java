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

package com.isahl.chess.queen.event.operator;

import static com.isahl.chess.queen.event.inf.IError.Type.READ_EOF;
import static com.isahl.chess.queen.event.inf.IError.Type.READ_FAILED;
import static com.isahl.chess.queen.event.inf.IError.Type.READ_ZERO;

import java.io.EOFException;
import java.nio.channels.CompletionHandler;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.async.AioWorker;
import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class AioReader
        implements
        CompletionHandler<Integer,
                          ISession>
{
    private final Logger _Logger = Logger.getLogger("io.queen.operator." + getClass().getSimpleName());

    @Override
    public void completed(Integer result, ISession session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        switch (result)
        {
            case -1 -> worker.publishReadError(session.getError(),
                                               READ_EOF,
                                               new EOFException("Read Negative"),
                                               session);
            case 0 ->
                {
                    worker.publishReadError(session.getError(),
                                            READ_ZERO,
                                            new IllegalStateException("Read Zero"),
                                            session);
                    try {
                        session.readNext(this);
                    }
                    catch (Exception e) {
                        // ignore
                    }
                }
            default ->
                {
                    _Logger.debug("read count: %d | %s", result, session);
                    try {
                        worker.publishRead(session.getDecoder(), new AioPacket(session.read(result)), session);
                    }
                    catch (Exception e) {
                        worker.publishReadError(session.getError(), READ_FAILED, e, session);
                    }
                }
        }
    }

    @Override
    public void failed(Throwable exc, ISession session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishReadError(session.getError(), READ_FAILED, exc, session);
    }
}
