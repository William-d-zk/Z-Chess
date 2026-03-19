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

import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;
import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.king.base.content.ByteBuf;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogMetaTest {
  @TempDir Path tempDir;

  private File metaFile;

  @BeforeEach
  void setUp() {
    metaFile = new File(tempDir.toFile(), "z-meta");
  }

  @AfterEach
  void tearDown() {
    // TempDir 会自动清理
  }

  @Test
  void testEncodeDecode() {
    LogMeta meta = new LogMeta(MIN_START, 5, 100, 5, 80, 80);

    ByteBuf buffer = meta.encode();
    assertNotNull(buffer, "编码后的 buffer 不应该为 null");
    assertTrue(buffer.writerIdx() > 0, "编码后的数据长度应该大于 0");

    LogMeta meta2 = new LogMeta();
    meta2.decode(buffer);

    // 验证所有字段
    assertEquals(meta.getStart(), meta2.getStart(), "解码后 start 应该匹配");
    assertEquals(meta.getTerm(), meta2.getTerm(), "解码后 term 应该匹配");
    assertEquals(meta.getIndex(), meta2.getIndex(), "解码后 index 应该匹配");
    assertEquals(meta.getIndexTerm(), meta2.getIndexTerm(), "解码后 indexTerm 应该匹配");
    assertEquals(meta.getCommit(), meta2.getCommit(), "解码后 commit 应该匹配");
    assertEquals(meta.getAccept(), meta2.getAccept(), "解码后 accept 应该匹配");
  }

  @Test
  void testFileLoad() throws IOException {
    LogMeta meta = new LogMeta(MIN_START, 1, 50, 1, 40, 40);

    // 创建并写入文件
    RandomAccessFile raf = new RandomAccessFile(metaFile, "rw");
    meta.ofFile(raf);
    meta.flush(true);

    // 关闭并重新打开
    meta.close();

    // 从文件加载
    raf = new RandomAccessFile(metaFile, "r");
    LogMeta loaded = BaseMeta.from(raf, LogMeta::new);

    assertNotNull(loaded, "加载的 LogMeta 不应该为 null");
    assertEquals(meta.getStart(), loaded.getStart(), "加载后 start 应该匹配");
    assertEquals(meta.getTerm(), loaded.getTerm(), "加载后 term 应该匹配");
    assertEquals(meta.getIndex(), loaded.getIndex(), "加载后 index 应该匹配");
    assertEquals(meta.getCommit(), loaded.getCommit(), "加载后 commit 应该匹配");
    assertEquals(meta.getAccept(), loaded.getAccept(), "加载后 accept 应该匹配");

    loaded.close();
  }

  @Test
  void testDefaultConstructor() {
    LogMeta meta = new LogMeta();

    assertEquals(MIN_START, meta.getStart(), "默认 start 应该为 MIN_START");
    assertEquals(0, meta.getTerm(), "默认 term 应该为 0");
    assertEquals(0, meta.getIndex(), "默认 index 应该为 0");
    assertEquals(0, meta.getIndexTerm(), "默认 indexTerm 应该为 0");
    assertEquals(0, meta.getCommit(), "默认 commit 应该为 0");
    assertEquals(0, meta.getAccept(), "默认 accept 应该为 0");
  }

  @Test
  void testSettersAndGetters() {
    LogMeta meta = new LogMeta();

    meta.setStart(MIN_START + 100);
    assertEquals(MIN_START + 100, meta.getStart(), "setStart 后 getStart 应该匹配");

    meta.setTerm(10);
    assertEquals(10, meta.getTerm(), "setTerm 后 getTerm 应该匹配");

    meta.setIndex(500);
    assertEquals(500, meta.getIndex(), "setIndex 后 getIndex 应该匹配");

    meta.setIndexTerm(10);
    assertEquals(10, meta.getIndexTerm(), "setIndexTerm 后 getIndexTerm 应该匹配");

    meta.setCommit(400);
    assertEquals(400, meta.getCommit(), "setCommit 后 getCommit 应该匹配");

    meta.setAccept(400);
    assertEquals(400, meta.getAccept(), "setAccept 后 getAccept 应该匹配");
  }

  @Test
  void testAccept() {
    LogMeta meta = new LogMeta(MIN_START, 1, 0, 0, 0, 0);

    LogEntry entry =
        new LogEntry(MIN_START, 1, 0xC001L, System.currentTimeMillis(), 0x01, new byte[] {0x01});
    meta.accept(entry);

    assertEquals(entry.index(), meta.getIndex(), "accept 后 index 应该更新");
    assertEquals(entry.index(), meta.getAccept(), "accept 后 accept 应该更新");
    assertEquals(entry.term(), meta.getIndexTerm(), "accept 后 indexTerm 应该更新");
  }

  @Test
  void testReset() {
    LogMeta meta = new LogMeta(MIN_START + 100, 10, 500, 10, 400, 400);

    meta.reset();

    assertEquals(MIN_START, meta.getStart(), "reset 后 start 应该重置");
    assertEquals(0, meta.getTerm(), "reset 后 term 应该重置");
    assertEquals(0, meta.getIndex(), "reset 后 index 应该重置");
    assertEquals(0, meta.getIndexTerm(), "reset 后 indexTerm 应该重置");
    assertEquals(0, meta.getCommit(), "reset 后 commit 应该重置");
    assertEquals(0, meta.getAccept(), "reset 后 accept 应该重置");
  }

  @Test
  void testLength() {
    LogMeta meta = new LogMeta(MIN_START, 5, 100, 5, 80, 80);

    int length = meta.length();
    assertTrue(length > 0, "length 应该大于 0");

    // length = 6个long(48字节) + super.length()
    assertTrue(length >= 48, "length 至少为 48 字节（6个 long）");
  }

  @Test
  void testToString() {
    LogMeta meta = new LogMeta(MIN_START, 5, 100, 5, 80, 80);

    String str = meta.toString();
    assertNotNull(str, "toString 不应该返回 null");
    // toString 应该包含关键字段信息
    assertTrue(str.length() > 0, "toString 应该返回非空字符串");
  }
}
