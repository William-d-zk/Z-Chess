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

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x03)
public class X03_Cipher
        extends ZControl
{
    public int    pubKeyId;
    public int    symmetricKeyId;
    public byte[] cipher;

    @Override
    public void decodec(ByteBuffer input)
    {
        pubKeyId = input.getInt();
        symmetricKeyId = input.getShort() & 0xFFFF;
        cipher = new byte[input.remaining()];
        input.get(cipher);
    }

    @Override
    public int length()
    {
        return super.length() + 6 + (Objects.isNull(cipher) ? 0 : cipher.length);
    }

    @Override
    public void encodec(ByteBuffer output)
    {
        output.putInt(pubKeyId);
        output.putShort((short) symmetricKeyId);
        output.put(cipher);
    }

    @Override
    public void dispose()
    {
        cipher = null;
        super.dispose();
    }

    @Override
    public String toString()
    {
        return String.format("%s,public-key: %d | rc4-key: %d", super.toString(), pubKeyId, symmetricKeyId);
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }
}
