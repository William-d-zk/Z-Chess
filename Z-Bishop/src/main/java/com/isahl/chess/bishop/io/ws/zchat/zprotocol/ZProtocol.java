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
package com.isahl.chess.bishop.io.ws.zchat.zprotocol;

import com.isahl.chess.bishop.io.ws.zchat.ZContext;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.I18nUtil;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.inf.ICommand;
import com.isahl.chess.queen.io.core.inf.IDuplicate;
import com.isahl.chess.queen.io.core.inf.IQoS;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @author William.d.zk
 */
public abstract class ZProtocol
        implements ICommand,
                   IQoS,
                   IDuplicate
{
    public final static  int      version                 = 0x3;
    private final static int      g_msg_uid_size          = 8;
    private final static int      min_no_msg_uid_size     = 1 + 1 + 1 + 4;
    private final static int      min_msg_uid_size        = min_no_msg_uid_size + g_msg_uid_size;
    private final static int      ctrl_mask_qos_left_bits = 4;
    private final static int      ctrl_mask_qos           = 3 << ctrl_mask_qos_left_bits;
    private final        int      _Command;
    private final        boolean  _HasMsgId;
    private              byte     mTypeByte;
    private              byte     mHAttr;
    private              long     mMsgId                  = -1;
    private              long     mSequence               = -1;
    private transient    long     tTransactionKey         = -1;
    private              byte     mCtrlCode;
    private              ZContext mContext;

    protected ZProtocol(int command, boolean hasMsgId, long msgId)
    {
        _Command = command;
        initGUid(msgId, _HasMsgId = hasMsgId);
        setVersion(version);
        mTypeByte = I18nUtil.getCharsetSerial(I18nUtil.CHARSET_UTF_8, I18nUtil.SERIAL_BINARY);
    }

    public ZProtocol(int command, long msgId)
    {
        this(command, true, msgId);
    }

    public ZProtocol(int command)
    {
        this(command, false, -1);
    }

    @Override
    public Level getLevel()
    {
        return Level.valueOf((mCtrlCode & ctrl_mask_qos) >>> ctrl_mask_qos_left_bits);
    }

    @Override
    public int priority()
    {
        return QOS_PRIORITY_00_NETWORK_CONTROL;
    }

    public void setCtrlCode(byte ctrl)
    {
        mCtrlCode = ctrl;
    }

    public byte getCtrlCode()
    {
        return mCtrlCode;
    }

    public boolean isTypeBin()
    {
        return I18nUtil.isTypeBin(mTypeByte);
    }

    public boolean isTypeTxt()
    {
        return I18nUtil.isTypeTxt(mTypeByte);
    }

    public boolean isTypeJson()
    {
        return I18nUtil.isTypeJson(mTypeByte);
    }

    public boolean isTypeXml()
    {
        return I18nUtil.isTypeXml(mTypeByte);
    }

    public int getCommand()
    {
        return _Command;
    }

    public int getVersion()
    {
        return mHAttr & 0x0F;
    }

    public void setVersion(int version)
    {
        mHAttr |= version;
    }

    private boolean isGlobalMsg()
    {
        return (mHAttr & 0x40) == 0;
    }

    private void initGUid(long _uid, boolean flag)
    {
        if(flag) {
            mHAttr &= ~0x40;
        }
        else {
            mHAttr |= 0x40;
        }
        mMsgId = flag ? _uid : 0;
    }

    public final void setCharset(Charset charset)
    {
        mTypeByte &= 0xF0;
        mTypeByte |= I18nUtil.getCharsetCode(charset);
    }

    public final void setSerialType(int typeCode)
    {
        mTypeByte &= 0x0F;
        mTypeByte |= typeCode;
    }

    private int addCrc(byte[] data, int lastPos)
    {
        lastPos += IoUtil.writeInt(CryptoUtil.crc32(data, 0, lastPos), data, lastPos);
        return lastPos;
    }

    private int checkCrc(byte[] data, int lastPos)
    {
        int l_crc = CryptoUtil.crc32(data, 0, lastPos);
        int crc = IoUtil.readInt(data, lastPos);
        if(l_crc != crc) { throw new SecurityException("crc check failed!  =" + data[1]); }
        return lastPos + 4;
    }

    @Override
    public final byte[] encode()
    {
        int length = dataLength();
        if(length == 0) { throw new ArrayIndexOutOfBoundsException("data_length == 0"); }
        byte[] output = new byte[length];
        prefix(output, 0);
        return output;
    }

    private int prefix(byte[] output, int pos)
    {
        pos += IoUtil.writeByte(mHAttr, output, pos);
        pos += IoUtil.writeByte(_Command, output, pos);
        if(isGlobalMsg()) {
            pos += IoUtil.writeLong(mMsgId, output, pos);
        }
        pos += IoUtil.writeByte(mTypeByte, output, pos);
        pos = encodec(output, pos);
        if(pos < output.length - 5) {
            pos += IoUtil.write(payload(), output, pos);
        }
        return addCrc(output, pos);
    }

    @Override
    public final int encode(byte[] output, int pos, int length)
    {
        Objects.requireNonNull(output);
        if(output.length < length || pos + length > output.length) {
            throw new ArrayIndexOutOfBoundsException("data length is too long for input buf");
        }
        return prefix(output, pos);
    }

    @Override
    public final int decode(byte[] input, int pos, int length)
    {
        if(input == null || input.length == 0 || length == 0) { return 0; }
        // dataLength 此处表达了最短长度值
        int len = dataLength();
        if(len > length || (input.length < len || pos + length > input.length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mHAttr = input[pos++];
        pos += 1;// skip [_Command] position
        if(isGlobalMsg()) {
            mMsgId = IoUtil.readLong(input, pos);
            pos += 8;
        }
        mTypeByte = input[pos++];
        pos = decodec(input, pos);
        if(pos < input.length - 5) {
            byte[] payload = new byte[input.length - 4 - pos];
            pos = IoUtil.read(input, pos, payload);
            putPayload(payload);
        }
        return checkCrc(input, pos);
    }

    public Charset getCharset()
    {
        return I18nUtil.getCharset(mTypeByte);
    }

    protected int minLength()
    {
        return isGlobalMsg() ? min_msg_uid_size : min_no_msg_uid_size;
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

    public long getTransactionKey()
    {
        return tTransactionKey;
    }

    public void setTransactionKey(long _key)
    {
        tTransactionKey = tTransactionKey < 0 ? _key : tTransactionKey;
    }

    public byte getTypeCode()
    {
        return mTypeByte;
    }

    public ZProtocol setMsgId(String hexGUid, long longGUid)
    {
        if(longGUid != -1) {
            setMsgId(longGUid);
        }
        else if(Objects.nonNull(hexGUid)) {
            setMsgId(Long.parseLong(hexGUid, 16));
        }
        else {
            throw new NullPointerException();
        }
        return this;
    }

    @Override
    public long getMsgId()
    {
        return mMsgId;
    }

    @Override
    public void setMsgId(long uid)
    {
        if(!_HasMsgId) { throw new UnsupportedOperationException(); }
        mMsgId = uid;
    }

    @Override
    public String toString()
    {
        return String.format("%s CMD: %#x | version:%d charset:%s serial-type:%s qos:%s msgId:%d |",
                             getClass().getSimpleName(),
                             _Command,
                             getVersion(),
                             getCharset(),
                             I18nUtil.getSerialType(mTypeByte),
                             getLevel(),
                             getMsgId());

    }

    public void setContext(ZContext context)
    {
        mContext = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZContext context()
    {
        return mContext;
    }
}
