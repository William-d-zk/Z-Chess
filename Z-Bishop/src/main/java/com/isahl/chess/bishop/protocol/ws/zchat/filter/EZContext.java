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

package com.isahl.chess.bishop.protocol.ws.zchat.filter;

import com.isahl.chess.bishop.protocol.ws.zchat.ZContext;
import com.isahl.chess.king.base.crypt.util.Rc4;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEContext;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEncryptor;

import static com.isahl.chess.king.base.cron.features.ITask.advanceState;
import static com.isahl.chess.queen.io.core.features.model.session.ISession.CAPACITY;

public class EZContext<A extends IPContext>
        extends ZContext
        implements IEContext,
                   IProxyContext<A>
{
    private final CryptoUtil _CryptoUtil = new CryptoUtil();
    private final A          _ActingContext;

    private int     mPubKeyId = -2;
    private boolean mUpdateKeyIn, mUpdateKeyOut;
    private int    mSymmetricKeyId;
    private byte[] mSymmetricKeyIn, mSymmetricKeyOut, mSymmetricKeyReroll;
    private Rc4 mEncryptRc4, mDecryptRc4;
    private IEncryptor mEncryptHandler;

    public EZContext(INetworkOption option, ISort.Mode mode, ISort.Type type, A acting)
    {
        super(option, mode, type);
        _ActingContext = acting;
    }

    @Override
    public A getActingContext()
    {
        return _ActingContext;
    }

    @Override
    public void reset()
    {
        mUpdateKeyIn = false;
        mUpdateKeyOut = false;
        mPubKeyId = -2;
        mSymmetricKeyId = 0;
        super.reset();
    }

    @Override
    public void dispose()
    {
        mEncryptHandler = null;
        mSymmetricKeyIn = mSymmetricKeyOut = mSymmetricKeyReroll = null;
        if(mEncryptRc4 != null) {
            mEncryptRc4.reset();
        }
        if(mDecryptRc4 != null) {
            mDecryptRc4.reset();
        }
        mEncryptRc4 = mDecryptRc4 = null;
        super.dispose();
    }

    @Override
    public Rc4 getSymmetricDecrypt()
    {
        return mDecryptRc4 == null ? mDecryptRc4 = new Rc4() : mDecryptRc4;
    }

    @Override
    public Rc4 getSymmetricEncrypt()
    {
        return mEncryptRc4 == null ? mEncryptRc4 = new Rc4() : mEncryptRc4;
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
        if(mUpdateKeyIn) {
            mUpdateKeyIn = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean needUpdateKeyOut()
    {
        if(mUpdateKeyOut) {
            mUpdateKeyOut = false;
            return true;
        }
        return false;
    }

    @Override
    public void updateIn()
    {
        updateKeyIn();
    }

    /*
     * 处理流中仅提供信号即可，
     * 真正的操作在decode 中使用 cryptIn最终执行
     */
    @Override
    public void updateKeyIn()
    {
        mUpdateKeyIn = true;
    }

    /*
     * 处理流中仅提供信号即可，
     * 真正的操作在encode 中使用 cryptOut最终执行
     */
    @Override
    public void updateOut()
    {
        updateKeyOut();
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
    public IEncryptor getEncryptHandler()
    {
        return mEncryptHandler;
    }

    @Override
    public void setEncryptHandler(IEncryptor handler)
    {
        mEncryptHandler = handler;
    }

    public CryptoUtil getCryptUtil()
    {
        return _CryptoUtil;
    }

    @Override
    public void cryptIn()
    {
        advanceInState(DECODE_PAYLOAD);
    }

    @Override
    public void cryptOut()
    {
        advanceOutState(ENCODE_PAYLOAD);
    }

    @Override
    public boolean isInCrypt()
    {
        return _DecodeState.get() == DECODE_PAYLOAD;
    }

    @Override
    public boolean isOutCrypt()
    {
        return _EncodeState.get() == ENCODE_PAYLOAD;
    }

    @Override
    public boolean isProxy()
    {
        return true;
    }

    @Override
    public void ready()
    {
        advanceState(_DecodeState, DECODE_FRAME, CAPACITY);
        advanceState(_EncodeState, ENCODE_FRAME, CAPACITY);
        _ActingContext.ready();
    }
}
