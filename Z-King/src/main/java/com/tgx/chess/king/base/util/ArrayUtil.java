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
package com.tgx.chess.king.base.util;

import static java.lang.System.arraycopy;

import java.util.Arrays;

/**
 * @author William.d.zk
 */
public interface ArrayUtil
{
    static long[] setFiFoAdd(long _l, long[] a)
    {
        if (a == null) throw new NullPointerException();
        int i = 0, size = a.length;
        for (; i < size; i++) {
            if (a[i] == _l) return a;
            else if (a[i] == 0 || i == size - 1) break;
        }
        long o = a[i];
        if (o > 0) {
            long[] t = new long[size + 1];
            arraycopy(a, 0, t, 1, size);
            t[0] = _l;
            return t;
        }
        else {
            a[0] = _l;
            return a;
        }
    }

    static long[] setSortAdd(long _l, long[] a)
    {
        if (a == null) throw new NullPointerException();
        int pos = Arrays.binarySearch(a, _l);
        if (pos >= 0) return a;
        else {
            pos = -1 - pos;
            long[] t = new long[a.length + 1];
            t[pos] = _l;
            if (pos > 0 && pos < a.length) {
                arraycopy(a, 0, t, 0, pos);
                arraycopy(a, pos, t, pos + 1, a.length - pos);
            }
            else if (pos == 0) arraycopy(a, 0, t, 1, a.length);
            else arraycopy(a, 0, t, 0, a.length);
            return t;
        }
    }

    static long[] setNoZeroFiFoRm(long _l, final long[] a)
    {
        if (a == null) throw new NullPointerException();
        if (_l != 0) {
            int k = -1;
            for (int i = 0; i < a.length; i++) {
                if (a[i] == _l) {
                    k = i;
                    break;
                }
            }
            if (k < 0) return a;

            long[] t = new long[a.length - 1];
            if (k > 0 && k < a.length - 1) {
                arraycopy(a, 0, t, 0, k);
                arraycopy(a, k + 1, t, k, a.length - k - 1);
            }
            else if (k == 0) arraycopy(a, 1, t, 0, t.length);
            else arraycopy(a, 0, t, 0, t.length);
            return t;
        }
        return a;
    }

    static long[] setNoZeroSortRm(long _l, final long[] a)
    {
        if (a == null) throw new NullPointerException();
        if (_l != 0) {
            int k = Arrays.binarySearch(a, _l);
            if (k < 0) return a;
            long[] t = new long[a.length - 1];
            if (k > 0 && k < a.length - 1) {
                arraycopy(a, 0, t, 0, k);
                arraycopy(a, k + 1, t, k, a.length - k - 1);
            }
            else if (k == 0) arraycopy(a, 1, t, 0, t.length);
            else arraycopy(a, 0, t, 0, t.length);
            return t;
        }
        return a;
    }

    static void swap(final long[] idx, int o, int p)
    {
        if (o == p) return;
        long t = idx[o];
        idx[o] = idx[p];
        idx[p] = t;
    }

    static void swap(int[] source, int a, int b)
    {
        int tmp = source[a];
        source[a] = source[b];
        source[b] = tmp;
    }

    static void swap(byte[] source, int a, int b)
    {
        byte tmp = source[a];
        source[a] = source[b];
        source[b] = tmp;
    }

    static String toHexString(long[] var0)
    {
        if (var0 == null) {
            return "null";
        }
        else {
            int var1 = var0.length - 1;
            if (var1 == -1) {
                return "[]";
            }
            else {
                StringBuilder var2 = new StringBuilder();
                var2.append('[');
                int var3 = 0;

                while (true) {
                    var2.append(Long.toHexString(var0[var3])
                                    .toUpperCase());
                    if (var3 == var1) { return var2.append(']')
                                                   .toString(); }

                    var2.append(", ");
                    ++var3;
                }
            }
        }
    }

    static String toHexString(int[] var0)
    {
        if (var0 == null) {
            return "null";
        }
        else {
            int var1 = var0.length - 1;
            if (var1 == -1) {
                return "[]";
            }
            else {
                StringBuilder var2 = new StringBuilder();
                var2.append('[');
                int var3 = 0;

                while (true) {
                    var2.append(Integer.toHexString(var0[var3])
                                       .toUpperCase());
                    if (var3 == var1) { return var2.append(']')
                                                   .toString(); }

                    var2.append(", ");
                    ++var3;
                }
            }
        }
    }

    static String toHexString(byte[] var0)
    {
        if (var0 == null) {
            return "null";
        }
        else {
            int var1 = var0.length - 1;
            if (var1 == -1) {
                return "[]";
            }
            else {
                StringBuilder var2 = new StringBuilder();
                var2.append('[');
                int var3 = 0;

                while (true) {
                    var2.append(Integer.toHexString(var0[var3] & 0xFF)
                                       .toUpperCase());
                    if (var3 == var1) { return var2.append(']')
                                                   .toString(); }

                    var2.append(", ");
                    ++var3;
                }
            }
        }
    }
}
