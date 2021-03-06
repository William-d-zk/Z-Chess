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

import java.util.Arrays;

import com.isahl.chess.king.base.disruptor.event.OperatorType;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.inf.IError;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.QEvent;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.IEContext;
import com.isahl.chess.queen.io.core.inf.IEncryptHandler;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.lmax.disruptor.EventHandler;

/**
 * @author William.d.zk
 */
public class DecodeHandler
        implements
        EventHandler<QEvent>
{
    protected final Logger _Logger = Logger.getLogger("io.queen.processor." + getClass().getSimpleName());

    private final IEncryptHandler _EncryptHandler;

    public DecodeHandler(IEncryptHandler encryptHandler)
    {
        _EncryptHandler = encryptHandler;
    }

    /**
     * 错误由接下去的 Handler 负责投递
     * 标记为Error后交由BarrierHandler进行分发
     */
    @Override
    public void onEvent(QEvent event, long sequence, boolean batch) throws Exception
    {
        /*
         * 错误事件已在同级旁路中处理，此处不再关心错误处理
         */
        IPair packetContent = event.getContent();
        ISession session = packetContent.getSecond();
        IOperator<IPacket,
                  ISession,
                  ITriple> packetOperator = event.getEventOp();
        IPacket packet = packetContent.getFirst();
        IPContext context = session.getContext();
        if (context instanceof IEContext) {
            IEContext eContext = (IEContext) context;
            eContext.setEncryptHandler(_EncryptHandler);
        }
        try {
            ITriple result = packetOperator.handle(packet, session);
            if (result != null) {
                IControl[] commands = result.getFirst();
                _Logger.trace("decoded commands:%s", Arrays.toString(commands));
                transfer(event, commands, session, result.getThird());
            }
            else {
                event.reset();
            }
        }
        catch (Exception e) {
            _Logger.warning("read decode error: %s", session, e);
            context.advanceInState(IPContext.DECODE_ERROR);
            // 此处为Pipeline中间环节，使用event进行事件传递，不使用dispatcher
            event.error(IError.Type.FILTER_DECODE, new Pair<>(e, session), session.getError());
        }
    }

    protected void transfer(QEvent event,
                            IControl[] commands,
                            ISession session,
                            IOperator<IControl[],
                                      ISession,
                                      ITriple> operator)
    {
        event.produce(OperatorType.DISPATCH, new Pair<>(commands, session), operator);
    }
}
