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

package com.isahl.chess.bishop.io.mqtt.ctrl;

import com.isahl.chess.bishop.io.mqtt.model.MqttProtocol;
import com.isahl.chess.bishop.io.mqtt.model.QttContext;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.Objects;

/**
 * @author william.d.zk
 * @date 2019-05-13
 */
public abstract class QttControl
        extends MqttProtocol
        implements IControl
{
    private final int      _Command;
    private       byte[]   mPayload;
    private       ISession mSession;

    public QttControl(int command)
    {
        _Command = command;
    }

    @Override
    public int dataLength()
    {
        return Objects.nonNull(mPayload) ? mPayload.length : 0;
    }

    @Override
    public byte ctrl()
    {
        return getOpCode();
    }

    @Override
    public void putCtrl(byte ctrl)
    {
        setOpCode(ctrl);
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public void putSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public int serial()
    {
        return _Command;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public byte[] payload()
    {
        return mPayload;
    }

    @Override
    public void putPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public void reset()
    {
        mSession = null;
        mPayload = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public QttContext context()
    {
        return mContext;
    }

}
