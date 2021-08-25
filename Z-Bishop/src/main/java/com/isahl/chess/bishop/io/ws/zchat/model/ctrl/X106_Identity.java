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
package com.isahl.chess.bishop.io.ws.zchat.model.ctrl;

import com.isahl.chess.bishop.io.ws.model.WsControl;
import com.isahl.chess.bishop.io.ws.model.WsFrame;
import com.isahl.chess.king.base.util.IoUtil;

import static com.isahl.chess.queen.io.core.features.model.session.ISessionManager.INVALID_INDEX;

/**
 * @author William.d.zk
 */
public class X106_Identity
        extends WsControl
{
    public final static int COMMAND = 0x106;

    public X106_Identity(long... id)
    {
        this(IoUtil.writeLongArray(id));
    }

    public X106_Identity(byte[] payload)
    {
        super(WsFrame.frame_op_code_ctrl_cluster, COMMAND, payload);
    }

    public long getIdentity()
    {
        if(payload() == null) { return INVALID_INDEX; }
        if(payload().length < 8) { throw new ArrayIndexOutOfBoundsException(); }
        return IoUtil.readLong(payload(), 0);
    }

    public long getSessionIdx()
    {
        if(payload() == null) { return INVALID_INDEX; }
        if(payload().length < 16) { throw new ArrayIndexOutOfBoundsException(); }
        return IoUtil.readLong(payload(), 8);
    }

    public long[] getIdentities()
    {
        int size = (payload().length >>> 3);
        if(size > 0) {
            long[] result = new long[size];
            IoUtil.readLongArray(payload(), 0, result);
            return result;
        }
        return new long[]{ INVALID_INDEX, INVALID_INDEX };
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public X106_Identity duplicate()
    {
        return new X106_Identity(payload());
    }

    @Override
    public String toString()
    {
        return String.format("X106 %s", IoUtil.longArrayToHex(getIdentities()));
    }
}
