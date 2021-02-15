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

package com.isahl.chess.bishop.io.zprotocol.control;

import static com.isahl.chess.bishop.io.ws.WsFrame.frame_op_code_no_ctrl_txt;

import com.isahl.chess.bishop.io.ws.WsContext;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IContext;
import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2021/2/14
 */
public class X10A_Text
        implements
        ICommand
{
    public final static int COMMAND = 0x10A;

    private final int  _Command;
    private final byte _CtrlCode;
    private ISession   mSession;
    private WsContext  mContext;
    private byte[]     mPayload;

    public X10A_Text(int command,
                     byte code)
    {
        _Command = command;
        _CtrlCode = code;
    }

    public X10A_Text()
    {
        this(COMMAND, frame_op_code_no_ctrl_txt);
    }

    @Override
    public int dataLength()
    {
        return (getPayload() != null ? getPayload().length: 0);
    }

    @Override
    public int serial()
    {
        return _Command;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        if (getPayload() != null) {
            pos += IoUtil.write(getPayload(), 0, data, pos, getPayload().length);
        }
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        byte[] payload = new byte[data.length - pos];
        pos += IoUtil.write(data, pos, payload, 0, payload.length);
        setPayload(payload);
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
        setSession(null);
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
    public <C extends IContext> C getContext()
    {
        return (C) mContext;
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public byte[] getPayload()
    {
        return mPayload;
    }

    @Override
    public void setSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public ISession getSession()
    {
        return mSession;
    }

    @Override
    public byte getCtrl()
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
