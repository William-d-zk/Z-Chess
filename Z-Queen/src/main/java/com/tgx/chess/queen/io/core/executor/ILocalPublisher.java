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

import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2016/12/22
 */
public interface ILocalPublisher<C extends IContext<C>>
        extends
        IActivity<C>
{
    RingBuffer<QEvent> getPublisher(ISession<C> session, IOperator.Type type);

    RingBuffer<QEvent> getCloser(ISession<C> session, IOperator.Type type);

    ReentrantLock getLock(ISession<C> session, IOperator.Type type);

    @Override
    @SuppressWarnings("unchecked")
    default boolean send(ISession<C> session, IOperator.Type type, IControl<C>... toSends)
    {
        if (session == null || toSends == null || toSends.length == 0) { return false; }
        final RingBuffer<QEvent> _LocalSendEvent = getPublisher(session, type);
        final ReentrantLock _LocalLock = getLock(session, type);
        if (_LocalLock.tryLock()) {
            try {
                long sequence = _LocalSendEvent.next();
                try {
                    QEvent event = _LocalSendEvent.get(sequence);
                    event.produce(type,
                                  new Pair<>(toSends, session),
                                  session.getContext()
                                         .getSort()
                                         .getTransfer());
                    return true;
                }
                finally {
                    _LocalSendEvent.publish(sequence);
                }
            }
            finally {
                _LocalLock.unlock();
            }
        }
        return false;
    }

    @Override
    default void close(ISession<C> session, IOperator.Type type)
    {
        if (session == null) { return; }
        final RingBuffer<QEvent> _LocalCloseEvent = getCloser(session, type);
        final ReentrantLock _LocalLock = getLock(session, type);
        _LocalLock.lock();
        try {
            long sequence = _LocalCloseEvent.next();
            try {
                QEvent event = _LocalCloseEvent.get(sequence);
                event.produce(IOperator.Type.CLOSE,
                              new Pair<>(null, session),
                              session.getContext()
                                     .getSort()
                                     .getCloser());
            }
            finally {
                _LocalCloseEvent.publish(sequence);
            }
        }
        finally {
            _LocalLock.unlock();
        }

    }
}
