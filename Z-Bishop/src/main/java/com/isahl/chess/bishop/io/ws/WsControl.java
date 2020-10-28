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

import static com.isahl.chess.queen.io.core.inf.IQoS.Level.ALMOST_ONCE;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author William.d.zk
 */
public abstract class WsControl
        implements
        IControl
{

    private final byte[] _Payload;
    private final int    _Command;
    private final byte   _CtrlCode;
    private ISession     mSession;

    public WsControl(byte code,
                     int command,
                     byte[] payload)
    {
        _CtrlCode = code;
        _Command = command;
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
        setSession(null);
    }

    public byte[] getPayload()
    {
        return _Payload;
    }

    public void setPayload(byte[] payload)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.write(_Payload, data, pos);
        return pos;
    }

    @Override
    public int serial()
    {
        return _Command;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte getCtrl()
    {
        return _CtrlCode;
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public ISession getSession()
    {
        return mSession;
    }

    @Override
    public WsControl setSession(ISession session)
    {
        mSession = session;
        return this;
    }

    @Override
    public int dataLength()
    {
        return Objects.nonNull(_Payload) ? _Payload.length
                                         : 0;
    }

    @Override
    public String toString()
    {
        int command = serial();
        return String.format("cmd: %#x, %s",
                             command,
                             _Payload == null ? "[NULL] payload"
                                              : new String(_Payload, StandardCharsets.UTF_8));
    }

    @Override
    public Level getLevel()
    {
        return ALMOST_ONCE;
    }
}
