/*
 * MIT License
 *
 * Copyright (c) 2021. Z-Chess
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

import java.io.IOException;
import java.io.RandomAccessFile;

class LogMetaTest
{
    @Test
    void test()
    {
        LogMeta meta = new LogMeta();
        ByteBuf buffer = meta.encode();

        LogMeta meta2 = new LogMeta();
        meta2.decode(buffer);
    }

    @Test
    void testFileLoad() throws IOException
    {
        LogMeta meta = new LogMeta();

        RandomAccessFile raf = new RandomAccessFile("z-meta", "rw");
        meta.with(raf);
        meta.close();

        raf = new RandomAccessFile("z-meta", "r");
        LogMeta load = BaseMeta.from(raf, LogMeta::new);
        System.out.println(load);
    }
}