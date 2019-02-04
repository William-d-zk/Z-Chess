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

public class X31_ConfirmMsg
        extends
        Command<ZContext>
{
    public final static int COMMAND = 0x31;

    public X31_ConfirmMsg()
    {
        super(COMMAND, true);
    }

    public X31_ConfirmMsg(long msgUID)
    {
        super(COMMAND, msgUID);
    }

    @Override
    public int getPriority()
    {
        return QOS_09_CONFIRM_MESSAGE;
    }

    private byte   status;
    private int    retryCount;
    private byte[] token = new byte[32];

    @Override
    public int dataLength()
    {
        return super.dataLength() + 35;
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

    public int getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(int count)
    {
        retryCount = count;
    }

    public void setStatus(byte status)
    {
        this.status = status;
    }

    public boolean isReceived()
    {
        return status == STATUS_RECEIVED;
    }

    public boolean isPending()
    {
        return status == STATUS_PENDING;
    }

    public boolean isAction()
    {
        return status == STATUS_ACTION;
    }

    public boolean isConfirm()
    {
        return status == STATUS_CONFIRM;
    }

    public final static byte STATUS_RECEIVED = -127;
    public final static byte STATUS_PENDING  = -126;
    public final static byte STATUS_ACTION   = -125;
    public final static byte STATUS_CONFIRM  = 0;

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = IoUtil.read(data, pos, token);
        status = data[pos++];
        retryCount = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.write(token, data, pos);
        pos += IoUtil.writeByte(status, data, pos);
        pos += IoUtil.writeShort(retryCount, data, pos);
        return pos;
    }
}
