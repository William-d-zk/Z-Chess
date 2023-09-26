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

package com.isahl.chess.bishop.protocol.mqtt.model;

import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public abstract class QttProtocol
        implements IProtocol
{
    protected final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getSimpleName());

    public final static byte VERSION_V3_1_1 = 4;
    public final static byte VERSION_V5_0   = 5;

    protected final static byte DUPLICATE_FLAG = 1 << 3;
    protected final static byte RETAIN_FLAG    = 1;
    protected final static byte QOS_MASK       = 3 << 1;

    protected byte   mFrameHeader;
    protected byte[] mPayload;

    public QttType getType()
    {
        return QttType.valueOf(mFrameHeader);
    }

    public void setType(QttType type)
    {
        mFrameHeader |= (byte) type.getValue();
    }

    @Override
    public int length()
    {
        return mPayload == null ? 0 : mPayload.length;
    }

    @Override
    public int sizeOf()
    {
        return length();
    }
}
