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
package com.isahl.chess.king.base.disruptor.features.flow;

import com.isahl.chess.king.base.disruptor.features.event.IEvent;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.lmax.disruptor.RingBuffer;

import java.util.List;

/**
 * @author William.d.zk
 */
public interface IPipeHandler<E extends IEvent>
        extends IBatchHandler<E>
{

    Logger _Logger();

    default <V, A, R> void publish(RingBuffer<E> publisher, IOperator.Type type, IPair content, IOperator<V, A, R> operator)
    {
        if(publisher == null || type == null || content == null) {return;}
        if(publisher.remainingCapacity() == 0) {
            _Logger().warning("publish block with %s", type.name());
        }
        long sequence = publisher.next();
        try {
            E event = publisher.get(sequence);
            event.produce(type, content, operator);
        }
        finally {
            publisher.publish(sequence);
        }
    }

    default void publish(RingBuffer<E> publisher, List<ITriple> contents)
    {
        if(publisher == null || contents == null || contents.isEmpty()) {return;}
        if(publisher.remainingCapacity() == 0) {
            _Logger().warning("publish block with writer");
        }
        long sequence = publisher.next();
        try {
            E event = publisher.get(sequence);
            event.produce(IOperator.Type.DISPATCH, contents);
        }
        finally {
            publisher.publish(sequence);
        }
    }

    default <V, A, R> void error(RingBuffer<E> publisher, IError.Type type, IPair content, IOperator<V, A, R> operator)
    {
        if(publisher == null || type == null || content == null) {return;}
        if(publisher.remainingCapacity() == 0) {
            _Logger().warning("error block with %s", type.name());
        }
        long sequence = publisher.next();
        try {
            E event = publisher.get(sequence);
            event.error(type, content, operator);
        }
        finally {
            publisher.publish(sequence);
        }
    }
}
