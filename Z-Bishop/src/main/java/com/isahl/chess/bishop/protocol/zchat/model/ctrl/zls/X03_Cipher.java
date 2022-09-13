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
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author William.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_CONTROL_SERIAL,
                  serial = 0x03)
public class X03_Cipher
        extends ZControl
{
    private int mPubKeyId;
    private int mSymmetricKeyId;

    @Override
    public int length()
    {
        return super.length() + 6;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putInt(mPubKeyId)
                    .putShort(mSymmetricKeyId);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPubKeyId = input.getInt();
        mSymmetricKeyId = input.getShort();
        return remain - 6;
    }

    public int getPubKeyId()
    {
        return mPubKeyId;
    }

    public void setPubKeyId(int pubKeyId)
    {
        this.mPubKeyId = pubKeyId;
    }

    public int getSymmetricKeyId()
    {
        return mSymmetricKeyId;
    }

    public void setSymmetricKeyId(int symmetricKeyId)
    {
        this.mSymmetricKeyId = symmetricKeyId;
    }

    @Override
    public String toString()
    {
        return String.format("%s\npublic-key: %d | rc4-key: %d [%s]", super.toString(), mPubKeyId, mSymmetricKeyId, IoUtil.bin2Hex(mPayload));
    }

    public void setCipher(byte[] cipher)
    {
        mPayload = cipher;
    }

    public byte[] getCipher()
    {
        return mPayload;
    }

    @Override
    public Level level()
    {
        return Level.AT_LEAST_ONCE;
    }
}
