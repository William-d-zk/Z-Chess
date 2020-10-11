/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

public class Crc64
{
    public static AlgoParams         Crc64   = new AlgoParams("CRC-64",
                                                              64,
                                                              0x42F0E1EBA9EA3693L,
                                                              0x00000000L,
                                                              false,
                                                              false,
                                                              0x00000000L,
                                                              0x6C40DF5F0B497347L);
    public static AlgoParams         Crc64We = new AlgoParams("CRC-64/WE",
                                                              64,
                                                              0x42F0E1EBA9EA3693L,
                                                              0xFFFFFFFFFFFFFFFFL,
                                                              false,
                                                              false,
                                                              0xFFFFFFFFFFFFFFFFL,
                                                              0x62EC59E3F1A4F00AL);
    public static AlgoParams         Crc64Xz = new AlgoParams("CRC-64/XZ",
                                                              64,
                                                              0x42F0E1EBA9EA3693L,
                                                              0xFFFFFFFFFFFFFFFFL,
                                                              true,
                                                              true,
                                                              0xFFFFFFFFFFFFFFFFL,
                                                              0x995DC9BBDF1939FAL);

    public static final AlgoParams[] Params  = new AlgoParams[] { Crc64,
                                                                  Crc64We,
                                                                  Crc64Xz
    };
}
