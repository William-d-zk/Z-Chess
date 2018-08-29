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
package com.tgx.z.queen.io.external.websokcet;

import com.tgx.z.queen.io.core.inf.ICommand;

/**
 * @author William.d.zk
 */
public abstract class WsHandshake
        implements
        ICommand
{

    private final String _Msg;
    private String       xMsg;

    public WsHandshake(String msg) {
        _Msg = msg;
    }

    @Override
    public int getPriority() {
        return QOS_00_NETWORK_CONTROL;
    }

    @Override
    public int getSuperSerial() {
        return CONTROL_SERIAL;
    }

    public String getMessage() {
        return _Msg != null ? _Msg : xMsg;
    }

    public byte getControl() {
        return WsFrame.frame_op_code_ctrl_handshake;
    }

    public byte[] getPayload() {
        return _Msg == null ? xMsg == null ? null : xMsg.getBytes() : _Msg.getBytes();
    }

    @Override
    public String toString() {
        return String.format("web socket handshake %s", _Msg);
    }

    @Override
    public int dataLength() {
        return _Msg != null ? _Msg.getBytes().length : xMsg != null ? xMsg.getBytes().length : 0;
    }

    public WsHandshake append(String x) {
        xMsg = xMsg == null ? x : xMsg + x;
        return this;
    }

    public WsHandshake ahead(String x) {
        xMsg = xMsg == null ? x : x + xMsg;
        return this;
    }

    @Override
    public byte[] encode() {
        return getPayload();
    }

    @Override
    public void dispose() {
        xMsg = null;
    }
}
