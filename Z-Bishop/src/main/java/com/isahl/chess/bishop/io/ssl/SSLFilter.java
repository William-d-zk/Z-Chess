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

package com.isahl.chess.bishop.io.ssl;

import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 */
public class SSLFilter<A extends IPContext<A>>
        extends
        AioFilterChain<SSLZContext<A>,
                       IPacket,
                       IPacket>
{
    public final static String NAME = "z-ssl";

    public SSLFilter()
    {
        super(NAME);
    }

    @Override
    public ResultType seek(SSLZContext<A> context, IPacket output)
    {
        return null;
    }

    @Override
    public ResultType peek(SSLZContext<A> context, IPacket input)
    {
        return null;
    }

    @Override
    public IPacket encode(SSLZContext<A> context, IPacket output)
    {
        return null;
    }

    @Override
    public IPacket decode(SSLZContext<A> context, IPacket input)
    {
        return null;
    }

    @Override
    public <C extends IPContext<C>,
            O extends IProtocol> ResultType pipeSeek(C context, O output)
    {
        return null;
    }

    @Override
    public <C extends IPContext<C>,
            I extends IProtocol> ResultType pipePeek(C context, I input)
    {
        return null;
    }

    @Override
    public <C extends IPContext<C>,
            O extends IProtocol,
            I extends IProtocol> I pipeEncode(C context, O output)
    {
        return null;
    }

    @Override
    public <C extends IPContext<C>,
            O extends IProtocol,
            I extends IProtocol> O pipeDecode(C context, I input)
    {
        return null;
    }

}
