/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

    public byte[] encode(KeyParams keyParams, FullPolynomial h, FullPolynomial f)
    {
        // Sanity-check inputs
        if ((h.p.length != keyParams.N) || (f.p.length != keyParams.N)) return null;

        // Convert f to a listed F.
        FullPolynomial F      = KeyFormatterUtil.recoverF(f);

        // Allocate output buffer
        int            len    = (KeyFormatterUtil.fillHeader(tag, keyParams.OIDBytes, null)
                                 + BitPack.pack(keyParams.N, keyParams.q)
                                 + BitPack.pack(2 * keyParams.df, keyParams.N));
        byte           ret[]  = new byte[len];

        // Encode the output
        int            offset = KeyFormatterUtil.fillHeader(tag, keyParams.OIDBytes, ret);
        offset += BitPack.pack(keyParams.N, keyParams.q, h.p, 0, ret, offset);
        offset += KeyFormatterUtil.packListedCoefficients(F, keyParams.df, keyParams.df, ret, offset);
        return ret;
    }

    public RawKeyData decode(byte keyBlob[]) throws ParamSetNotSupportedException
    {
        // Parse the header, recover the key parameters.
        if (keyBlob[0] != tag) throw new IllegalArgumentException("key blob tag not recognized");
        KeyParams keyParams  = KeyFormatterUtil.parseOID(keyBlob, 1, 3);

        // Make sure the input will be fully consumed
        int       headerLen  = KeyFormatterUtil.getHeaderEndOffset(keyBlob);
        int       packedHLen = BitPack.unpack(keyParams.N, keyParams.q);
        int       listedFLen = BitPack.unpack(2 * keyParams.df, keyParams.N);
        if (headerLen + packedHLen + listedFLen != keyBlob.length) throw new IllegalArgumentException("blob length invalid");

        // Recover h
        int            offset = headerLen;
        FullPolynomial h      = new FullPolynomial(keyParams.N);
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
