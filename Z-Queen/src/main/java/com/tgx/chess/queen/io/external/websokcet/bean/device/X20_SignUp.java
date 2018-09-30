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

public class X20_SignUp
        extends
        Command<WsContext>
{
    public final static int COMMAND = 0x20;

    public X20_SignUp() {
        super(COMMAND, false);
    }

    @Override
    public boolean isMappingCommand() {
        return true;
    }

    @Override
    public int getPriority() {
        return QOS_06_META_CREATE;
    }

    private byte[] sn = new byte[32];
    private String password;
    private long   passwordId;

    @Override
    public int dataLength() {
        return super.dataLength() + 41;
    }

    @Override
    public int decodec(byte[] data, int pos) {
        pos = IoUtil.read(data, pos, sn);
        passwordId = IoUtil.readLong(data, pos);
        pos += 8;
        int passwordBytesLength = data[pos++] & 0xFF;
        password = IoUtil.readString(data, pos, passwordBytesLength);
        pos += passwordBytesLength;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos) {
        pos += IoUtil.write(sn, data, pos);
        pos += IoUtil.writeLong(passwordId, data, pos);
        byte[] passwordBytes = password.getBytes();
        pos += IoUtil.writeByte(passwordBytes.length, data, pos);
        pos += IoUtil.write(passwordBytes, data, pos);
        return pos;
    }

    public byte[] getSn() {
        return sn;
    }

    public void setSn(byte[] sn) {
        System.arraycopy(sn, 0, this.sn, 0, 32);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getPasswordId() {
        return passwordId;
    }

    public void setPasswordId(long passwordId) {
        this.passwordId = passwordId;
    }
}
