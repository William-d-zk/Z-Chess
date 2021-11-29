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
package com.isahl.chess.bishop.protocol.zchat.model.base;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.king.base.features.IDuplicate;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.I18nUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static java.lang.String.format;

/**
 * @author William.d.zk
 */
public abstract class ZProtocol
        implements IProtocol,
                   IQoS,
                   IDuplicate
{
    public final static  int version                 = 0x3;
    private final static int g_msg_uid_size          = 8;
    private final static int min_no_msg_uid_size     = 1 + 1 + 1 + 4;
    private final static int min_msg_uid_size        = min_no_msg_uid_size + g_msg_uid_size;
    private final static int ctrl_mask_qos_left_bits = 4;
    private final static int ctrl_mask_qos           = 3 << ctrl_mask_qos_left_bits;

    private byte mCtrl = I18nUtil.getCharsetSerial(I18nUtil.CHARSET_UTF_8, I18nUtil.SERIAL_BINARY);

    private   byte     mHeaderAttr;
    private   long     mMsgId    = -1;
    private   long     mSequence = -1;
    private   ZContext mContext;
    protected byte[]   mPayload;

    public ZProtocol()
    {
        setVersion(version);
    }

    @Override
    public Level getLevel()
    {
        return Level.valueOf((mHeaderAttr & ctrl_mask_qos) >>> ctrl_mask_qos_left_bits);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public void setHeader(byte header)
    {
        mHeaderAttr = header;
    }

    public byte getHeader()
    {
        return mHeaderAttr;
    }

    public boolean isTypeBin()
    {
        return I18nUtil.isTypeBin(mCtrl);
    }

    public boolean isTypeTxt()
    {
        return I18nUtil.isTypeTxt(mCtrl);
    }

    public boolean isTypeJson()
    {
        return I18nUtil.isTypeJson(mCtrl);
    }

    public boolean isTypeXml()
    {
        return I18nUtil.isTypeXml(mCtrl);
    }

    public int getVersion()
    {
        return mHeaderAttr & 0x0F;
    }

    public void setVersion(int version)
    {
        mHeaderAttr |= version;
    }

    @SuppressWarnings("unchecked")
    public <T extends ZProtocol> T withId(boolean flag)
    {
        mHeaderAttr = (byte) (flag ? (mHeaderAttr & ~0x40) : (mHeaderAttr | 0x40));
        return (T) this;
    }

    public final void setCharset(Charset charset)
    {
        mCtrl &= 0xF0;
        mCtrl |= I18nUtil.getCharsetCode(charset);
    }

    public final void setSerialType(int typeCode)
    {
        mCtrl &= 0x0F;
        mCtrl |= typeCode;
    }

    private void addCrc(ByteBuffer output)
    {
        output.putInt(CryptoUtil.crc32(output.array(), 0, output.position()));
    }

    private void checkCrc(ByteBuffer input)
    {
        int l_crc = CryptoUtil.crc32(input.array(), 0, input.position());
        int crc = input.getInt();
        if(CryptoUtil.crc32(input.array(), 0, input.position()) != input.getInt()) {
            throw new SecurityException(format("crc check failed! = %#x", input.array()[1]));
        }
    }

    @Override
    public final ByteBuffer encode()
    {
        int len = length();
        if(len > 0) {
            ByteBuffer buf = ByteBuffer.allocate(len);
            prefix(buf);
            return buf;
        }
        return null;
    }

    private void prefix(ByteBuffer output)
    {
        output.put(mHeaderAttr);
        output.put((byte) serial());
        if(isWithId()) {output.putLong(mMsgId);}
        output.put(mCtrl);
        encodec(output);
        if(mPayload != null) {
            output.put(mPayload);
        }
        addCrc(output);
    }

    @Override
    public final void decode(ByteBuffer input)
    {
        mHeaderAttr = input.get();
        input.get();
        if(isWithId()) {
            mMsgId = input.getLong();
        }
        mCtrl = input.get();
        input.limit(input.limit() - 4);
        decodec(input);
        if(input.hasRemaining()) {
            mPayload = new byte[input.remaining()];
            input.get(mPayload);
        }
        input.limit(input.limit() + 4);
        checkCrc(input);
    }

    public Charset getCharset()
    {
        return I18nUtil.getCharset(mCtrl);
    }

    protected int minLength()
    {
        return isWithId() ? min_msg_uid_size : min_no_msg_uid_size;
    }

    @Override
    public long getSequence()
    {
        return mSequence;
    }

    @Override
    public void setSequence(long sequence)
    {
        mSequence = mSequence < 0 ? sequence : mSequence;
    }

    public boolean isWithId()
    {
        return (mHeaderAttr & 0x40) == 0;
    }

    public long getMsgId()
    {
        return mMsgId;
    }

    public void setMsgId(long msgId)
    {
        mMsgId = msgId;
    }

    @Override
    public String toString()
    {
        return format("%s CMD: %#x | version:%d charset:%s serial-type:%s qos:%s msg-id:%d |",
                      getClass().getSimpleName(),
                      serial(),
                      getVersion(),
                      getCharset(),
                      I18nUtil.getSerialType(mCtrl),
                      getLevel(),
                      getMsgId());

    }

    public void putContext(ZContext context)
    {
        mContext = context;
    }

    public ZContext context()
    {
        return mContext;
    }

    @Override
    public void put(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public ByteBuffer payload()
    {
        return ByteBuffer.wrap(mPayload);
    }

    @Override
    public int length()
    {
        return minLength() + (mPayload == null ? 0 : mPayload.length);
    }
}
