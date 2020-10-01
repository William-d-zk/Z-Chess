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
package com.tgx.chess.bishop.io.zfilter;

import com.tgx.chess.king.base.crypt.util.Rc4;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioContext;
import com.tgx.chess.queen.io.core.inf.IEncryptHandler;
import com.tgx.chess.queen.io.core.inf.IFrame;
import com.tgx.chess.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 * @date 2017-02-10
 */
public class ZContext
        extends
        AioContext<ZContext>
{

    private final CryptUtil _CryptUtil = new CryptUtil();

    private int             mPubKeyId = -2;
    private boolean         mUpdateKeyIn, mUpdateKeyOut;
    private int             mSymmetricKeyId;
    private byte[]          mSymmetricKeyIn, mSymmetricKeyOut, mSymmetricKeyReroll;
    private Rc4             mEncryptRc4, mDecryptRc4;
    private IEncryptHandler mEncryptHandler;
    private IFrame          mCarrier;

    public ZContext(ISessionOption option,
                    ISort<ZContext> sort)
    {
        super(option, sort);
    }

    @Override
    public void reset()
    {
        super.reset();
        mUpdateKeyIn = false;
        mUpdateKeyOut = false;
        mPubKeyId = -2;
        mSymmetricKeyId = 0;
        if (mCarrier != null) {
            mCarrier.reset();
        }
        mCarrier = null;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        mEncryptHandler = null;
        mSymmetricKeyIn = mSymmetricKeyOut = mSymmetricKeyReroll = null;
        if (mEncryptRc4 != null) {
            mEncryptRc4.reset();
        }
        if (mDecryptRc4 != null) {
            mDecryptRc4.reset();
        }
        mEncryptRc4 = mDecryptRc4 = null;
        mCarrier = null;
    }

    @Override
    public void finish()
    {
        super.finish();
        mCarrier = null;
    }

    @Override
    public Rc4 getSymmetricDecrypt()
    {
        return mDecryptRc4 == null ? mDecryptRc4 = new Rc4()
                                   : mDecryptRc4;
    }

    @Override
    public Rc4 getSymmetricEncrypt()
    {
        return mEncryptRc4 == null ? mEncryptRc4 = new Rc4()
                                   : mEncryptRc4;
    }

    @Override
    public int getSymmetricKeyId()
    {
        return mSymmetricKeyId;
    }

    @Override
    public void setSymmetricKeyId(int symmetricKeyId)
    {
        mSymmetricKeyId = symmetricKeyId;
    }

    @Override
    public byte[] getSymmetricKeyIn()
    {
        return mSymmetricKeyIn;
    }

    @Override
    public byte[] getSymmetricKeyOut()
    {
        return mSymmetricKeyOut;
    }

    @Override
    public byte[] getReRollKey()
    {
        return mSymmetricKeyReroll;
    }

    @Override
    public boolean needUpdateKeyIn()
    {
        if (mUpdateKeyIn) {
            mUpdateKeyIn = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean needUpdateKeyOut()
    {
        if (mUpdateKeyOut) {
            mUpdateKeyOut = false;
            return true;
        }
        return false;
    }

    @Override
    public void updateKeyIn()
    {
        mUpdateKeyIn = true;
    }

    @Override
    public void updateKeyOut()
    {
        mUpdateKeyOut = true;
    }

    @Override
    public void reRollKey(byte[] key)
    {
        mSymmetricKeyReroll = key;
    }

    @Override
    public void swapKeyIn(byte[] key)
    {
        mSymmetricKeyIn = key;
    }

    @Override
    public void swapKeyOut(byte[] key)
    {
        mSymmetricKeyOut = key;
    }

    @Override
    public int getPubKeyId()
    {
        return mPubKeyId;
    }

    @Override
    public void setPubKeyId(int pubKeyId)
    {
        mPubKeyId = pubKeyId;
    }

    @Override
    public IEncryptHandler getEncryptHandler()
    {
        return mEncryptHandler;
    }

    @Override
    public void setEncryptHandler(IEncryptHandler handler)
    {
        mEncryptHandler = handler;
    }

    public CryptUtil getCryptUtil()
    {
        return _CryptUtil;
    }

    @SuppressWarnings("unchecked")
    public <F extends IFrame> F getCarrier()
    {
        return (F) mCarrier;
    }

    public void setCarrier(IFrame frame)
    {
        mCarrier = frame;
    }
}
