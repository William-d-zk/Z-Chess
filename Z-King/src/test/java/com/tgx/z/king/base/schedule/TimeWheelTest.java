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

package com.tgx.z.king.base.schedule;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;

public class TimeWheelTest
{

    static Logger LOG = LoggerFactory.getLogger(TimeWheelTest.class.getName());

    @Test
    void acquire() throws InterruptedException
    {
        Random random = new Random();
        TimeWheel timeWheel = new TimeWheel();
        timeWheel.acquire("t  1", new ScheduleHandler<>(7, true));
        Thread.sleep(random.nextInt(1500));
        timeWheel.acquire("t  2", new ScheduleHandler<>(7, true));
        timeWheel.acquire("t  3", new ScheduleHandler<>(11, true));
        timeWheel.acquire("t  4", new ScheduleHandler<>(9, true));
        Thread.sleep(random.nextInt(8500));
        timeWheel.acquire("t  5", new ScheduleHandler<>(5, true));
        timeWheel.acquire("t  6", new ScheduleHandler<>(17, true));
        timeWheel.acquire("t  7", new ScheduleHandler<>(23, true));
        timeWheel.acquire("t  8", new ScheduleHandler<>(31, true));
        Thread.sleep(TimeUnit.SECONDS.toMillis(200));
    }
}