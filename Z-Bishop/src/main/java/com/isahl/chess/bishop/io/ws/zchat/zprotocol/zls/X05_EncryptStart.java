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
package com.isahl.chess.bishop.io.ws.zchat.zprotocol.zls;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZCommand;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.IPContext;

/**
 * @author William.d.zk
 */
public class X05_EncryptStart
        extends ZCommand
{
    public final static int COMMAND = 0x05;
    public              int symmetricKeyId;
    public              int salt;

    public X05_EncryptStart()
    {
        super(COMMAND, false);
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        symmetricKeyId = IoUtil.readUnsignedShort(data, pos);
        pos += 5;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 5;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(symmetricKeyId, data, pos);
        pos += IoUtil.writeByte(salt, data, pos);
        pos += IoUtil.writeByte(salt >> 8, data, pos);
        pos += IoUtil.writeByte(salt >> 16, data, pos);
        return pos;
    }

    @Override
    public <C extends IPContext> void afterDecode(C ctx)
    {
        ctx.updateIn();
    }

    @Override
    public <C extends IPContext> void afterEncode(C ctx)
    {
        ctx.updateOut();
    }

    @Override
    public String toString()
    {
        return String.format("%s,rc4-key:%d", super.toString(), symmetricKeyId);
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }
}
