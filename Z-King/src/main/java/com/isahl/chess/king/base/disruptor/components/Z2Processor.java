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

package com.isahl.chess.king.base.disruptor.components;

import com.isahl.chess.king.base.disruptor.features.event.IEvent;
import com.isahl.chess.king.base.disruptor.features.flow.IBatchHandler;
import com.isahl.chess.king.base.log.Logger;
import com.lmax.disruptor.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * @author William.d.zk
 */
public class Z2Processor<T extends IEvent>
        implements EventProcessor
{
    private final Logger _Logger;

    private static final int IDLE    = 0;
    private static final int HALTED  = IDLE + 1;
    private static final int RUNNING = HALTED + 1;

    private final AtomicInteger     _Running = new AtomicInteger(IDLE);
    private final DataProvider<T>[] _Providers;
    private final SequenceBarrier[] _Barriers;
    private final IBatchHandler<T>  _Handler;
    private final Sequence[]        _Sequences;
    private       String            mThreadName;
    private       boolean           mIsGating;

    public Z2Processor(DataProvider<T>[] providers, SequenceBarrier[] barriers, IBatchHandler<T> handler)
    {
        if(providers.length != barriers.length) {throw new IllegalArgumentException();}
        _Logger = Logger.getLogger(String.format("base.king.%s.%s",
                                                 getClass().getSimpleName(),
                                                 handler.getClass()
                                                        .getSimpleName()));
        _Providers = providers;
        _Barriers = barriers;
        _Handler = handler;
        _Sequences = new Sequence[providers.length];
        for(int i = 0; i < _Sequences.length; i++) {
            _Sequences[i] = new Sequence(-1);
        }
    }

    public void setThreadName(String name)
    {
        mThreadName = name;
    }

    public Z2Processor<T> setGating()
    {
        mIsGating = true;
        return this;
    }

    @Override
    public void run()
    {
        if(_Running.compareAndSet(IDLE, RUNNING)) {
            if(mThreadName != null) {
                Thread.currentThread()
                      .setName(mThreadName);
            }
            for(SequenceBarrier barrier : _Barriers) {
                barrier.clearAlert();
            }
            final int barrierLength = _Barriers.length;
            int count = 0;
            for(int i = 0, c = 0; i < barrierLength; i = i == barrierLength - 1 ? 0 : i + 1, c = c == 100 ? 0 : c + 1) {
                DataProvider<T> provider = _Providers[i];
                SequenceBarrier barrier = _Barriers[i];
                Sequence sequence = _Sequences[i];
                count += (int) processEvents(provider, barrier, sequence);
                // 没有任何 前置生产者的存在事件的时候暂停 5ms 释放 CPU，不超过100个事件，将释放 CPU
                if(c == 50 && count < 50) {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                }
                else if(c == 99) {
                    count = 0;
                }
            }
        }
        else {
            if(_Running.get() == RUNNING) {throw new IllegalStateException("Thread is already running");}
            else {
                halt();
            }
        }

    }

    @Override
    public Sequence getSequence()
    {
        throw new UnsupportedOperationException();
    }

    public Sequence[] getSequences()
    {
        return _Sequences;
    }

    @Override
    public void halt()
    {
        _Running.set(HALTED);
        for(SequenceBarrier barrier : _Barriers) {
            barrier.alert();
        }
    }

    @Override
    public boolean isRunning()
    {
        return _Running.get() != IDLE;
    }

    private long processEvents(DataProvider<T> provider, SequenceBarrier barrier, Sequence sequence)
    {
        long nextSequence = sequence.get() + 1L;
        long available = -1L;
        try {
            available = barrier.waitFor(-1);
            _Handler.onBatchStart(nextSequence - 1);
            while(nextSequence <= available) {
                T event = provider.get(nextSequence);
                _Handler.onEvent(event, nextSequence);
                nextSequence++;
                if(mIsGating) {
                    event.reset();
                }
            }
            sequence.set(available);
        }
        catch(TimeoutException | AlertException e) {
            /*
             * AlertException设计是为了动态终止processor，多路归并的场景中，reduce 不能被终止
             * TimeoutException 在多路归并的场景中不适用，waitFor(-1）一定会取得当前最后一个可用
             */
        }
        catch(Throwable ex) {
            _Logger.warning(ex);
            sequence.set(nextSequence);
            nextSequence++;
        }
        finally {
            _Handler.onBatchComplete(sequence.get());
        }
        return available - nextSequence + 1;
    }
}
