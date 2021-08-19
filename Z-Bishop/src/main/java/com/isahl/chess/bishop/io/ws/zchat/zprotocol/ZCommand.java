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

package com.isahl.chess.bishop.io.ws.zchat.zprotocol;

import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2019-07-14
 */
public class ZCommand
        extends ZProtocol
{

    protected ZCommand(int command, boolean hasMsgId)
    {
        super(command, hasMsgId, hasMsgId ? 0 : -1);
    }

    public ZCommand(int command, long msgId)
    {
        super(command, msgId);
    }

    private ISession mSession;
    private byte[]   mPayload;

    @Override
    public void putSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public void reset()
    {
        mSession = null;
    }

    @Override
    public void putCtrl(byte ctrl)
    {
        setCtrlCode(ctrl);
    }

    @Override
    public void putPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public byte ctrl()
    {
        return getCtrlCode();
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public int dataLength()
    {
        return minLength() + (mPayload == null ? 0 : mPayload.length);
    }

    @Override
    public int serial()
    {
        return getCommand();
    }

}
