/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.bishop.io.ws.zchat.zcrypt;

import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.NtruUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.inf.IEncryptHandler;

import java.util.Random;

/**
 * @author William.d.zk
 */
public class EncryptHandler
        implements IEncryptHandler
{

    private final static int        KEY_PAIR_INDEX_PUBLIC_KEY = 0;
    private final static int        KEY_PAIR_INDEX_PASSWORD   = KEY_PAIR_INDEX_PUBLIC_KEY + 1;
    private final static int        KEY_PAIR_INDEX_TIME       = KEY_PAIR_INDEX_PASSWORD + 1;
    private final static int        KEY_PAIR_INDEX_VERSION    = KEY_PAIR_INDEX_TIME + 1;
    private final static int        PUBLIC_KEY_TIME_MAX       = 1 << 16;
    private final static int        _TotalSizeWidth           = 8;
    private final        int        _PairSize                 = 1 << _TotalSizeWidth;
    private final        int        _PairSizeMask             = _PairSize - 1;
    private final        int        _VersionWidth             = 12;
    private final        int        _VersionMask              = ((1 << _VersionWidth) - 1) << _TotalSizeWidth;
    private final        NtruUtil   _Ntru                     = new NtruUtil();
    private final        Random     _Random                   = new Random();
    private final byte[][][] _PublicKeyPair = new byte[_PairSize][][];
    private final CryptoUtil cryptoUtil     = new CryptoUtil();
    private       int        mIndexAdd;

    private boolean isPubKeyAvailable(int _ReqPubKeyId)
    {
        return _ReqPubKeyId >= 0;
    }

    private Pair<Integer, byte[][]> createPair(Random random, int _PubKeyId)
    {
        int saltWidth = _TotalSizeWidth + _VersionWidth;
        int saltMask = (0xFFFFFFFF << saltWidth) ^ 0x80000000;
        int keyIndex = _PubKeyId & _PairSizeMask;
        int keyVersion = (_PubKeyId & _VersionMask) >>> _TotalSizeWidth;
        int sequence;
        byte[][] keyPair = null;
        if(isPubKeyAvailable(_PubKeyId)) {
            keyPair = _PublicKeyPair[keyIndex];
        }
        boolean recreate = false;
        int curVersion = -1;
        int pubKeyId;
        if(keyPair != null) {
            sequence = IoUtil.readInt(keyPair[KEY_PAIR_INDEX_TIME], 0) + 1;
            curVersion = IoUtil.readUnsignedShort(keyPair[KEY_PAIR_INDEX_VERSION], 0);
            if(sequence < PUBLIC_KEY_TIME_MAX && keyVersion == curVersion) {
                IoUtil.writeInt(sequence, keyPair[KEY_PAIR_INDEX_TIME], 0);
                pubKeyId = (random.nextInt() << saltWidth) & saltMask | keyIndex | curVersion;
            }
            else {
                int nextVersion = (curVersion + 1) & (_VersionMask >>> _TotalSizeWidth);
                pubKeyId = (random.nextInt() << saltWidth) & saltMask | keyIndex | (nextVersion << _TotalSizeWidth);
                recreate = true;
            }
        }
        else {
            keyIndex = mIndexAdd++ & _PairSizeMask;
            keyPair = _PublicKeyPair[keyIndex];
            pubKeyId = (random.nextInt() << saltWidth) & saltMask | keyIndex;
            recreate = true;
        }
        if(recreate) {
            byte[] password = new byte[32];
            random.nextBytes(password);
            byte[] time = new byte[]{ 0, 0, 0, 0 };
            byte[] version = new byte[]{ 0, 0 };

            curVersion = (curVersion + 1) & (_VersionMask >>> _TotalSizeWidth);
            IoUtil.writeShort(curVersion, version, 0);
            try {
                byte[][] keys = _Ntru.getKeys(password);
                keyPair = new byte[][]{ keys[KEY_PAIR_INDEX_PUBLIC_KEY], keys[KEY_PAIR_INDEX_PASSWORD], time, version };
            }
            catch(Exception e) {
                return null;
            }
            _PublicKeyPair[keyIndex] = keyPair;
        }
        return new Pair<>(pubKeyId, keyPair);
    }

    @Override
    public Pair<Integer, byte[]> getAsymmetricPubKey(final int _InPubKeyId)
    {
        Pair<Integer, byte[][]> keyPair = createPair(_Random, _InPubKeyId);
        if(keyPair == null) { return null; }
        return new Pair<>(keyPair.getFirst(), keyPair.getSecond()[KEY_PAIR_INDEX_PUBLIC_KEY]);
    }

    @Override
    public byte[] getSymmetricKey(final int _InPubKeyId, byte[] cipher)
    {
        if(_InPubKeyId < 0) { return null; }
        int keyIndex = _InPubKeyId & _PairSizeMask;
        int keyVersion = (_InPubKeyId & _VersionMask) >>> _TotalSizeWidth;
        byte[][] keyPair = null;
        if(isPubKeyAvailable(_InPubKeyId)) {
            keyPair = _PublicKeyPair[keyIndex];
        }
        if(keyPair != null && keyPair[KEY_PAIR_INDEX_VERSION] != null &&
           IoUtil.readUnsignedShort(keyPair[KEY_PAIR_INDEX_VERSION], 0) == keyVersion)
        {
            if(keyPair[KEY_PAIR_INDEX_PASSWORD] == null) { return null; }
            return _Ntru.decrypt(cipher, keyPair[KEY_PAIR_INDEX_PASSWORD]);
        }
        return null;
    }

    @Override
    public Pair<Integer, byte[]> getCipher(byte[] pubKey, byte[] symmetricKey)
    {
        int symmetricKeyId = _Random.nextInt() & 0xFFFF;
        byte[] cipher = _Ntru.encrypt(symmetricKey, pubKey);
        return cipher != null ? new Pair<>(symmetricKeyId, cipher) : null;
    }

    @Override
    public byte[] getSymmetricKeySign(byte[] symmetricKey)
    {
        return cryptoUtil.sha256(symmetricKey);
    }

    @Override
    public int nextRandomInt()
    {
        return _Random.nextInt();
    }
}
