/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.king.base.util.crc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** CrcUtil 测试类 */
public class CrcUtilTest {

  // 测试数据：ASCII "123456789"
  private static final byte[] TEST_DATA = "123456789".getBytes();

  @Test
  public void testCrc8() {
    long crc = CrcUtil.crc8(TEST_DATA);
    // CRC-8 标准校验值
    assertEquals(0xF4, crc, "CRC-8 calculation should match expected value");
  }

  @Test
  public void testCrc16() {
    long crc = CrcUtil.crc16(TEST_DATA);
    // CRC-16 (IBM) 标准校验值
    assertEquals(0xBB3D, crc, "CRC-16 calculation should match expected value");
  }

  @Test
  public void testCrc16Ccitt() {
    CrcUtil.CrcEngine engine = CrcUtil.getEngine(CrcUtil.CRC16_CCITT);
    long crc = engine.calculate(TEST_DATA);
    assertEquals(0x29B1, crc, "CRC-16/CCITT calculation should match expected value");
  }

  @Test
  public void testCrc32() {
    long crc = CrcUtil.crc32(TEST_DATA);
    // CRC-32 (IEEE) 标准校验值
    assertEquals(0xCBF43926L, crc, "CRC-32 calculation should match expected value");
  }

  @Test
  public void testCrc32c() {
    long crc = CrcUtil.crc32c(TEST_DATA);
    // CRC-32C (Castagnoli) 标准校验值
    assertEquals(0xE3069283L, crc, "CRC-32C calculation should match expected value");
  }

  @Test
  public void testCrc64() {
    CrcUtil.CrcEngine engine = CrcUtil.getEngine(CrcUtil.CRC64);
    long crc = engine.calculate(TEST_DATA);
    assertEquals(0x6C40DF5F0B497347L, crc, "CRC-64 calculation should match expected value");
  }

  @Test
  public void testEngineCaching() {
    // 测试引擎缓存
    CrcUtil.clearCache();
    assertEquals(0, CrcUtil.getCacheSize(), "Cache should be empty after clear");

    // 第一次获取，创建新引擎
    CrcUtil.CrcEngine engine1 = CrcUtil.getEngine(CrcUtil.CRC32);
    assertEquals(1, CrcUtil.getCacheSize(), "Cache should contain one engine");

    // 第二次获取，应该返回缓存的引擎
    CrcUtil.CrcEngine engine2 = CrcUtil.getEngine(CrcUtil.CRC32);
    assertEquals(1, CrcUtil.getCacheSize(), "Cache should still contain one engine");
    assertSame(engine1, engine2, "Should return cached engine");
  }

  @Test
  public void testStreamingCalculation() {
    CrcUtil.CrcEngine engine = CrcUtil.getEngine(CrcUtil.CRC32);
    CrcUtil.CrcStream stream = engine.createStream();

    // 分块更新
    stream.update(TEST_DATA, 0, 3); // "123"
    stream.update(TEST_DATA, 3, 3); // "456"
    stream.update(TEST_DATA, 6, 3); // "789"

    long crc = stream.getValue();
    assertEquals(0xCBF43926L, crc, "Streaming CRC should match full calculation");

    // 测试重置
    stream.reset();
    stream.update(TEST_DATA);
    crc = stream.getValue();
    assertEquals(0xCBF43926L, crc, "CRC after reset should match");
  }

  @Test
  public void testEmptyData() {
    byte[] empty = new byte[0];

    // CRC32 空数据应返回初始化值的补码
    long crc = CrcUtil.crc32(empty);
    assertEquals(CrcUtil.CRC32.init, crc, "CRC of empty data should equal init value");
  }

  @Test
  public void testNullData() {
    // 测试空数据处理
    long crc = CrcUtil.crc32(null);
    assertEquals(CrcUtil.CRC32.init, crc, "CRC of null data should equal init value");
  }

  @Test
  public void testConcurrency() throws InterruptedException {
    // 测试并发安全性
    final int threadCount = 10;
    final int iterations = 1000;
    Thread[] threads = new Thread[threadCount];
    final boolean[] passed = {true};

    for (int i = 0; i < threadCount; i++) {
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < iterations; j++) {
                  long crc = CrcUtil.crc32(TEST_DATA);
                  if (crc != 0xCBF43926L) {
                    passed[0] = false;
                  }
                }
              });
      threads[i].start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    assertTrue(passed[0], "All concurrent calculations should produce correct results");
    assertTrue(CrcUtil.getCacheSize() <= 64, "Cache size should be bounded");
  }

  @Test
  public void testDifferentAlgorithms() {
    // 测试不同算法产生不同结果
    long crc8 = CrcUtil.crc8(TEST_DATA);
    long crc16 = CrcUtil.crc16(TEST_DATA);
    long crc32 = CrcUtil.crc32(TEST_DATA);
    long crc64 = CrcUtil.getEngine(CrcUtil.CRC64).calculate(TEST_DATA);

    assertNotEquals(crc8, crc16, "Different algorithms should produce different results");
    assertNotEquals(crc16, crc32, "Different algorithms should produce different results");
    assertNotEquals(crc32, crc64, "Different algorithms should produce different results");
  }

  @Test
  public void testParameterEquality() {
    // 测试参数相等性
    CrcUtil.CrcParams params1 =
        new CrcUtil.CrcParams("test", 32, 0x04C11DB7L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0);
    CrcUtil.CrcParams params2 =
        new CrcUtil.CrcParams(
            "different_name", 32, 0x04C11DB7L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0);

    assertEquals(params1, params2, "Params with same algorithm settings should be equal");
    assertEquals(
        params1.hashCode(), params2.hashCode(), "Hash codes should match for equal params");
  }
}
