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

package com.tgx.chess.queen.io.core.manager;

import static com.tgx.chess.queen.event.inf.IOperator.Type.CONSENSUS;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.lmax.disruptor.RingBuffer;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.executor.ServerCore;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public abstract class QueenManager<C extends IContext<C>>
        extends
        AioSessionManager<C>
        implements
        IActivity<C>
{
    protected final ServerCore<C> _ServerCore;

    public QueenManager(IBizIoConfig config,
                        ServerCore<C> serverCore)
    {
        super(config);
        _ServerCore = serverCore;
    }

    @Override
    public void close(ISession<C> session, IOperator.Type type)
    {
        _ServerCore.close(session, type);
    }

    @Override
    @SafeVarargs
    public final boolean send(ISession<C> session, IOperator.Type type, IControl<C>... commands)
    {
        return _ServerCore.send(session, type, commands);
    }

    protected ServerCore<C> getServerCore()
    {
        return _ServerCore;
    }

    public <T> void publishConsensus(T content,
                                     IOperator<T,
                                               Void,
                                               List<ITriple>> operator)
    {
        final RingBuffer<QEvent> _LocalSendEvent = _ServerCore.getConsensusEvent();
        final ReentrantLock _LocalLock = _ServerCore.getConsensusLock();
        if (_LocalLock.tryLock()) {
            try {
                long sequence = _LocalSendEvent.next();
                try {
                    QEvent event = _LocalSendEvent.get(sequence);
                    event.produce(CONSENSUS, new Pair<>(content, null), operator);

                }
                finally {
                    _LocalSendEvent.publish(sequence);
                }
            }
            finally {
                _LocalLock.unlock();
            }
        }
    }
}
