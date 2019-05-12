/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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

import java.nio.channels.AsynchronousSocketChannel;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ICommandCreator;
import com.tgx.chess.queen.io.core.inf.IConnectionContext;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.IPipeTransfer;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionCreated;
import com.tgx.chess.queen.io.core.inf.ISessionCreator;

/**
 * @author william.d.zk
 */
public class ConnectedOperator<C extends IContext>
        implements
        IOperator<IConnectionContext<C>,
                  AsynchronousSocketChannel,
                  ITriple>
{
    private final IPipeEncoder<C>  _Encoder;
    private final IPipeDecoder<C>  _Decoder;
    private final IPipeTransfer<C> _Transfer;
    private final AioReader<C>     _AioReader;

    public ConnectedOperator(IPipeEncoder<C> encoder,
                             IPipeDecoder<C> decoder)
    {
        _Encoder = encoder;
        _Decoder = decoder;
        _Transfer = new TransferOperator<>(_Encoder);
        _AioReader = new AioReader<>(_Decoder);
    }

    @Override
    public ITriple handle(IConnectionContext<C> context, AsynchronousSocketChannel channel)
    {
        ISessionCreated<C> sessionCreated = context.getSessionCreated();
        ISessionCreator<C> sessionCreator = context.getSessionCreator();
        ISession<C> session = sessionCreator.createSession(channel, context);
        ICommandCreator<C> commandCreator = context.getCommandCreator();
        sessionCreated.onCreate(session);
        session.readNext(_AioReader);
        ICommand[] commands = commandCreator.createCommands(session);
        return new Triple<>(commands, session, _Transfer);
    }
}
