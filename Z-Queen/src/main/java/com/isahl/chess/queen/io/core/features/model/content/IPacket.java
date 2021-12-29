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
package com.isahl.chess.queen.io.core.features.model.content;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;

/**
 * @author William.d.zk
 */

public interface IPacket
        extends IProtocol

{
    boolean isSending();

    boolean isSent();

    IPacket send();

    IPacket waitSend();

    IPacket noSend();

    IPacket sent();

    Status getStatus();

    void setAbandon();

    ByteBuf getBuffer();

    enum Status
    {
        No_Send,
        To_Send,
        Sending,
        Sent,
        Abandon;
    }

    @Override
    default IoSerial subContent()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default IProtocol withSub(IoSerial sub)
    {
        throw new UnsupportedOperationException();
    }

    default IProtocol withSub(byte[] sub)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    default void deserializeSub(IoFactory factory)
    {
        throw new UnsupportedOperationException();
    }
}
