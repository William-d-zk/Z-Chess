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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anthony on 11.05.2017.
 */
public class CrcCalculator
{

    private final AlgoParams _Parameters;
    private byte             hashSize = 8;
    private long             mask     = 0xFFFFFFFFFFFFFFFFL;
    private final long[]     _table   = new long[256];

    CrcCalculator(AlgoParams params)
    {
        _Parameters = params;

        hashSize = (byte) params.HashSize;
        if (hashSize < 64) {
            mask = (1L << hashSize) - 1;
        }

        CreateTable();
    }

    public long Calc(byte[] data, int offset, int length)
    {
        long init = _Parameters.RefOut ? CrcHelper.ReverseBits(_Parameters.Init, hashSize): _Parameters.Init;
        long hash = ComputeCrc(init, data, offset, length);
        return (hash ^ _Parameters.XorOut) & mask;
    }

    private long ComputeCrc(long init, byte[] data, int offset, int length)
    {
        long crc = init;

        if (_Parameters.RefOut) {
            for (int i = offset; i < offset + length; i++) {
                crc = (_table[(int) ((crc ^ data[i]) & 0xFF)] ^ (crc >>> 8));
                crc &= mask;
            }
        }
        else {
            int toRight = (hashSize - 8);
            toRight = Math.max(toRight, 0);
            for (int i = offset; i < offset + length; i++) {
                crc = (_table[(int) (((crc >> toRight) ^ data[i]) & 0xFF)] ^ (crc << 8));
                crc &= mask;
            }
        }

        return crc;
    }

    private void CreateTable()
    {
        for (int i = 0; i < _table.length; i++)
            _table[i] = CreateTableEntry(i);
    }

    private long CreateTableEntry(int index)
    {
        long r = index;

        if (_Parameters.RefIn) r = CrcHelper.ReverseBits(r, hashSize);
        else if (hashSize > 8) r <<= (hashSize - 8);

        long lastBit = (1L << (hashSize - 1));

        for (int i = 0; i < 8; i++) {
            if ((r & lastBit) != 0) r = ((r << 1) ^ _Parameters.Poly);
            else r <<= 1;
        }

        if (_Parameters.RefOut) r = CrcHelper.ReverseBits(r, hashSize);

        return r & mask;
    }

    private static Map<AlgoParams,
                       CrcCalculator> _CalcMap = new HashMap<>();

    public static long calc(AlgoParams algo, byte[] data)
    {
        CrcCalculator calculator = _CalcMap.computeIfAbsent(algo, CrcCalculator::new);
        return calculator.Calc(data, 0, data.length);
    }

    //@formatter:off
    private static byte[] table_byte = {
            0x0, 0x7, 0xe, 0x9, 0x5, 0x2, 0xb, 0xc, 0xa, 0xd, 0x4, 0x3, 0xf, 0x8, 0x1, 0x6,
            0xd, 0xa, 0x3, 0x4, 0x8, 0xf, 0x6, 0x1, 0x7, 0x0, 0x9, 0xe, 0x2, 0x5, 0xc, 0xb,
            0x3, 0x4, 0xd, 0xa, 0x6, 0x1, 0x8, 0xf, 0x9, 0xe, 0x7, 0x0, 0xc, 0xb, 0x2, 0x5,
            0xe, 0x9, 0x0, 0x7, 0xb, 0xc, 0x5, 0x2, 0x4, 0x3, 0xa, 0xd, 0x1, 0x6, 0xf, 0x8,
            0x6, 0x1, 0x8, 0xf, 0x3, 0x4, 0xd, 0xa, 0xc, 0xb, 0x2, 0x5, 0x9, 0xe, 0x7, 0x0,
            0xb, 0xc, 0x5, 0x2, 0xe, 0x9, 0x0, 0x7, 0x1, 0x6, 0xf, 0x8, 0x4, 0x3, 0xa, 0xd,
            0x5, 0x2, 0xb, 0xc, 0x0, 0x7, 0xe, 0x9, 0xf, 0x8, 0x1, 0x6, 0xa, 0xd, 0x4, 0x3,
            0x8, 0xf, 0x6, 0x1, 0xd, 0xa, 0x3, 0x4, 0x2, 0x5, 0xc, 0xb, 0x7, 0x0, 0x9, 0xe,
            0xc, 0xb, 0x2, 0x5, 0x9, 0xe, 0x7, 0x0, 0x6, 0x1, 0x8, 0xf, 0x3, 0x4, 0xd, 0xa,
            0x1, 0x6, 0xf, 0x8, 0x4, 0x3, 0xa, 0xd, 0xb, 0xc, 0x5, 0x2, 0xe, 0x9, 0x0, 0x7,
            0xf, 0x8, 0x1, 0x6, 0xa, 0xd, 0x4, 0x3, 0x5, 0x2, 0xb, 0xc, 0x0, 0x7, 0xe, 0x9,
            0x2, 0x5, 0xc, 0xb, 0x7, 0x0, 0x9, 0xe, 0x8, 0xf, 0x6, 0x1, 0xd, 0xa, 0x3, 0x4,
            0xa, 0xd, 0x4, 0x3, 0xf, 0x8, 0x1, 0x6, 0x0, 0x7, 0xe, 0x9, 0x5, 0x2, 0xb, 0xc,
            0x7, 0x0, 0x9, 0xe, 0x2, 0x5, 0xc, 0xb, 0xd, 0xa, 0x3, 0x4, 0x8, 0xf, 0x6, 0x1,
            0x9, 0xe, 0x7, 0x0, 0xc, 0xb, 0x2, 0x5, 0x3, 0x4, 0xd, 0xa, 0x6, 0x1, 0x8, 0xf,
            0x4, 0x3, 0xa, 0xd, 0x1, 0x6, 0xf, 0x8, 0xe, 0x9, 0x0, 0x7, 0xb, 0xc, 0x5, 0x2
    };
    //@formatter:on
    public static long calc4itu(byte[] data)
    {
        if (data == null) { return 0; }
        int pos = 0;
        byte crc = 0;
        int len = data.length;
        while (len > 0) {
            crc = table_byte[(crc ^ data[pos++]) & 0xFF];
            len--;
        }
        return crc;
    }
}
