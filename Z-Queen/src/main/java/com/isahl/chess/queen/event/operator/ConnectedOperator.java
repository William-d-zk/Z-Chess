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

package com.isahl.chess.queen.event.operator;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.event.inf.IOperator;
import com.isahl.chess.queen.io.core.inf.IConnectActivity;
import com.isahl.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class ConnectedOperator
        implements
        IOperator<IConnectActivity,
                  AsynchronousSocketChannel,
                  ITriple>
{
    private final AioReader _AioReader = new AioReader();

    @Override
    public ITriple handle(IConnectActivity activity, AsynchronousSocketChannel channel) throws ZException
    {

        ISession session = null;
        try {
            session = activity.createSession(channel, activity);
            // session == null 已经throw IOException了
            activity.onCreate(session);
            session.ready();
            session.readNext(_AioReader);
            return new Triple<>(true, session, activity.createCommands(session));
        }
        catch (IOException e) {
            try {
                channel.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            return new Triple<>(false, channel, e);
        }
        catch (Exception e) {
            // 此时session!=null
            return session != null ? new Triple<>(false, session, e)
                                   : new Triple<>(false, channel, e);
        }
    }

    @Override
    public String getName()
    {
        return "operator.connected";
    }
}
