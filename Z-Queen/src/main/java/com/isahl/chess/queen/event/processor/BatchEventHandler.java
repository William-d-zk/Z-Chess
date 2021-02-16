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

package com.isahl.chess.queen.event.processor;

import java.util.LinkedList;
import java.util.Queue;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IEvent;
import com.lmax.disruptor.EventHandler;

/**
 * @author william.d.zk
 * @date 2020/12/26
 */
public abstract class BatchEventHandler<E extends IEvent>
        implements
        EventHandler<E>
{

    private final Queue<E> _BatchBuffered = new LinkedList<>();

    @Override
    public void onEvent(E event, long sequence, boolean endOfBatch) throws Exception
    {
        if (endOfBatch && _BatchBuffered.isEmpty()) {
            doEvent(event);
        }
        else {
            bufferBatch(event);
            if (endOfBatch) {
                doBatch(_BatchBuffered);
                _BatchBuffered.clear();
            }
        }
    }

    public abstract void doEvent(E event);

    public abstract void doBatch(Queue<E> events);

    private void bufferBatch(E event)
    {
        if (event == null || event.getEventType() == OperatorType.IGNORE) { return; }
        _BatchBuffered.offer(event);
    }
}
