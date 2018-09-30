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

package com.tgx.chess.queen.io.external.websokcet.bean.device;

import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.external.websokcet.WsContext;
import com.tgx.chess.queen.io.external.zprotocol.Command;

public class X23_SignInResult
        extends
        Command<WsContext>
{
    public final static int COMMAND = 0x23;

    public X23_SignInResult() {
        super(COMMAND, false);
    }

    @Override
    public int getPriority() {
        return QOS_09_CONFIRM_MESSAGE;
    }

    private boolean success;

    private String  token;

    @Override
    public int dataLength() {
        return super.dataLength() + 65;
    }

    @Override
    public int decodec(byte[] data, int pos) {
        success = data[pos++] > 0;
        token = IoUtil.readString(data, pos, 64);
        pos += 64;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos) {
        pos += IoUtil.writeByte(isSuccess() ? 1 : 0, data, pos);
        pos += IoUtil.write(token.getBytes(), data, pos);
        return pos;
    }

    public void setSuccess() {
        success = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setFailed() {
        success = false;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
