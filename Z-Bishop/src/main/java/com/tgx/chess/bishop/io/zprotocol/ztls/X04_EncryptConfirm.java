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
package com.tgx.chess.bishop.io.zprotocol.ztls;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.ZCommand;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author William.d.zk
 */
public class X04_EncryptConfirm
        extends
        ZCommand
{
    public final static int COMMAND = 0x04;
    public int              code;
    public int              symmetricKeyId;

    /* SHA256 */
    private byte[]          mSign;

    public X04_EncryptConfirm()
    {
        super(COMMAND, false);
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    public byte[] getSign()
    {
        return mSign;
    }

    public void setSign(byte[] sign)
    {
        mSign = sign;
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + 36;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        code = IoUtil.readShort(data, pos);
        pos += 2;
        symmetricKeyId = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        mSign = new byte[32];
        pos = IoUtil.read(data, pos, mSign);
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(code, data, pos);
        pos += IoUtil.writeShort(symmetricKeyId, data, pos);
        pos += IoUtil.write(mSign, data, pos);
        return pos;
    }

    @Override
    public void afterEncode(ZContext ctx)
    {
        ctx.updateKeyOut();
    }

    @Override
    public void afterDecode(ZContext ctx)
    {
        ctx.updateKeyIn();
    }

    @Override
    public String toString()
    {
        return String.format("%s,code:%s,rc4-key: %d ,sign: %s",
                             super.toString(),
                             code,
                             symmetricKeyId,
                             IoUtil.bin2Hex(mSign));
    }

    @Override
    public void dispose()
    {
        mSign = null;
        super.dispose();
    }

    @Override
    public Level getLevel()
    {
        return Level.EXACTLY_ONCE;
    }
}
