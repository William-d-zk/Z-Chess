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
package com.tgx.chess.queen.io.external.websokcet.bean.ztls;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.external.websokcet.ZContext;
import com.tgx.chess.queen.io.external.zprotocol.Command;

/**
 * @author William.d.zk
 */
public class X02_AsymmetricPub
        extends
        Command<ZContext>
{

    public final static int COMMAND = 0x02;
    public byte[]           pubKey;
    public int              pubKeyId;
    private int             mKeyLength;

    public X02_AsymmetricPub()
    {
        super(COMMAND, false);
    }

    @Override
    public boolean isMappingCommand()
    {
        return true;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        pubKeyId = IoUtil.readInt(data, pos);
        pos += 4;
        mKeyLength = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        if (mKeyLength > 0) {
            pubKey = new byte[mKeyLength];
            pos = IoUtil.read(data, pos, pubKey);
        }
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeInt(pubKeyId, data, pos);
        pos += IoUtil.writeShort(mKeyLength, data, pos);
        pos += IoUtil.write(pubKey, 0, data, pos, mKeyLength);
        return pos;
    }

    @Override
    public void dispose()
    {
        pubKey = null;
        super.dispose();
    }

    @Override
    public int dataLength()
    {
        return super.dataLength() + mKeyLength + 6;
    }

    @Override
    public int getPriority()
    {
        return QOS_00_NETWORK_CONTROL;
    }

    public X02_AsymmetricPub setPubKey(int _id, byte[] key)
    {
        pubKey = key;
        pubKeyId = _id;
        mKeyLength = key == null ? 0
                                 : key.length;
        return this;
    }

    @Override
    public String toString()
    {
        return String.format("%s,public-key: %d | %s", super.toString(), pubKeyId, IoUtil.bin2Hex(pubKey));
    }
}
