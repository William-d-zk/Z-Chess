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

package com.isahl.chess.bishop.protocol.mqtt.ctrl;

import com.isahl.chess.bishop.protocol.mqtt.model.MqttProtocol;
import com.isahl.chess.bishop.protocol.mqtt.model.QttContext;
import com.isahl.chess.bishop.protocol.mqtt.model.QttType;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.IDuplicate;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.Objects;

/**
 * @author william.d.zk
 * @date 2019-05-13
 */
public abstract class QttControl
        extends MqttProtocol
        implements IControl<QttContext>,
                   IDuplicate
{
    protected QttContext mContext;
    private   ISession   mSession;

    protected void generateCtrl(boolean dup, boolean retain, IQoS.Level qosLevel, QttType qttType)
    {
        mFrameHeader = 0;
        mFrameHeader |= dup ? DUPLICATE_FLAG : 0;
        mFrameHeader |= retain ? RETAIN_FLAG : 0;
        mFrameHeader |= qosLevel.getValue() << 1;
        mFrameHeader |= qttType.getValue();
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public QttControl with(ISession session)
    {
        if(session == null) {return this;}
        mSession = session;
        wrap(session.getContext(QttContext.class));
        return this;
    }

    @Override
    public QttContext context()
    {
        return mContext;
    }

    @Override
    public QttControl wrap(QttContext context)
    {
        mContext = Objects.requireNonNull(context);
        return this;
    }

    @Override
    public boolean isCtrl()
    {
        return switch(QttType.valueOf(mFrameHeader)) {
            case CONNECT, CONNACK, PINGREQ, PINGRESP, DISCONNECT, AUTH -> true;
            default -> false;
        };
    }

    @Override
    public byte header()
    {
        return mFrameHeader;
    }

    @Override
    public void header(int header)
    {
        mFrameHeader = (byte) header;
    }

    @Override
    public IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public QttControl withSub(IoSerial sub)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public QttControl withSub(byte[] sub)
    {
        mPayload = sub == null || sub.length > 0 ? sub : null;
        return this;
    }

    @Override
    public void deserializeSub(IoFactory factory)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public IQoS.Level getLevel()
    {
        return IQoS.Level.valueOf((mFrameHeader & QOS_MASK) >> 1);
    }

    public void setLevel(IQoS.Level level)
    {
        mFrameHeader &= ~QOS_MASK;
        mFrameHeader |= level.getValue() << 1;
        if(level == Level.ALMOST_ONCE) {
            mFrameHeader &= ~DUPLICATE_FLAG;
        }
    }

    public boolean isRetain()
    {
        return (mFrameHeader & RETAIN_FLAG) == RETAIN_FLAG;
    }

    public void setRetain()
    {
        mFrameHeader |= RETAIN_FLAG;
    }

    @Override
    public boolean isDuplicate()
    {
        return (mFrameHeader & DUPLICATE_FLAG) == DUPLICATE_FLAG;
    }

    public IDuplicate duplicate()
    {
        mFrameHeader |= DUPLICATE_FLAG;
        return this;
    }

    private void checkOpCode()
    {
        if(getLevel() == IQoS.Level.ALMOST_ONCE && isDuplicate()) {
            throw new IllegalStateException("level == 0 && duplicate");
        }
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain];
            input.get(mPayload);
        }
    }

    @Override
    public ByteBuf encode()
    {
        ByteBuf output = ByteBuf.allocate(sizeOf() + 1);
        output.put(header());
        return suffix(output);
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        return input == null ? 0 : input.readableBytes();
    }

}
