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
package com.isahl.chess.queen.io.core.inf;

import com.isahl.chess.king.base.crypt.inf.ISymmetric;

/**
 * @author William.d.zk
 */
public interface IEContext
{

    default boolean needUpdateKeyIn()
    {
        return false;
    }

    default boolean needUpdateKeyOut()
    {
        return false;
    }

    default void updateKeyIn()
    {
    }

    default void updateKeyOut()
    {
    }

    default int getSymmetricKeyId()
    {
        return -2;
    }

    default void setSymmetricKeyId(int rc4KeyId)
    {
    }

    default byte[] getSymmetricKeyIn()
    {
        return null;
    }

    default byte[] getSymmetricKeyOut()
    {
        return null;
    }

    default byte[] getReRollKey()
    {
        return null;
    }

    default void reRollKey(byte[] key)
    {
    }

    default void swapKeyIn(byte[] key)
    {
    }

    default void swapKeyOut(byte[] key)
    {
    }

    default ISymmetric getSymmetricEncrypt()
    {
        return null;
    }

    default ISymmetric getSymmetricDecrypt()
    {
        return null;
    }

    default int getPubKeyId()
    {
        return -2;
    }

    default void setPubKeyId(int pubKeyId)
    {
    }

    int inState();

    int outState();

    void cryptIn();

    void cryptOut();

    boolean isInCrypt();

    boolean isOutCrypt();

    default IEncryptHandler getEncryptHandler()
    {
        return null;
    }

    default void setEncryptHandler(IEncryptHandler handler)
    {
    }

}
