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

package com.isahl.chess.bishop.io.zprotocol.device;

import com.isahl.chess.bishop.io.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
public class X21_SignUpResult
        extends
        ZCommand
{
    public final static int COMMAND = 0x21;

    public X21_SignUpResult()
    {
        super(COMMAND, false);
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_09_CONFIRM_MESSAGE;
    }

    private boolean success;
    private byte[]  token;
    private long    passwordId;

    public void setSuccess()
    {
        success = true;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setFailed()
    {
        success = false;
    }

    public long getPasswordId()
    {
        return passwordId;
    }

    public void setPasswordId(long pwdId)
    {
        passwordId = pwdId;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength()
               + 9
               + (isSuccess() ? 32
                              : 0);
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        success = data[pos++] > 0;
        passwordId = IoUtil.readLong(data, pos);
        pos += 8;
        if (success) {
            token = new byte[32];
            pos = IoUtil.read(data, pos, token);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeByte(isSuccess() ? 1
                                            : 0,
                                data,
                                pos);
        pos += IoUtil.writeLong(passwordId, data, pos);
        if (isSuccess()) {
            pos += IoUtil.write(token, data, pos);
        }
        return pos;
    }

    public byte[] getToken()
    {
        return token;
    }

    public void setToken(byte[] token)
    {
        this.token = token;
    }
}
