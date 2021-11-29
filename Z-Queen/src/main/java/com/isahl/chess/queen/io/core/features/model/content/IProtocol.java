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

import java.nio.ByteBuffer;

/**
 * @author William.d.zk
 */
public interface IProtocol
        extends ISerial,
                IEncode,
                IDecode,
                IPortChannel
{
    @Override
    default ByteBuffer encode()
    {
        int len = length();
        if(len > 0) {
            ByteBuffer buf = ByteBuffer.allocate(len);
            encodec(buf);
            return buf;
        }
        return null;
    }

    @Override
    default void decode(ByteBuffer input)
    {
        decodec(input);
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
