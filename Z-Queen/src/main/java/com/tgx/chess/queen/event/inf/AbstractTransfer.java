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

package com.tgx.chess.queen.event.inf;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tgx.chess.king.base.exception.MissingParameterException;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * 应用于解码完成后进入logic 处理时，向write 操作注入 writer 的操作
 *
 * @author william.d.zk
 *
 */
public class AbstractTransfer<C extends IContext>
        implements
        IOperator<ICommand[], ISession<C>, List<IPair>>
{
    private final IOperator<ICommand, ISession<C>, IPacket> _Encoder;

    public AbstractTransfer(IOperator<ICommand, ISession<C>, IPacket> encoder) {
        _Encoder = encoder;
    }

    @Override
    public List<IPair> handle(ICommand[] commands, ISession<C> session) {
        if (Objects.isNull(commands) || commands.length == 0) { throw new MissingParameterException(toString() + ".transfer", "commands"); }
        return Stream.of(commands)
                     .map(command -> new Pair<>(command.setSession(session), session))
                     .collect(Collectors.toList());
    }

    public IOperator<ICommand, ISession<C>, IPacket> getNextOperator() {
        return _Encoder;
    }
}
