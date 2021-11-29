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
package com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls;

import com.isahl.chess.bishop.protocol.zchat.model.ctrl.ZControl;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x04)
public class X04_EncryptConfirm
        extends ZControl
{
    public int code;
    public int symmetricKeyId;

    /* SHA256 */
    private byte[] mSign;

    public byte[] getSign()
    {
        return mSign;
    }

    public void setSign(byte[] sign)
    {
        mSign = sign;
    }

    @Override
    public int length()
    {
        return super.length() + 36;
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        code = input.getShort();
        symmetricKeyId = input.getShort() & 0xFFFF;
        mSign = new byte[32];
        input.get(mSign);
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.putShort((short) code);
        output.putShort((short) symmetricKeyId);
        output.put(mSign);
    }

    @Override
    public <C extends IPContext> void afterEncode(C ctx)
    {
        Objects.requireNonNull(ctx)
               .updateOut();
    }

    @Override
    public <C extends IPContext> void afterDecode(C ctx)
    {
        Objects.requireNonNull(ctx)
               .updateIn();
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
