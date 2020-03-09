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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import com.tgx.chess.king.base.log.Logger;

/**
 * @author William.d.zk
 */
public class TimeWheel
        extends
        ForkJoinPool
{
    private final Logger        _Logger = Logger.getLogger(getClass().getSimpleName());
    private final Thread        _Timer;
    private final int           _SlotBitLeft;//must <= 10
    private final int           _HashMod;
    private final long          _Tick;
    private final TickSlot<?>[] _ModHashEntryArray;
    private final ReentrantLock _Lock;
    private volatile int        vCtxSlot, vCtxLoop;

    public TimeWheel()
    {
        this(1, TimeUnit.SECONDS, 3);
    }

    public TimeWheel(long tick,
                     TimeUnit timeUnit,
                     int bitLeft)
    {
        super(bitLeft, ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        _Lock = new ReentrantLock();
        _Tick = timeUnit.toMillis(tick);
        _SlotBitLeft = bitLeft;
        _ModHashEntryArray = new TickSlot[1 << _SlotBitLeft];
        Arrays.setAll(_ModHashEntryArray, TickSlot::new);
        _HashMod = _ModHashEntryArray.length - 1;
        _Timer = new Thread(() ->
        {
            int correction = 13;// 5~20
            for (long align = 0, t, sleep, expect; !isShutdown();) {
                t = System.currentTimeMillis();
                sleep = _Tick - align;
                expect = t + sleep;
                if (sleep > correction) {
                    try {
                        Thread.sleep(sleep - correction);
                    }
                    catch (InterruptedException e) {
                        //ignore 没有地方执行interrupt操作
                    }
                }
                current_millisecond = System.currentTimeMillis();
                _Lock.lock();
                try {
                    List<HandleTask<?>> readyList = filterReady();
                    if (!readyList.isEmpty()) {
                        for (HandleTask<?> ready : readyList) {
                            submit(ready);
                        }
                    }
                    if (vCtxSlot == _HashMod) {
                        vCtxSlot = 0;
                        vCtxLoop++;
                    }
                    else {
                        vCtxSlot++;
                    }
                }
                finally {
                    _Lock.unlock();
                }
                //此处-sleep 计算当期这次过程的偏差值
                align = System.currentTimeMillis() - expect;
            }
        });
        _Timer.setName(String.format("TimerWheel-%d", _Timer.getId()));
        _Timer.start();
        _Logger.info("timer wheel start %s", _Timer.getName());
    }

    private int getCurrentLoop()
    {
        return vCtxLoop;
    }

    private int getCtxSlot()
    {
        return vCtxSlot;
    }

    private <A> void acquire(IWheelItem<A> item)
    {
        acquire(new HandleTask<>(item));
    }

    @SuppressWarnings("unchecked")
    private <A> void acquire(HandleTask<A> task)
    {
        IWheelItem<A> item = task._Item;
        _Lock.lock();
        try {
            int slot = task.acquire(getCurrentLoop(), getCtxSlot());
            TickSlot<A> tickSlot = (TickSlot<A>) _ModHashEntryArray[slot & _HashMod];
            int index = Collections.binarySearch(tickSlot, task);
            tickSlot.add(index < 0 ? -index - 1
                                   : index,
                         task);
            item.setup();
        }
        finally {
            _Lock.unlock();
        }
    }

    private List<HandleTask<?>> filterReady()
    {
        List<HandleTask<?>> readyList = new LinkedList<>();
        for (Iterator<? extends HandleTask<?>> it = _ModHashEntryArray[getCtxSlot()
                                                                       & _HashMod].iterator(); it.hasNext();)
        {
            HandleTask<?> handleTask = it.next();
            if (handleTask.getLoop() == getCurrentLoop()) {
                readyList.add(handleTask);
                it.remove();
            }
        }
        return readyList;
    }

    public interface IWheelItem<V>
            extends
            Comparable<IWheelItem<V>>,
            ITimeoutHandler<V>
    {

        int PRIORITY_NORMAL = 0;
        int PRIORITY_LV1    = 1 << 1;
        int PRIORITY_LV2    = 1 << 2;
        int PRIORITY_LV3    = 1 << 3;

        int getPriority();

        long getTick();

        void attach(V v);

        void setup();

        void cancel();

        void lock();

        void unlock();

        @Override
        default int compareTo(IWheelItem o)
        {
            int loopCmp = Long.compare(getTick(), o.getTick());
            return loopCmp == 0 ? Integer.compare(getPriority(), o.getPriority())
                                : loopCmp;
        }
    }

    interface ICycle
    {
        default boolean isCycle()
        {
            return false;
        }
    }

    interface ITimeoutHandler<A>
            extends
            Supplier<A>,
            ICycle
    {
        void beforeCall();

        void onCall();

        boolean isCanceled();
    }

    private class TickSlot<V>
            extends
            ArrayList<HandleTask<V>>
    {
        private final int _Slot;

        private TickSlot(int slot)
        {
            super(3);
            _Slot = slot;
        }

        public int getSlot()
        {
            return _Slot;
        }
    }

    private class HandleTask<V>
            implements
            Callable<IWheelItem<V>>,
            Comparable<HandleTask<V>>
    {
        private final IWheelItem<V> _Item;
        private final int           _Slot;
        private final int           _Loop;
        private int                 loop;

        HandleTask(IWheelItem<V> wheelItem)
        {
            _Item = wheelItem;
            int tok = (int) (_Item.getTick() / _Tick) - 1;
            _Loop = tok >>> _SlotBitLeft;
            _Slot = tok & _HashMod;
        }

        int getLoop()
        {
            return loop;
        }

        int acquire(int currentLoop, int currentSlot)
        {
            int slot;
            if (currentSlot + _Slot > _HashMod) {
                loop = _Loop + currentLoop + 1;
                slot = _Slot + currentSlot - _HashMod - 1;
            }
            else {
                loop = currentLoop + _Loop;
                slot = currentSlot + _Slot;
            }
            return slot;
        }

        @Override
        public IWheelItem<V> call()
        {
            _Item.lock();
            try {
                _Item.beforeCall();
                if (!_Item.isCanceled()) {
                    _Item.onCall();
                    if (_Item.isCycle()) {
                        TimeWheel.this.acquire(HandleTask.this);
                    }
                }
                return _Item;
            }
            finally {
                _Item.unlock();
            }
        }

        @Override
        public int compareTo(HandleTask<V> o)
        {
            return _Item.compareTo(o._Item);
        }
    }

    private volatile long current_millisecond;

    public long getCurrentMillisecond()
    {
        return current_millisecond;
    }

    public long getCurrentSecond()
    {
        return TimeUnit.MILLISECONDS.toSeconds(current_millisecond);
    }

    public <A> void acquire(A attach, IWheelItem<A> item)
    {
        item.attach(attach);
        acquire(item);
    }

}
