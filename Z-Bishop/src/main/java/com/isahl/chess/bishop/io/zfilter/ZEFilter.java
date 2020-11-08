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
package com.isahl.chess.bishop.io.zfilter;

import com.isahl.chess.bishop.io.ZContext;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.inf.IEContext;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPacket;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class ZEFilter<T extends ZContext & IEContext>
        extends
        AioFilterChain<T,
                       IPacket,
                       IPacket>
{
    public final static String NAME = "z-tls";

    public ZEFilter()
    {
        super(NAME);
    }

    @Override
    public ResultType seek(T context, IPacket output)
    {
        return null;
    }

    @Override
    public ResultType peek(T context, IPacket input)
    {
        return null;
    }

    @Override
    public IPacket encode(T context, IPacket output)
    {
        if (context.isOutCrypt() && output.outIdempotent(getLeftIdempotentBit())) {
            context.getSymmetricEncrypt()
                   .digest(output.getBuffer(), context.getSymmetricKeyOut());
            /* cipher with old symmetric key -> new symmetric key */
            if (context.needUpdateKeyOut()) {
                context.swapKeyOut(context.getReRollKey());
                context.getSymmetricEncrypt()
                       .reset();
            }
        }
        else if (context.needUpdateKeyOut()) {
            /* plain -> cipher X04/X05 encoded in command-zfilter */
            _Logger.debug("X04/X05 done,change state from plain to cipher in next encoding conversion");
            context.swapKeyOut(context.getReRollKey());
            context.getSymmetricEncrypt()
                   .reset();
            context.cryptOut();
        }
        return output;
    }

    @Override
    public IPacket decode(T context, IPacket input)
    {
        if (context.needUpdateKeyIn()) {
            context.swapKeyIn(context.getReRollKey());
            context.getSymmetricDecrypt()
                   .reset();
            context.cryptIn();
        }
        if (context.isInCrypt() && input.inIdempotent(getRightIdempotentBit())) {
            context.getSymmetricDecrypt()
                   .digest(input.getBuffer(), context.getSymmetricKeyIn());
        }
        return input;
    }

    @Override
    public <C extends IPContext,
            O extends IProtocol> ResultType pipeSeek(C context, O output)
    {
        return null;
    }

    @Override
    public <C extends IPContext,
            I extends IProtocol> ResultType pipePeek(C context, I input)
    {
        return null;
    }

    @Override
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> I pipeEncode(C context, O output)
    {
        return null;
    }

    @Override
    public <C extends IPContext,
            O extends IProtocol,
            I extends IProtocol> O pipeDecode(C context, I input)
    {
        return null;
    }

}
