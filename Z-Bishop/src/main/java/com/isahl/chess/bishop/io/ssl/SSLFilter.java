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

import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;

import java.nio.ByteBuffer;

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
            return new AioPacket(context.doWrap(output.getBuffer())
                                        .flip());
        }
        return output;
    }

    @Override
    public ResultType seek(SSLZContext<A> context, IPacket output)
    {
        if(context.isOutFrame() &&
           (context.getHandShakeStatus() == NOT_HANDSHAKING || context.getHandShakeStatus() == FINISHED))
        {
            context.updateOut();
            _Logger.info("SSL ready to write");
            return ResultType.NEXT_STEP;
        }
        else if(context.isOutConvert()) {return ResultType.NEXT_STEP;}
        return ResultType.IGNORE;
    }

    @Override
    public IPacket decode(SSLZContext<A> context, IPacket input)
    {
        return context.getCarrier();
    }

    @Override
    public ResultType peek(SSLZContext<A> context, IPacket input)
    {
        ByteBuffer appInBuffer;
        ByteBuffer netInBuffer = input.getBuffer();
        ByteBuffer localBuffer = context.getRvBuffer();
        if(netInBuffer.hasRemaining()) {
            if(localBuffer.position() > 0) {
                IoUtil.write(netInBuffer, localBuffer);
                localBuffer.flip();
                netInBuffer = localBuffer;
            }
            appInBuffer = context.doUnwrap(netInBuffer);
            if(netInBuffer == localBuffer && localBuffer.hasRemaining()) {
                /*
                ssl 并非流式解码，而是存在块状解码的，所以粘包【半包】需要处理
                上文中 netInBuffer 的数据转移到了localBuffer里
                 */
                netInBuffer = input.getBuffer();
                netInBuffer.position(netInBuffer.position() - localBuffer.remaining());
                localBuffer.clear();
            }
            if(appInBuffer != null) {
                context.setCarrier(new AioPacket(appInBuffer.flip()));
                return ResultType.NEXT_STEP;
            }
        }
        return ResultType.NEED_DATA;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isProxy() && context instanceof SSLZContext) {
            return new Pair<>(seek((SSLZContext<A>) context, (IPacket) output), context);
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL) && context.isProxy() && context.isInConvert() &&
           context instanceof SSLZContext)
        {
            return new Pair<>(peek((SSLZContext<A>) context, (IPacket) input), context);
        }
        return new Pair<>(ResultType.IGNORE, context);
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
