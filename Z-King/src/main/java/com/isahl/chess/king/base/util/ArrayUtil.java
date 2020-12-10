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
package com.isahl.chess.king.base.util;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author William.d.zk
 */
public interface ArrayUtil
{
    static long[] setFiFoAdd(long _l, long[] a)
    {
        if (a == null) { return new long[]{_l}; }
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
        if (a == null) { return new long[]{_l}; }
        int pos = Arrays.binarySearch(a, _l);
        return pos >= 0 ? a: setAdd(_l, a, -1 - pos);
    }

    static long[] setSortAdd(long _l, long[] a, long prefix)
    {
        if (a == null) { return new long[]{_l}; }
        int pos = binarySearch0(a, _l, prefix);
        return pos >= 0 ? a: setAdd(_l, a, -1 - pos);
    }

    static long[] setAdd(long _l, long[] a, int pos)
    {
        if (a == null) { return new long[]{_l}; }
        long[] t = new long[a.length + 1];
        if (pos > a.length) pos = a.length;
        if (pos > 0 && pos < a.length) {
            arraycopy(a, 0, t, 0, pos);
            arraycopy(a, pos, t, pos + 1, a.length - pos);
        }
        else if (pos == 0) arraycopy(a, 0, t, 1, a.length);
        else arraycopy(a, 0, t, 0, a.length);
        t[pos] = _l;
        return t;
    }

    static int binarySearch0(long[] a, long key, long prefix)
    {
        Objects.requireNonNull(a);
        int low = 0;
        int high = a.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid] & prefix;

            if (midVal < key) low = mid + 1;
            else if (midVal > key) high = mid - 1;
            else return mid; // key found
        }
        return -(low + 1);  // key not found.
    }

    static long[] setNoZeroFiFoRm(long _l, final long[] a)
    {
        Objects.requireNonNull(a);
        if (_l != 0) {
            int k = -1;
            for (int i = 0; i < a.length; i++) {
                if (a[i] == _l) {
                    k = i;
                    break;
                }
            }
            if (k < 0) return a;
            return newArray(a, k);
        }
        return a;
    }

    static long[] setNoZeroSortRm(long _l, final long[] a)
    {
        if (a == null) { return null; }
        if (_l != 0) {
            int k = Arrays.binarySearch(a, _l);
            if (k < 0) return a;
            switch (a.length)
            {
                case 0, 1 ->
                    {
                        return null;
                    }
                case 2 ->
                    {
                        return k == 0 ? new long[]{a[1]}: new long[]{a[0]};
                    }
                default ->
                    {
                        return newArray(a, k);
                    }
            }
        }
        return a;
    }

    private static long[] newArray(long[] a, int k)
    {
        long[] t = new long[a.length - 1];
        if (k > 0 && k < a.length - 1) {
            arraycopy(a, 0, t, 0, k);
            arraycopy(a, k + 1, t, k, a.length - k - 1);
        }
        else if (k == 0) arraycopy(a, 1, t, 0, t.length);
        else arraycopy(a, 0, t, 0, t.length);
        return t;
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
