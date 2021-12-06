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
package com.isahl.chess.bishop.protocol.ws.model;

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.ALMOST_ONCE;

/**
 * @author William.d.zk
 */
public abstract class WsControl
        implements IControl
{

    protected final byte[]    _Payload;
    private final   byte      _CtrlCode;
    private         ISession  mSession;
    private         WsContext mContext;

    public WsControl(byte code, byte[] payload)
    {
        _CtrlCode = code;
        _Payload = payload;
    }

    @Override
    public void dispose()
    {
        reset();
    }

    @Override
    public void reset()
    {
        putSession(null);
    }

    @Override
    public ByteBuffer payload()
    {
        return _Payload == null ? null : ByteBuffer.wrap(_Payload);
    }

    public void put(byte[] payload)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.put(_Payload);
    }

    @Override
    public void decodec(ByteBuffer input)
    {

    }

    @Override
    public void put(byte ctrl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte ctrl()
    {
        return _CtrlCode;
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public void putSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public int length()
    {
        return Objects.nonNull(_Payload) ? _Payload.length : 0;
    }

    @Override
    public String toString()
    {
        return String.format("cmd: %#x, %s",
                             serial(),
                             _Payload == null ? "[NULL] payload" : new String(_Payload, StandardCharsets.UTF_8));
    }

    @Override
    public Level getLevel()
    {
        return ALMOST_ONCE;
    }

    public void putContext(WsContext context)
    {
        mContext = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WsContext context()
    {
        return mContext;
    }
}
