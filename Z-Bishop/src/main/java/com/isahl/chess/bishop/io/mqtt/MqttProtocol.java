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

package com.isahl.chess.bishop.io.mqtt;

import com.isahl.chess.queen.io.core.inf.IDuplicate;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.IQoS;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-25
 */
public abstract class MqttProtocol
        implements
        IProtocol,
        IQoS,
        IDuplicate
{
    public final static byte VERSION_V3_1_1 = 4;
    public final static byte VERSION_V5_0   = 5;

    private final static byte DUPLICATE_FLAG = 1 << 3;
    private final static byte RETAIN_FLAG    = 1;
    private final static byte QOS_MASK       = 3 << 1;

    protected byte       mFrameOpCode;
    protected int        mVersion;
    protected QttContext mContext;

    private boolean mDuplicate;
    private boolean mRetain;
    private byte    mQosLevel;
    private QttType mType;

    private void checkOpCode()
    {
        if (getLevel() == Level.ALMOST_ONCE && mDuplicate) {
            throw new IllegalStateException("level == 0 && duplicate");
        }
    }

    public static byte generateCtrl(boolean dup, boolean retain, Level qosLevel, QttType qttType)
    {
        byte ctrl = 0;
        ctrl |= dup ? DUPLICATE_FLAG: 0;
        ctrl |= retain ? RETAIN_FLAG: 0;
        ctrl |= qosLevel.ordinal() << 1;
        ctrl |= qttType.getValue();
        return ctrl;
    }

    public void setDuplicate(boolean duplicate)
    {
        this.mDuplicate = duplicate;
        if (duplicate) {
            mFrameOpCode |= DUPLICATE_FLAG;
        }
        else {
            mFrameOpCode &= ~DUPLICATE_FLAG;
        }
    }

    @Override
    public boolean isDuplicate()
    {
        return mDuplicate;
    }

    public void setRetain(boolean retain)
    {
        this.mRetain = retain;
        if (retain) {
            mFrameOpCode |= RETAIN_FLAG;
        }
        else {
            mFrameOpCode &= ~RETAIN_FLAG;
        }
    }

    public boolean isRetain()
    {
        return mRetain;
    }

    public void setLevel(Level level)
    {
        mQosLevel = (byte) level.ordinal();
        mFrameOpCode &= ~QOS_MASK;
        mFrameOpCode |= mQosLevel << 1;
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
        if (mType == null) { throw new IllegalArgumentException(); }
        mDuplicate = (mFrameOpCode & DUPLICATE_FLAG) == DUPLICATE_FLAG;
        mRetain = (mFrameOpCode & RETAIN_FLAG) == RETAIN_FLAG;
        mQosLevel = (byte) ((mFrameOpCode & QOS_MASK) >> 1);
        checkOpCode();
    }

    protected byte getOpCode()
    {
        return mFrameOpCode;
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public IQoS.Level getLevel()
    {
        return IQoS.Level.valueOf(mQosLevel);
    }

    public int getVersion()
    {
        return mVersion;
    }

    public void setVersion(int version)
    {
        mVersion = version;
    }

    public void setContext(QttContext context)
    {
        mContext = context;
    }

}
