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

import com.tgx.chess.king.base.exception.ZException;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.event.inf.IOperator;

/**
 * @author William.d.zk
 */
public interface IPipeDecoder<C extends IContext>
        extends
        IOperator<IPacket, ISession<C>, ITriple>
{
    @SuppressWarnings("unchecked")
    default IControl<C>[] filterRead(IPacket input, IFilterChain<C> filterChain, ISession<C> session) {
        final IFilterChain<C> _Header = filterChain.getChainHead();
        final C _Context = session.getContext();
        IControl<C>[] commands = null;
        IFilter.ResultType resultType;
        IProtocol protocol = input;
        for (IFilterChain<C> next = _Header;; next = _Header, protocol = input) {
            Chain:
            while (next != null) {
                resultType = next.getFilter()
                                 .preDecode(_Context, protocol);
                switch (resultType) {
                    case ERROR:
                        throw new ZException(String.format("filter:%s error", next.getName()));
                    case NEED_DATA:
                        return commands;
                    case NEXT_STEP:
                        protocol = next.getFilter()
                                       .decode(_Context, protocol);
                        break;
                    case HANDLED:
                        IControl<C> cmd = (IControl<C>) next.getFilter()
                                                            .decode(_Context, protocol);
                        if (cmd != null) {
                            cmd.setSession(session);
                            if (commands == null) {
                                commands = new IControl[] { cmd };
                            }
                            else {
                                IControl<C>[] nCmd = new IControl[commands.length + 1];
                                IoUtil.addArray(commands, nCmd, cmd);
                                commands = nCmd;
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
