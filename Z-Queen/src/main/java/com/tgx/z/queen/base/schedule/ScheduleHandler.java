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

import java.util.function.Consumer;

import com.tgx.z.queen.base.schedule.TimeWheel.ITimeoutHandler;

public class ScheduleHandler<A>
        implements
        ITimeoutHandler<A>
{
    private A             attach;
    private final boolean _Cycle;
    private Consumer<A>   _Callback;

    public ScheduleHandler(boolean cycle, Consumer<A> callback) {
        _Cycle = cycle;
        _Callback = callback;
    }

    @Override
    public boolean isCycle() {
        return _Cycle;
    }

    @Override
    public A get() {
        return attach;
    }

    @Override
    public A call() throws Exception {
        _Callback.accept(attach);
        return attach;
    }

    @Override
    public void attach(A attachment) {
        attach = attachment;
    }
}
