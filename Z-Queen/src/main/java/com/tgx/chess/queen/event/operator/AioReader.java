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

import static com.tgx.chess.queen.event.inf.IError.Type.READ_EOF;
import static com.tgx.chess.queen.event.inf.IError.Type.READ_FAILED;
import static com.tgx.chess.queen.event.inf.IError.Type.READ_ZERO;

import java.io.EOFException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ShutdownChannelGroupException;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class AioReader<C extends IContext>
        implements
        CompletionHandler<Integer,
                          ISession<C>>
{
    private final Logger           _Log           = Logger.getLogger(getClass().getSimpleName());
    private final CloseOperator<C> _CloseOperator = new CloseOperator<>();
    private final ErrorOperator<C> _ErrorOperator = new ErrorOperator<>(_CloseOperator);
    private final IPipeDecoder<C>  _Decoder;

    public AioReader(IPipeDecoder<C> decoder)
    {
        _Decoder = decoder;
    }

    @Override
    public void completed(Integer result, ISession<C> session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        switch (result)
        {
            case -1:
                worker.publishReadError(_ErrorOperator, READ_EOF, new EOFException("Read Negative"), session);
                break;
            case 0:
                worker.publishReadError(_ErrorOperator, READ_ZERO, new IllegalStateException("Read Zero"), session);
                session.readNext(this);
                break;
            default:
                _Log.info("read count: %d", result);
                ByteBuffer recvBuf = session.read(result);
                worker.publishRead(_Decoder, new AioPacket(recvBuf), session);
                try {
                    session.readNext(this);
                }
                catch (NotYetConnectedException |
                       ShutdownChannelGroupException e)
                {
                    worker.publishReadError(_ErrorOperator, READ_FAILED, e, session);
                }
                break;
        }
    }

    @Override
    public void failed(Throwable exc, ISession<C> session)
    {
        AioWorker worker = (AioWorker) Thread.currentThread();
        worker.publishReadError(_ErrorOperator, READ_FAILED, exc, session);
    }
}
