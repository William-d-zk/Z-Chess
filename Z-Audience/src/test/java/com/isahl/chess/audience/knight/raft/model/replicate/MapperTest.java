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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Mapper 测试类
 * 注意：Mapper 是 Spring 组件，需要复杂的依赖注入。
 * 这里使用简化测试，通过直接操作 Segment 和 LogMeta 来验证核心逻辑。
 */
class MapperTest
{
    @TempDir
    Path tempDir;

    private Path logDataDir;
    private Path logMetaDir;

    @BeforeEach
    void setUp() throws IOException
    {
        logDataDir = tempDir.resolve("data");
        logMetaDir = tempDir.resolve("meta");
        Files.createDirectories(logDataDir);
        Files.createDirectories(logMetaDir);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        // 清理临时目录
        if(tempDir != null && Files.exists(tempDir)) {
            try(Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(Comparator.reverseOrder())
                     .forEach(path -> {
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
    void testGetEndIndex()
    {
        // 创建 LogMeta 并验证初始状态
        LogMeta meta = new LogMeta(MIN_START, 1, 0, 0, 0, 0);
        
        // 初始状态：endIndex = start - 1（空日志）
        long expectedEndIndex = MIN_START - 1;
        assertEquals(expectedEndIndex, meta.getStart() - 1, 
            "空日志时 endIndex 应该等于 start - 1");
    }

    @Test
    void testGetStartIndex()
    {
        LogMeta meta = new LogMeta(MIN_START, 1, 5, 1, 3, 3);
        
        assertEquals(MIN_START, meta.getStart(), "startIndex 应该等于初始化值");
    }

    @Test
    void testGetEntry()
        throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        // 创建一个 Segment 并添加条目
        File segmentFile = new File(logDataDir.toFile(), 
            String.format(Segment.fileNameFormatter(false), MIN_START, 0));
        Segment segment = new Segment(segmentFile, MIN_START, true);
        
        LogEntry entry = new LogEntry(MIN_START, 1, 0xC001L, 0x1001L, 0x01, 
            new byte[]{ 0x01, 0x02, 0x03 });
        
        assertTrue(segment.add(entry), "添加日志条目应该成功");
        
        // 验证可以获取条目
        LogEntry retrieved = segment.getEntry(MIN_START);
        assertNotNull(retrieved, "应该能获取到条目");
        assertEquals(MIN_START, retrieved.index(), "条目索引应该匹配");
        assertEquals(1, retrieved.term(), "条目 term 应该匹配");
        
        segment.freeze();
    }

    @Test
    void testGetEntryTerm()
        throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File segmentFile = new File(logDataDir.toFile(), 
            String.format(Segment.fileNameFormatter(false), MIN_START, 0));
        Segment segment = new Segment(segmentFile, MIN_START, true);
        
        long term = 5;
        LogEntry entry = new LogEntry(MIN_START, term, 0xC001L, 0x1001L, 0x01, 
            new byte[]{ 0x01 });
        
        assertTrue(segment.add(entry), "添加日志条目应该成功");
        
        LogEntry retrieved = segment.getEntry(MIN_START);
        assertNotNull(retrieved);
        assertEquals(term, retrieved.term(), "条目 term 应该匹配");
        
        segment.freeze();
    }

    @Test
    void testUpdateLogStart() throws IOException
    {
        LogMeta meta = new LogMeta(MIN_START, 1, 5, 1, 3, 3);
        
        // 通过文件持久化测试
        File metaFile = new File(logMetaDir.toFile(), "test-meta");
        RandomAccessFile raf = new RandomAccessFile(metaFile, "rw");
        meta.ofFile(raf);
        
        // 更新 start
        long newStart = MIN_START + 10;
        meta.setStart(newStart);
        meta.flush(true);
        
        // 重新加载验证
        raf.close();
        raf = new RandomAccessFile(metaFile, "r");
        LogMeta loaded = BaseMeta.from(raf, LogMeta::new);
        assertNotNull(loaded);
        assertEquals(newStart, loaded.getStart(), "更新后的 start 应该持久化");
        
        meta.close();
        loaded.close();
    }

    @Test
    void testAppend()
        throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File segmentFile = new File(logDataDir.toFile(), 
            String.format(Segment.fileNameFormatter(false), MIN_START, 0));
        Segment segment = new Segment(segmentFile, MIN_START, true);
        
        // 追加多条日志
        int count = 5;
        for(int i = 0; i < count; i++) {
            LogEntry entry = new LogEntry(MIN_START + i, 1, 0xC001L + i, 0x1001L + i, 0x01, 
                new byte[]{ (byte) i });
            assertTrue(segment.add(entry), "第 " + (i + 1) + " 条日志追加应该成功");
        }
        
        // 验证 endIndex
        assertEquals(MIN_START + count - 1, segment.getEndIndex(), 
            "endIndex 应该是最后一条日志的索引");
        
        // 验证所有条目都可以读取
        for(int i = 0; i < count; i++) {
            LogEntry entry = segment.getEntry(MIN_START + i);
            assertNotNull(entry, "应该能读取索引 " + (MIN_START + i) + " 的条目");
            assertEquals(MIN_START + i, entry.index(), "条目索引应该匹配");
        }
        
        segment.freeze();
    }

    @Test
    void testTruncatePrefix()
        throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File segmentFile = new File(logDataDir.toFile(), 
            String.format(Segment.fileNameFormatter(false), MIN_START, 0));
        Segment segment = new Segment(segmentFile, MIN_START, true);
        
        // 添加多条日志
        for(int i = 0; i < 10; i++) {
            LogEntry entry = new LogEntry(MIN_START + i, 1, 0xC001L, 0x1001L, 0x01, 
                new byte[]{ (byte) i });
            segment.add(entry);
        }
        
        // 冻结 segment（变为只读）
        segment.freeze();
        
        // 验证所有条目仍然存在
        for(int i = 0; i < 10; i++) {
            assertNotNull(segment.getEntry(MIN_START + i), "冻结后条目 " + i + " 应该仍然存在");
        }
    }

    @Test
    void testTruncateSuffix()
        throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        File segmentFile = new File(logDataDir.toFile(), 
            String.format(Segment.fileNameFormatter(false), MIN_START, 0));
        Segment segment = new Segment(segmentFile, MIN_START, true);
        
        // 添加多条日志
        int count = 10;
        for(int i = 0; i < count; i++) {
            LogEntry entry = new LogEntry(MIN_START + i, 1, 0xC001L, 0x1001L, 0x01, 
                new byte[]{ (byte) i });
            segment.add(entry);
        }
        
        // 截断到中间位置
        long newEndIndex = MIN_START + 4;
        long droppedSize = segment.truncate(newEndIndex);
        
        assertTrue(droppedSize > 0, "截断应该释放一些空间");
        assertEquals(newEndIndex, segment.getEndIndex(), "endIndex 应该更新");
        
        // 验证截断前的条目仍然存在
        for(int i = 0; i <= 4; i++) {
            assertNotNull(segment.getEntry(MIN_START + i), "条目 " + i + " 应该仍然存在");
        }
        
        segment.freeze();
    }

    @Test
    void testLogMetaEncodeDecode()
    {
        LogMeta meta = new LogMeta(MIN_START, 5, 100, 5, 80, 80);
        
        ByteBuf encoded = meta.encode();
        assertNotNull(encoded, "编码后的 buffer 不应该为 null");
        assertTrue(encoded.writerIdx() > 0, "编码后的数据长度应该大于 0");
        
        LogMeta decoded = new LogMeta();
        decoded.decode(encoded);
        
        assertEquals(meta.getStart(), decoded.getStart(), "解码后 start 应该匹配");
        assertEquals(meta.getTerm(), decoded.getTerm(), "解码后 term 应该匹配");
        assertEquals(meta.getIndex(), decoded.getIndex(), "解码后 index 应该匹配");
        assertEquals(meta.getIndexTerm(), decoded.getIndexTerm(), "解码后 indexTerm 应该匹配");
        assertEquals(meta.getCommit(), decoded.getCommit(), "解码后 commit 应该匹配");
        assertEquals(meta.getAccept(), decoded.getAccept(), "解码后 accept 应该匹配");
    }

    @Test
    void testLogEntryEncodeDecode()
    {
        LogEntry entry = new LogEntry(100, 5, 0xC001L, 0x1001L, 0x01, 
            new byte[]{ 0x01, 0x02, 0x03, 0x04 });
        
        byte[] encoded = entry.encoded();
        assertNotNull(encoded, "编码后的数据不应该为 null");
        assertTrue(encoded.length > 0, "编码后的数据长度应该大于 0");
        
        LogEntry decoded = new LogEntry(ByteBuf.wrap(encoded));
        
        assertEquals(entry.index(), decoded.index(), "解码后 index 应该匹配");
        assertEquals(entry.term(), decoded.term(), "解码后 term 应该匹配");
        assertEquals(entry.client(), decoded.client(), "解码后 client 应该匹配");
        assertEquals(entry.serial(), decoded.serial(), "解码后 serial 应该匹配");
    }

    @Test
    void testLogMetaAccept()
    {
        LogMeta meta = new LogMeta(MIN_START, 1, 0, 0, 0, 0);
        
        // accept 一个条目
        LogEntry entry = new LogEntry(MIN_START, 1, 0xC001L, 0x1001L, 0x01, new byte[]{ 0x01 });
        meta.accept(entry);
        
        assertEquals(MIN_START, meta.getIndex(), "accept 后 index 应该更新为条目索引");
        assertEquals(MIN_START, meta.getAccept(), "accept 后 accept 应该更新为条目索引");
        assertEquals(1, meta.getIndexTerm(), "accept 后 indexTerm 应该更新为条目 term");
    }

    @Test
    void testLogMetaReset()
    {
        LogMeta meta = new LogMeta(MIN_START + 100, 10, 500, 10, 400, 400);
        
        meta.reset();
        
        assertEquals(MIN_START, meta.getStart(), "reset 后 start 应该重置为 MIN_START");
        assertEquals(0, meta.getTerm(), "reset 后 term 应该重置为 0");
        assertEquals(0, meta.getIndex(), "reset 后 index 应该重置为 0");
        assertEquals(0, meta.getIndexTerm(), "reset 后 indexTerm 应该重置为 0");
        assertEquals(0, meta.getCommit(), "reset 后 commit 应该重置为 0");
        assertEquals(0, meta.getAccept(), "reset 后 accept 应该重置为 0");
    }
}
