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

package com.tgx.chess.queen.event.operator;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.IConnectActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class ConnectedOperator<C extends IContext<C>>
        implements
        IOperator<IConnectActivity<C>,
                  AsynchronousSocketChannel,
                  ITriple>
{
    private final AioReader<C> _AioReader = new AioReader<>();

    @Override
    public ITriple handle(IConnectActivity<C> activity, AsynchronousSocketChannel channel) throws ZException
    {

        ISession<C> session;
        try {
            session = activity.createSession(channel, activity);
        }
        catch (IOException e) {
            try {
                channel.close();
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            throw new ZException(e, "session create failed");
        }
        if (session != null) try {
            activity.onCreate(session);
            session.readNext(_AioReader);
            IControl<C>[] commands = activity.createCommands(session);
            return new Triple<>(commands,
                                session,
                                session.getContext()
                                       .getSort()
                                       .getTransfer());
        }
        catch (Exception e) {
            try {
                session.close();
            }
            catch (IOException oe) {
                //ignore
            }
            throw new ZException(e, "session create success,but failed onCreate || session.readNext || createCommands");
        }
        else {
            throw new ZException("session create failed %s", channel);
        }
    }

    @Override
    public String getName()
    {
        return "operator.connected";
    }
}
