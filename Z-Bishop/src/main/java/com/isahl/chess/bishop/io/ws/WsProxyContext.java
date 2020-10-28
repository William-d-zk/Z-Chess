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

import java.util.Base64;
import java.util.Random;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IProxyContext;
import com.isahl.chess.queen.io.core.inf.ISessionOption;

/**
 * @author william.d.zk
 */
public class WsProxyContext<A extends IPContext<A>>
        extends
        ZContext<WsProxyContext<A>>
        implements
        IProxyContext<A>,
        IWsContext
{

    private final String _SecKey, _SecAcceptExpect;
    private final byte[] _Mask = new byte[4];
    private final int    _MaxPayloadSize;
    private final A      _ActingContext;
    private int          mHandshakeState;
    private WsHandshake  mHandshake;

    public WsProxyContext(ISessionOption option,
                          ISort<WsProxyContext<A>> sort,
                          A actingContext)
    {
        super(option, sort);
        _ActingContext = actingContext;
        _MaxPayloadSize = option.getSnfInByte() - 2;
        if (sort.getType()
                .equals(ISort.Type.CONSUMER))
        {
            Random r = new Random(System.nanoTime());
            byte[] seed = new byte[17];
            r.nextBytes(seed);
            _SecKey = Base64.getEncoder()
                            .encodeToString(CryptUtil.SHA1(seed));
            _SecAcceptExpect = getSecAccept(_SecKey);
            int pos = Math.abs(r.nextInt() % 13);
            IoUtil.read(seed, pos, _Mask);
        }
        else {
            _SecKey = _SecAcceptExpect = null;
        }

        switch (sort.getMode())
        {
            case CLUSTER:
                advanceState(_DecodeState, DECODE_FRAME);
                advanceState(_EncodeState, DECODE_FRAME);
                break;
            case LINK:
            default:
                break;
        }
    }

    @Override
    public A getActingContext()
    {
        return _ActingContext;
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
        if (mHandshake != null) {
            mHandshake.dispose();
        }
        mHandshake = null;
        super.reset();
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
    public boolean isProxy()
    {
        return true;
    }
}
