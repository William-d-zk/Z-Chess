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
package com.tgx.z.queen.io.external.websokcet.bean;

import com.tgx.z.queen.base.util.IoUtil;
import com.tgx.z.queen.io.external.websokcet.ZContext;
import com.tgx.z.queen.io.external.zprotocol.Command;

/**
 * @author William.d.zk
 */
public class X05_EncryptStart
        extends
        Command<ZContext>
{
    public final static int COMMAND = 0x05;
    public int              symmetricKeyId;
    public int              salt;

    public X05_EncryptStart() {
        super(COMMAND, false);
    }

    @Override
    public int decodec(byte[] data, int pos) {
        symmetricKeyId = IoUtil.readUnsignedShort(data, pos);
        pos += 5;
        return pos;
    }

    @Override
    public int dataLength() {
        return super.dataLength() + 5;
    }

    @Override
    public int encodec(byte[] data, int pos) {
        pos += IoUtil.writeShort(symmetricKeyId, data, pos);
        pos += IoUtil.writeByte(salt, data, pos);
        pos += IoUtil.writeByte(salt >> 8, data, pos);
        pos += IoUtil.writeByte(salt >> 16, data, pos);
        return pos;
    }

    @Override
    public void afterDecode(ZContext ctx) {
        ctx.updateKeyIn();
    }

    @Override
    public void afterEncode(ZContext ctx) {
        ctx.updateKeyOut();
    }

    @Override
    public int getPriority() {
        return QOS_00_NETWORK_CONTROL;
    }

    @Override
    public String toString() {
        return String.format("%s,rc4-key:%d", super.toString(), symmetricKeyId);
    }
}
