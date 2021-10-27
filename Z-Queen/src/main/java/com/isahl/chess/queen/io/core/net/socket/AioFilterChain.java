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
package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilter;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeFilter;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;

/**
 * @author William.d.zk
 */
public abstract class AioFilterChain<C extends IPContext, O extends IProtocol, I extends IProtocol>
        implements IFilterChain,
                   IPipeFilter,
                   IFilter<C, O, I>
{

    protected final Logger _Logger;

    private final String       _Name;
    private       IFilterChain next;
    private       IFilterChain previous;

    private int mRightIdempotent = 0x80000000;
    private int mLeftIdempotent  = 1;

    protected AioFilterChain(String name)
    {
        _Name = name;
        _Logger = Logger.getLogger("io.queen.chain." + _Name);
    }

    @Override
    public int getRightIdempotentBit()
    {
        return mRightIdempotent;
    }

    @Override
    public int getLeftIdempotentBit()
    {
        return mLeftIdempotent;
    }

    @Override
    public void idempotentRightShift(int previous)
    {
        if(previous != 0) {
            int rightShift = previous >>> 1;
            if(rightShift != 0) {
                mRightIdempotent = rightShift;
            }
            else {
                throw new IllegalArgumentException("no space for right shift");
            }
        }
        IoUtil.requireNonNull(getNext(), f->f.idempotentRightShift(mRightIdempotent));
    }

    @Override
    public void idempotentLeftShift(int next)
    {
        if(next != 0) {
            int leftShift = next << 1;
            if(leftShift != 0) {
                mLeftIdempotent = leftShift;
            }
            else {
                throw new IllegalArgumentException("no space for left shift");
            }
        }
        IoUtil.requireNonNull(getPrevious(), f->f.idempotentLeftShift(mLeftIdempotent));
    }

    @Override
    public IFilterChain getPrevious()
    {
        return previous;
    }

    @Override
    public void setPrevious(IFilterChain previous)
    {
        this.previous = previous;
    }

    @Override
    public IFilterChain getNext()
    {
        return next;
    }

    @Override
    public void setNext(IFilterChain next)
    {
        this.next = next;
    }

    @Override
    public IFilterChain getChainHead()
    {
        IFilterChain node = previous;
        while(node != null && node.getPrevious() != null) {
            node = node.getPrevious();
        }
        return node == null ? this : node;
    }

    @Override
    public IFilterChain getChainTail()
    {
        IFilterChain node = next;
        while(node != null && node.getNext() != null) {
            node = node.getNext();
        }
        return node == null ? this : node;
    }

    @Override
    public IFilterChain linkAfter(IFilterChain current)
    {
        if(current == null) {return this;}
        current.setNext(this);
        setPrevious(current);
        idempotentRightShift(current.getRightIdempotentBit());
        current.idempotentLeftShift(getLeftIdempotentBit());
        return this;
    }

    @Override
    public IFilterChain linkFront(IFilterChain current)
    {
        if(current == null) {return this;}
        current.setPrevious(this);
        setNext(current);
        current.idempotentRightShift(getRightIdempotentBit());
        idempotentLeftShift(current.getLeftIdempotentBit());
        return current;
    }

    @Override
    public void dispose()
    {
        IFilterChain nextNext;
        IFilterChain next = this.next;
        while(next != null) {
            nextNext = next.getNext();
            next.setNext(null);
            next = nextNext;
        }
    }

    protected boolean checkType(IProtocol protocol, int type_serial)
    {
        return protocol.superSerial() == type_serial;
    }

    @Override
    public IPipeFilter getPipeFilter()
    {
        return this;
    }

    @Override
    public String getName()
    {
        return _Name;
    }
}
