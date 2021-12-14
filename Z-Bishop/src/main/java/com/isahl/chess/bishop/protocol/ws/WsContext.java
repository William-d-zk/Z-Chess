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
package com.isahl.chess.bishop.protocol.ws;

import com.isahl.chess.bishop.protocol.ProtocolContext;
import com.isahl.chess.bishop.protocol.ws.ctrl.X101_HandShake;
import com.isahl.chess.bishop.protocol.ws.features.IWsContext;
import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;

import java.util.Base64;
import java.util.Random;

/**
 * @author William.d.zk
 */
public class WsContext
        extends ProtocolContext<WsFrame>
        implements IWsContext
{

    private final String _SecKey, _SecAcceptExpect;
    private final int    _MaxPayloadSize;
    private final byte[] _Mask;

    private int            mHandshakeState;
    private X101_HandShake mWsHandshake;

    public WsContext(INetworkOption option, ISort.Mode mode, ISort.Type type)
    {
        super(option, mode, type);
        _MaxPayloadSize = option.getSnfByte() - 2;
        if(_Type == ISort.Type.CLIENT) {
            Random r = new Random(System.nanoTime());
            byte[] seed = new byte[17];
            r.nextBytes(seed);
            _SecKey = Base64.getEncoder()
                            .encodeToString(CryptoUtil.SHA1(seed));
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
    public final int getMaxPayloadSize()
    {
        return _MaxPayloadSize;
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
    public void updateOut()
    {
        advanceOutState(ENCODE_PAYLOAD);
    }

    @Override
    public void updateIn()
    {
        advanceInState(DECODE_PAYLOAD);
    }

    public X101_HandShake responseHandShake(String host)
    {
        return new X101_HandShake(host, getSeKey(), getWsVersion());
    }

    public X101_HandShake getHandshake()
    {
        return mWsHandshake;
    }

    public void setHandshake(X101_HandShake handShake)
    {
        mWsHandshake = handShake;
    }
}
