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
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IError;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;

abstract class BaseDispatcher<C extends IContext>
        extends
        BasePipeEventHandler<C>
{
    final RingBuffer<QEvent>           _Link;
    final RingBuffer<QEvent>           _Cluster;
    final RingBuffer<QEvent>           _Error;

    private final RingBuffer<QEvent>[] _Workers;
    private final int                  _WorkerMask;

    @SafeVarargs
    BaseDispatcher(RingBuffer<QEvent> link, RingBuffer<QEvent> cluster, RingBuffer<QEvent> error, RingBuffer<QEvent>... workers) {
        _Link = link;
        _Cluster = cluster;
        _Error = error;
        _Workers = workers;
        _WorkerMask = _Workers.length - 1;
        if (Integer.bitCount(_Workers.length) != 1) { throw new IllegalArgumentException("workers' length must be a power of 2"); }
    }

    RingBuffer<QEvent> dispatchWorker(long seq) {
        return _Workers[(int) (seq & _WorkerMask)];
    }

    <V, A, R> void dispatch(ISort sorter, IOperator.Type type, V v, A a, IOperator<V, A, R> op) {
        switch (sorter.getMode()) {
            case CLUSTER:
                publish(_Cluster, type, new Pair<>(v, a), op);
                break;
            case LINK:
                publish(_Link, type, new Pair<>(v, a), op);
                break;
            default:
                break;
        }
    }

    <V, A, R> void dispatchError(ISort sorter, IError.Type type, V v, A a, IOperator<V, A, R> op) {
        switch (sorter.getMode()) {
            case CLUSTER:
                error(_Cluster, type, new Pair<>(v, a), op);
                break;
            case LINK:
                error(_Link, type, new Pair<>(v, a), op);
                break;
            default:
                break;
        }
    }

}
