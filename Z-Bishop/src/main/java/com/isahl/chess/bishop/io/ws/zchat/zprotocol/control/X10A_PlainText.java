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

package com.isahl.chess.bishop.io.ws.zchat.zprotocol.control;

import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IContext;
import com.isahl.chess.queen.io.core.inf.ISession;

import static com.isahl.chess.bishop.io.ws.WsFrame.frame_op_code_no_ctrl_txt;

/**
 * @author william.d.zk
 * @date 2021/2/14
 */
public class X10A_PlainText
        implements ICommand
{
    public final static int COMMAND = 0x10A;

    private final int       _Command;
    private final byte      _CtrlCode;
    private       ISession  mSession;
    private       WsContext mContext;
    private       byte[]    mPayload;

    public X10A_PlainText(int command, byte code)
    {
        _Command = command;
        _CtrlCode = code;
    }

    public X10A_PlainText()
    {
        this(COMMAND, frame_op_code_no_ctrl_txt);
    }

    @Override
    public int dataLength()
    {
        return (payload() != null ? payload().length : 0);
    }

    @Override
    public int serial()
    {
        return _Command;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        if(payload() != null) {
            pos += IoUtil.write(payload(), 0, data, pos, payload().length);
        }
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        byte[] payload = new byte[data.length - pos];
        pos += IoUtil.write(data, pos, payload, 0, payload.length);
        putPayload(payload);
        return pos;
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
    public long getMsgId()
    {
        return 0;
    }

    @Override
    public void setMsgId(long msgId)
    {

    }

    public void setContext(WsContext context)
    {
        mContext = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C extends IContext> C context()
    {
        return (C) mContext;
    }

    @Override
    public void putCtrl(byte ctrl)
    {
        throw new UnsupportedOperationException();
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
    public byte ctrl()
    {
        return _CtrlCode;
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public Level getLevel()
    {
        return Level.ALMOST_ONCE;
    }
}
