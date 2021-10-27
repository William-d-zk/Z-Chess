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
package com.isahl.chess.bishop.io.mqtt.command;

import com.isahl.chess.bishop.io.mqtt.model.MqttProtocol;
import com.isahl.chess.bishop.io.mqtt.model.QttContext;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.ICommand;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public abstract class QttCommand
        extends MqttProtocol
        implements ICommand
{

    private final int _Command;

    public QttCommand(int command)
    {
        _Command = command;
    }

    private long     mMsgId = -1;
    private ISession mSession;
    private byte[]   mPayload;

    @Override
    public void putCtrl(byte ctrl)
    {
        setOpCode(ctrl);
    }

    @Override
    public void setMsgId(long id)
    {
        mMsgId = id;
    }

    @Override
    public long getMsgId()
    {
        return mMsgId;
    }

    @Override
    public void putPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public byte ctrl()
    {
        return getOpCode();
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public void reset()
    {
        mPayload = null;
    }

    @Override
    public int serial()
    {
        return _Command;
    }

    @Override
    public void putSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_08_IMMEDIATE_MESSAGE;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mMsgId = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort((int) mMsgId, data, pos);
        return pos;
    }

    @Override
    public int dataLength()
    {
        return 2 + (mPayload == null ? 0 : mPayload.length);
    }

    @Override
    @SuppressWarnings("unchecked")
    public QttContext context()
    {
        return mContext;
    }
}
