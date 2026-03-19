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

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.king.base.content.ByteBuf;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class IoUtilTest {

  @Test
  void long2Hex() {
    long l = 0x00FF00FF00FF00L;
    String hex = "00:00:FF:00:FF:00:FF:00";
    String result = IoUtil.long2Hex(l, ":");
    assertEquals(hex, result, "long2Hex 转换应该正确");

    // 测试不同的分隔符
    String resultDash = IoUtil.long2Hex(l, "-");
    assertEquals("00-00-FF-00-FF-00-FF-00", resultDash, "使用 - 分隔符应该正确");

    // 测试空分隔符
    String resultNoSep = IoUtil.long2Hex(l, "");
    assertEquals("0000FF00FF00FF00", resultNoSep, "无分隔符应该正确");
  }

  @Test
  void variableIntLength() {
    byte[] varLength = IoUtil.variableLength(16384);
    ByteBuffer buf = ByteBuffer.wrap(varLength);
    int decoded = IoUtil.readVariableIntLength(buf);
    assertEquals(16384, decoded, "可变长度整数编解码应该正确");

    // 测试边界值
    testVariableLengthRoundTrip(0);
    testVariableLengthRoundTrip(127);
    testVariableLengthRoundTrip(128);
    testVariableLengthRoundTrip(16383);
    testVariableLengthRoundTrip(16384);
    testVariableLengthRoundTrip(65535);
  }

  private void testVariableLengthRoundTrip(int value) {
    byte[] encoded = IoUtil.variableLength(value);
    ByteBuffer buf = ByteBuffer.wrap(encoded);
    int decoded = IoUtil.readVariableIntLength(buf);
    assertEquals(value, decoded, "可变长度整数 " + value + " 编解码应该正确");
  }

  @Test
  void writeAndReadLong() {
    long value = 2L << 62;
    byte[] p = IoUtil.writeLongArray(value);
    long read = IoUtil.readLong(p, 0);
    assertEquals(value, read, "long 读写应该正确");

    // 格式化输出验证
    String hexStr = String.format("%#x", value);
    assertEquals("0x8000000000000000", hexStr);

    String readHexStr = String.format("%#x", read);
    assertEquals(hexStr, readHexStr, "读写后的十六进制表示应该相同");
  }

  @Test
  void copy() {
    ByteBuf buffer = ByteBuf.allocate(10);
    buffer.put("abcd".getBytes(StandardCharsets.UTF_8));
    buffer.get();
    buffer.discard();
    buffer.get();

    // 验证 buffer 操作后的状态
    // discard: writerIdx = 4 - readerIdx(1) = 3
    assertEquals(3, buffer.writerIdx(), "写入后 writerIdx 应该是 4 - 1（get后）= 3");
  }

  @Test
  void intToChars() {
    char[] chars = {'M', 'Q', 'T', 'T'};
    String mqtt = new String(chars);
    assertEquals("MQTT", mqtt, "字符数组转换应该正确");

    int a = 'M' << 24 | 'Q' << 16 | 'T' << 8 | 'T';
    String decoded = IoUtil.int2Chars(a);
    assertArrayEquals(chars, decoded.toCharArray(), "int2Chars 转换应该正确");

    // 验证解码后的字符串
    assertEquals("MQTT", decoded, "解码后的字符串应该是 MQTT");
  }

  @Test
  void testHex2Bin() {
    // 测试十六进制字符串转字节数组
    byte[] result = IoUtil.hex2bin("ABC0");
    assertNotNull(result, "hex2bin 结果不应该为 null");
    assertEquals(2, result.length, "ABC0 应该转换为 2 字节");
    assertEquals((byte) 0xAB, result[0], "第一个字节应该是 0xAB");
    assertEquals((byte) 0xC0, result[1], "第二个字节应该是 0xC0");
  }

  @Test
  void testBin2Hex() {
    byte[] data = new byte[] {(byte) 0xAB, (byte) 0xC0};
    String hex = IoUtil.bin2Hex(data);
    assertEquals("ABC0", hex, "bin2Hex 应该返回大写十六进制字符串");
  }

  @Test
  void testSwapLhb() {
    int value = 0x1234;
    int swapped = IoUtil.swapLhb(value);
    assertEquals(0x3412, swapped, "swapLhb 应该交换高低字节");

    // 再次交换应该还原
    int swappedBack = IoUtil.swapLhb(swapped);
    assertEquals(value, swappedBack, "两次交换应该还原");
  }

  @Test
  void testIsBlank() {
    assertTrue(IoUtil.isBlank(null), "null 应该被认为是 blank");
    assertTrue(IoUtil.isBlank(""), "空字符串应该被认为是 blank");
    assertTrue(IoUtil.isBlank("   "), "空白字符串应该被认为是 blank");
    assertFalse(IoUtil.isBlank("test"), "非空字符串不应该被认为是 blank");
    assertFalse(IoUtil.isBlank("  test  "), "包含非空白字符的字符串不应该被认为是 blank");
  }

  @Test
  void testWriteByte() {
    byte[] buffer = new byte[4];
    IoUtil.writeByte(0xAB, buffer, 0);
    assertEquals((byte) 0xAB, buffer[0], "writeByte 应该正确写入字节");

    IoUtil.writeByte(0xCD, buffer, 1);
    assertEquals((byte) 0xCD, buffer[1], "writeByte 应该在正确位置写入字节");
  }

  @Test
  void testReadLong() {
    byte[] data = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
    long value = IoUtil.readLong(data, 0);
    assertNotEquals(0, value, "readLong 应该读取非零值");

    // 验证读取的字节序
    long expected =
        ((long) data[0] << 56)
            | ((long) (data[1] & 0xFF) << 48)
            | ((long) (data[2] & 0xFF) << 40)
            | ((long) (data[3] & 0xFF) << 32)
            | ((long) (data[4] & 0xFF) << 24)
            | ((long) (data[5] & 0xFF) << 16)
            | ((long) (data[6] & 0xFF) << 8)
            | ((long) (data[7] & 0xFF));
    assertEquals(expected, value, "readLong 应该正确读取大端序 long");
  }

  @Test
  void testLong2Hex() {
    long value = 0x0123456789ABCDEFL;
    String hex = IoUtil.long2Hex(value, "");
    assertEquals("0123456789ABCDEF", hex, "long2Hex 应该返回大写十六进制字符串");

    String hexUpper = IoUtil.long2Hex(value, ":").toUpperCase();
    assertEquals("01:23:45:67:89:AB:CD:EF", hexUpper, "long2Hex 应该正确使用分隔符");
  }
}
