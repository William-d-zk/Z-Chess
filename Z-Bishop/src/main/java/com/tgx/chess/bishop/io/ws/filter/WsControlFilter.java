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
package com.tgx.chess.bishop.io.ws.filter;

import com.tgx.chess.bishop.io.ws.bean.WsContext;
import com.tgx.chess.bishop.io.ws.bean.WsControl;
import com.tgx.chess.bishop.io.ws.bean.WsFrame;
import com.tgx.chess.bishop.io.zprotocol.control.X101_HandShake;
import com.tgx.chess.bishop.io.zprotocol.control.X102_SslHandShake;
import com.tgx.chess.bishop.io.zprotocol.control.X103_Close;
import com.tgx.chess.bishop.io.zprotocol.control.X104_Ping;
import com.tgx.chess.bishop.io.zprotocol.control.X105_Pong;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.control.X107_Redirect;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class WsControlFilter<C extends WsContext>
        extends
        AioFilterChain<C>
{
    private final Logger _Log = Logger.getLogger(getClass().getName());

    public WsControlFilter() {
        super("network-control-zfilter");
    }

    @Override
    public ResultType preEncode(C context, IProtocol output) {
        if (context == null || output == null) { return ResultType.ERROR; }
        if (context.isOutConvert()) {
            switch (output.superSerial()) {
                case IProtocol.CONTROL_SERIAL:
                    switch (output.getSerial()) {
                        case X101_HandShake.COMMAND:
                        case X102_SslHandShake.COMMAND:
                            return ResultType.ERROR;
                        default:
                            return ResultType.NEXT_STEP;
                    }
                case IProtocol.COMMAND_SERIAL:
                case IProtocol.FRAME_SERIAL:
                    return ResultType.IGNORE;
                default:
                    return ResultType.ERROR;
            }
        }
        return ResultType.IGNORE;
    }

    @Override
    public WsFrame encode(C context, IProtocol output) {
        WsFrame frame = new WsFrame();
        WsControl control = (WsControl) output;
        _Log.info("control %s", control);
        frame.setPayload(control.getPayload());
        frame.setCtrl(control.getControl());
        return frame;
    }

    @Override
    public ResultType preDecode(C context, IProtocol input) {
        if (context == null || input == null) { return ResultType.ERROR; }
        if (!context.isInConvert()) { return ResultType.IGNORE; }
        if (!(input instanceof WsFrame)) { return ResultType.ERROR; }
        WsFrame frame = (WsFrame) input;
        if (frame.isNoCtrl()) { return ResultType.IGNORE; }
        WsFrame wsFrame = (WsFrame) input;
        switch (wsFrame.frame_op_code & 0x0F) {
            case WsFrame.frame_op_code_ctrl_close:
            case WsFrame.frame_op_code_ctrl_ping:
            case WsFrame.frame_op_code_ctrl_pong:
            case WsFrame.frame_op_code_ctrl_cluster:
            case WsFrame.frame_op_code_ctrl_redirect:
                return ResultType.HANDLED;
            default:
                return ResultType.ERROR;
        }
    }

    @Override
    public WsControl decode(C context, IProtocol input) {
        WsFrame wsFrame = (WsFrame) input;
        switch (wsFrame.frame_op_code & 0x0F) {
            case WsFrame.frame_op_code_ctrl_close:
                return new X103_Close(wsFrame.getPayload());
            case WsFrame.frame_op_code_ctrl_ping:
                return new X104_Ping(wsFrame.getPayload());
            case WsFrame.frame_op_code_ctrl_pong:
                return new X105_Pong(wsFrame.getPayload());
            case WsFrame.frame_op_code_ctrl_cluster:
                return new X106_Identity(wsFrame.getPayload());
            case WsFrame.frame_op_code_ctrl_redirect:
                return new X107_Redirect(wsFrame.getPayload());
            default:
                return null;
        }
    }
}
