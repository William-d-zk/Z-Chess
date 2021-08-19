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

package com.isahl.chess.bishop.io.ws.filter;

import com.isahl.chess.bishop.io.ws.IWsContext;
import com.isahl.chess.bishop.io.ws.WsFrame;
import com.isahl.chess.bishop.io.ws.WsProxyContext;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.async.AioFilterChain;
import com.isahl.chess.queen.io.core.async.AioPacket;
import com.isahl.chess.queen.io.core.inf.*;

import java.nio.ByteBuffer;

/**
 * @author william.d.zk
 * @date 2019-05-07
 */
public class WsProxyFilter<A extends IPContext>
        extends AioFilterChain<WsProxyContext<A>, IPacket, WsFrame>
{

    public WsProxyFilter()
    {
        super("ws_proxy");

    }

    @Override
    public WsFrame encode(WsProxyContext<A> context, IPacket output)
    {
        WsFrame frame = new WsFrame();
        frame.putPayload(output.getBuffer()
                               .array());
        frame.putCtrl(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public IPacket decode(WsProxyContext<A> context, WsFrame input)
    {
        return new AioPacket(ByteBuffer.wrap(input.payload()));
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PACKET_SERIAL)) {
            IPContext acting = context;
            while(acting.isProxy()) {
                if(acting instanceof IWsContext && acting.isOutConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
                acting = ((IProxyContext<?>) acting).getActingContext();
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.FRAME_SERIAL) && !((IFrame) input).isCtrl()) {
            IPContext acting = context;
            while(acting.isProxy()) {
                if(acting instanceof IWsContext && acting.isInConvert()) {
                    return new Pair<>(ResultType.PROXY, acting);
                }
                acting = ((IProxyContext<?>) acting).getActingContext();
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((WsProxyContext<A>) context, (IPacket) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((WsProxyContext<A>) context, (WsFrame) input);
    }

}
