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
package com.tgx.chess.bishop.io.ws.filter;

import com.tgx.chess.bishop.io.ws.WsControl;
import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.ws.control.X101_HandShake;
import com.tgx.chess.bishop.io.ws.control.X102_SslHandShake;
import com.tgx.chess.bishop.io.ws.control.X103_Close;
import com.tgx.chess.bishop.io.ws.control.X104_Ping;
import com.tgx.chess.bishop.io.ws.control.X105_Pong;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.control.X107_Redirect;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class WsControlFilter
        extends
        AioFilterChain<ZContext,
                       WsControl,
                       WsFrame>
{
    public WsControlFilter()
    {
        super("ws_control");
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        ResultType result = preControlEncode(context, output);
        if (result.equals(ResultType.NEXT_STEP)) {
            return output.serial() == X101_HandShake.COMMAND
                   || output.serial() == X102_SslHandShake.COMMAND ? ResultType.ERROR
                                                                   : result;
        }
        return result;
    }

    @Override
    public WsFrame encode(ZContext context, WsControl output)
    {
        WsFrame frame = new WsFrame();
        _Logger.debug("control %s", output);
        frame.setPayload(output.getPayload());
        frame.setCtrl(output.getCtrl());
        return frame;
    }

    @Override
    public ResultType preDecode(ZContext context, WsFrame input)
    {
        return preControlDecode(context, input);
    }

    @Override
    public WsControl decode(ZContext context, WsFrame input)
    {
        switch (input.frame_op_code & 0x0F)
        {
            case WsFrame.frame_op_code_ctrl_close:
                return new X103_Close(input.getPayload());
            case WsFrame.frame_op_code_ctrl_ping:
                return new X104_Ping(input.getPayload());
            case WsFrame.frame_op_code_ctrl_pong:
                return new X105_Pong(input.getPayload());
            case WsFrame.frame_op_code_ctrl_cluster:
                return new X106_Identity(input.getPayload());
            case WsFrame.frame_op_code_ctrl_redirect:
                return new X107_Redirect(input.getPayload());
            default:
                throw new UnsupportedOperationException(String.format("web socket frame with control code %d.",
                                                                      input.frame_op_code & 0x0F));
        }
    }
}
