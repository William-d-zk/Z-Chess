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

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

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
    public ByteBuf suffix(ByteBuf output)
    {
        output.markWriter();
        output.put(mAddress)
              .put(mCtrl)
              .put(mPayload)
              .putShort((short) (mCrc = CryptoUtil.crc16_modbus(output.array(),
                                                                output.writerMark(),
                                                                output.writerIdx() - output.writerMark())));
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        input.markReader();
        mAddress = input.get();
        mCtrl = input.get();
        return input.readableBytes();
    }

    @Override
    public void fold(ByteBuf input, int remain)
    {
        if(remain > 0) {
            mPayload = new byte[remain - 2];
            input.get(mPayload);
            mCrc = input.getUnsignedShort();
            if(CryptoUtil.crc16_modbus(input.array(), input.readerMark(), input.readerIdx() - input.readerMark()) !=
               mCrc)
            {throw new ZException("modbus crc error");}

        }
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

    @Override
    public ModbusRtuProtocol withSub(IoSerial sub)
    {
        mPayload = sub.encode()
                      .array();
        return this;
    }

}
