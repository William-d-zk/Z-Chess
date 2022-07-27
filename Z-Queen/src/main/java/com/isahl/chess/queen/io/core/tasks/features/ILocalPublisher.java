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

package com.isahl.chess.queen.io.core.tasks.features;

import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.queen.events.model.QEvent;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author william.d.zk
 * @date 2016/12/22
 */
public interface ILocalPublisher
{
    RingBuffer<QEvent> selectPublisher(IOperator.Type type);

    RingBuffer<QEvent> selectCloser(IOperator.Type type);

    ReentrantLock selectLock(IOperator.Type type);

    default <T, U, R> boolean publish(IOperator.Type type, IOperator<T, U, R> next, IPair... contents)
    {
        if(contents == null || contents.length == 0 || type == null) {
            return false;
        }
        RingBuffer<QEvent> producer = selectPublisher(type);
        ReentrantLock lock = selectLock(type);
        if(lock.tryLock()) {
            try {
                /*
                 * send-event → write-dispatcher
                 * 虽然可以利用dispatcher 擦除 type 信息，直接转头 write过程，
                 * 但是这样写太过晦涩，还是直接在这一层完成分拆比较好
                 */
                for(IPair content : contents) {
                    long sequence = producer.next();
                    try {
                        QEvent event = producer.get(sequence);
                        event.produce(type, content, next);
                    }
                    finally {
                        producer.publish(sequence);
                    }
                }
                return true;
            }
            finally {
                lock.unlock();
            }
        }
        return false;
    }

    default <T, U, R> void close(IOperator.Type type, IPair content, IOperator<T, U, R> next)
    {
        if(content == null || content.isEmpty() || type == null) {
            return;
        }
        RingBuffer<QEvent> producer = selectPublisher(type);
        ReentrantLock lock = selectLock(type);
        lock.lock();
        try {
            long sequence = producer.next();
            try {
                QEvent event = producer.get(sequence);
                event.produce(IOperator.Type.LOCAL_CLOSE, content, next);
            }
            finally {
                producer.publish(sequence);
            }
        }
        finally {
            lock.unlock();
        }

    }

}
