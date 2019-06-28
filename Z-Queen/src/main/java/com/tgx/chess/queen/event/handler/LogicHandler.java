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

package com.tgx.chess.queen.event.handler;

import static com.tgx.chess.queen.event.inf.IOperator.Type.WRITE;

import java.util.Objects;

import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.event.inf.IPipeEventHandler;
import com.tgx.chess.queen.event.processor.QEvent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
public class LogicHandler<C extends IContext<C>>
        implements
        IPipeEventHandler<QEvent>
{
    private final Logger             _Log = Logger.getLogger(getClass().getName());
    private final ICommandHandler<C> _CommandHandler;

    public LogicHandler(IPipeEncoder<C> encoder,
                        ICommandHandler<C> commandHandler)
    {
        _CommandHandler = commandHandler;
    }

    @Override
    public void onEvent(QEvent event, long sequence, boolean endOfBatch) throws Exception
    {
        if (IOperator.Type.LOGIC.equals(event.getEventType())) {
            IPair logicContent = event.getContent();
            IControl<C> cmd = logicContent.first();
            ISession<C> session = logicContent.second();
            if (Objects.isNull(cmd)) {
                _Log.warning("cmd null");
            }
            else {
                cmd = _CommandHandler.onCommand(cmd, session, this);
                if (Objects.nonNull(cmd)) {
                    event.produce(WRITE,
                                  new Pair<>(new IControl[] { cmd }, session),
                                  session.getContext()
                                         .getTransfer());
                }
                else {
                    event.ignore();
                }
            }
        }

    }

    public interface ICommandHandler<C extends IContext<C>>
    {
        IControl<C> onCommand(IControl<C> input, ISession<C> session, LogicHandler<C> handler);
    }
}
