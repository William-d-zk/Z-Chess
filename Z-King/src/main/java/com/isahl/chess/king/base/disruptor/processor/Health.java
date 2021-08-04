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

package com.isahl.chess.king.base.disruptor.processor;

import com.isahl.chess.king.base.disruptor.event.inf.IHealth;
import com.isahl.chess.king.base.log.Logger;

import java.time.Duration;

public class Health
        implements IHealth
{
    private final Logger _Logger;
    private final int    _ThreadSlot;

    public Health(int slot)
    {
        _Logger = Logger.getLogger("base.king." + (_ThreadSlot = slot) + "@" + getClass().getSimpleName());
    }

    private boolean  mEnable;
    private Duration mStatisticDuration = Duration.ZERO;
    private Duration mAverageDuration   = Duration.ZERO;
    private Duration mDuration          = Duration.ZERO;
    private long     mStartTime;
    private long     mCount;
    private long     mNumber;
    private long     mStart;

    public void enable()
    {
        mEnable = true;
    }

    public void disable()
    {
        mEnable = false;
    }

    /**
     * 衰减函数
     */
    private void attenuation()
    {
        mStatisticDuration = mStatisticDuration.plus(mDuration);
        //TODO count 和 统计的总次数都进行衰减
    }

    @Override
    public boolean isEnabled()
    {
        return mEnable;
    }

    @Override
    public void collectOn(long start)
    {
        mStartTime = System.nanoTime();
        mStart = start;
        mNumber++;
    }

    @Override
    public void collectOff(long end)
    {
        if(mNumber > Long.MAX_VALUE - 1) {
            mNumber = 0;
        }
        mCount += end - mStart;
        mDuration = Duration.ofNanos(System.nanoTime() - mStartTime);
        attenuation();
        if(mCount > 0) {
            mAverageDuration = mStatisticDuration.dividedBy(mCount);
        }
    }

    @Override
    public boolean isHealthy()
    {
        return false;
    }

    @Override
    public Duration averageEventHandling()
    {
        return mAverageDuration;
    }
}
