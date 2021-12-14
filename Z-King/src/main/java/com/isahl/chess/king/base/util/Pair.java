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

import com.isahl.chess.king.base.features.model.IPair;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public class Pair<FIRST, SECOND>
        implements IPair,
                   Comparable<Pair<FIRST, SECOND>>
{
    private FIRST  first;
    private SECOND second;

    public Pair(FIRST first, SECOND second)
    {
        this.first = first;
        this.second = second;
    }

    public Pair()
    {
    }

    public static <F, S> Pair<F, S> of(F first, S second)
    {
        return new Pair<>(first, second);
    }

    public void setFirst(FIRST f)
    {
        first = f;
    }

    public void setSecond(SECOND s)
    {
        second = s;
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
    public Pair<FIRST, SECOND> clone()
    {
        return new Pair<>(first, second);
    }

    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof Pair)) {return false;}
        if(this != obj) {
            @SuppressWarnings("unchecked")
            Pair<FIRST, SECOND> other = (Pair<FIRST, SECOND>) obj;
            return first.equals(other.first) && second.equals(other.second);
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("< %s:%s, %s:%s >",
                             first.getClass()
                                  .getSimpleName(),
                             first,
                             second.getClass()
                                   .getSimpleName(),
                             second);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(first, second);
    }

    @Override
    public int compareTo(Pair<FIRST, SECOND> o)
    {
        int a = first.toString()
                     .compareTo(o.first.toString());
        int b = second.toString()
                      .compareTo(o.second.toString());
        return a == 0 ? b : a;
    }
}
