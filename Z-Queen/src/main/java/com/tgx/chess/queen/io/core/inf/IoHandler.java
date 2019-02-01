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

package com.tgx.chess.queen.io.core.inf;

import static com.tgx.chess.queen.event.inf.IError.Type.READ_EOF;
import static com.tgx.chess.queen.event.inf.IError.Type.READ_FAILED;
import static com.tgx.chess.queen.event.inf.IError.Type.READ_ZERO;
import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_EOF;
import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_FAILED;
import static com.tgx.chess.queen.event.inf.IError.Type.WRITE_ZERO;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ShutdownChannelGroupException;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.io.core.async.AioPacket;
import com.tgx.chess.queen.io.core.async.socket.AioWorker;

@SuppressWarnings("unchecked")
public interface IoHandler
        extends
        ISort,
        IOperatorSupplier<IPacket,
                          ICommand[],
                          ISession>
{
    Logger                         LOG             = Logger.getLogger(IoHandler.class.getName());
    IOperator<Void,
              ISession>            close_operator  = new IOperator<Void,
                                                                   ISession>()
                                                             {

                                                                 @Override
                                                                 public <T,
                                                                         E> Triple<T,
                                                                                   E,
                                                                                   IOperator<T,
                                                                                             E>> handle(Void v, ISession session)
                                                                 {
                                                                     try {
                                                                         session.close();
                                                                     }
                                                                     catch (IOException e) {
                                                                         LOG.warning("session close: %s", e, session.toString());
                                                                     }
                                                                     LOG.warning("closed operator %s", session.toString());
                                                                     return null;
                                                                 }

                                                                 @Override
                                                                 public String toString()
                                                                 {
                                                                     return "close_operator";
                                                                 }
                                                             };;
    IOperator<Throwable,
              ISession>            error_operator  = new IOperator<Throwable,
                                                                   ISession>()
                                                             {
                                                                 @Override
                                                                 public Triple<Void,
                                                                               ISession,
                                                                               IOperator<Void,
                                                                                         ISession>> handle(Throwable throwable, ISession session)
                                                                 {
                                                                     LOG.warning("error session:%s", throwable, session);
                                                                     return new Triple<>(null, session, close_operator);
                                                                 }

                                                                 @Override
                                                                 public String toString()
                                                                 {
                                                                     return "error_operator";
                                                                 }
                                                             };;
    IOperator<Throwable,
              ISession>            ignore_operator = new IOperator<Throwable,
                                                                   ISession>()
                                                             {
                                                                 @Override
                                                                 public <T,
                                                                         E> Triple<T,
                                                                                   E,
                                                                                   IOperator<T,
                                                                                             E>> handle(Throwable throwable, ISession iSession)
                                                                 {
                                                                     LOG.warning("ignore error", throwable);
                                                                     return null;
                                                                 }

                                                                 @Override
                                                                 public String toString()
                                                                 {
                                                                     return "ignore_operator";
                                                                 }
                                                             };

    IOperator<Integer,
              ISession>                  wrote_operator = new IOperator<Integer,
                                                                        ISession>()
                                                                  {
                                                                      @Override
                                                                      public Triple<Throwable,
                                                                                    ISession,
                                                                                    IOperator<Throwable,
                                                                                              ISession>> handle(Integer wroteCnt, ISession session)
                                                                      {
                                                                          try {
                                                                              session.writeNext(wroteCnt, aio_writer);
                                                                          }
                                                                          catch (Exception e) {
                                                                              return new Triple<>(e, session, error_operator);
                                                                          }
                                                                          return null;
                                                                      }

                                                                      @Override
                                                                      public String toString()
                                                                      {
                                                                          return "wrote_operator";
                                                                      }
                                                                  };
    CompletionHandler<Integer,
                      ISession>          aio_writer     = new CompletionHandler<Integer,
                                                                                ISession>()
                                                                          {
                                                                              @Override
                                                                              public void completed(Integer result, ISession session)
                                                                              {
                                                                                  AioWorker worker = (AioWorker) Thread.currentThread();
                                                                                  switch (result)
                                                                                  {
                                                                                      case -1:
                                                                                          worker.publishWroteError(error_operator,
                                                                                                                   WRITE_EOF,
                                                                                                                   new EOFException("wrote -1!"),
                                                                                                                   session);
                                                                                          break;
                                                                                      case 0:
                                                                                          worker.publishWroteError(error_operator,
                                                                                                                   WRITE_ZERO,
                                                                                                                   new IllegalArgumentException("wrote zero!"),
                                                                                                                   session);
                                                                                          break;
                                                                                      default:
                                                                                          LOG.info("wrote %d", result);
                                                                                          worker.publishWrote(wrote_operator, result, session);
                                                                                          break;
                                                                                  }
                                                                              }

                                                                              @Override
                                                                              public void failed(Throwable exc, ISession session)
                                                                              {
                                                                                  AioWorker worker = (AioWorker) Thread.currentThread();
                                                                                  worker.publishWroteError(error_operator, WRITE_FAILED, exc, session);
                                                                              }

                                                                              @Override
                                                                              public String toString()
                                                                              {
                                                                                  return "aio_writer";
                                                                              }
                                                                          };
    CompletionHandler<Integer,
                      ISession>          aio_reader     = new CompletionHandler<Integer,
                                                                                ISession>()
                                                                          {
                                                                              @Override
                                                                              public void completed(Integer read, ISession session)
                                                                              {
                                                                                  AioWorker worker = (AioWorker) Thread.currentThread();
                                                                                  switch (read)
                                                                                  {
                                                                                      case -1:
                                                                                          worker.publishReadError(error_operator,
                                                                                                                  READ_EOF,
                                                                                                                  new EOFException("Read Negative"),
                                                                                                                  session);
                                                                                          break;
                                                                                      case 0:
                                                                                          worker.publishReadError(error_operator,
                                                                                                                  READ_ZERO,
                                                                                                                  new IllegalStateException("Read Zero"),
                                                                                                                  session);
                                                                                          session.readNext(this);
                                                                                          break;
                                                                                      default:
                                                                                          LOG.info("read count: %d", read);
                                                                                          ByteBuffer recvBuf = session.read(read);
                                                                                          worker.publishRead(session.getDecodeOperator(),
                                                                                                             new AioPacket(recvBuf),
                                                                                                             session);
                                                                                          try {
                                                                                              session.readNext(this);
                                                                                          }
                                                                                          catch (NotYetConnectedException |
                                                                                                 ShutdownChannelGroupException e)
                    {
                                                                                              worker.publishReadError(error_operator, READ_FAILED, e, session);
                                                                                          }
                                                                                          break;
                                                                                  }
                                                                              }

                                                                              @Override
                                                                              public void failed(Throwable exc, ISession session)
                                                                              {
                                                                                  AioWorker worker = (AioWorker) Thread.currentThread();
                                                                                  worker.publishReadError(error_operator, READ_FAILED, exc, session);
                                                                              }

                                                                              @Override
                                                                              public String toString()
                                                                              {
                                                                                  return "aio_reader";
                                                                              }
                                                                          };

    IOperator<IConnectionContext,
              AsynchronousSocketChannel> connected_operator = new IOperator<IConnectionContext,
                                                                            AsynchronousSocketChannel>()
              {
                  //都在 LinkIo-Processor 中处理
                  @Override
                  public Triple<ICommand[],
                                ISession,
                                IOperator<ICommand[],
                                          ISession>> handle(IConnectionContext ctx, AsynchronousSocketChannel channel)
                  {
                      ISession session = ctx.getSessionCreator()
                                            .createSession(channel, ctx.getConnectActive());
                      ICommand[] commands = ctx.getCommandCreator()
                                               .createCommands(session);
                      session.readNext(aio_reader);
                      return new Triple<>(commands,
                                          session,
                                          session.getHandler()
                                                 .getOutOperator());
                  }

                  @Override
                  public String toString()
                  {
                      return "connected_operator";
                  }
              };

    static IOperator<Void,
                     ISession> CLOSE_OPERATOR()
    {
        return close_operator;
    }

    static IOperator<Throwable,
                     ISession> ERROR_OPERATOR()
    {
        return error_operator;
    }

    static IOperator<Throwable,
                     ISession> IGNORE_OPERATOR()
    {
        return ignore_operator;
    }

    static IOperator<Integer,
                     ISession> WROTE_OPERATOR()
    {
        return wrote_operator;
    }

    static IOperator<IConnectionContext,
                     AsynchronousSocketChannel> CONNECTED_OPERATOR()
    {
        return connected_operator;
    }

    static CompletionHandler<Integer,
                             ISession> AIO_WRITER()
    {
        return aio_writer;
    }

    static CompletionHandler<Integer,
                             ISession> AIO_READER()
    {
        return aio_reader;
    }

    interface IEncoder
            extends
            IOperator<ICommand,
                      ISession>,
            IPipeEncoder
    {
    }

    interface IDecoder
            extends
            IOperator<IPacket,
                      ISession>,
            IPipeDecode
    {
    }

    default boolean isSSL()
    {
        return false;
    }
}
