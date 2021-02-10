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
package com.isahl.chess.bishop.io.ws;

import static com.isahl.chess.king.base.schedule.inf.ITask.advanceState;
import static com.isahl.chess.queen.io.core.inf.ISession.CAPACITY;

import java.util.Base64;
import java.util.Random;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.bishop.io.ws.control.X101_HandShake;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class WsContext
        extends
        ZContext
        implements
        IWsContext
{

    private final String _SecKey, _SecAcceptExpect;
    private final int    _MaxPayloadSize;
    private final byte[] _Mask;
    private int          mHandshakeState;
    private WsHandshake  mHandshake;

    public WsContext(ISessionOption option,
                     ISort.Mode mode,
                     ISort.Type type)
    {
        super(option, mode, type);
        _MaxPayloadSize = option.getSnfByte() - 2;
        if (_Type == ISort.Type.CONSUMER) {
            Random r = new Random(System.nanoTime());
            byte[] seed = new byte[17];
            r.nextBytes(seed);
            _SecKey = Base64.getEncoder()
                            .encodeToString(CryptUtil.SHA1(seed));
            _SecAcceptExpect = getSecAccept(_SecKey);
            _Mask = new byte[4];
            r.nextBytes(_Mask);
        }
        else {
            _SecKey = _SecAcceptExpect = null;
            _Mask = null;
        }
    }

    @Override
    public WsHandshake getHandshake()
    {
        return mHandshake;
    }

    @Override
    public void setHandshake(WsHandshake handshake)
    {
        mHandshake = handshake;
    }

    @Override
    public final int getMaxPayloadSize()
    {
        return _MaxPayloadSize;
    }

    @Override
    public void reset()
    {
        super.reset();
        if (mHandshake != null) {
            mHandshake.dispose();
        }
        mHandshake = null;
    }

    @Override
    public void dispose()
    {
        mHandshake = null;
        super.dispose();
    }

    @Override
    public void finish()
    {
        super.finish();
        if (mHandshake != null) {
            mHandshake.dispose();
        }
        mHandshake = null;
    }

    @Override
    public String getSeKey()
    {
        return _SecKey;
    }

    @Override
    public byte[] getMask()
    {
        return _Mask;
    }

    @Override
    public final void updateHandshakeState(int state)
    {
        mHandshakeState |= state;
    }

    @Override
    public final boolean checkState(int state)
    {
        return mHandshakeState == state || (mHandshakeState & state) == state;
    }

    @Override
    public String getSecAcceptExpect()
    {
        return _SecAcceptExpect;
    }

    @Override
    public void ready()
    {

        switch (_Mode)
        {
            case CLUSTER ->
                {
                    advanceState(_DecodeState, DECODE_PAYLOAD, CAPACITY);
                    advanceState(_EncodeState, ENCODE_PAYLOAD, CAPACITY);
                }
            case LINK ->
                {
                    advanceState(_DecodeState, DECODE_FRAME, CAPACITY);
                    advanceState(_EncodeState, ENCODE_FRAME, CAPACITY);
                }
        }
    }

    @Override
    public void updateOut()
    {
        advanceOutState(ENCODE_PAYLOAD);
    }

    @Override
    public void updateIn()
    {
        advanceInState(DECODE_PAYLOAD);
    }

    public WsHandshake handshake(String host)
    {
        return new X101_HandShake(host, getSeKey(), getWsVersion());
    }
}
