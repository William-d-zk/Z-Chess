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
package com.tgx.z.queen.io.external.filter;

import java.util.Objects;

import com.tgx.z.queen.base.log.Logger;
import com.tgx.z.queen.io.core.async.AioContext;
import com.tgx.z.queen.io.core.async.AioFilterChain;
import com.tgx.z.queen.io.core.inf.IPacket;
import com.tgx.z.queen.io.core.inf.IProtocol;
import com.tgx.z.queen.io.core.inf.ITlsContext;

/**
 * @author William.d.zk
 */
public class ZTlsFilter<C extends AioContext>
        extends
        AioFilterChain<C>
{

    public ZTlsFilter() {
        name = "queen-tls-filter";
    }

    private  final Logger _Log = Logger.getLogger(getClass().getName());

    @Override
    public ResultType preEncode(C context, IProtocol output) {
        if (Objects.isNull(context) || Objects.isNull(output)) return ResultType.ERROR;
        ResultType resultType = context.outState()
                                       .equals(ITlsContext.EncryptState.PLAIN) ? ResultType.IGNORE : ResultType.NEXT_STEP;
        /* plain -> cipher X04/X05 encoded in command-filter */
        if (resultType.equals(ResultType.IGNORE) && context.needUpdateKeyOut()) {
            context.swapKeyOut(context.getReRollKey());
            context.getSymmetricEncrypt()
                   .reset();
            context.cryptOut();
        }
        return resultType;
    }

    @Override
    public IProtocol encode(C context, IProtocol output) {
        IPacket _package = (IPacket) output;
        context.getSymmetricEncrypt()
               .digest(_package.getBuffer(), context.getSymmetricKeyOut());
        /* cipher with old symmetric key -> new symmetric key */
        if (context.needUpdateKeyOut()) {
            context.swapKeyOut(context.getReRollKey());
            context.getSymmetricEncrypt()
                   .reset();
        }
        return _package;
    }

    @Override
    public ResultType preDecode(C context, IProtocol input) {
        if (Objects.isNull(context) || Objects.isNull(input)) return ResultType.ERROR;
        if (context.needUpdateKeyIn()) {
            context.swapKeyIn(context.getReRollKey());
            context.getSymmetricDecrypt()
                   .reset();
            context.cryptIn();
        }
        return context.inState()
                      .equals(ITlsContext.EncryptState.PLAIN) ? ResultType.IGNORE
                                                              : input.idempotent(getIdempotentBit()) ? ResultType.IGNORE
                                                                                                     : ResultType.NEXT_STEP;
    }

    @Override
    public IProtocol decode(C context, IProtocol input) {
        IPacket _package = (IPacket) input;
        context.getSymmetricDecrypt()
               .digest(_package.getBuffer(), context.getSymmetricKeyIn());
        return input;
    }

}
