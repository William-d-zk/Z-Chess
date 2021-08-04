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

package com.isahl.chess.queen.event.handler;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IBatchEventHandler;
import com.isahl.chess.king.base.disruptor.event.inf.IHealth;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.disruptor.processor.Health;
import com.isahl.chess.king.base.inf.IError;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.inf.*;

/**
 * @author william.d.zk
 */
public class EncodeHandler
        implements IBatchEventHandler<QEvent>
{
    private final Logger          _Logger = Logger.getLogger("io.queen.processor." + getClass().getSimpleName());
    private final IEncryptHandler _EncryptHandler;
    private final IHealth         _Health;

    public EncodeHandler(IEncryptHandler encryptHandler, int slot)
    {
        _EncryptHandler = encryptHandler;
        _Health = new Health(slot);
    }

    @Override
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public void onEvent(QEvent event, long sequence) throws Exception
    {
        if(event.hasError()) {
            switch(event.getErrorType()) {
                case FILTER_ENCODE:
                case ILLEGAL_STATE:
                case ILLEGAL_BIZ_STATE:
                default:
                    /*
                     * 透传到 EncodedHandler 去投递 _Error
                     */
                    break;
            }
        }
        else {
            switch(event.getEventType()) {
                case WRITE:
                    IPair pairWriteContent = event.getContent();
                    IControl cmd = pairWriteContent.getFirst();
                    ISession session = pairWriteContent.getSecond();
                    IOperator<IControl, ISession, ITriple> writeOperator = event.getEventOp();
                    _Logger.debug("%s→  %s | %s", OperatorType.WRITE, cmd, session);
                    encodeHandler(event, cmd, session, writeOperator);
                    cmd.dispose();
                    break;
                case WROTE:
                    IPair pairWroteContent = event.getContent();
                    int wroteCnt = pairWroteContent.getFirst();
                    session = pairWroteContent.getSecond();
                    IOperator<Integer, ISession, ITriple> wroteOperator = event.getEventOp();
                    _Logger.debug("%s→  %s | %s", OperatorType.WROTE, wroteCnt, session);
                    encodeHandler(event, wroteCnt, session, wroteOperator);
                    break;
                default:
                    break;
            }
        }
    }

    private <A> void encodeHandler(QEvent event, A a, ISession session, IOperator<A, ISession, ITriple> operator)
    {
        IPContext context = session.getContext();
        if(context instanceof IEContext) {
            IEContext eContext = (IEContext) context;
            eContext.setEncryptHandler(_EncryptHandler);
        }
        try {
            operator.handle(a, session);
            event.reset();
        }
        catch(Exception e) {
            _Logger.warning(String.format("write encode error: %s", session.toString()), e);
            context.advanceOutState(IPContext.ENCODE_ERROR);
            event.error(IError.Type.FILTER_ENCODE, new Pair<>(e, session), session.getError());
        }
    }
}
