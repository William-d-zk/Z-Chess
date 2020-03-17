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

package com.tgx.chess.king.base.schedule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import com.tgx.chess.king.base.log.Logger;

/**
 * @author William.d.zk
 */
public class ScheduleHandler<A>
        implements
        TimeWheel.IWheelItem<A>
{
    private final static Logger _LOG = Logger.getLogger(ScheduleHandler.class.getSimpleName());
    private A                   attach;
    private final boolean       _Cycle;
    private final Consumer<A>   _Callback;
    private final int           _Priority;
    private final long          _Tick;
    private final ReentrantLock _Lock;

    public ScheduleHandler(long delaySecond,
                           boolean cycle,
                           Consumer<A> callback,
                           int priority)
    {
        _Cycle = cycle;
        _Tick = TimeUnit.SECONDS.toMillis(delaySecond);
        _Callback = callback;
        _Priority = priority;
        _Lock = new ReentrantLock();
    }

    public ScheduleHandler(long delaySecond,
                           boolean cycle)
    {
        this(delaySecond, cycle, null, PRIORITY_NORMAL);
    }

    public ScheduleHandler(long delaySecond,
                           Consumer<A> callback)
    {
        this(delaySecond, false, callback, PRIORITY_NORMAL);
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
        _LOG.info("on time:%s %s:%d",
                  get(),
                  delta > 0 ? "ahead"
                            : "delay",
                  Math.abs(delta));
    }

    @Override
    public void onCall()
    {
        if (_Callback != null && attach != null) {
            _Callback.accept(attach);
        }
    }

    private long expect;

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
