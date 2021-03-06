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
package com.isahl.chess.king.base.disruptor.event.inf;

import java.util.List;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.inf.IError;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.base.inf.ITriple;

/**
 * @author William.d.zk
 */
public interface IEvent
        extends
        IReset
{
    OperatorType getEventType();

    IError.Type getErrorType();

    <T,
     U,
     R> IOperator<T,
                  U,
                  R> getEventOp();

    IPair getContent();

    List<ITriple> getContentList();

    <V,
     A,
     R> void produce(OperatorType t,
                     IPair content,
                     IOperator<V,
                               A,
                               R> operator);

    <E,
     H,
     R> void error(IError.Type t,
                   IPair content,
                   IOperator<E,
                             H,
                             R> operator);

    void produce(OperatorType t, List<ITriple> cp);

    default boolean hasError()
    {
        return getErrorType() != IError.Type.NO_ERROR;
    }
}
