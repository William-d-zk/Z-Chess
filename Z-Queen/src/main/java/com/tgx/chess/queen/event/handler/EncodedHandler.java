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

package com.tgx.chess.queen.event.handler;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ISession;

public class EncodedHandler
        implements
        IPipeEventHandler<QEvent, QEvent>,
        ISessionHandler
{
    private final Logger             _Log = Logger.getLogger(getClass().getName());

    private final RingBuffer<QEvent> _Error;

    public EncodedHandler(RingBuffer<QEvent> error)
    {
        _Error = error;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.hasError()) {
            switch (event.getErrorType())
            {
                case FILTER_ENCODE:
                case ILLEGAL_STATE:
                case ILLEGAL_BIZ_STATE:
                default:
                    IOperator<Throwable, ISession> errorOperator = event.getEventOp();
                    Pair<Throwable, ISession> errorContent = event.getContent();
                    ISession session = errorContent.second();
                    Throwable throwable = errorContent.first();
                    Triple<Void, ISession, IOperator<Void, ISession>> errorResult = errorOperator.handle(throwable, session);
                    error(_Error, IError.Type.CLOSED, errorResult.first(), session, errorResult.third());
                    break;
            }
        }
        event.reset();
    }
}
