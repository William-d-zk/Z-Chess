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
package com.tgx.chess.king.base.util;

import java.util.Objects;

import com.tgx.chess.king.base.inf.ISquare;

/**
 * @author William.d.zk
 */
public class Square<FIRST, SECOND, THIRD, FOURTH>
        implements
        ISquare
{
    private FIRST  first;
    private SECOND second;
    private THIRD  third;
    private FOURTH fourth;

    public Square(FIRST first, SECOND second, THIRD third, FOURTH fourth)
    {
        this.first = first;
        this.second = second;
        this.third = third;
        this.fourth = fourth;
    }

    @Override
    public FIRST getFirst()
    {
        return first;
    }

    @Override
    public SECOND getSecond()
    {
        return second;
    }

    @Override
    public THIRD getThird()
    {
        return third;
    }

    @Override
    public FOURTH getFourth()
    {
        return fourth;
    }

    public void setFirst(FIRST first)
    {
        this.first = first;
    }

    public void setSecond(SECOND second)
    {
        this.second = second;
    }

    public void setThird(THIRD third)
    {
        this.third = third;
    }

    public void setFourth(FOURTH fourth)
    {
        this.fourth = fourth;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Square))
        { return false; }
        if (this != obj)
        {
            @SuppressWarnings("unchecked")
            Square<FIRST, SECOND, THIRD, FOURTH> other = (Square<FIRST, SECOND, THIRD, FOURTH>) obj;
            return first.equals(other.first) && second.equals(other.second)
                   && third.equals(other.third)
                   && fourth.equals(other.fourth);
        }
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("<%s,%s,%s,%s>", first, second, third, fourth);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(first, second, third, fourth);
    }

    @Override
    public Square<FIRST, SECOND, THIRD, FOURTH> clone()
    {
        return new Square<>(first, second, third, fourth);
    }
}
