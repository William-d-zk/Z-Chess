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

import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest
{
    @Test
    void testIo()
    {
        LogEntry entry = new LogEntry(1,
                                      2,
                                      0x12334,
                                      0x932321,
                                      0x11341134,
                                      new byte[]{ 1,
                                                  2,
                                                  3,
                                                  4 });
        
        // 验证长度计算
        int length = entry.length();
        assertTrue(length > 0, "length 应该大于 0");
        
        int sizeOf = entry.sizeOf();
        assertTrue(sizeOf > 0, "sizeOf 应该大于 0");
        assertTrue(sizeOf >= length, "sizeOf 应该大于等于 length");
        
        // 编码
        byte[] encoded = entry.encoded();
        assertNotNull(encoded, "编码后的数据不应该为 null");
        assertTrue(encoded.length > 0, "编码后的数据长度应该大于 0");
        assertEquals(sizeOf, encoded.length, "编码后的数据长度应该等于 sizeOf");
        
        // 解码
        LogEntry decoded = new LogEntry(ByteBuf.wrap(encoded));
        
        // 验证所有字段
        assertEquals(entry.index(), decoded.index(), "解码后 index 应该匹配");
        assertEquals(entry.term(), decoded.term(), "解码后 term 应该匹配");
        assertEquals(entry.client(), decoded.client(), "解码后 client 应该匹配");
        assertEquals(entry.serial(), decoded.serial(), "解码后 serial 应该匹配");
        assertEquals(entry.origin(), decoded.origin(), "解码后 origin 应该匹配");
        assertArrayEquals(entry.content(), decoded.content(), "解码后 content 应该匹配");
    }
    
    @Test
    void testLogEntryCreation()
    {
        long index = 100;
        long term = 5;
        long client = 0xC001L;
        long origin = 0x1001L;
        int factory = 0x1234;
        byte[] content = new byte[]{ 0x01, 0x02, 0x03 };
        
        LogEntry entry = new LogEntry(index, term, client, origin, factory, content);
        
        assertEquals(index, entry.index(), "index 应该匹配");
        assertEquals(term, entry.term(), "term 应该匹配");
        assertEquals(client, entry.client(), "client 应该匹配");
        assertEquals(origin, entry.origin(), "origin 应该匹配");
        assertEquals(factory, entry.factory(), "factory 应该匹配");
        assertArrayEquals(content, entry.content(), "content 应该匹配");
    }
    
    @Test
    void testLogEntryWithEmptyContent()
    {
        LogEntry entry = new LogEntry(1, 1, 0xC001L, 0x1001L, 0x01, new byte[0]);
        
        assertNotNull(entry.content(), "content 不应该为 null");
        assertEquals(0, entry.content().length, "content 长度应该为 0");
        
        // 验证编解码
        byte[] encoded = entry.encoded();
        LogEntry decoded = new LogEntry(ByteBuf.wrap(encoded));
        
        assertArrayEquals(entry.content(), decoded.content(), "空 content 编解码应该匹配");
    }
    
    @Test
    void testLogEntryWithNullContent()
    {
        LogEntry entry = new LogEntry(1, 1, 0xC001L, 0x1001L, 0x01, null);
        
        // null content 可能返回 null 或空数组，取决于实现
        assertNull(entry.content(), "content 应该为 null");
    }
    
    @Test
    void testToString()
    {
        LogEntry entry = new LogEntry(100, 5, 0xC001L, 0x1001L, 0x1234, new byte[]{ 0x01 });
        
        String str = entry.toString();
        assertNotNull(str, "toString 不应该返回 null");
        // toString 格式: raft_log{ id: %d@%d, client:%#x, biz-session:%#x, ... }
        assertTrue(str.contains("100"), "toString 应该包含 index");
        assertTrue(str.contains("5"), "toString 应该包含 term");
    }
}
