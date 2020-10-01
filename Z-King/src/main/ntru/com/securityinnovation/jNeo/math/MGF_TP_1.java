/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.securityinnovation.jNeo.math;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class implements the MGF-TP-1 algorithm for converting a byte
 * stream into a sequence of trits. It implements both the forward
 * direction (bytes -> trits as defined in X9.98) and the reverse
 * (trits -> bytes).
 */
public class MGF_TP_1
{
    /**
     * Generate a trinomial of degree N using the MGF-TP-1 algorithm
     * to convert the InputStream of bytes into trits.
     *
     * @param N
     *            the degree of the new polynomial.
     * @param gen
     *            the byte sequence to be converted.
     * @return the derived trinomial.
     */
    public static FullPolynomial genTrinomial(int N, InputStream gen)
    {
        try {
            FullPolynomial p = new FullPolynomial(N);

            int limit = 5 * (N / 5);
            int i = 0;
            while (i < limit) {
                int o = gen.read();
                if (o >= 243) continue;

                int b;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
            }

            while (i < N) {
                int o = gen.read();
                if (o >= 243) continue;

                int b;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                if (i == N) break;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                if (i == N) break;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                if (i == N) break;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                if (i == N) break;
                b = (o % 3);
                p.p[i++] = (byte) b;
                o = (o - b) / 3;
                if (i == N) break;
            }

            // Renormalize from [0..2] to [-1..1]
            for (i = 0; i < p.p.length; i++)
                if (p.p[i] == 2) p.p[i] = -1;

            return p;
        }
        catch (IOException e) {
            throw new InternalError("MGF-TP-1 byte source was unable to generate input");
        }
    }

    /**
     * Given a trit in the range [0..2], return a trit in
     * the range [-1..1] that is equal to the input mod 3.
     */
    private final static byte recenterTritTo0(short in)
    {
        if (in == -1) return 2;
        return (byte) (in);
    }

    /**
     * Generate a byte stream that is the encoding of a trinomial.
     * The byte stream will have the property that when it is run
     * through the MGF-TP-1 algorithm it will recover the original
     * trinomial.
     *
     * @param poly
     *            the trinomial. All coefficients must be in the range [0..2].
     * @param out
     *            the output stream that will collect the input.
     */
    public static void encodeTrinomial(FullPolynomial poly, OutputStream out)
    {
        try {
            int N = poly.p.length;
            int end, accum;
            // Encode 5 trits per byte, as long as we have >= 5 trits.
            for (end = 5; end <= N; end += 5) {
                accum = recenterTritTo0(poly.p[end - 1]);
                accum = 3 * accum + recenterTritTo0(poly.p[end - 2]);
                accum = 3 * accum + recenterTritTo0(poly.p[end - 3]);
                accum = 3 * accum + recenterTritTo0(poly.p[end - 4]);
                accum = 3 * accum + recenterTritTo0(poly.p[end - 5]);
                out.write(accum);
            }

            // Encode the remaining trits.
            end = N - (N % 5);
            if (end < N) {
                accum = recenterTritTo0(poly.p[--N]);
                while (end < N)
                    accum = 3 * accum + recenterTritTo0(poly.p[--N]);
                out.write(accum);
            }
        }
        catch (IOException e) {
            throw new InternalError("MGF-TP-1 byte sink was unable to process output");
        }
    }
}
