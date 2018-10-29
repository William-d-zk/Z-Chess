/*
 * NTRU Cryptography Reference Source Code
 *  Copyright (c) 2009-2013, by Security Innovation, Inc. All rights reserved
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301,
 *  USA.
 */

package com.securityinnovation.jNeo.ntruencrypt.encoder;

import com.securityinnovation.jNeo.ParamSetNotSupportedException;
import com.securityinnovation.jNeo.math.BitPack;
import com.securityinnovation.jNeo.math.FullPolynomial;
import com.securityinnovation.jNeo.ntruencrypt.KeyParams;

public class KeyFormatterUtil
{
    static int fillHeader(byte tag, byte oid[], byte out[])
    {
        if (out != null) {
            out[0] = tag;
            System.arraycopy(oid, 0, out, 1, oid.length);
        }
        return 1 + oid.length;
    }

    static KeyParams parseOID(byte keyBlob[], int oidStartIndex, int oidLen) throws ParamSetNotSupportedException
    {
        if (oidStartIndex + oidLen > keyBlob.length) throw new IllegalArgumentException("keyblob not large enough to hold OID");
        byte oid[] = new byte[oidLen];
        System.arraycopy(keyBlob, oidStartIndex, oid, 0, oidLen);
        return KeyParams.getKeyParams(oid);
    }

    static int getHeaderEndOffset(byte keyBlob[])
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
        int  len = packListedCoefficients(F, numOnes, numNegOnes, null, 0);
        byte b[] = new byte[len];
        packListedCoefficients(F, numOnes, numNegOnes, b, 0);
        return b;
    }

    static public int packListedCoefficients(FullPolynomial F, int numOnes, int numNegOnes, byte out[], int offset)
    {
        if (out == null) return BitPack.pack(numOnes + numNegOnes, F.p.length);

        short coefficients[] = new short[numOnes + numNegOnes];
        int   ones           = 0, negOnes = numOnes;
        for (int i = 0; i < F.p.length; i++)
            if (F.p[i] == 1) coefficients[ones++] = (short) i;
            else if (F.p[i] == -1) coefficients[negOnes++] = (short) i;
        int len = BitPack.pack(numOnes + numNegOnes, F.p.length, coefficients, 0, out, offset);
        java.util.Arrays.fill(coefficients, (short) 0);
        return len;
    }

    static public int unpackListedCoefficients(FullPolynomial F, int N, int numOnes, int numNegOnes, byte in[], int offset)
    {
        short coefficients[] = new short[numOnes + numNegOnes];
        int   len            = BitPack.unpack(coefficients.length, N, in, offset, coefficients, 0);
        java.util.Arrays.fill(F.p, (short) 0);
        for (int i = 0; i < numOnes; i++)
            F.p[coefficients[i]] = 1;
        for (int i = numOnes; i < coefficients.length; i++)
            F.p[coefficients[i]] = -1;
        java.util.Arrays.fill(coefficients, (short) 0);
        return len;
    }
}
