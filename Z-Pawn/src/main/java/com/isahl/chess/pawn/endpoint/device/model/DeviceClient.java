/*
 * MIT License
 *
 * Copyright (c) 2022~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.model;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.model.BinarySerial;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;

import java.io.Serial;
import java.util.List;

/**
 * @author william.d.zk
 * @date 2022-01-09
 */

public class DeviceClient
        extends BinarySerial
{
    @Serial
    private static final long serialVersionUID = -3243787401563001082L;

    private long                      mIdx;
    private long                      mKeepAlive;
    private boolean                   mWillRetain;
    private IThread.Topic             mWillTopic;
    private IProtocol                 mWillMessage;
    private ListSerial<IThread.Topic> mSubscribes;

    public DeviceClient(long idx,
                        long keepalive,
                        List<IThread.Topic> subscribes,
                        boolean willRetain,
                        IThread.Topic willTopic,
                        IProtocol willMessage)
    {
        mIdx = idx;
        mWillRetain = willRetain;
        mWillTopic = willTopic;
        mWillMessage = willMessage;
        mKeepAlive = keepalive;
        mSubscribes = subscribes == null ? new ListSerial<>(IThread.Topic::new)
                                         : new ListSerial<>(subscribes, IThread.Topic::new);
    }

    public DeviceClient(long idx)
    {
        mIdx = idx;
    }

    public DeviceClient(ByteBuf input) {super(input);}

    public DeviceClient() {}

    public long getIdx() {return mIdx;}

    public long getKeepAlive()
    {
        return mKeepAlive;
    }

    public void setKeepAlive(long keepalive)
    {
        mKeepAlive = keepalive;
    }

    public IThread.Topic getWillTopic() {return mWillTopic;}

    public IProtocol getWillMessage() {return mWillMessage;}

    public List<IThread.Topic> getSubscribes() {return mSubscribes;}

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mIdx = input.getLong();
        remain -= 8;
        mKeepAlive = input.getLong();
        mWillRetain = input.get() != 0;
        remain -= 9;
        if(remain > 0) {
            mWillTopic = new IThread.Topic(input);
            remain -= mWillTopic.sizeOf();
        }
        if(_Factory != null && remain > 0) {
            mWillMessage = _Factory.create(input);
            remain -= mWillMessage.sizeOf();
        }
        if(remain > 0) {
            mSubscribes = new ListSerial<>(IThread.Topic::new);
            mSubscribes.decode(input);
            remain -= mSubscribes.sizeOf();
        }
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mIdx)
                      .putLong(mKeepAlive)
                      .put(mWillRetain ? 1 : 0);
        if(mWillTopic != null) {
            output.put(mWillTopic.encode());
        }
        if(mWillMessage != null) {
            output.put(mWillMessage.encode());
        }
        if(mSubscribes != null) {
            output.put(mSubscribes.encoded());
        }
        return output;
    }

    @Override
    public int length()
    {
        return super.length() + // super
               8 + // idx
               8 + // keep-alive-duration
               1 + // will-retain
               (mWillTopic != null ? mWillTopic.sizeOf() : 0) + // will-topic
               (mWillMessage != null ? mWillMessage.sizeOf() : 0) + // will-message
               (mSubscribes != null ? mSubscribes.sizeOf() : 0); // subscribes
    }

    public static IoFactory<IProtocol> _Factory;

}
