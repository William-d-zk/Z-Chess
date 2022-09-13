/*
 * MIT License
 *
 * Copyright (c) 2021. Z-Chess
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

package com.isahl.chess.bishop.protocol;

import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.net.socket.AioContext;

import java.util.concurrent.atomic.AtomicInteger;

import static com.isahl.chess.king.base.cron.features.ITask.*;
import static com.isahl.chess.queen.io.core.features.model.session.ISession.CAPACITY;

/**
 * @author william.d.zk
 */
public abstract class ProtocolContext<F>
        extends AioContext<INetworkOption>
        implements IPContext
{
    protected final AtomicInteger _EncodeState = new AtomicInteger(ENCODE_NULL);
    protected final AtomicInteger _DecodeState = new AtomicInteger(DECODE_NULL);
    protected final ISort.Mode    _Mode;
    protected final ISort.Type    _Type;
    /*----------------------------------------------------------------------------------------------------------------*/
    private         F             mCarrier;

    public ProtocolContext(INetworkOption option, ISort.Mode mode, ISort.Type type)
    {
        super(option);
        _Mode = mode;
        _Type = type;
        recedeState(_EncodeState, ENCODE_NULL, CAPACITY);
        advanceState(_DecodeState, DECODE_NULL, CAPACITY);
    }

    public F getCarrier()
    {
        return mCarrier;
    }

    public void setCarrier(F carrier)
    {
        mCarrier = carrier;
    }

    @Override
    public void reset()
    {
        mCarrier = null;
    }

    @Override
    public int outState()
    {
        return stateOf(_EncodeState.get(), CAPACITY);
    }

    @Override
    public void advanceOutState(int state)
    {
        recedeState(_EncodeState, state, CAPACITY);
    }

    @Override
    public void recedeOutState(int state)
    {
        advanceState(_EncodeState, state, CAPACITY);
    }

    @Override
    public int inState()
    {
        return stateOf(_DecodeState.get(), CAPACITY);
    }

    @Override
    public void advanceInState(int state)
    {
        advanceState(_DecodeState, state, CAPACITY);
    }

    @Override
    public void recedeInState(int state)
    {
        recedeState(_DecodeState, state, CAPACITY);
    }

    @Override
    public boolean isInInit()
    {
        return _DecodeState.get() == DECODE_NULL;
    }

    @Override
    public boolean isOutInit()
    {
        return _EncodeState.get() == ENCODE_NULL;
    }

    @Override
    public boolean isInConvert()
    {
        return _DecodeState.get() == DECODE_PAYLOAD;
    }

    @Override
    public boolean isOutConvert()
    {
        return _EncodeState.get() == ENCODE_PAYLOAD;
    }

    @Override
    public boolean isInErrorState()
    {
        return _DecodeState.get() == DECODE_ERROR;
    }

    @Override
    public boolean isOutErrorState()
    {
        return _EncodeState.get() == DECODE_ERROR;
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

    @Override
    public void promotionOut()
    {
        advanceOutState(_EncodeState.get() == ENCODE_NULL ? ENCODE_PAYLOAD
                                                          : _EncodeState.get() == ENCODE_PAYLOAD ? ENCODE_FRAME
                                                                                                 : ENCODE_ERROR);
    }

    @Override
    public void promotionIn()
    {
        advanceInState(_DecodeState.get() == DECODE_NULL ? DECODE_FRAME
                                                         : _DecodeState.get() == DECODE_FRAME ? DECODE_PAYLOAD
                                                                                              : DECODE_ERROR);
    }

    @Override
    public void demotionOut()
    {
        recedeOutState(_EncodeState.get() == ENCODE_FRAME ? ENCODE_PAYLOAD : ENCODE_NULL);
    }

    @Override
    public void demotionIn()
    {
        recedeInState(_DecodeState.get() == DECODE_PAYLOAD ? DECODE_FRAME : DECODE_NULL);
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
