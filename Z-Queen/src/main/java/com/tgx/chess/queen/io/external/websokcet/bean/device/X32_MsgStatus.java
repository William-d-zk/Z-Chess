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

package com.tgx.chess.queen.io.external.websokcet.bean.device;

import static com.tgx.chess.queen.io.external.websokcet.bean.device.X31_ConfirmMsg.STATUS_ACTION;
import static com.tgx.chess.queen.io.external.websokcet.bean.device.X31_ConfirmMsg.STATUS_CONFIRM;
import static com.tgx.chess.queen.io.external.websokcet.bean.device.X31_ConfirmMsg.STATUS_PENDING;
import static com.tgx.chess.queen.io.external.websokcet.bean.device.X31_ConfirmMsg.STATUS_RECEIVED;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.external.websokcet.WsContext;
import com.tgx.chess.queen.io.external.zprotocol.Command;

public class X32_MsgStatus
        extends
        Command<WsContext>
{
    public final static int COMMAND = 0x32;

    public X32_MsgStatus()
    {
        super(COMMAND, true);
    }

    public X32_MsgStatus(long msgUID)
    {
        super(COMMAND, msgUID);
    }

    @Override
    public int getPriority()
    {
        return QOS_08_IMMEDIATE_MESSAGE;
    }

    private byte   status;
    private byte[] token = new byte[32];

    @Override
    public int dataLength()
    {
        return super.dataLength() + 33;
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

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = IoUtil.read(data, pos, token);
        status = data[pos++];
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.write(token, data, pos);
        pos += IoUtil.writeByte(status, data, pos);
        return pos;
    }
}
