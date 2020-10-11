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

import com.securityinnovation.jNeo.CiphertextBadLengthException;
import com.securityinnovation.jNeo.DecryptionFailureException;
import com.securityinnovation.jNeo.FormatNotSupportedException;
import com.securityinnovation.jNeo.NoPrivateKeyException;
import com.securityinnovation.jNeo.NtruException;
import com.securityinnovation.jNeo.OID;
import com.securityinnovation.jNeo.ObjectClosedException;
import com.securityinnovation.jNeo.ParamSetNotSupportedException;
import com.securityinnovation.jNeo.PlaintextBadLengthException;
import com.securityinnovation.jNeo.Random;
import com.securityinnovation.jNeo.ntruencrypt.KeyParams;
import com.securityinnovation.jNeo.ntruencrypt.NtruEncryptKey;

/**
 * @author William.d.zk
 */
public class NtruUtil
{
    private static boolean LOADED_LIB = false;
    private static int     CIPHER_BUFF_LEN;
    private static int     PLAIN_TEXT_MAX;
    private static OID     oid        = OID.ees401ep1;

    static
    {
        try
        {
            LOADED_LIB = true;
            switch (oid)
            {
                case ees401ep1:
                    CIPHER_BUFF_LEN = 552;
                    PLAIN_TEXT_MAX = 60;
                    break;
                default:
                    break;
            }
            KeyParams.getKeyParams(oid);
        }
        catch (Throwable e)
        {
            // Ignore
        }

    }

    public byte[] getCipherBuf()
    {
        return new byte[CIPHER_BUFF_LEN];
    }

    public byte[][] getKeys(byte[] seed) throws NtruException
    {
        Random         prng = new Random(seed);
        NtruEncryptKey key  = NtruEncryptKey.genKey(oid, prng);
        return new byte[][] { key.getPubKey(),
                              key.getPrivKey()

        };
    }

    public byte[] encrypt(byte[] message, byte[] pubKey)
    {// 客户端用的
        byte[]           seed = new byte[32];
        java.util.Random rs   = new java.util.Random();
        rs.nextBytes(seed);
        Random prng = new Random(seed);

        try
        {
            NtruEncryptKey ntruKey = new NtruEncryptKey(pubKey);
            return ntruKey.encrypt(message, prng);
        }
        catch (FormatNotSupportedException |
               ParamSetNotSupportedException |
               ObjectClosedException |
               PlaintextBadLengthException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decrypt(byte[] cipher, byte[] priKey)
    {
        try
        {
            NtruEncryptKey ntruKey = new NtruEncryptKey(priKey);
            return ntruKey.decrypt(cipher);
        }
        catch (FormatNotSupportedException |
               ParamSetNotSupportedException |
               ObjectClosedException |
               NoPrivateKeyException |
               CiphertextBadLengthException |
               DecryptionFailureException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
