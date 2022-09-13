/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

/**
 * @author william.d.zk
 */
public class SSLFilter<A extends IPContext>
        extends AioFilterChain<SSLZContext<A>, IPacket, IPacket>
{
    public final static String NAME = "z-ssl";

    public SSLFilter()
    {
        super(NAME);
    }

    @Override
    public IPacket encode(SSLZContext<A> context, IPacket output)
    {
        if(output.outIdempotent(getLeftIdempotentBit())) {
            return new AioPacket(context.doWrap(output.getBuffer()));
        }
        return output;
    }

    @Override
    public IPacket decode(SSLZContext<A> context, IPacket input)
    {
        return context.getCarrier();
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isProxy() &&
           context instanceof SSLZContext ssl_ctx && output instanceof IPacket out_packet)
        {
            NEXT_STEP:
            {
                if(ssl_ctx.isOutFrame() &&
                   (ssl_ctx.getHandShakeStatus() == NOT_HANDSHAKING || ssl_ctx.getHandShakeStatus() == FINISHED))
                {
                    ssl_ctx.promotionOut();
                    _Logger.info("SSL ready to write");
                }
                else if(ssl_ctx.isOutConvert()) {
                    _Logger.info("SSL to write");
                }
                else {
                    break NEXT_STEP;
                }
                return Pair.of(ResultType.NEXT_STEP, ssl_ctx);
            }
        }
        return Pair.of(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isProxy() && context.isInConvert() &&
           context instanceof SSLZContext ssl_ctx && input instanceof IPacket in_packet)
        {
            ByteBuf appInBuffer;
            ByteBuf netInBuffer = in_packet.getBuffer();
            ByteBuf localBuffer = ssl_ctx.getRvBuffer();
            if(localBuffer.isReadable()) {
                //存在上次没处理完的内容，否则直接解码，减少一次内存copy
                localBuffer.append(netInBuffer);
                netInBuffer = localBuffer;
            }
            appInBuffer = ssl_ctx.doUnwrap(netInBuffer);
            if(netInBuffer.isReadable()) {
                /*
                ssl 并非流式解码，而是存在块状解码的，所以粘包【半包】需要处理
                上文中 netInBuffer 的数据转移到了localBuffer里
                 */
                if(netInBuffer == localBuffer) {
                    localBuffer.discard();
                }
                else {
                    localBuffer.append(netInBuffer);
                }
                if(appInBuffer != null) {
                    ssl_ctx.setCarrier(new AioPacket(appInBuffer));
                    return Pair.of(ResultType.NEXT_STEP, ssl_ctx);
                }
            }
            return Pair.of(ResultType.NEED_DATA, ssl_ctx);
        }
        return Pair.of(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((SSLZContext<A>) context, (IPacket) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((SSLZContext<A>) context, (IPacket) input);
    }

}
