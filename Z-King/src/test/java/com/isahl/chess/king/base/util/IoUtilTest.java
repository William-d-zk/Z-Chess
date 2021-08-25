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

package com.isahl.chess.king.base.util;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class IoUtilTest
{

    @Test
    void long2Hex()
    {
        long l = 0x00FF00FF00FF00L;
        String hex = "00:00:FF:00:FF:00:FF:00";
        String result = IoUtil.long2Hex(l, ":");
        assert hex.equals(result);
    }

    @Test
    void variableIntLength()
    {
        byte[] varLength = IoUtil.variableLength(16384);
        ByteBuffer buf = ByteBuffer.wrap(varLength);
        assert IoUtil.readVariableIntLength(buf) == 16384;
    }

    @Test
    void writeAndReadLong()
    {
        byte[] p = IoUtil.writeLongArray(2L << 62);
        System.out.println(String.format("%#x", 2L << 62));
        System.out.println(String.format("%#x", IoUtil.readLong(p, 0)));
    }
}