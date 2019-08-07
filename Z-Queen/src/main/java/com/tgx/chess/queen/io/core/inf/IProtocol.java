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
package com.tgx.chess.queen.io.core.inf;

import java.util.Objects;

/**
 * @author William.d.zk
 */
public interface IProtocol
        extends
        IEncode,
        IDecode
{
    int PACKET_SERIAL   = 0;
    int COMMAND_SERIAL  = 0x0FF;//1~0xFE(254)
    int CONTROL_SERIAL  = 0x100;//0x101~0x1FE(257~510)
    int FRAME_SERIAL    = 0x200;//0x201~0x2FE(513~766)
    int INTERNAL_SERIAL = 0x400;//0x401~0x4FE(1025~1278)
    int DB_SERIAL       = 0x800;//0x801~0x8FE(2049~2302)

    /**
     * @return max in encoding min in decoding
     */
    int dataLength();

    int serial();

    int superSerial();

    default byte[] encode()
    {
        int len = dataLength();
        if (len > 0) {
            byte[] a = new byte[len];
            encodec(a, 0);
            return a;
        }
        return null;
    }

    default int encode(byte[] buf, int pos, int length)
    {
        int len = dataLength();
        if (len > length
            || len > 0 && Objects.isNull(buf)
            || (Objects.nonNull(buf) && (buf.length < len || pos + length > buf.length)))
        {
            throw new ArrayIndexOutOfBoundsException("data length is too long for input buf");
        }
        pos = encodec(buf, pos);
        return pos;
    }

    default int decode(byte[] data, int pos, int length)
    {
        //dataLength 此处表达了最短长度值
        int len = dataLength();
        if (len > length || (Objects.nonNull(data) && (data.length < len || pos + length > data.length))) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return decodec(data, pos);
    }

    default int decode(byte[] data)
    {
        //dataLength 此处表达了最短长度值
        if (Objects.nonNull(data) && data.length < dataLength()) { throw new ArrayIndexOutOfBoundsException(); }
        return decodec(data, 0);
    }

    default boolean idempotent(int bitIdempotent)
    {
        return false;
    }
}
