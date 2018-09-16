/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.chess.queen.io.core.manager;

import static com.tgx.chess.queen.event.operator.OperatorHolder.CLOSE_OPERATOR;

import java.nio.channels.AsynchronousChannelGroup;

import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.config.Config;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IOperator.Type;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

public abstract class QueenManager
        extends
        AioSessionManager
{
    private final Logger   _Logger       = Logger.getLogger(getClass().getName());

    private final Sender[] _DomainSender = new Sender[4];

    public QueenManager(Config config) {
        super(config);
    }

    public void add(long code, AsynchronousChannelGroup channelGroup, RingBuffer<QEvent> localBack, RingBuffer<QEvent> localSend) {
        _DomainSender[getSlot(code)] = new Sender(channelGroup, localBack, localSend);
    }

    private class Sender
    {

        private final AsynchronousChannelGroup _ChannelGroup;
        private final RingBuffer<QEvent>       _LocalBackPublisher, _LocalSendPublisher;

        private Sender(AsynchronousChannelGroup channelGroup,
                       RingBuffer<QEvent> localBackPublisher,
                       RingBuffer<QEvent> localSendPublisher) {
            _ChannelGroup = channelGroup;
            _LocalBackPublisher = localBackPublisher;
            _LocalSendPublisher = localSendPublisher;
        }

        void localClose(final ISession session) {
            if (session != null) try {
                long sequence = _LocalBackPublisher.tryNext();
                try {
                    QEvent event = _LocalBackPublisher.get(sequence);
                    event.produce(Type.CLOSE, null, session, CLOSE_OPERATOR());
                }
                finally {
                    _LocalBackPublisher.publish(sequence);
                }

            }
            catch (InsufficientCapacityException e) {
                _Logger.warning("local close -> full", e);
            }
        }

        void localSend(ICommand toSend, ISession session, IOperator<ICommand, ISession> write_operator) {
            if (session != null) try {
                long sequence = _LocalSendPublisher.tryNext();
                try {
                    QEvent event = _LocalSendPublisher.get(sequence);
                    event.produce(Type.WRITE, toSend, session, write_operator);
                }
                finally {
                    _LocalSendPublisher.publish(sequence);
                }

            }
            catch (InsufficientCapacityException e) {
                _Logger.warning("local send -> full", e);
            }
        }

    }

    public void localClose(long index) {
        _DomainSender[getSlot(index)].localClose(findSessionByIndex(index));
    }

    public void localSend(ICommand toSend, ISession session, IOperator<ICommand, ISession> write_operator) {
        _DomainSender[getSlot(session.getIndex())].localSend(toSend, session, write_operator);
    }

}
