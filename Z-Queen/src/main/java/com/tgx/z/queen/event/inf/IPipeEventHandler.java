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
package com.tgx.z.queen.event.inf;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.RingBuffer;

/**
 * @author William.d.zk
 */
public interface IPipeEventHandler<T extends IEvent, R extends IEvent>
        extends
        EventHandler<T>
{

    default <V, A> boolean tryPublish(RingBuffer<R> publisher, IOperator.Type t, V v, A a, IOperator<V, A> operator) {
        if (publisher == null) return true;
        try {
            long sequence = publisher.tryNext();
            try {
                R event = publisher.get(sequence);
                event.produce(t, v, a, operator);
                return true;
            }
            finally {
                publisher.publish(sequence);
            }
        }
        catch (InsufficientCapacityException e) {
            e.printStackTrace();
        }
        return false;
    }

    default <V, A> void publish(RingBuffer<R> publisher, IOperator.Type t, V v, A a, IOperator<V, A> operator) {
        if (publisher == null) return;
        long sequence = publisher.next();
        try {
            R event = publisher.get(sequence);
            event.produce(t, v, a, operator);
        }
        finally {
            publisher.publish(sequence);
        }
    }

    default <V, A> boolean tryError(RingBuffer<R> publisher, IError.Type t, V v, A a, IOperator<V, A> operator) {
        if (publisher == null) return true;
        try {
            long sequence = publisher.tryNext();
            try {
                R event = publisher.get(sequence);
                event.error(t, v, a, operator);
                return true;
            }
            finally {
                publisher.publish(sequence);
            }
        }
        catch (InsufficientCapacityException e) {
            e.printStackTrace();
        }
        return false;
    }

    default <V, A> void error(RingBuffer<R> publisher, IError.Type t, V v, A a, IOperator<V, A> operator) {
        if (publisher == null) return;
        long sequence = publisher.next();
        try {
            R event = publisher.get(sequence);
            event.error(t, v, a, operator);
        }
        finally {
            publisher.publish(sequence);
        }
    }

}
