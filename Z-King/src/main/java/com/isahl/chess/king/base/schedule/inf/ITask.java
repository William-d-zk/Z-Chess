/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.king.base.schedule.inf;

import java.util.concurrent.atomic.AtomicInteger;

import com.isahl.chess.king.base.inf.ILifeCycle;
import com.isahl.chess.king.base.schedule.Status;

/**
 * @author william.d.zk
 */
public interface ITask
        extends
        ILifeCycle
{
    int RETRY_COUNT_BITS = 2;
    int RETRY_LIMIT      = (1 << RETRY_COUNT_BITS) - 1;

    static int runStateOf(int state)
    {
        return state & ~RETRY_LIMIT;
    }

    static int retryCountOf(int c)
    {
        return c & RETRY_LIMIT;
    }

    static int ctlOf(int rs, int rc)
    {
        return rs | rc;
    }

    static boolean compareAndIncrementRetry(AtomicInteger _Ctl, int expect)
    {
        return _Ctl.compareAndSet(expect, expect + 1);
    }

    static void advanceRunState(AtomicInteger _Ctl, int targetState)
    {
        for (;;)
        {
            int c = _Ctl.get();
            if (c >= targetState || _Ctl.compareAndSet(c, ctlOf(targetState, retryCountOf(c)))) break;
        }
    }

    static boolean runStateLessThan(int c, int s)
    {
        return c < s;
    }

    static boolean runStateAtLeast(int c, int s)
    {
        return c >= s;
    }

    static boolean isRunning(int c)
    {
        return c < Status.STOP.getCode() && c > Status.PENDING.getCode();
    }

}
