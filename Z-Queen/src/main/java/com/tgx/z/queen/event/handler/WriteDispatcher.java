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

package com.tgx.z.queen.event.handler;

import com.lmax.disruptor.RingBuffer;
import com.tgx.z.queen.event.inf.IPipeEventHandler;
import com.tgx.z.queen.event.processor.QEvent;

public class WriteDispatcher
        implements
        IPipeEventHandler<QEvent, QEvent>
{
    final RingBuffer<QEvent>[] _Workers;
    final int                  _WorkerMask;

    public WriteDispatcher(RingBuffer<QEvent>... workers) {
        _Workers = workers;
        _WorkerMask = _Workers.length - 1;

    }

    RingBuffer<QEvent> dispatchWorker(int seq) {
        return _Workers[seq & _WorkerMask];
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception {
        if (event.hasError()) {

        }
        else {
            switch (event.getEventType()) {
                case NULL://在前一个处理器event.reset().
                case IGNORE://没有任何时间需要跨 Barrier 投递向下一层 Pipeline
                    break;
            }
        }
    }
}
