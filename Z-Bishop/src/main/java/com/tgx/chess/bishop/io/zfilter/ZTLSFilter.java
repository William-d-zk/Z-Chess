/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.bishop.io.zfilter;

import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class ZTLSFilter
        extends
        AioFilterChain<ZContext,
                       IPacket,
                       IPacket>
{
    public final static String NAME = "z-tls";

    public ZTLSFilter()
    {
        super(NAME);
    }

    @Override
    public boolean checkType(IProtocol protocol)
    {
        return checkType(protocol, IPacket.PACKET_SERIAL);
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return prePacketEncode(context, output);
    }

    @Override
    public IPacket encode(ZContext context, IPacket output)
    {
        if (!context.isOutCrypt() && context.needUpdateKeyOut()) {
            /* plain -> cipher X04/X05 encoded in command-zfilter */
            _Logger.debug("X04/X05 done,change state from plain to cipher in next encoding conversion");
            context.swapKeyOut(context.getReRollKey());
            context.getSymmetricEncrypt()
                   .reset();
            context.cryptOut();
        }
        else if (context.isOutCrypt()) {
            context.getSymmetricEncrypt()
                   .digest(output.getBuffer(), context.getSymmetricKeyOut());
            /* cipher with old symmetric key -> new symmetric key */
            if (context.needUpdateKeyOut()) {
                context.swapKeyOut(context.getReRollKey());
                context.getSymmetricEncrypt()
                       .reset();
            }
        }
        return output;
    }

    @Override
    public ResultType preDecode(ZContext context, IPacket input)
    {
        return prePacketDecode(context, input);
    }

    @Override
    public IPacket decode(ZContext context, IPacket input)
    {
        if (context.needUpdateKeyIn()) {
            context.swapKeyIn(context.getReRollKey());
            context.getSymmetricDecrypt()
                   .reset();
            context.cryptIn();
        }
        if (context.isInCrypt() && !input.idempotent(getIdempotentBit())) {
            context.getSymmetricDecrypt()
                   .digest(input.getBuffer(), context.getSymmetricKeyIn());
        }
        input.frameReady();
        return input;
    }

}
