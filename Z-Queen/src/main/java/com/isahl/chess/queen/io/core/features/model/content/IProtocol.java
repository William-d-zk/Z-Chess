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
package com.isahl.chess.queen.io.core.features.model.content;

import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.queen.io.core.features.model.pipe.IDecode;
import com.isahl.chess.queen.io.core.features.model.pipe.IEncode;
import com.isahl.chess.queen.io.core.features.model.routes.IPortChannel;

/**
 * @author William.d.zk
 */
public interface IProtocol
        extends ISerial,
                IEncode,
                IDecode,
                IPortChannel
{
    default byte[] encode()
    {
        int len = length();
        if(len > 0) {
            byte[] a = new byte[len];
            encodec(a, 0);
            return a;
        }
        return null;
    }

    default int encode(byte[] buf, int pos, int length)
    {
        int len = length();
        if(len > length || len > 0 && buf == null || (buf != null && (buf.length < len || pos + length > buf.length))) {
            throw new ArrayIndexOutOfBoundsException("data length is too long for input buf");
        }
        pos = encodec(buf, pos);
        return pos;
    }

    default int encodec(byte[] buf, int pos)
    {
        return pos;
    }

    default int decode(byte[] input, int pos, int length)
    {
        if(input == null || input.length == 0) {return 0;}
        // length() 此处表达了最短长度值
        int len = length();
        if(len > length || (input.length < len || pos + length > input.length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return decodec(input, pos);
    }

    default int decode(byte[] data)
    {
        if(data == null || data.length == 0) {return 0;}
        return decode(data, 0, data.length);
    }

    default int decodec(byte[] data, int pos)
    {
        return pos;
    }

    default boolean inIdempotent(int bitIdempotent)
    {
        return true;
    }

    default boolean outIdempotent(int bitIdempotent)
    {
        return true;
    }

}
