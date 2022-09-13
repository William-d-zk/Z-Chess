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
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

/**
 * @author William.d.zk
 */
public abstract class WsControl<C extends WsContext>
        implements IControl<C>
{

    protected byte     mFrameHeader;
    protected byte[]   mPayload;
    private   ISession mSession;
    private   C        mContext;

    public WsControl(byte opCode)
    {
        header(opCode);
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public void header(int header)
    {
        mFrameHeader = (byte) header;
    }

    @Override
    public byte header()
    {
        return mFrameHeader;
    }

    @Override
    public WsControl<C> withSub(IoSerial sub)
    {
        if(sub != null) {
            ByteBuf buf = sub.encode();
            if(buf.capacity() > 0) {
                mPayload = buf.array();
            }
        }
        return this;
    }

    @Override
    public WsControl<C> withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        if(mPayload != null) {
            output.put(mPayload);
        }
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        return input != null ? input.readableBytes() : 0;
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
        }
    }

    @Override
    public boolean isCtrl()
    {
        return (mFrameHeader & 0x08) != 0;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    @SuppressWarnings("unchecked")
    public WsControl<C> with(ISession session)
    {
        if(session == null) {return this;}
        mSession = session;
        wrap((C) session.getContext(WsContext.class));
        return this;
    }

    @Override
    public int length()
    {
        return mPayload == null ? 0 : mPayload.length;
    }

    @Override
    public int sizeOf()
    {
        return length();
    }

    @Override
    public String toString()
    {
        return String.format("cmd: %#x, %s",
                             serial(),
                             mPayload == null ? "[NULL] payload" : IoUtil.bin2Hex(mPayload, ","));
    }

    @Override
    public WsControl<C> wrap(C context)
    {
        mContext = context;
        return this;
    }

    @Override
    public C context()
    {
        return mContext;
    }

    @Override
    public IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends IoSerial> T deserializeSub(IoFactory<T> factory)
    {
        throw new UnsupportedOperationException();
    }

}
