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

package com.tgx.z.queen.base.schedule;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tgx.z.queen.base.schedule.TimeWheel.ITimeoutHandler;

class TimeWheelTest
{

    static Logger LOG = LoggerFactory.getLogger(TimeWheelTest.class.getName());

    @Test
    void acquire() throws InterruptedException {
        TimeWheel timeWheel = new TimeWheel(1000, TimeUnit.MILLISECONDS);
        Future<String> future = timeWheel.acquire(3, TimeUnit.SECONDS, "timeout", new ITimeoutHandler<String>()
        {

            @Override
            public void attach(String attachment) {
                attach = attachment;
            }

            String attach;

            @Override
            public String get() {
                return attach;
            }

            @Override
            public String call() throws Exception {
                LOG.info("time out handler");
                return attach;
            }

            @Override
            public boolean isCycle() {
                return true;
            }

        });

        Thread.sleep(TimeUnit.SECONDS.toMillis(100));
    }
}