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

package com.tgx.chess.bishop.io.mqtt.bean;

import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IRouteLv4;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2019-05-13
 */
public abstract class QttControl
        implements
        ICommand<QttContext>,
        IRouteLv4
{
    private final int            _Command;
    private byte                 mCtrlCode;
    private byte[]               mPayload;
    private ISession<QttContext> mSession;

    public QttControl(int command)
    {
        _Command = command;
    }

    @Override
    public byte getCtrl()
    {
        return mCtrlCode;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        mCtrlCode = ctrl;
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public ICommand<QttContext> setSession(ISession<QttContext> session)
    {
        mSession = session;
        return this;
    }

    @Override
    public int getSerial()
    {
        return _Command;
    }

    @Override
    public ISession<QttContext> getSession()
    {
        return mSession;
    }

    public byte[] getPayload()
    {
        return mPayload;
    }

    @Override
    public void setPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public void reset()
    {
        mCtrlCode = 0;
        mSession = null;
        mPayload = null;
    }
}
