/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
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
package com.tgx.z.queen.event.handler;

import static com.tgx.z.queen.event.inf.IError.Type.FILTER_DECODE;
import static com.tgx.z.queen.event.inf.IOperator.Type.DISPATCH;
import static com.tgx.z.queen.event.operator.OperatorHolder.ERROR_OPERATOR;
import static com.tgx.z.queen.io.core.inf.IContext.DECODE_ERROR;

import com.lmax.disruptor.EventHandler;
import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.base.util.Pair;
import com.tgx.z.queen.base.util.Triple;
import com.tgx.z.queen.event.inf.IOperator;
import com.tgx.z.queen.event.processor.QEvent;
import com.tgx.z.queen.io.core.inf.ICommand;
import com.tgx.z.queen.io.core.inf.IContext;
import com.tgx.z.queen.io.core.inf.IPacket;
import com.tgx.z.queen.io.core.inf.ISession;
import com.tgx.z.queen.io.external.crypt.EncryptHandler;

/**
 * @author William.d.zk
 */
public class DecodeHandler
        implements
        EventHandler<QEvent>
{
    protected final Logger         _Log            = Logger.getLogger(getClass().getName());
    protected final EncryptHandler _EncryptHandler = new EncryptHandler();

    /**
     * 错误由接下去的 Handler 负责投递 Close 事件给 IoDispatcher
     *
     * @see IoDispatcher
     */
    @Override
    public void onEvent(QEvent event, long sequence, boolean batch) throws Exception {
        /*
        错误事件已在同级旁路中处理，此处不再关心错误处理
         */
        IOperator<IPacket, ISession> packetOperator = event.getEventOp();
        Pair<IPacket, ISession> packetContent = event.getContent();
        ISession session = packetContent.second();
        IContext context = session.getContext();
        context.setEncryptHandler(_EncryptHandler);
        IPacket packet = packetContent.first();
        if (!context.isInErrorState()) {
            try {
                Triple<ICommand[], ISession, IOperator<ICommand[], ISession>> result = packetOperator.handle(packet, session);
                transfer(event, result.first(), session, result.third());
            }
            catch (Exception e) {
                _Log.warning(String.format("read decode error\n %s", session.toString()), e);
                context.setInState(DECODE_ERROR);
                event.error(FILTER_DECODE, e, session, ERROR_OPERATOR());
            }
        }
    }

    protected void transfer(QEvent event, ICommand[] commands, ISession session, IOperator<ICommand[], ISession> operator) {
        event.produce(DISPATCH, commands, session, operator);
    }

}
