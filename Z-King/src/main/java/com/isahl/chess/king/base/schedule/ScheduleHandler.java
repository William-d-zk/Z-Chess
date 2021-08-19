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

package com.isahl.chess.king.base.schedule;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.isahl.chess.king.base.inf.IValid;

/**
 * @author William.d.zk
 */
public class ScheduleHandler<A extends IValid>
        implements
        TimeWheel.IWheelItem<A>
{
    private A                   attach;
    private final boolean       _Cycle;
    private final Consumer<A>   _Callback;
    private final int           _Priority;
    private final long          _Tick;
    private final ReentrantLock _Lock;

    public ScheduleHandler(Duration delay,
                           boolean cycle,
                           Consumer<A> callback,
                           int priority)
    {
        _Cycle = cycle;
        _Tick = delay.toMillis();
        _Callback = callback;
        _Priority = priority;
        _Lock = new ReentrantLock();
    }

    public ScheduleHandler(Duration delay,
                           boolean cycle)
    {
        this(delay, cycle, null, PRIORITY_NORMAL);
    }

    public ScheduleHandler(Duration delay,
                           Consumer<A> callback)
    {
        this(delay, false, callback, PRIORITY_NORMAL);
    }

    public ScheduleHandler(Duration delay,
                           boolean cycle,
                           Consumer<A> callback)
    {
        this(delay, cycle, callback, PRIORITY_NORMAL);
    }

    @Override
    public boolean isCycle()
    {
        return _Cycle;
    }

    @Override
    public A get()
    {
        return attach;
    }

    @Override
    public void beforeCall()
    {
        long delta = expect - System.currentTimeMillis();
    }

    @Override
    public void onCall()
    {
        if (_Callback != null && attach != null && attach.isValid()) {
            _Callback.accept(attach);
        }
    }

    private long expect;

    public long getExpect()
    {
        return expect;
    }

    @Override
    public void setup()
    {
        expect = System.currentTimeMillis() + _Tick;
    }

    @Override
    public int getPriority()
    {
        return _Priority;
    }

    @Override
    public long getTick()
    {
        return _Tick;
    }

    @Override
    public void attach(A a)
    {
        attach = a;
    }

    @Override
    public void lock()
    {
        _Lock.lock();
    }

    @Override
    public void unlock()
    {
        _Lock.unlock();
    }

    @Override
    public String toString()
    {
        return "ScheduleHandler{"
               + "attach="
               + attach
               + ", _Cycle="
               + _Cycle
               + ", _Callback="
               + _Callback
               + ", _Priority="
               + _Priority
               + ", _MilliTick="
               + _Tick
               + '}';
    }

}
