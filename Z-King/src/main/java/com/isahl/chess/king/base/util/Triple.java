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
package com.isahl.chess.king.base.util;

import com.isahl.chess.king.base.features.model.ITriple;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public class Triple<FIRST, SECOND, THIRD>
        implements ITriple,
                   Comparable<Triple<FIRST, SECOND, THIRD>>
{
    private FIRST  first;
    private SECOND second;
    private THIRD  third;

    public Triple(FIRST first, SECOND second, THIRD third)
    {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public Triple()
    {
    }

    @Override
    @SuppressWarnings("unchecked")
    public FIRST getFirst()
    {
        return first;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SECOND getSecond()
    {
        return second;
    }

    @Override
    @SuppressWarnings("unchecked")
    public THIRD getThird()
    {
        return third;
    }

    public void setFirst(FIRST f)
    {
        first = f;
    }

    public void setSecond(SECOND s)
    {
        second = s;
    }

    public void setThird(THIRD t)
    {
        third = t;
    }

    @Override
    public Triple<FIRST, SECOND, THIRD> clone()
    {
        return new Triple<>(first, second, third);
    }

    public static <F, S, T> Triple<F, S, T> of(F f, S s, T t)
    {
        return new Triple<>(f, s, t);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Triple)) {return false;}
        if(this != obj) {
            @SuppressWarnings("unchecked")
            Triple<FIRST, SECOND, THIRD> other = (Triple<FIRST, SECOND, THIRD>) obj;
            return first.equals(other.first) && second.equals(other.second) && third.equals(other.third);
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("< %s, %s, %s >", first, second, third);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(first, second, third);
    }

    @Override
    public int compareTo(Triple<FIRST, SECOND, THIRD> o)
    {
        int a = first.toString()
                     .compareTo(o.first.toString());
        int b = second.toString()
                      .compareTo(o.second.toString());
        int c = third.toString()
                     .compareTo(o.third.toString());
        return a == 0 ? b == 0 ? c : b : a;
    }
}
