/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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

package com.tgx.chess.queen.io.external.websokcet.bean.device;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.external.websokcet.WsContext;
import com.tgx.chess.queen.io.external.zprotocol.Command;

/**
 * @author William.d.zk
 */
public class X50_DeviceMsg
        extends
        Command<WsContext>
{
    public final static int COMMAND = 0x50;
    private byte[]          mPayload;
    private int             mPayloadLength;

    public X50_DeviceMsg()
    {
        super(COMMAND, true);
    }

    public X50_DeviceMsg(long msgUid)
    {
        super(COMMAND, msgUid);
    }

    @Override
    public int getPriority()
    {
        return QOS_08_IMMEDIATE_MESSAGE;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 2 + mPayloadLength;
    }

    public void setPayload(byte[] payload)
    {
        mPayload = payload;
        if (payload.length > 4096) { throw new IllegalArgumentException("payload length is over 4096"); }
        mPayloadLength = payload.length;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPayloadLength  = IoUtil.readShort(data, pos);
        pos            += 2;
        mPayload        = new byte[mPayloadLength];
        pos             = IoUtil.read(data, pos, mPayload);
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(mPayloadLength, data, pos);
        pos += IoUtil.write(mPayload, 0, data, pos, mPayload.length);
        return pos;
    }

}
