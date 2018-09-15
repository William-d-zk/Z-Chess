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

class PrivKeyFormatter_PrivateKeyListedFv1
        implements
        PrivKeyFormatter
{
    private final static byte tag = NtruEncryptKeyNativeEncoder.PRIVATE_KEY_DEFAULT_v1;

    public byte[] encode(KeyParams keyParams, FullPolynomial h, FullPolynomial f) {
        // Sanity-check inputs
        if ((h.p.length != keyParams.N) || (f.p.length != keyParams.N)) return null;

        // Convert f to a listed F.
        FullPolynomial F = KeyFormatterUtil.recoverF(f);

        // Allocate output buffer
        int len = (KeyFormatterUtil.fillHeader(tag, keyParams.OIDBytes, null)
                   + BitPack.pack(keyParams.N, keyParams.q)
                   + BitPack.pack(2 * keyParams.df, keyParams.N));
        byte ret[] = new byte[len];

        // Encode the output
        int offset = KeyFormatterUtil.fillHeader(tag, keyParams.OIDBytes, ret);
        offset += BitPack.pack(keyParams.N, keyParams.q, h.p, 0, ret, offset);
        offset += KeyFormatterUtil.packListedCoefficients(F, keyParams.df, keyParams.df, ret, offset);
        return ret;
    }

    public RawKeyData decode(byte keyBlob[]) throws ParamSetNotSupportedException {
        // Parse the header, recover the key parameters.
        if (keyBlob[0] != tag) throw new IllegalArgumentException("key blob tag not recognized");
        KeyParams keyParams = KeyFormatterUtil.parseOID(keyBlob, 1, 3);

        // Make sure the input will be fully consumed
        int headerLen = KeyFormatterUtil.getHeaderEndOffset(keyBlob);
        int packedHLen = BitPack.unpack(keyParams.N, keyParams.q);
        int listedFLen = BitPack.unpack(2 * keyParams.df, keyParams.N);
        if (headerLen + packedHLen + listedFLen != keyBlob.length) throw new IllegalArgumentException("blob length invalid");

        // Recover h
        int offset = headerLen;
        FullPolynomial h = new FullPolynomial(keyParams.N);
        offset += BitPack.unpack(keyParams.N, keyParams.q, keyBlob, offset, h.p, 0);

        // Recover F
        FullPolynomial f = new FullPolynomial(keyParams.N);
        offset += KeyFormatterUtil.unpackListedCoefficients(f, keyParams.N, keyParams.df, keyParams.df, keyBlob, offset);
        // Compute f = 1+p*F
        for (int i = 0; i < f.p.length; i++)
            f.p[i] *= keyParams.p;
        f.p[0]++;

        // Return the key material
        return new RawKeyData(keyParams, h, f);
    }
}
