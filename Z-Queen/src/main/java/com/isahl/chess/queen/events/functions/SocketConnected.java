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

package com.isahl.chess.queen.events.functions;

import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.net.socket.features.IAioConnection;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author william.d.zk
 */
public class SocketConnected
        implements IOperator<IAioConnection, AsynchronousSocketChannel, ITriple>
{
    private final AioReader _AioReader = new AioReader();

    @Override
    public ITriple handle(IAioConnection connection, AsynchronousSocketChannel channel) throws ZException
    {
        ISession session = null;
        try {
            session = connection.create(channel, connection);
            // session == null 已经throw IOException了
            connection.onCreated(session);
            session.ready();
            session.readNext(_AioReader);
            return Triple.of(true, session, connection.afterConnected(session));
        }
        catch(IOException e) {
            try {
                channel.close();
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
            return Triple.of(false, channel, e);
        }
        catch(Exception e) {
            // 此时session!=null
            return session != null ? Triple.of(false, session, e) : Triple.of(false, channel, e);
        }
    }

    @Override
    public String getName()
    {
        return "operator.connected";
    }
}
