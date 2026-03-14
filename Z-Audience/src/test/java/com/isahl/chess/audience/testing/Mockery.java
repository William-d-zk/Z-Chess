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

package com.isahl.chess.audience.testing;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public class Mockery
{

    private Mockery()
    {
    }

    public static <T> T mock(Class<T> classToMock)
    {
        return Mockito.mock(classToMock);
    }

    public static <T> T mock(Class<T> classToMock, Consumer<T> setup)
    {
        T mockInstance = Mockito.mock(classToMock);
        setup.accept(mockInstance);
        return mockInstance;
    }

    public static <T> T spy(T object)
    {
        return Mockito.spy(object);
    }

    public static <T> T spy(Class<T> classToSpy, Supplier<T> factory)
    {
        return Mockito.spy(factory.get());
    }

    public static <T> T anyMock()
    {
        return Mockito.any();
    }

    public static <T> T refEq(T obj)
    {
        return Mockito.refEq(obj);
    }

    public static <T> T same(T obj)
    {
        return Mockito.same(obj);
    }

    public static <T> T isNull()
    {
        return Mockito.isNull();
    }

    public static <T> T notNull()
    {
        return Mockito.notNull();
    }

    public static <T> T isA(Class<T> clazz)
    {
        return Mockito.isA(clazz);
    }

    public static void verifyNoMoreInteractions(Object... mocks)
    {
        Mockito.verifyNoMoreInteractions(mocks);
    }

    public static void reset(Object... mocks)
    {
        Mockito.reset(mocks);
    }

    public static void clearInvocations(Object... mocks)
    {
        Mockito.clearInvocations(mocks);
    }

    public static <T> Answer<T> returns(T value)
    {
        return invocation -> value;
    }

    public static <T> Answer<T> throwsException(Throwable throwable)
    {
        return invocation ->
        {
            throw throwable;
        };
    }
}
