/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.z.queen.base.crypt.util;

import java.nio.ByteBuffer;
import java.util.Random;

import com.tgx.z.queen.base.crypt.inf.ISymmetric;
import com.tgx.z.queen.base.util.ArrayUtil;

/**
 * @author William.d.zk
 */
public class Rc4
        implements
        ISymmetric
{
    private final byte[] S = new byte[256];
    private int          i, j;
    private boolean      initialized;

    public static byte[] decrypt(byte[] data, byte[] key) {
        return rc4(data, key);
    }

    public static byte[] encrypt(byte[] data, byte[] key) {
        return rc4(data, key);
    }

    private static byte[] rc4(byte[] data, byte[] key) {
        if (!isKeyValid(key)) throw new IllegalArgumentException("key is fail!");
        if (data.length < 1) throw new IllegalArgumentException("data is fail!");
        int[] S = new int[256];

        // KSA
        for (int i = 0; i < S.length; i++)
            S[i] = i;
        int j = 0;
        for (int i = 0; i < S.length; i++) {
            j = (j + S[i] + (key[i % key.length] & 0xFF)) & 0xFF;
            ArrayUtil.swap(S, i, j);
        }

        // PRGA
        int i = 0;
        j = 0;

        byte[] encodeData = new byte[data.length];

        for (int x = 0; x < encodeData.length; x++) {
            i = (i + 1) & 0xFF;
            j = (j + S[i]) & 0xFF;
            ArrayUtil.swap(S, i, j);
            int k = S[(S[i] + S[j]) & 0xFF];
            int K = k;
            encodeData[x] = (byte) (data[x] ^ K);
        }
        return encodeData;
    }

    public static boolean isKeyValid(byte[] key) {
        byte[] bKey = key;
        int len = bKey.length;
        int num = 0;// 0x0E计数
        if (len > 0 && len <= 256) {
            for (int i = 0; i < len; i++) {
                if ((bKey[i] & 0xFF) == 0x0E) {
                    num++;
                    if (num > 3) return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public byte[] createKey(String seed) {
        long curTime = System.currentTimeMillis();
        long code = hashCode();
        long tick = curTime ^ (code << 31);
        Random rd = new Random(tick);
        byte[] xc;
        if (seed == null || "".equals(seed.trim())) seed = "Tgx.Tina.Rc4" + System.nanoTime();
        xc = seed.getBytes();
        byte[] key = new byte[20];
        for (int i = 0, j = 1; i < key.length; i++) {
            for (byte b : xc) {
                long dx = System.nanoTime() ^ tick ^ rd.nextLong() ^ b;
                key[i] ^= dx >> j++;
                if (j > 40) j = 1;
            }
        }
        return key;
    }

    private void ksa(byte[] key) {
        if (initialized || key == null) return;
        for (int i = 0; i < S.length; i++)
            S[i] = (byte) i;
        for (int i = 0, j = 0; i < S.length; i++) {
            j = (j + (S[i] & 0xFF) + (key[i % key.length] & 0xFF)) & 0xFF;
            ArrayUtil.swap(S, i, j);
        }
        initialized = true;
    }

    @Override
    public void digest(ByteBuffer buffer, byte[] key) {
        if (!buffer.hasRemaining() || key == null) return;
        ksa(key);
        int limit = buffer.limit();
        for (int x = buffer.position(); x < limit; x++) {
            i = ((i + 1) & Integer.MAX_VALUE) & 0xFF;
            j = ((j + (S[i] & 0xFF)) & Integer.MAX_VALUE) & 0xFF;
            ArrayUtil.swap(S, i, j);
            int k = S[((S[i] & 0xFF) + (S[j] & 0xFF)) & 0xFF] & 0xFF;
            buffer.put(x, (byte) (buffer.get(x) ^ k));
        }
    }

    @Override
    public void digest(byte[] dst, byte[] key) {
        if (dst == null || key == null) return;
        ksa(key);
        int limit = dst.length;
        for (int x = 0; x < limit; x++) {
            i = ((i + 1) & Integer.MAX_VALUE) & 0xFF;
            j = ((j + (S[i] & 0xFF)) & Integer.MAX_VALUE) & 0xFF;
            ArrayUtil.swap(S, i, j);
            int k = S[((S[i] & 0xFF) + (S[j] & 0xFF)) & 0xFF] & 0xFF;
            dst[x] = (byte) (dst[x] ^ k);
        }
    }

    @Override
    public final void reset() {
        initialized = false;
    }

}
