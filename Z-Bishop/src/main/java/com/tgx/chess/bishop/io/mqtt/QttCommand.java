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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.chess.bishop.io.mqtt;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2019-05-25
 */
public abstract class QttCommand
        extends
        MqttProtocol
        implements
        ICommand<ZContext>
{

    private final int _Command;

    public QttCommand(int command)
    {
        _Command = command;
    }

    private long               mMsgId = -1;
    private int                mLocalId;
    private ISession<ZContext> mSession;
    private byte[]             mPayload;

    @Override
    public void setCtrl(byte ctrl)
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
    public int getLocalId()
    {
        return mLocalId;
    }

    @Override
    public void setLocalId(int localId)
    {
        mLocalId = localId;
    }

    @Override
    public void setPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public byte[] getPayload()
    {
        return mPayload;
    }

    @Override
    public byte getCtrl()
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
    public ICommand<ZContext> setSession(ISession<ZContext> session)
    {
        mSession = session;
        return this;
    }

    @Override
    public ISession<ZContext> getSession()
    {
        return mSession;
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_08_IMMEDIATE_MESSAGE;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mLocalId = IoUtil.readUnsignedShort(data, pos);
        pos += 2;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeShort(mLocalId, data, pos);
        return pos;
    }

    @Override
    public int dataLength()
    {
        return 2
               + (mPayload == null ? 0
                                   : mPayload.length);
    }
}
