/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
package com.tgx.chess.queen.io.external.websokcet.bean;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.external.websokcet.ZContext;
import com.tgx.chess.queen.io.external.zprotocol.Command;

/**
 * @author William.d.zk
 */
public class X03_Cipher
        extends
        Command<ZContext>
{
    public final static int COMMAND = 0x03;
    public int              pubKeyId;
    public int              symmetricKeyId;
    public byte[]           cipher;

    public X03_Cipher() {
        super(COMMAND, false);
    }

    @Override
    public boolean isMappingCommand() {
        return true;
    }

    @Override
    public int decodec(byte[] data, int pos) {
        pubKeyId = IoUtil.readInt(data, pos);
        pos += 4;
        symmetricKeyId = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        cipher = new byte[data.length - super.dataLength() - 6];
        pos = IoUtil.read(data, pos, cipher);
        return pos;
    }

    @Override
    public int dataLength() {
        return super.dataLength() + 6 + (cipher == null ? 0 : cipher.length);
    }

    @Override
    public int encodec(byte[] data, int pos) {
        pos += IoUtil.writeInt(pubKeyId, data, pos);
        pos += IoUtil.writeShort(symmetricKeyId, data, pos);
        pos += IoUtil.write(cipher, 0, data, pos, cipher.length);
        return pos;
    }

    @Override
    public void dispose() {
        cipher = null;
        super.dispose();
    }

    @Override
    public int getPriority() {
        return QOS_00_NETWORK_CONTROL;
    }

    @Override
    public String toString() {
        return String.format("%s,public-key: %d | rc4-key: %d", super.toString(), pubKeyId, symmetricKeyId);
    }
}
