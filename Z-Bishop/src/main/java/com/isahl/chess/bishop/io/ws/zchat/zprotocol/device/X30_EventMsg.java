/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.bishop.io.ws.zchat.zprotocol.device;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
public class X30_EventMsg
        extends ZCommand
{

    public final static int COMMAND = 0x30;

    public X30_EventMsg()
    {
        super(COMMAND, true);
    }

    public X30_EventMsg(long msgUID)
    {
        super(COMMAND, msgUID);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_08_IMMEDIATE_MESSAGE;
    }

    private byte[] token = new byte[32];
    /**
     * 取值范围
     * -128 ~ 127;
     * <p>
     * -127: script
     * 0000: text
     */
    private byte   ctrl;
    private int    payloadLength;
    private byte[] payload;

    @Override
    public int dataLength()
    {
        return super.dataLength() + 32 + 1 + 2 + payloadLength;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = IoUtil.read(data, pos, token);
        ctrl = data[pos++];
        payloadLength = IoUtil.readShort(data, pos);
        pos += 2;
        if(payloadLength > 0) {
            payload = new byte[payloadLength];
            pos = IoUtil.read(data, pos, payload);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.write(token, data, pos);
        pos += IoUtil.writeByte(ctrl, data, pos);
        pos += IoUtil.writeShort(payload.length, data, pos);
        pos += IoUtil.write(payload, data, pos);
        return pos;
    }

    public void putPayload(byte[] payload)
    {
        this.payload = payload;
        payloadLength = payload.length;
    }

    public byte[] payload()
    {
        return payload;
    }

    public void setToken(byte[] token)
    {
        IoUtil.read(token, 0, this.token);
    }

    public void setToken(String hexToken)
    {
        IoUtil.hex2bin(hexToken, token, 0);
    }

    public byte[] getToken()
    {
        return token;
    }

    public boolean isScript()
    {
        return ctrl == CTRL_SCRIPT;
    }

    public boolean isText()
    {
        return ctrl == CTRL_TEXT;
    }

    public void putCtrl(byte ctrl)
    {
        this.ctrl = ctrl;
    }

    public final static byte CTRL_SCRIPT = -127;
    public final static byte CTRL_TEXT   = 0;
}
