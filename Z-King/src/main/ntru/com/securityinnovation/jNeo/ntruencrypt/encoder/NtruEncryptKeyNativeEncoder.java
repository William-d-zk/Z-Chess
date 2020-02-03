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

import com.securityinnovation.jNeo.FormatNotSupportedException;
import com.securityinnovation.jNeo.ParamSetNotSupportedException;
import com.securityinnovation.jNeo.math.FullPolynomial;
import com.securityinnovation.jNeo.ntruencrypt.KeyParams;

public class NtruEncryptKeyNativeEncoder
        implements
        NtruEncryptKeyEncoder
{
    /**
     * The format for the public key.
     * Format 1 consists of the following:
     * 1 byte blob type tag == PUBLIC_KEY_v1
     * 1 byte OID length
     * <> bytes OID
     * <> bytes h, bit packed.
     */
    public final static byte PUBLIC_KEY_v1 = 1;

    /**
     * The format for the private key blob where the private
     * key is represented by the trinomial F, packed 5 trits per
     * output byte.
     * Format 1 consists of the following:
     * 1 byte blob type tag == PUBLIC_KEY_v1
     * 1 byte OID length
     * <> bytes OID
     * <> bytes h, bit packed.
     * <> bytes F, packed 5 trits per output byte:
     * out[i] = F[5*i]*3^4 + F[5*i+1]*3^3 + F[5*i+2]*3^2 +
     * F[5*i+3]*3 + F[5*i+4]
     */
    public final static byte PRIVATE_KEY_DEFAULT_v1 = 2;

    /**
     * The format for the private key blob where the private key is
     * represented by the trinomial F, represented as a list of
     * indices with coefficients of '1' (df of them) followed by a
     * list of indices with coefficients of '-1' (df of those). Each
     * index is <N, so it bit-packed into log2(N).
     */
    public final static byte PRIVATE_KEY_PACKED_F_v1 = (byte) 0xfe;
    public final static byte PRIVATE_KEY_LISTED_F_v1 = (byte) 0xff;

    /**
     * Encode a public key as a byte array.
     */
    public byte[] encodePubKey(KeyParams keyParams, FullPolynomial h)
    {
        PubKeyFormatter_PUBLIC_KEY_v1 formatter = new PubKeyFormatter_PUBLIC_KEY_v1();
        return formatter.encode(keyParams, h);
    }

    PrivKeyFormatter pickDefaultPrivKeyFormatter(KeyParams keyParams)
    {
        int packedFLength = (keyParams.N + 4) / 5;
        int packedListedFLength = (keyParams.df
                                   * 2
                                   * com.securityinnovation.jNeo.math.BitPack.countBits(keyParams.q + 7)
                                   / 8);
        if (packedFLength < packedListedFLength) return new PrivKeyFormatter_PrivateKeyPackedFv1();
        else return new PrivKeyFormatter_PrivateKeyListedFv1();
    }

    /**
     * Encode a private key as a byte array.
     */
    public byte[] encodePrivKey(KeyParams keyParams, FullPolynomial h, FullPolynomial f)
    {
        PrivKeyFormatter formatter = pickDefaultPrivKeyFormatter(keyParams);
        return formatter.encode(keyParams, h, f);
    }

    /**
     * Parse a public or private key blob.
     */
    public RawKeyData decodeKeyBlob(byte[] keyBlob) throws FormatNotSupportedException, ParamSetNotSupportedException
    {
        switch (keyBlob[0])
        {
            case (PUBLIC_KEY_v1):
            {
                PubKeyFormatter_PUBLIC_KEY_v1 formatter = new PubKeyFormatter_PUBLIC_KEY_v1();
                return formatter.decode(keyBlob);
            }
            case (PRIVATE_KEY_DEFAULT_v1):
            {
                PrivKeyFormatter formatter = pickDefaultPrivKeyFormatter(KeyFormatterUtil.parseOID(keyBlob, 1, 3));
                return formatter.decode(keyBlob);
            }
        }
        throw new FormatNotSupportedException(keyBlob[0]);
    }
}
