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
package com.isahl.chess.king.base.disruptor.features.event;

import com.isahl.chess.king.base.disruptor.features.functions.IBinaryOperator;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.IError;
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;

import java.util.List;

/**
 * @author William.d.zk
 */
public interface IEvent
        extends IReset
{
    OperateType getEventType();

    IError.Type getErrorType();

    <T, U, R> IBinaryOperator<T, U, R> getEventBinaryOp();

    <T, R> IOperator<T, R> getEventOp();

    IPair getComponent();

    IoSerial getContent();

    List<ITriple> getResultList();

    <V, A, R> void produce(OperateType t, IPair component, IBinaryOperator<V, A, R> binaryOperator);

    <V, R> void produce(OperateType t, IoSerial content, IOperator<V, R> operator);

    <E, H, R> void error(IError.Type t, IPair component, IBinaryOperator<E, H, R> binaryOperator);

    <R> void error(IError.Type t, Throwable throwable, IOperator<Throwable, R> operator);

    void produce(OperateType t, List<ITriple> cp);

    default boolean hasError()
    {
        return getErrorType() != IError.Type.NO_ERROR;
    }
}
