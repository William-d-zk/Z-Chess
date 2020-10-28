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
package com.isahl.chess.bishop.io;

import java.util.concurrent.atomic.AtomicInteger;

import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.async.AioContext;
import com.isahl.chess.queen.io.core.inf.IFrame;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionCloser;
import com.isahl.chess.queen.io.core.inf.ISessionError;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 * 
 * @date 2017-02-10
 */
public class ZContext<C extends IPContext<C>>
        extends
        AioContext
        implements
        IPContext<C>
{
    protected final AtomicInteger _EncodeState      = new AtomicInteger(ENCODE_NULL);
    protected final AtomicInteger _DecodeState      = new AtomicInteger(DECODE_NULL);
    private int                   mDecodingPosition = -1, mLackData = 1;
    private IFrame                mCarrier;
    private final ISort<C>        _Sort;

    public ZContext(ISessionOption option,
                    ISort<C> sort)
    {
        super(option);
        _Sort = sort;
    }

    @Override
    public void reset()
    {
        super.reset();
        _EncodeState.set(ctlOf(ENCODE_FRAME, 0));
        _DecodeState.set(ctlOf(DECODE_FRAME, 0));
        mDecodingPosition = -1;
        mLackData = 1;
        if (mCarrier != null) {
            mCarrier.reset();
        }
        mCarrier = null;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        mCarrier = null;
    }

    @Override
    public void finish()
    {
        mDecodingPosition = -1;
        mLackData = 1;
        getRvBuffer().clear();
        mCarrier = null;
    }

    @SuppressWarnings("unchecked")
    public <F extends IFrame> F getCarrier()
    {
        return (F) mCarrier;
    }

    public void setCarrier(IFrame frame)
    {
        mCarrier = frame;
    }

    @Override
    public int lackLength(int length, int target)
    {
        mDecodingPosition += length;
        mLackData = target - mDecodingPosition;
        return mLackData;
    }

    @Override
    public int position()
    {
        return mDecodingPosition;
    }

    @Override
    public int lack()
    {
        return mLackData;
    }

    @Override
    public int outState()
    {
        return stateOf(_EncodeState.get());
    }

    @Override
    public void setOutState(int state)
    {
        advanceState(_EncodeState, state);
    }

    @Override
    public int inState()
    {
        return stateOf(_DecodeState.get());
    }

    @Override
    public void setInState(int state)
    {
        advanceState(_DecodeState, state);
    }

    @Override
    public boolean isInConvert()
    {
        return isInConvert(_DecodeState.get());
    }

    @Override
    public boolean isOutConvert()
    {
        return isOutConvert(_EncodeState.get());
    }

    @Override
    public boolean isInErrorState()
    {
        return isInErrorState(_DecodeState.get());
    }

    @Override
    public boolean isOutErrorState()
    {
        return isOutErrorState(_EncodeState.get());
    }

    @Override
    public ISort<C> getSort()
    {
        return _Sort;
    }

    @Override
    public ISessionError getError()
    {
        return _Sort.getError();
    }

    @Override
    public ISessionCloser getCloser()
    {
        return _Sort.getCloser();
    }

    @Override
    public IOperator<IPacket,
                     ISession,
                     ITriple> getReader()
    {
        return _Sort.getDecoder();
    }

    @Override
    public ISort.Mode getMode()
    {
        return _Sort.getMode();
    }
}
