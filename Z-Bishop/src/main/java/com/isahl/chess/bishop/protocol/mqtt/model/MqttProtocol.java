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

import com.isahl.chess.king.base.features.IDuplicate;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public abstract class MqttProtocol
        implements IProtocol,
                   IQoS,
                   IDuplicate
{
    protected final Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getSimpleName());

    public final static byte VERSION_V3_1_1 = 4;
    public final static byte VERSION_V5_0   = 5;

    private final static byte DUPLICATE_FLAG = 1 << 3;
    private final static byte RETAIN_FLAG    = 1;
    private final static byte QOS_MASK       = 3 << 1;

    protected byte       mFrameOpCode;
    protected int        mVersion;
    protected QttContext mContext;

    protected byte[]  mPayload;
    private   QttType mType;

    private void checkOpCode()
    {
        if(getLevel() == Level.ALMOST_ONCE && isDuplicate()) {
            throw new IllegalStateException("level == 0 && duplicate");
        }
    }

    public static byte generateCtrl(boolean dup, boolean retain, Level qosLevel, QttType qttType)
    {
        byte ctrl = 0;
        ctrl |= dup ? DUPLICATE_FLAG : 0;
        ctrl |= retain ? RETAIN_FLAG : 0;
        ctrl |= qosLevel.getValue() << 1;
        ctrl |= qttType.getValue();
        return ctrl;
    }

    public void setDuplicate(boolean duplicate)
    {
        if(duplicate) {
            mFrameOpCode |= DUPLICATE_FLAG;
        }
        else {
            mFrameOpCode &= ~DUPLICATE_FLAG;
        }
    }

    @Override
    public boolean isDuplicate()
    {
        return (mFrameOpCode & DUPLICATE_FLAG) == DUPLICATE_FLAG;
    }

    public void setRetain(boolean retain)
    {
        if(retain) {
            mFrameOpCode |= RETAIN_FLAG;
        }
        else {
            mFrameOpCode &= ~RETAIN_FLAG;
        }
    }

    public boolean isRetain()
    {
        return (mFrameOpCode & RETAIN_FLAG) == RETAIN_FLAG;
    }

    public void setLevel(Level level)
    {
        mFrameOpCode &= ~QOS_MASK;
        mFrameOpCode |= level.getValue() << 1;
        if(level.getValue() == 0) {
            setDuplicate(false);
        }
    }

    public QttType getType()
    {
        return mType;
    }

    public void setType(QttType type)
    {
        this.mType = type;
        mFrameOpCode |= type.getValue();
    }

    protected void setOpCode(byte opCode)
    {
        mFrameOpCode = opCode;
        mType = QttType.valueOf(getOpCode());
        if(mType == null) {throw new IllegalArgumentException();}
        checkOpCode();
    }

    protected byte getOpCode()
    {
        return mFrameOpCode;
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public IQoS.Level getLevel()
    {
        return IQoS.Level.valueOf((mFrameOpCode & QOS_MASK) >> 1);
    }

    public int getVersion()
    {
        return mVersion;
    }

    public void setVersion(int version)
    {
        mVersion = version;
    }

    public void putContext(QttContext context)
    {
        mContext = context;
        if(context != null) {mVersion = context.getVersion();}
    }

    @Override
    public void put(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public ByteBuffer payload()
    {
        return mPayload == null ? null : ByteBuffer.wrap(mPayload);
    }

    @Override
    public int length()
    {
        return mPayload == null ? 0 : mPayload.length;
    }
}
