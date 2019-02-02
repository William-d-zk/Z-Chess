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

import java.util.Objects;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.zprotocol.Command;

public class X22_SignIn
        extends
        Command<WsContext>
{
    public final static int COMMAND = 0x22;

    public X22_SignIn()
    {
        super(COMMAND, false);
    }

    @Override
    public int getPriority()
    {
        return QOS_06_META_CREATE;
    }

    @Override
    public boolean isMappingCommand()
    {
        return true;
    }

    private byte[] token = new byte[32];
    private String password;

    @Override
    public int dataLength()
    {
        return super.dataLength()
               + 33
               + (Objects.nonNull(password) ? password.getBytes().length
                                            : 0);
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        pos = IoUtil.read(data, pos, token);
        int passwordBytesLength = data[pos++] & 0xFF;
        password = IoUtil.readString(data, pos, passwordBytesLength);
        pos += passwordBytesLength;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.write(token, data, pos);
        byte[] passwordBytes = password.getBytes();
        pos += IoUtil.writeByte(passwordBytes.length, data, pos);
        pos += IoUtil.write(passwordBytes, data, pos);
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

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }
}
