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

package com.isahl.chess.bishop.io.modbus.rtu;

import com.isahl.chess.king.base.util.CryptUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.IProtocol;

/**
 * @author william.d.zk
 * @date 2021/3/21
 */
public abstract class ModbusRtuProtocol
        implements IProtocol
{

    public ModbusRtuProtocol(byte address, byte ctrl, byte[] payload)
    {
        mAddress = address;
        mCtrl = ctrl;
        mPayload = payload;
    }

    public ModbusRtuProtocol()
    {
    }

    protected int    mLength;
    protected byte   mAddress;
    protected byte   mCtrl;
    protected byte[] mPayload;
    protected int    mCrc;

    @Override
    public int encodec(byte[] data, int pos)
    {
        int off = pos;
        pos += IoUtil.writeByte(mAddress, data, pos);
        pos += IoUtil.writeByte(mCtrl, data, pos);
        pos += IoUtil.write(mPayload, data, pos);
        pos += IoUtil.writeShort(mCrc = CryptUtil.crc16_modbus(data, off, pos - off), data, pos);
        return pos;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        int off = pos;
        mAddress = data[pos++];
        mCtrl = data[pos++];
        mPayload = new byte[data.length - 2 - pos];
        pos += IoUtil.read(data, pos, mPayload);
        mCrc = IoUtil.readShort(data, pos);
        pos += 2;
        mLength = pos - off;
        return pos;
    }

    @Override
    public int dataLength()
    {
        return mLength > 0 ? mLength : (mLength = 4 + (mPayload == null ? 0 : mPayload.length));
    }

    public int getCrc()
    {
        return mCrc;
    }

    public void setAddress(byte address)
    {
        mAddress = address;
    }

    public byte getAddress()
    {
        return mAddress;
    }

}
