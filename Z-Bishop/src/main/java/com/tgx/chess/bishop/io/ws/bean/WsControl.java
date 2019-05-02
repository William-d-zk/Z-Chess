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

import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IRouteLv4;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author William.d.zk
 */
public abstract class WsControl<C extends WsContext>
        implements
        ICommand<C>,
        IRouteLv4
{

    private final byte[] _Msg;
    private final int    _Command;
    protected byte       mCtrlCode;
    private ISession<C>  mSession;

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

    public byte[] getPayload()
    {
        return _Msg;
    }

    @Override
    public int getPriority()
    {
        return QOS_00_NETWORK_CONTROL;
    }

    @Override
    public int superSerial()
    {
        return CONTROL_SERIAL;
    }

    @Override
    public int getSerial()
    {
        return _Command;
    }

    public byte getControl()
    {
        return mCtrlCode;
    }

    @Override
    public ISession<C> getSession()
    {
        return mSession;
    }

    @Override
    public WsControl setSession(ISession<C> session)
    {
        mSession = session;
        return this;
    }

    @Override
    public WsControl duplicate()
    {
        return null;
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
