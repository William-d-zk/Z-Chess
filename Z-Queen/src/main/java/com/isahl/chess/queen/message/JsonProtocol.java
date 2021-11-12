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

package com.isahl.chess.queen.message;

import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * @author william.d.zk
 */
public abstract class JsonProtocol
        implements IProtocol
{
    protected           int    mLength;
    protected transient byte[] tPayload;

    @Override
    public byte[] encode()
    {
        tPayload = JsonUtil.writeValueAsBytes(this);
        if(tPayload != null) {
            mLength = tPayload.length;
        }
        return tPayload;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        return pos + data.length;
    }

    /**
     * Json-Protocol 以外部性 decode 过程
     */
    @Override
    public int decode(byte[] data)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] payload()
    {
        return tPayload;
    }

    public void put(byte[] data)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int length()
    {
        return mLength;
    }

}
