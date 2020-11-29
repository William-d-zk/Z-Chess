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
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IPipeDecoder
        extends
        IOperator<IPacket,
                  ISession,
                  ITriple>
{
    default IControl[] filterRead(IPacket input, ISession session)
    {
        IPContext context = session.getContext();
        if (context == null || input == null) { return null; }
        final IFilterChain _Header = session.getFilterChain()
                                            .getChainHead();
        IControl[] commands = null;
        IProtocol protocol = input;
        IPacket proxy = null;

        for (IFilterChain next = _Header;; next = _Header, protocol = input, context = session.getContext()) {
            Chain:
            {
                IFilter.ResultType resultType = IFilter.ResultType.IGNORE;
                while (next != null) {
                    IPipeFilter pipeFilter = next.getPipeFilter();
                    Pair<IFilter.ResultType,
                         IPContext> peekResult = pipeFilter.pipePeek(context, protocol);
                    resultType = peekResult.getFirst();
                    context = peekResult.getSecond();
                    switch (resultType)
                    {
                        case ERROR:
                            throw new ZException("error input: %s ;filter: %s ", protocol, next.getName());
                        case NEED_DATA:
                            if (commands != null) {
                                return commands;
                            }
                            else if (proxy != null) {
                                protocol = proxy.rewind();
                                proxy = null;
                                break;
                            }
                            return null;
                        case NEXT_STEP:
                            protocol = pipeFilter.pipeDecode(context, protocol);
                            break;
                        case PROXY:
                            proxy = pipeFilter.pipeDecode(context, protocol);
                            break Chain;
                        case HANDLED:
                            IControl cmd = pipeFilter.pipeDecode(context, protocol);
                            if (cmd != null) {
                                cmd.setSession(session);
                                if (commands == null) {
                                    commands = new IControl[]{cmd};
                                }
                                else {
                                    IControl[] nCmd = new IControl[commands.length + 1];
                                    IoUtil.addArray(commands, nCmd, cmd);
                                    commands = nCmd;
                                }
                            }
                            break Chain;
                        case CANCEL:
                            return null;
                        default:
                            break;
                    }
                    next = next.getNext();
                }
                if (resultType == IFilter.ResultType.IGNORE) {
                    throw new ZException("no filter handle input: %s ", protocol);
                }
            }
        }
    }
}
