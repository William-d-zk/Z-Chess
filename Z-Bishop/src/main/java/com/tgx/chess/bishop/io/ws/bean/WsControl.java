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
package com.tgx.chess.bishop.io.ws.bean;

import java.util.Objects;

import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author William.d.zk
 */
public abstract class WsControl
        implements
        IControl<WsContext>
{

    private final byte[]        _Msg;
    private final int           _Command;
    protected byte              mCtrlCode;
    private ISession<WsContext> mSession;

    public WsControl(int command,
                     byte[] msg)
    {
        _Command = command;
        _Msg = msg;
    }

    public WsControl(String msg,
                     int command)
    {
        this(command,
             Objects.nonNull(msg) ? msg.getBytes()
                                  : null);
    }

    public WsControl(int command)
    {
        this(command, null);
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
        return _Msg;
    }

    public void setPayload(byte[] payload)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSerial()
    {
        return _Command;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        mCtrlCode = ctrl;
    }

    @Override
    public byte getCtrl()
    {
        return mCtrlCode;
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public ISession<WsContext> getSession()
    {
        return mSession;
    }

    @Override
    public WsControl setSession(ISession<WsContext> session)
    {
        mSession = session;
        return this;
    }

    @Override
    public int dataLength()
    {
        return Objects.nonNull(_Msg) ? _Msg.length
                                     : 0;
    }

    @Override
    public String toString()
    {
        int command = getSerial();
        return String.format("cmd: 0X%X", command);
    }
}
