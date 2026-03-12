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

import com.isahl.chess.bishop.protocol.mqtt.factory.QttFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;
import static org.junit.jupiter.api.Assertions.*;

class SegmentTest
{
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException
    {
        tempDir = Files.createTempDirectory("segment_test");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        // 清理临时目录
        if(tempDir != null && Files.exists(tempDir)) {
            try(Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(Comparator.reverseOrder())
                     .forEach(path->{
                         try {
                             Files.delete(path);
                         }
                         catch(IOException e) {
                             // ignore
                         }
                     });
            }
        }
    }

    @Test
    void testCreate() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File file = new File(tempDir.toFile(), String.format(Segment.fileNameFormatter(false), 1, 0));
        Segment segment = new Segment(file, 1, true);
        LogEntry entry = new LogEntry(1,
                                      segment.getEndIndex() + 1,
                                      9981234,
                                      9123,
                                      0x99123,
                                      new byte[]{ 1,
                                                  2,
                                                  3,
                                                  4 });
        segment.add(entry);
        segment.freeze();
    }

    @Test
    void testLoad() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File file = new File(tempDir.toFile(), String.format(Segment.fileNameFormatter(false), 1, 0));
        Segment segment = new Segment(file, 1, true);
    }

    @Test
    void testTruncate() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {

    }

    /**
     * 测试 Segment fsync 功能
     */
    @Test
    void testFsyncEnabled() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File file = new File(tempDir.toFile(), String.format(Segment.fileNameFormatter(false), 1, 0));
        Segment segment = new Segment(file, 1, true);

        // 默认开启 fsync
        assertTrue(segment.isFsyncEnabled(), "默认应该开启 fsync");

        // 添加日志条目
        LogEntry entry1 = new LogEntry(1, 1, 0xC001L, 0x1001L, 0x01, new byte[]{ 0x01, 0x02 });
        assertTrue(segment.add(entry1), "添加第一条日志应该成功");
        assertEquals(1, segment.getEndIndex());

        // 手动刷盘
        assertDoesNotThrow(segment::flush, "手动 flush 不应该抛出异常");

        // 关闭 fsync
        segment.setFsyncEnabled(false);
        assertFalse(segment.isFsyncEnabled(), "关闭后 fsync 应该为 false");

        // 添加更多条目（不自动 fsync）
        LogEntry entry2 = new LogEntry(2, 1, 0xC001L, 0x1001L, 0x01, new byte[]{ 0x03, 0x04 });
        assertTrue(segment.add(entry2), "添加第二条日志应该成功");

        // 再次开启 fsync 并手动刷盘
        segment.setFsyncEnabled(true);
        assertTrue(segment.tryFlush(), "tryFlush 应该返回 true");

        segment.freeze();
    }

    /**
     * 测试多个条目的写入和刷盘
     */
    @Test
    void testMultipleEntriesWithFsync() throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File file = new File(tempDir.toFile(), String.format(Segment.fileNameFormatter(false), 1, 0));
        Segment segment = new Segment(file, 1, true);

        // 写入 10 条日志
        int count = 10;
        for(int i = 1; i <= count; i++) {
            LogEntry entry = new LogEntry(i, 1, 0xC001L, 0x1001L + i, 0x01, new byte[]{ (byte) i, (byte) (i + 1) });
            assertTrue(segment.add(entry), "添加第 " + i + " 条日志应该成功");
        }

        assertEquals(count, segment.getEndIndex(), "结束索引应该等于写入的条目数");

        // 验证可以读取所有条目
        for(int i = 1; i <= count; i++) {
            LogEntry entry = segment.getEntry(i);
            assertNotNull(entry, "应该能读取到索引 " + i + " 的条目");
            assertEquals(i, entry.index(), "条目索引应该匹配");
            assertEquals(1, entry.term(), "条目 term 应该匹配");
        }

        segment.freeze();
    }

    private List<LogEntry> mockEntryInput()
    {
        long term = 1;
        long index = MIN_START;
        List<LogEntry> logs = new ArrayList<>();
        for(long size = 10 + MIN_START; index <= size; index++) {
            LogEntry entry = new LogEntry(index,
                                          term,
                                          0xC002000000000000L,
                                          0x6079376BC6400L,
                                          QttFactory._Instance.serial(),
                                          new byte[]{ (byte) 0x82,
                                                      0x66,
                                                      (byte) 0xE2,
                                                      0x00,
                                                      0x04,
                                                      0x74,
                                                      0x65,
                                                      0x73,
                                                      0x74,
                                                      0x00 });
        }
        return logs;
    }
}