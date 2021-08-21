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

import org.junit.jupiter.api.Test;

/**
 * @author william.d.zk
 * 
 * @date 2020/7/12
 */
class CryptoUtilTest
{

    private CryptoUtil cryptoUtil = new CryptoUtil();

    @Test
    void randomPassword()
    {
        for (int i = 0; i < 100; i++) {
            String passwd = cryptoUtil.randomPassword(17, 31);
            System.out.println(passwd);
        }
    }

    @Test
    void crcTest()
    {
        byte[] input = IoUtil.hex2bin("ABC0");
        int crc16 = CryptoUtil.crc16(input, 0, input.length);
        System.out.println(IoUtil.long2Hex(IoUtil.swapLhb(crc16), ":"));
        int crc32 = CryptoUtil.crc32(input, 0, input.length);
        System.out.println(IoUtil.long2Hex(crc32, ":"));
    }
}