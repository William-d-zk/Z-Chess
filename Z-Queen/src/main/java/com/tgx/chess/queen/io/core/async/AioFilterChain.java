/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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
package com.tgx.chess.queen.io.core.async;

import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IFilterChain;

/**
 * @author William.d.zk
 */
public abstract class AioFilterChain<C extends IContext>
        implements
        IFilterChain<C>
{

    protected String        name;
    private IFilterChain<C> nextFilter;
    private IFilterChain<C> preFilter;
    private int             mIdempotent = 0x80000000;

    @Override
    public int getIdempotentBit()
    {
        return mIdempotent;
    }

    @Override
    public void idempotentRightShift(int previous)
    {
        if (previous == 1) throw new IllegalArgumentException();
        mIdempotent = previous == 0 && mIdempotent == 0x80000000 ? 1 : previous != 0 ? previous >>> 1 : mIdempotent;
    }

    @Override
    public IFilterChain<C> getPrevious()
    {
        return preFilter;
    }

    @Override
    public void setPrevious(IFilterChain<C> filter)
    {
        preFilter = filter;
    }

    @Override
    public IFilterChain<C> getNext()
    {
        return nextFilter;
    }

    @Override
    public void setNext(IFilterChain<C> filter)
    {
        nextFilter = filter;
    }

    @Override
    public IFilterChain<C> getChainHead()
    {
        IFilterChain<C> filter = preFilter;
        while (filter != null && filter.getPrevious() != null)
            filter = filter.getPrevious();
        return filter == null ? this : filter;
    }

    @Override
    public IFilterChain<C> getChainTail()
    {
        IFilterChain<C> filter = nextFilter;
        while (filter != null && filter.getNext() != null)
            filter = filter.getNext();
        return filter == null ? this : filter;
    }

    @Override
    public IFilterChain<C> linkAfter(IFilterChain<C> curFilter)
    {
        if (curFilter == null) return this;
        curFilter.setNext(this);
        setPrevious(curFilter);
        idempotentRightShift(curFilter.getIdempotentBit());
        return this;
    }

    @Override
    public IFilterChain<C> linkFront(IFilterChain<C> curFilter)
    {
        if (curFilter == null) return this;
        curFilter.setPrevious(this);
        setNext(curFilter);
        curFilter.idempotentRightShift(getIdempotentBit());
        return curFilter;
    }

    @Override
    public void dispose()
    {
        IFilterChain<C> nnFilter;
        IFilterChain<C> nFilter = nextFilter;
        while (nFilter != null) {
            nnFilter = nextFilter.getNext();
            nFilter.setNext(null);
            nFilter = nnFilter;
        }
    }
}
