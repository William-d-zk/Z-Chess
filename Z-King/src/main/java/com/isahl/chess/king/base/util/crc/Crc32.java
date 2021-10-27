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

package com.isahl.chess.king.base.util.crc;

/**
 * Created by anthony on 15.05.2017.
 */
public class Crc32
{
    public static AlgoParams Crc32       = new AlgoParams("CRC-32",
                                                          32,
                                                          0x04C11DB7L,
                                                          0xFFFFFFFFL,
                                                          true,
                                                          true,
                                                          0xFFFFFFFFL,
                                                          0xCBF43926L);
    public static AlgoParams Crc32Bzip2  = new AlgoParams("CRC-32/BZIP2",
                                                          32,
                                                          0x04C11DB7L,
                                                          0xFFFFFFFFL,
                                                          false,
                                                          false,
                                                          0xFFFFFFFFL,
                                                          0xFC891918L);
    public static AlgoParams Crc32C      = new AlgoParams("CRC-32C",
                                                          32,
                                                          0x1EDC6F41L,
                                                          0xFFFFFFFFL,
                                                          true,
                                                          true,
                                                          0xFFFFFFFFL,
                                                          0xE3069283L);
    public static AlgoParams Crc32D      = new AlgoParams("CRC-32D",
                                                          32,
                                                          0xA833982BL,
                                                          0xFFFFFFFFL,
                                                          true,
                                                          true,
                                                          0xFFFFFFFFL,
                                                          0x87315576L);
    public static AlgoParams Crc32Jamcrc = new AlgoParams("CRC-32/JAMCRC",
                                                          32,
                                                          0x04C11DB7L,
                                                          0xFFFFFFFFL,
                                                          true,
                                                          true,
                                                          0x00000000L,
                                                          0x340BC6D9L);
    public static AlgoParams Crc32Mpeg2  = new AlgoParams("CRC-32/MPEG-2",
                                                          32,
                                                          0x04C11DB7L,
                                                          0xFFFFFFFFL,
                                                          false,
                                                          false,
                                                          0x00000000L,
                                                          0x0376E6E7L);
    public static AlgoParams Crc32Posix  = new AlgoParams("CRC-32/POSIX",
                                                          32,
                                                          0x04C11DB7L,
                                                          0x00000000L,
                                                          false,
                                                          false,
                                                          0xFFFFFFFFL,
                                                          0x765E7680L);
    public static AlgoParams Crc32Q      = new AlgoParams("CRC-32Q",
                                                          32,
                                                          0x814141ABL,
                                                          0x00000000L,
                                                          false,
                                                          false,
                                                          0x00000000L,
                                                          0x3010BF7FL);
    public static AlgoParams Crc32Xfer   = new AlgoParams("CRC-32/XFER",
                                                          32,
                                                          0x000000AFL,
                                                          0x00000000L,
                                                          false,
                                                          false,
                                                          0x00000000L,
                                                          0xBD0BE338L);

    public static final AlgoParams[] Params = new AlgoParams[]{
            Crc32,
            Crc32Bzip2,
            Crc32C,
            Crc32D,
            Crc32Jamcrc,
            Crc32Mpeg2,
            Crc32Posix,
            Crc32Q,
            Crc32Xfer
    };
}