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

package com.isahl.chess.queen.io.core.inf;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IPipeDecoder<C extends IContext<C>>
        extends IOperator<IPacket, ISession<C>, ITriple>
{
    @SuppressWarnings("unchecked")
    default IControl<C>[] filterRead(IPacket input, ISession<C> session)
    {
        final IFilterChain<C> _Header  = session.getContext().getSort().getFilterChain().getChainHead();
        C                     context  = session.getContext();
        IControl<C>[]         commands = null;
        IFilter.ResultType    resultType;
        IProtocol             protocol = input;
        for (IFilterChain<C> next = _Header;; next = _Header, protocol = input)
        {
            Chain:
            while (next != null)
            {
                resultType = next.getFilter().checkType(protocol) ?
                        next.getFilter().preDecode(context, protocol):
                        IFilter.ResultType.IGNORE;
                switch (resultType)
                {
                    case ERROR:
                        throw new ZException(String.format("filter:%s error", next.getName()));
                    case NEED_DATA:
                        return commands;
                    case NEXT_STEP:
                        protocol = next.getFilter().decode(context, protocol);
                        break;
                    case HANDLED:
                        IControl<C> cmd = (IControl<C>) next.getFilter().decode(context, protocol);
                        if (cmd != null && !cmd.isProxy())
                        {
                            cmd.setSession(session);
                            if (commands == null)
                            {
                                commands = new IControl[] { cmd
                                };
                            }
                            else
                            {
                                IControl<C>[] nCmd = new IControl[commands.length + 1];
                                IoUtil.addArray(commands, nCmd, cmd);
                                commands = nCmd;
                            }
                        }
                        else if (cmd != null && cmd.isProxy())
                        {
                            IControl<C>[] bodies = cmd.getProxyBodies();
                            if (bodies != null)
                            {
                                for (IControl<C> body : bodies)
                                {
                                    if (body != null)
                                    {
                                        if (commands == null)
                                        {
                                            commands = new IControl[] { body
                                            };
                                        }
                                        else
                                        {
                                            IControl<C>[] nCmd = new IControl[commands.length + 1];
                                            IoUtil.addArray(commands, nCmd, cmd);
                                            commands = nCmd;
                                        }
                                    }
                                }
                            }
                        }
                        break Chain;
                    default:
                        break;
                }
                next = next.getNext();
            }
        }
    }
}
