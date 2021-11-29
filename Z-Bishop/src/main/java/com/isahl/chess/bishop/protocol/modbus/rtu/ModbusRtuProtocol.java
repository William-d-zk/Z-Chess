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

package com.isahl.chess.bishop.protocol.modbus.rtu;

import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

import java.nio.ByteBuffer;

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

    protected byte   mAddress;
    protected byte   mCtrl;
    protected byte[] mPayload;
    protected int    mCrc;

    @Override
    public void encodec(ByteBuffer output)
    {
        int off = output.position();
        output.put(mAddress);
        output.put(mCtrl);
        output.put(mPayload);
        output.putShort((short) (mCrc = CryptoUtil.crc16_modbus(output.array(), off, output.position() - off)));
    }

    @Override
    public void decodec(ByteBuffer input)
    {
        mAddress = input.get();
        mCtrl = input.get();
        mPayload = new byte[input.remaining() - 2];
        input.get(mPayload);
        mCrc = input.getShort();
    }

    @Override
    public int length()
    {
        return 4 + (mPayload == null ? 0 : mPayload.length);
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
