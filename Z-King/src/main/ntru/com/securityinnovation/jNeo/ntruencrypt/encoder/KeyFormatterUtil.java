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

package com.securityinnovation.jNeo.ntruencrypt.encoder;

import com.securityinnovation.jNeo.ParamSetNotSupportedException;
import com.securityinnovation.jNeo.math.BitPack;
import com.securityinnovation.jNeo.math.FullPolynomial;
import com.securityinnovation.jNeo.ntruencrypt.KeyParams;

public class KeyFormatterUtil
{
    static int fillHeader(byte tag, byte[] oid, byte[] out)
    {
        if (out != null) {
            out[0] = tag;
            System.arraycopy(oid, 0, out, 1, oid.length);
        }
        return 1 + oid.length;
    }

    static KeyParams parseOID(byte[] keyBlob, int oidStartIndex, int oidLen) throws ParamSetNotSupportedException
    {
        if (oidStartIndex
            + oidLen > keyBlob.length) throw new IllegalArgumentException("keyblob not large enough to hold OID");
        byte[] oid = new byte[oidLen];
        System.arraycopy(keyBlob, oidStartIndex, oid, 0, oidLen);
        return KeyParams.getKeyParams(oid);
    }

    static int getHeaderEndOffset(byte[] keyBlob)
    {
        // For all currently defined blobs, the header is
        // 1 byte tag
        // 3 byte OID.
        return 4;
    }

    static FullPolynomial recoverF(FullPolynomial f)
    {
        FullPolynomial F = new FullPolynomial(f.p.length);
        F.p[0] = (short) ((f.p[0] - 1) / 3);
        for (int i = 1; i < f.p.length; i++)
            F.p[i] = (short) (f.p[i] / 3);
        return F;
    }

    static public byte[] packListedCoefficients(FullPolynomial F, int numOnes, int numNegOnes)
    {
        int len = packListedCoefficients(F, numOnes, numNegOnes, null, 0);
        byte[] b = new byte[len];
        packListedCoefficients(F, numOnes, numNegOnes, b, 0);
        return b;
    }

    static public int packListedCoefficients(FullPolynomial F, int numOnes, int numNegOnes, byte[] out, int offset)
    {
        if (out == null) return BitPack.pack(numOnes + numNegOnes, F.p.length);

        short[] coefficients = new short[numOnes + numNegOnes];
        int ones = 0, negOnes = numOnes;
        for (int i = 0; i < F.p.length; i++)
            if (F.p[i] == 1) coefficients[ones++] = (short) i;
            else if (F.p[i] == -1) coefficients[negOnes++] = (short) i;
        int len = BitPack.pack(numOnes + numNegOnes, F.p.length, coefficients, 0, out, offset);
        java.util.Arrays.fill(coefficients, (short) 0);
        return len;
    }

    static public int unpackListedCoefficients(FullPolynomial F,
                                               int N,
                                               int numOnes,
                                               int numNegOnes,
                                               byte[] in,
                                               int offset)
    {
        short[] coefficients = new short[numOnes + numNegOnes];
        int len = BitPack.unpack(coefficients.length, N, in, offset, coefficients, 0);
        java.util.Arrays.fill(F.p, (short) 0);
        for (int i = 0; i < numOnes; i++)
            F.p[coefficients[i]] = 1;
        for (int i = numOnes; i < coefficients.length; i++)
            F.p[coefficients[i]] = -1;
        java.util.Arrays.fill(coefficients, (short) 0);
        return len;
    }
}
