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

import static com.isahl.chess.king.base.schedule.inf.ITask.advanceState;
import static com.isahl.chess.king.base.schedule.inf.ITask.stateAtLeast;
import static com.isahl.chess.king.base.schedule.inf.ITask.stateLessThan;
import static com.isahl.chess.queen.io.core.inf.ISession.stateOf;

import java.util.concurrent.atomic.AtomicInteger;

import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.async.AioContext;
import com.isahl.chess.queen.io.core.inf.IFrame;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 * 
 * @date 2017-02-10
 */
public abstract class ZContext
        extends
        AioContext
        implements
        IPContext
{
    protected final AtomicInteger _EncodeState = new AtomicInteger(ENCODE_NULL);
    protected final AtomicInteger _DecodeState = new AtomicInteger(DECODE_NULL);
    protected final ISort.Mode    _Mode;
    protected final ISort.Type    _Type;
    /*----------------------------------------------------------------------------------------------------------------*/
    private int    mDecodingPosition = -1, mLackData = 1;
    private IFrame mCarrier;

    public ZContext(ISessionOption option,
                    ISort.Mode mode,
                    ISort.Type type)
    {
        super(option);
        _Mode = mode;
        _Type = type;
        advanceState(_EncodeState, ENCODE_NULL);
        advanceState(_DecodeState, DECODE_NULL);
    }

    @Override
    public void reset()
    {
        advanceState(_EncodeState, ENCODE_NULL);
        advanceState(_DecodeState, DECODE_NULL);
        super.reset();
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
        return stateAtLeast(_DecodeState.get(), DECODE_PAYLOAD) && stateLessThan(_DecodeState.get(), DECODE_ERROR);
    }

    @Override
    public boolean isOutConvert()
    {
        return stateAtLeast(_EncodeState.get(), ENCODE_PAYLOAD) && stateLessThan(_EncodeState.get(), ENCODE_ERROR);
    }

    @Override
    public boolean isInErrorState()
    {
        return stateAtLeast(_DecodeState.get(), DECODE_ERROR);
    }

    @Override
    public boolean isOutErrorState()
    {
        return stateAtLeast(_EncodeState.get(), DECODE_ERROR);
    }

    @Override
    public boolean isInFrame()
    {
        return _DecodeState.get() == DECODE_FRAME;
    }

    @Override
    public boolean isOutFrame()
    {
        return _EncodeState.get() == ENCODE_FRAME;
    }

    public ISort.Type getType()
    {
        return _Type;
    }

    public ISort.Mode getMode()
    {
        return _Mode;
    }
}
