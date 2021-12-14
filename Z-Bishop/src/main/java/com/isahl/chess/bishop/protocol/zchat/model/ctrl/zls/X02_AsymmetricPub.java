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
                  serial = 0x02)
public class X02_AsymmetricPub
        extends ZControl
{
    private int mPubKeyId = -1;

    public int getPubKeyId()
    {
        return mPubKeyId;
    }

    public void setPubKeyId(int pubKeyId)
    {
        this.mPubKeyId = pubKeyId;
    }

    @Override
    public int length()
    {
        return super.length() + 4;
    }

    public X02_AsymmetricPub setPubKey(int _id, byte[] key)
    {
        mPubKeyId = _id;
        mPayload = key;
        return this;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putInt(mPubKeyId);
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPubKeyId = input.getInt();
        return remain - 4;
    }

    public byte[] getPubKey()
    {
        return mPayload;
    }

    public void setPubKey(byte[] pubKey)
    {
        mPayload = pubKey;
    }

    @Override
    public String toString()
    {
        return String.format("%s\npublic-key: %d | %s", super.toString(), mPubKeyId, IoUtil.bin2Hex(mPayload));
    }

    @Override
    public Level getLevel()
    {
        return Level.AT_LEAST_ONCE;
    }
}
