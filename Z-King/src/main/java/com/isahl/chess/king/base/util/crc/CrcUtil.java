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

import java.util.concurrent.ConcurrentHashMap;

/**
 * 改进的 CRC 计算工具类
 *
 * <p>改进点： 1. 使用线程安全的 ConcurrentHashMap 替代 HashMap 2. 使用 SoftReference 允许 GC 在内存紧张时回收缓存 3. 支持常用的 CRC
 * 算法预定义常量 4. 更高效的查找表计算 5. 支持流式计算（增量 CRC）
 *
 * @author Z-Chess Team
 */
public final class CrcUtil {

  private CrcUtil() {
    // 工具类，禁止实例化
  }

  // 使用 ConcurrentHashMap 保证线程安全
  // 使用 SoftReference 允许 GC 在内存紧张时回收
  private static final ConcurrentHashMap<CrcParams, java.lang.ref.SoftReference<CrcEngine>>
      ENGINE_CACHE = new ConcurrentHashMap<>(16, 0.75f, 1);

  // 最大缓存大小，防止内存泄漏
  private static final int MAX_CACHE_SIZE = 64;

  /** CRC 参数定义（不可变，线程安全） */
  public static final class CrcParams {
    public final String name;
    public final int width;
    public final long poly;
    public final long init;
    public final boolean refIn;
    public final boolean refOut;
    public final long xorOut;
    public final long check;

    public CrcParams(
        String name,
        int width,
        long poly,
        long init,
        boolean refIn,
        boolean refOut,
        long xorOut,
        long check) {
      this.name = name;
      this.width = width;
      this.poly = poly;
      this.init = init;
      this.refIn = refIn;
      this.refOut = refOut;
      this.xorOut = xorOut;
      this.check = check;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof CrcParams)) return false;
      CrcParams that = (CrcParams) o;
      return width == that.width
          && poly == that.poly
          && init == that.init
          && refIn == that.refIn
          && refOut == that.refOut
          && xorOut == that.xorOut;
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(width, poly, init, refIn, refOut, xorOut);
    }

    @Override
    public String toString() {
      return name + "(" + width + "-bit)";
    }
  }

  /** CRC 计算引擎（线程安全） */
  public static final class CrcEngine {
    private final CrcParams params;
    private final long mask;
    private final long[] table;

    public CrcEngine(CrcParams params) {
      this.params = params;
      this.mask = params.width == 64 ? ~0L : (1L << params.width) - 1;
      this.table = buildLookupTable();
    }

    /** 计算 CRC（完整数据） */
    public long calculate(byte[] data) {
      if (data == null || data.length == 0) {
        return params.init;
      }
      return calculate(data, 0, data.length);
    }

    /** 计算 CRC（指定范围） */
    public long calculate(byte[] data, int offset, int length) {
      if (data == null || length <= 0) {
        return params.init ^ params.xorOut;
      }

      long crc = params.init;

      if (params.refIn) {
        // 反射输入模式（如 CRC32）
        for (int i = offset; i < offset + length; i++) {
          crc = table[(int) ((crc ^ (data[i] & 0xFF)) & 0xFF)] ^ (crc >>> 8);
        }
      } else {
        // 标准模式（如 CRC16/CCITT）
        int shift = Math.max(0, params.width - 8);
        for (int i = offset; i < offset + length; i++) {
          crc = table[(int) (((crc >> shift) ^ (data[i] & 0xFF)) & 0xFF)] ^ (crc << 8);
          crc &= mask;
        }
      }

      return (crc ^ params.xorOut) & mask;
    }

    /** 创建流式计算器（支持增量计算） */
    public CrcStream createStream() {
      return new CrcStream(this);
    }

    private long[] buildLookupTable() {
      long[] table = new long[256];

      if (params.refIn) {
        // 反射模式（如 CRC32）：使用反射多项式
        long poly = reverseBits(params.poly, params.width);
        for (int i = 0; i < 256; i++) {
          long crc = i;
          for (int j = 0; j < 8; j++) {
            if ((crc & 1) != 0) {
              crc = (crc >>> 1) ^ poly;
            } else {
              crc >>>= 1;
            }
          }
          table[i] = crc & mask;
        }
      } else {
        // 标准模式（如 CRC16/CCITT）
        long topBit = 1L << (params.width - 1);
        for (int i = 0; i < 256; i++) {
          long crc = (long) i << (params.width - 8);
          for (int j = 0; j < 8; j++) {
            if ((crc & topBit) != 0) {
              crc = (crc << 1) ^ params.poly;
            } else {
              crc <<= 1;
            }
          }
          table[i] = crc & mask;
        }
      }

      return table;
    }

    private static long reverseBits(long value, int bits) {
      long result = 0;
      for (int i = 0; i < bits; i++) {
        result = (result << 1) | ((value >>> i) & 1);
      }
      return result;
    }
  }

  /** 流式 CRC 计算器（支持增量计算） */
  public static final class CrcStream {
    private final CrcEngine engine;
    private long currentCrc;

    private CrcStream(CrcEngine engine) {
      this.engine = engine;
      this.currentCrc = engine.params.init;
    }

    public void update(byte[] data) {
      update(data, 0, data.length);
    }

    public void update(byte[] data, int offset, int length) {
      if (data == null || length <= 0) return;

      if (engine.params.refIn) {
        for (int i = offset; i < offset + length; i++) {
          currentCrc =
              engine.table[(int) ((currentCrc ^ (data[i] & 0xFF)) & 0xFF)] ^ (currentCrc >>> 8);
        }
      } else {
        int shift = Math.max(0, engine.params.width - 8);
        for (int i = offset; i < offset + length; i++) {
          currentCrc =
              engine.table[(int) (((currentCrc >> shift) ^ (data[i] & 0xFF)) & 0xFF)]
                  ^ (currentCrc << 8);
          currentCrc &= engine.mask;
        }
      }
    }

    public void update(byte b) {
      if (engine.params.refIn) {
        currentCrc = engine.table[(int) ((currentCrc ^ (b & 0xFF)) & 0xFF)] ^ (currentCrc >>> 8);
      } else {
        int shift = Math.max(0, engine.params.width - 8);
        currentCrc =
            engine.table[(int) (((currentCrc >> shift) ^ (b & 0xFF)) & 0xFF)] ^ (currentCrc << 8);
        currentCrc &= engine.mask;
      }
    }

    public long getValue() {
      return (currentCrc ^ engine.params.xorOut) & engine.mask;
    }

    public void reset() {
      this.currentCrc =
          engine.params.refOut
              ? reverseBits(engine.params.init, engine.params.width)
              : engine.params.init;
    }

    private static long reverseBits(long value, int bits) {
      long result = 0;
      for (int i = 0; i < bits; i++) {
        result = (result << 1) | ((value >>> i) & 1);
      }
      return result;
    }
  }

  // ==================== 预定义常用 CRC 算法 ====================

  /** CRC-8 (ITU-T I.432.1) */
  public static final CrcParams CRC8 =
      new CrcParams("CRC-8", 8, 0x07, 0x00, false, false, 0x00, 0xF4);

  /** CRC-8/CDMA2000 */
  public static final CrcParams CRC8_CDMA2000 =
      new CrcParams("CRC-8/CDMA2000", 8, 0x9B, 0xFF, false, false, 0x00, 0xDA);

  /** CRC-16 (IBM/ARC) */
  public static final CrcParams CRC16 =
      new CrcParams("CRC-16", 16, 0x8005, 0x0000, true, true, 0x0000, 0xBB3D);

  /** CRC-16/CCITT-FALSE */
  public static final CrcParams CRC16_CCITT =
      new CrcParams("CRC-16/CCITT", 16, 0x1021, 0xFFFF, false, false, 0x0000, 0x29B1);

  /** CRC-16/XMODEM */
  public static final CrcParams CRC16_XMODEM =
      new CrcParams("CRC-16/XMODEM", 16, 0x1021, 0x0000, false, false, 0x0000, 0x31C3);

  /** CRC-16/MODBUS */
  public static final CrcParams CRC16_MODBUS =
      new CrcParams("CRC-16/MODBUS", 16, 0x8005, 0xFFFF, true, true, 0x0000, 0x4B37);

  /** CRC-32 (IEEE 802.3/Ethernet) */
  public static final CrcParams CRC32 =
      new CrcParams("CRC-32", 32, 0x04C11DB7L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0xCBF43926L);

  /** CRC-32C (Castagnoli) - iSCSI, SCTP */
  public static final CrcParams CRC32C =
      new CrcParams("CRC-32C", 32, 0x1EDC6F41L, 0xFFFFFFFFL, true, true, 0xFFFFFFFFL, 0xE3069283L);

  /** CRC-64 (ISO 3309) */
  public static final CrcParams CRC64 =
      new CrcParams(
          "CRC-64",
          64,
          0x42F0E1EBA9EA3693L,
          0x0000000000000000L,
          false,
          false,
          0x0000000000000000L,
          0x6C40DF5F0B497347L);

  /** CRC-64/XZ */
  public static final CrcParams CRC64_XZ =
      new CrcParams(
          "CRC-64/XZ",
          64,
          0x42F0E1EBA9EA3693L,
          0xFFFFFFFFFFFFFFFFL,
          true,
          true,
          0xFFFFFFFFFFFFFFFFL,
          0x995DC9BBDF1939FAL);

  // ==================== 公共 API ====================

  /** 获取或创建 CRC 引擎（带缓存） */
  public static CrcEngine getEngine(CrcParams params) {
    // 检查缓存
    java.lang.ref.SoftReference<CrcEngine> ref = ENGINE_CACHE.get(params);
    CrcEngine engine = ref != null ? ref.get() : null;

    if (engine != null) {
      return engine;
    }

    // 创建新引擎
    engine = new CrcEngine(params);

    // 限制缓存大小
    if (ENGINE_CACHE.size() >= MAX_CACHE_SIZE) {
      // 简单策略：随机移除一个条目
      // 实际生产环境可以使用 LRU 策略
      ENGINE_CACHE.clear();
    }

    // 放入缓存
    ENGINE_CACHE.put(params, new java.lang.ref.SoftReference<>(engine));
    return engine;
  }

  /** 计算 CRC（便捷方法） */
  public static long calculate(CrcParams params, byte[] data) {
    return getEngine(params).calculate(data);
  }

  /** 计算 CRC-32（最常用） */
  public static long crc32(byte[] data) {
    return getEngine(CRC32).calculate(data);
  }

  /** 计算 CRC-16 */
  public static long crc16(byte[] data) {
    return getEngine(CRC16).calculate(data);
  }

  /** 计算 CRC-8 */
  public static long crc8(byte[] data) {
    return getEngine(CRC8).calculate(data);
  }

  /** 计算 CRC-32C（Castagnoli） */
  public static long crc32c(byte[] data) {
    return getEngine(CRC32C).calculate(data);
  }

  /** 清空缓存 */
  public static void clearCache() {
    ENGINE_CACHE.clear();
  }

  /** 获取当前缓存大小 */
  public static int getCacheSize() {
    return ENGINE_CACHE.size();
  }
}
