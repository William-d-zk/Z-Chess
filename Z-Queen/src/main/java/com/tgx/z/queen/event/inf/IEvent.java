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

import java.util.List;

import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IError.Type;

/**
 * @author William.d.zk
 */
public interface IEvent
{
    IOperator.Type getEventType();

    IError.Type getErrorType();

    <V, A> IOperator<V, A> getEventOp();

    <V, A> Pair<V, A> getContent();

    <V, A> List<Triple<V, A, IOperator<V, A>>> getContentList();

    <V, A> void produce(IOperator.Type t, V v, A a, IOperator<V, A> operator);

    <E, H> void error(IError.Type t, E e, H h, IOperator<E, H> operator);

    <V, A> void produce(IOperator.Type t, List<Triple<V, A, IOperator<V, A>>> cp);

    default boolean hasError() {
        return !getErrorType().equals(Type.NO_ERROR);
    }
}
