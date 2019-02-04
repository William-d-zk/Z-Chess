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

package com.tgx.chess.bishop.io.zprotocol.device;

import com.tgx.chess.bishop.io.zprotocol.Command;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author William.d.zk
 */
public class X50_DeviceMsg
        extends
        Command<ZContext>
{
    public final static int COMMAND = 0x50;
    private byte[]          payload;
    private int             payloadLength;//MAX:64K

    public X50_DeviceMsg()
    {
        super(COMMAND, true);
    }

    public X50_DeviceMsg(long msgUID)
    {
        super(COMMAND, msgUID);
    }

    @Override
    public int getPriority()
    {
        return QOS_08_IMMEDIATE_MESSAGE;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 2 + payloadLength;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
        if (payload.length > 4096) { throw new IllegalArgumentException("payload length is over 4096"); }
        payloadLength = payload.length;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        payloadLength = IoUtil.readShort(data, pos);
        pos += 2;
        payload = new byte[payloadLength];
        pos = IoUtil.read(data, pos, payload);
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(payloadLength, data, pos);
        pos += IoUtil.write(payload, 0, data, pos, payload.length);
        return pos;
    }

}
