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

package com.isahl.chess.bishop.io.ws.zchat.model.command;

import com.isahl.chess.bishop.io.ws.zchat.model.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author william.d.zk
 */
public class X20_SignUp
        extends ZCommand
{
    public final static int COMMAND = 0x20;

    public X20_SignUp()
    {
        super(COMMAND, false);
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_06_META_CREATE;
    }

    private String sn;
    private String password;
    private long   passwordId;

    @Override
    public int dataLength()
    {
        return super.dataLength() + 8 + sn.getBytes(StandardCharsets.UTF_8).length +
               (Objects.nonNull(password) ? password.getBytes(StandardCharsets.UTF_8).length : 0);
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        int snBytesLength = data[pos++] & 0xFF;
        sn = IoUtil.readString(data, pos, snBytesLength, StandardCharsets.UTF_8);
        pos += snBytesLength;
        int passwordBytesLength = data[pos++] & 0xFF;
        password = IoUtil.readString(data, pos, passwordBytesLength);
        pos += passwordBytesLength;
        passwordId = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        byte[] snBytes = sn.getBytes(StandardCharsets.UTF_8);
        pos += IoUtil.writeByte(snBytes.length, data, pos);
        pos += IoUtil.write(snBytes, data, pos);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        pos += IoUtil.writeByte(passwordBytes.length, data, pos);
        pos += IoUtil.write(passwordBytes, data, pos);
        pos += IoUtil.writeLong(passwordId, data, pos);
        return pos;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public long getPasswordId()
    {
        return passwordId;
    }

    public void setPasswordId(long passwordId)
    {
        this.passwordId = passwordId;
    }

    public String getSn()
    {
        return sn;
    }

    public void setSn(String sn)
    {
        this.sn = sn;
    }
}
