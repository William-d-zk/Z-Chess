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
package com.tgx.chess.bishop.io.zprotocol;

import java.util.Objects;

import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.CryptUtil;
import com.tgx.chess.king.base.util.I18nUtil;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.queen.io.core.async.AioContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IRouteLv4;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author William.d.zk
 */
public abstract class BaseCommand<C extends AioContext>
        implements
        IControl<C>,
        IRouteLv4
{
    private final Logger _Logger = Logger.getLogger(getClass().getName());

    public final static int  version             = 0x3;
    private final static int g_msg_uid_size      = 8;
    private final static int min_no_msg_uid_size = 1 + 1 + 1 + 4;
    private final static int min_msg_uid_size    = min_no_msg_uid_size + g_msg_uid_size;
    private final int        _Command;
    private final boolean    _HasUID;
    private String           charset             = "UTF-8";
    private byte             mTypeByte;
    private byte             mHAttr;
    private long             mMsgUID;
    private ISession<C>      mSession;
    private long             mSequence           = -1;
    private transient long   tTransactionKey     = _DEFAULT_TRANSACTION_KEY;
    private byte[]           mPayload;
    private byte             mCtrlCode;

    protected BaseCommand(int command,
                          boolean hasUID)
    {
        _Command = command;
        initGUid(0, _HasUID = hasUID);
        setEncrypt();
        setCompress();
        setVersion(version);
        setCharsetSerial(I18nUtil.CHARSET_UTF_8, I18nUtil.SERIAL_BINARY);
    }

    public BaseCommand(int command,
                       long uidWithoutSequence)
    {
        _Command = command;
        initGUid(uidWithoutSequence, _HasUID = true);
        setEncrypt();
        setCompress();
        setVersion(version);
        setCharsetSerial(I18nUtil.CHARSET_UTF_8, I18nUtil.SERIAL_BINARY);
    }

    @Override
    public void setCtrl(byte ctrl)
    {
        mCtrlCode = ctrl;
    }

    @Override
    public byte getCtrl()
    {
        return mCtrlCode;
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public void setPayload(byte[] payload)
    {
        mPayload = payload;
    }

    @Override
    public byte[] getPayload()
    {
        return mPayload;
    }

    @Override
    public void reset()
    {
        setPayload(null);
        setSession(null);
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

    @Override
    public int getSerial()
    {
        return _Command;
    }

    @Override
    public int superSerial()
    {
        return COMMAND_SERIAL;
    }

    public int getVersion()
    {
        return mHAttr & 0x0F;
    }

    public void setVersion(int version)
    {
        mHAttr |= version;
    }

    @Override
    public boolean isCluster()
    {
        return (mHAttr & 0x80) != 0;

    }

    @Override
    public BaseCommand<C> setCluster(boolean b)
    {
        mHAttr |= b ? 0x80
                    : 0;
        return this;
    }

    public boolean isCompress()
    {
        return (mHAttr & 0x20) != 0;
    }

    public boolean isEncrypt()
    {
        return (mHAttr & 0x10) != 0;
    }

    public boolean isGlobalMsg()
    {
        return (mHAttr & 0x40) == 0;
    }

    private void initGUid(long _uid, boolean flag)
    {
        if (flag) {
            mHAttr &= ~0x40;
        }
        else {
            mHAttr |= 0x40;
        }
        mMsgUID = flag ? _uid
                       : 0;
    }

    public void setEncrypt()
    {
        mHAttr |= 0x10;
    }

    public void setNoEncrypt()
    {
        mHAttr &= 0xEF;
    }

    public void setCompress()
    {
        mHAttr |= 0x20;
    }

    public void setEvent()
    {
        mHAttr |= 0x80;
    }

    public final void setCharsetSerial(int charset_, int serial_)
    {
        mTypeByte = I18nUtil.getCharsetSerial(charset_, serial_);
    }

    public final void setCharsetSerial(byte type)
    {
        this.mTypeByte = type;
    }

    private int addCrc(byte[] data, int lastPos)
    {
        lastPos += IoUtil.writeInt(CryptUtil.crc32(data, 0, lastPos), data, lastPos);
        return lastPos;
    }

    private int checkCrc(byte[] data, int lastPos)
    {
        int l_crc = CryptUtil.crc32(data, 0, lastPos);
        int crc = IoUtil.readInt(data, lastPos);
        if (l_crc != crc) { throw new SecurityException("crc check failed!  =" + data[1]); }
        return lastPos + 4;
    }

    @Override
    public final byte[] encode()
    {
        int length = dataLength();
        byte[] data = new byte[length];
        int pos = 0;
        pos += IoUtil.writeByte(mHAttr, data, pos);
        pos += IoUtil.writeByte(_Command, data, pos);
        if (isGlobalMsg()) {
            pos += IoUtil.writeLong(mMsgUID, data, pos);
        }
        pos += IoUtil.writeByte(mTypeByte, data, pos);
        int ppos = pos;
        int dpos = encodec(data, pos);
        _Logger.info("command:0x%03X,payload:%s", _Command, IoUtil.bin2Hex(data, ppos, dpos - ppos, "."));
        addCrc(data, dpos);
        return data;
    }

    @Override
    public final int encode(byte[] buf, int pos, int length)
    {
        int len = dataLength();
        if (len <= 0) { throw new IllegalArgumentException("data length is negative or zero"); }
        if (buf == null) {
            buf = new byte[len];
            pos = 0;
        }
        else if (len > length || buf.length < len || pos + length > buf.length) {
            throw new ArrayIndexOutOfBoundsException("data length is too long for input buf");
        }
        pos += IoUtil.writeByte(mHAttr, buf, pos);
        pos += IoUtil.writeByte(_Command, buf, pos);
        if (isGlobalMsg()) {
            pos += IoUtil.writeLong(mMsgUID, buf, pos);
        }
        pos += IoUtil.writeByte(mTypeByte, buf, pos);
        pos = addCrc(buf, encodec(buf, pos));
        return pos;
    }

    @Override
    public final int decode(byte[] data)
    {
        if (data == null) { throw new NullPointerException(); }
        if (data.length < dataLength()) { throw new ArrayIndexOutOfBoundsException(); }
        mHAttr = data[0];
        int pos = 2;
        if (isGlobalMsg()) {
            mMsgUID = IoUtil.readLong(data, pos);
            pos += 8;
        }
        mTypeByte = data[pos++];
        charset = getCharset(mTypeByte);
        return checkCrc(data, decodec(data, pos));
    }

    @Override
    public final int decode(byte[] data, int pos, int length)
    {
        if (data == null) { throw new NullPointerException(); }
        if (data.length - length < dataLength()) { throw new ArrayIndexOutOfBoundsException(); }

        mHAttr = data[pos];
        pos += 2;
        if (isGlobalMsg()) {
            mMsgUID = IoUtil.readLong(data, pos);
            pos += 8;
        }
        mTypeByte = data[pos++];
        charset = getCharset(mTypeByte);
        return checkCrc(data, decodec(data, pos));
    }

    private String getCharset(byte charsetCode)
    {
        return I18nUtil.getCharset(charsetCode);
    }

    public String getCharset()
    {
        return charset;
    }

    protected int getCharsetCode(String charset)
    {
        return I18nUtil.getCharsetCode(charset);
    }

    @Override
    public int dataLength()
    {
        return isGlobalMsg() ? min_msg_uid_size
                             : min_no_msg_uid_size;
    }

    @Override
    public void dispose()
    {
        charset = null;
        mSession = null;
    }

    @Override
    public long getSequence()
    {
        return mSequence;
    }

    @Override
    public void setSequence(long sequence)
    {
        mSequence = mSequence < 0 ? sequence
                                  : mSequence;
    }

    @Override
    public long getTransactionKey()
    {
        return tTransactionKey;
    }

    @Override
    public void setTransactionKey(long _key)
    {
        tTransactionKey = tTransactionKey < 0 ? _key
                                              : tTransactionKey;
    }

    public byte getTgxType()
    {
        return mTypeByte;
    }

    public BaseCommand<C> setPType(byte type_b)
    {
        this.mTypeByte = type_b;
        return this;
    }

    public BaseCommand<C> setUID(String hexGUid, long longGUid)
    {
        if (longGUid != -1) {
            setUID(longGUid);
        }
        else if (Objects.nonNull(hexGUid)) {
            setUID(Long.parseLong(hexGUid, 16));
        }
        else {
            throw new NullPointerException();
        }
        return this;
    }

    @Override
    public long getUID()
    {
        return mMsgUID;
    }

    @Override
    public void setUID(long uid)
    {
        if (!_HasUID) { throw new UnsupportedOperationException(); }
        mMsgUID = uid;
    }

    @Override
    public ISession<C> getSession()
    {
        return mSession;
    }

    @Override
    public BaseCommand<C> setSession(ISession<C> session)
    {
        this.mSession = session;
        return this;
    }

    @Override
    public BaseCommand<C> duplicate()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return String.format("CMD: 0x%X,version:%d,charset:%s,serial type:%s",
                             _Command,
                             getVersion(),
                             getCharset(mTypeByte),
                             I18nUtil.getSerialType(mTypeByte & 0x0F));

    }
}
