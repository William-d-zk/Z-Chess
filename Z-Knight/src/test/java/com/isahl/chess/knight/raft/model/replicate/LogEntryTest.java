/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.knight.raft.model.replicate;

import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

class LogEntryTest
{
    @Test
    void testIo()
    {
        LogEntry entry = new LogEntry(1,
                                      2,
                                      9981234,
                                      9124,
                                      new byte[]{ 1,
                                                  2,
                                                  3,
                                                  4 });
        System.out.println(entry.length());
        System.out.println(entry.sizeOf());
        byte[] encoded = entry.encoded();
        LogEntry new1 = new LogEntry(ByteBuf.wrap(encoded));

        System.out.println(new1);

    }
}