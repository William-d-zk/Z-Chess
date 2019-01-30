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

package com.tgx.chess.queen.io.core.executor;

import static com.tgx.chess.queen.event.operator.ZOperatorHolder.CLOSE_OPERATOR;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public interface ILocalPublisher
{
    RingBuffer<QEvent> getLocalPublisher(ISession session);

    RingBuffer<QEvent> getLocalCloser(ISession session);

    ReentrantLock getLocalLock();

    default boolean localSend(ISession session, ICommand... toSends)
    {
        Objects.requireNonNull(toSends);
        Objects.requireNonNull(session);
        final RingBuffer<QEvent> _BizLocalSendEvent = getLocalPublisher(session);
        final ReentrantLock _LocalLock = getLocalLock();
        if (_LocalLock.tryLock()) try {
            long sequence = _BizLocalSendEvent.next();
            try {
                QEvent event = _BizLocalSendEvent.get(sequence);
                event.produce(IOperator.Type.LOCAL,
                              toSends,
                              session,
                              session.getMode()
                                     .getOutOperator());
                return true;
            }
            finally {
                _BizLocalSendEvent.publish(sequence);
            }
        }
        finally {
            _LocalLock.unlock();
        }
        return false;
    }

    default void localClose(ISession session)
    {
        Objects.requireNonNull(session);
        final RingBuffer<QEvent> _BizLocalCloseEvent = getLocalCloser(session);
        final ReentrantLock _LocalLock = getLocalLock();
        _LocalLock.lock();
        try {
            long sequence = _BizLocalCloseEvent.next();
            try {
                QEvent event = _BizLocalCloseEvent.get(sequence);
                event.produce(IOperator.Type.CLOSE, null, session, CLOSE_OPERATOR());
            }
            finally {
                _BizLocalCloseEvent.publish(sequence);
            }
        }
        finally {
            _LocalLock.unlock();
        }

    }
}
