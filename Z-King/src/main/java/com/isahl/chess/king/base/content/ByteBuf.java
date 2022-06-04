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
package com.isahl.chess.king.base.content;

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.util.IoUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author william.d.zk
 * @date 2021-12-06
 */
public class ByteBuf
{
    public ByteBuf(int size, boolean isDirect)
    {
        _Direct = isDirect;
        if(size > 0) {mBuffer = _Direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);}
        mCapacity = size;
    }

    private ByteBuf(byte[] bytes)
    {
        _Direct = false;
        if(bytes != null && bytes.length > 0) {
            mBuffer = ByteBuffer.wrap(bytes);
            mCapacity = bytes.length;
        }
        mWriterIdx = mCapacity;
    }

    public ByteBuf(ByteBuf exist)
    {
        _Direct = exist._Direct;
        mCapacity = exist.mCapacity;
        mBuffer = exist.mBuffer;
        mReaderIdx = exist.mReaderIdx;
        mWriterIdx = exist.mWriterIdx;
    }

    public ByteBuf()
    {
        _Direct = false;
    }

    public ByteBuf(ByteBuffer source)
    {
        _Direct = source.isDirect();
        mBuffer = source;
        mCapacity = mBuffer.capacity();
        mWriterIdx = mCapacity;
    }

    protected     ByteBuffer mBuffer;
    protected     int        mCapacity;
    protected     int        mReaderIdx;
    protected     int        mWriterIdx;
    private       int        mReaderMark = -1;
    private       int        mWriterMark = -1;
    private final boolean    _Direct;

    public int capacity()
    {
        return mCapacity;
    }

    public static ByteBuf allocate(int size)
    {
        return new ByteBuf(size, false);
    }

    public static ByteBuf wrap(byte[] bytes)
    {
        return new ByteBuf(Objects.requireNonNull(bytes));
    }

    public static ByteBuf wrap(ByteBuffer buffer)
    {
        return new ByteBuf(buffer);
    }

    public ByteBuffer toReadBuffer()
    {
        return mBuffer.slice(mReaderIdx, readableBytes());
    }

    public ByteBuffer toWriteBuffer()
    {
        return mBuffer.slice(mWriterIdx, writableBytes());
    }

    public ByteBuf discard()
    {
        if(mReaderIdx > 0) {
            int gap = mWriterIdx - mReaderIdx;
            mBuffer.put(0, mBuffer, mReaderIdx, gap);
            mWriterIdx -= mReaderIdx;
            mReaderIdx = 0;
            mReaderMark = mWriterMark = -1;
        }
        return this;
    }

    public ByteBuf discardOnHalf()
    {
        return writableBytes() < (capacity() >> 1) ? discard() : this;
    }

    public ByteBuf clear()
    {
        mWriterIdx = mReaderIdx = 0;
        mBuffer.clear();
        mReaderMark = mWriterMark = -1;
        return this;
    }

    public byte[] array()
    {
        if(mCapacity > 0) {
            if(_Direct) {
                byte[] v = new byte[mCapacity];
                mBuffer.get(0, v);
                return v;
            }
            return mBuffer.array();
        }
        return null;
    }

    public int writableBytes()
    {
        return mCapacity - mWriterIdx;
    }

    public int readableBytes()
    {
        return mWriterIdx - mReaderIdx;
    }

    public ByteBuf skip(int length)
    {
        checkOffset(length);
        mReaderIdx += length;
        return this;
    }

    public ByteBuf seek(int length)
    {
        checkCapacity(length);
        mWriterIdx += length;
        return this;
    }

    public int readerIdx() {return mReaderIdx;}

    public int writerIdx() {return mWriterIdx;}

    public ByteBuf copy()
    {
        ByteBuf newBuf = new ByteBuf(mCapacity, _Direct);
        if(_Direct) {
            byte[] v = new byte[mCapacity];
            mBuffer.get(0, v);
            newBuf.mBuffer.put(v)
                          .clear();
        }
        else {
            newBuf.mBuffer.put(mBuffer.array())
                          .clear();
        }
        newBuf.mWriterIdx = mWriterIdx;
        newBuf.mReaderIdx = mReaderIdx;
        return newBuf;
    }

    public boolean isReadable()
    {
        return mReaderIdx < mWriterIdx;
    }

    public boolean isWritable()
    {
        return mWriterIdx < mCapacity;
    }

    public byte peek(int offset)
    {
        checkOffset(offset);
        return mBuffer.get(mReaderIdx + offset);
    }

    public byte[] peekAll()
    {
        return peekAll(0, -1);
    }

    public byte[] peekAll(int offset, int length)
    {
        if(length < 1) {
            length = readableBytes() - offset;
        }
        if(length > 0) {
            checkOffset(offset + length);
            byte[] v = new byte[length];
            mBuffer.get(mReaderIdx + offset, v);
            return v;
        }
        return null;
    }

    public int peekUnsigned(int offset)
    {
        return peek(offset) & 0xFF;
    }

    public int peekShort(int offset)
    {
        checkOffset(offset + 2);
        return mBuffer.getShort(mReaderIdx + offset);
    }

    public int peekUnsignedShort(int offset)
    {
        return peekShort(offset) & 0xFFFF;
    }

    public byte get()
    {
        checkOffset(0);
        return mBuffer.get(mReaderIdx++);
    }

    public int getUnsigned()
    {
        return get() & 0xFF;
    }

    public short getShort()
    {
        checkOffset(2);
        short v = mBuffer.getShort(mReaderIdx);
        mReaderIdx += 2;
        return v;
    }

    public int getUnsignedShort()
    {
        return getShort() & 0xFFFF;
    }

    public int getInt()
    {
        checkOffset(4);
        int v = mBuffer.getInt(mReaderIdx);
        mReaderIdx += 4;
        return v;
    }

    public long getLong()
    {
        checkOffset(8);
        long v = mBuffer.getLong(mReaderIdx);
        mReaderIdx += 8;
        return v;
    }

    public long peekLong(int offset)
    {
        checkOffset(offset + 8);
        return mBuffer.getLong(mReaderIdx + offset);
    }

    public void get(byte[] dst)
    {
        checkOffset(dst.length);
        mBuffer.get(mReaderIdx, dst);
        mReaderIdx += dst.length;
    }

    public void get(byte[] dst, int off, int len)
    {
        checkOffset(len);
        mBuffer.get(mReaderIdx, dst, off, len);
        mReaderIdx += len;
    }

    public byte[] vGet()
    {
        int len = vLength();
        if(len > 0) {
            byte[] v = new byte[len];
            get(v);
            return v;
        }
        return null;
    }

    public byte[] vPeek(int offset)
    {
        int len = vPeekLength(offset);
        if(len > 0) {
            byte[] v = new byte[len];
            checkOffset(len);
            mBuffer.get(mReaderIdx, v);
            return v;
        }
        return null;
    }

    public String readUTF()
    {
        byte[] b = vGet();
        return b == null ? null : new String(b, StandardCharsets.UTF_8);
    }

    public String readUTF(int len)
    {
        if(len > 0) {
            checkOffset(len);
            String str;
            if(_Direct) {
                byte[] v = new byte[len];
                mBuffer.get(mReaderIdx, v);
                str = new String(v, StandardCharsets.UTF_8);
            }
            else {
                str = new String(mBuffer.array(), mReaderIdx, len, StandardCharsets.UTF_8);
            }
            mReaderIdx += len;
            return str;
        }
        return null;
    }

    public String readLine()
    {
        if(isReadable()) {
            int offset = 0;
            int remain = readableBytes();
            while(offset < remain) {
                if(mBuffer.get(mReaderIdx + offset++) == '\n') {
                    return readUTF(offset).replace("\r", "")
                                          .replace("\n", "");
                }
            }
        }
        return null;
    }

    public boolean isOffsetReadable(int offset)
    {
        return mReaderIdx + offset <= mWriterIdx;
    }

    private void checkOffset(int offset)
    {
        if(!isOffsetReadable(offset)) {
            throw new ZException("read out of bounds");
        }
    }

    public boolean isCapacityWritable(int capacity)
    {
        return mWriterIdx + capacity <= this.mCapacity;
    }

    private void checkCapacity(int capacity)
    {
        if(!isCapacityWritable(capacity)) {
            throw new ZException("write out of bounds");
        }
    }

    private void checkPutAt(int position)
    {
        if(position < 0 || position >= mCapacity) {
            throw new ZException("position out of bounds");
        }
    }

    public int vLength()
    {
        int length = 0;
        int cur, pos = 0;
        do {
            cur = get();
            length += (cur & 0x7F) << (pos * 7);
            pos++;
        }
        while((cur & 0x80) > 0);
        return length;
    }

    public int vPeekLength(int offset)
    {
        int length = 0;
        int cur, pos = 0;
        do {
            cur = peek(offset + pos);
            length += (cur & 0x7F) << (pos * 7);
            pos++;
        }
        while((cur & 0x80) > 0);
        return length;
    }

    public ByteBuf vPutLength(int length)
    {
        length = Math.max(length, 0);
        if(length < 128) {
            return put(length);
        }
        else if(length < 16384) {
            return put(0x80 | (length & 0x7F)).put(length >>> 7);
        }
        else if(length < 2097152) {
            return put(0x80 | (length & 0x7F)).put(0x80 | (length & 0x7F80) >>> 7)
                                              .put(length >>> 14);
        }
        else if(length < 268435456) {
            return put(0x80 | (length & 0x7F)).put(0x80 | (length & 0x7F80) >>> 7)
                                              .put(0x80 | (length & 0x3FC000) >>> 14)
                                              .put(length >>> 21);
        }
        throw new ZException("malformed length");
    }

    public static int vSizeOf(int length)
    {
        if(length <= 0) {
            return 1;
        }
        else if(length < 128) {
            return length + 1;
        }
        else if(length < 16384) {
            return length + 2;
        }
        else if(length < 2097152) {
            return length + 3;
        }
        else if(length < 268435456) {
            return length + 4;
        }
        throw new ZException("malformed length");
    }

    public static int vLengthOff(int length)
    {
        if(length < 128) {
            return 1;
        }
        else if(length < 16384) {
            return 2;
        }
        else if(length < 2097152) {
            return 3;
        }
        else if(length < 268435456) {
            return 4;
        }
        throw new ZException("malformed length");
    }

    public static int maxLength()
    {
        return 268435455;
    }

    public void markReader()
    {
        mReaderMark = mReaderIdx;
    }

    public void markWriter()
    {
        mWriterMark = mWriterIdx;
    }

    public int readerMark()
    {
        return mReaderMark;
    }

    public int writerMark()
    {
        return mWriterMark;
    }

    public ByteBuf put(byte v)
    {
        checkCapacity(1);
        mBuffer.put(mWriterIdx++, v);
        return this;
    }

    public ByteBuf put(int v, int pos)
    {
        checkPutAt(pos);
        mBuffer.put(pos, (byte) v);
        return this;
    }

    public ByteBuf put(int v)
    {
        checkCapacity(1);
        mBuffer.put(mWriterIdx++, (byte) v);
        return this;
    }

    public ByteBuf putShort(short v)
    {
        checkCapacity(2);
        mBuffer.putShort(mWriterIdx, v);
        mWriterIdx += 2;
        return this;
    }

    public ByteBuf putInt(int v)
    {
        checkCapacity(4);
        mBuffer.putInt(mWriterIdx, v);
        mWriterIdx += 4;
        return this;
    }

    public ByteBuf putLong(long v)
    {
        checkCapacity(8);
        mBuffer.putLong(mWriterIdx, v);
        mWriterIdx += 8;
        return this;
    }

    public ByteBuf vPut(byte[] v)
    {
        vPutLength(v == null ? 0 : v.length);
        put(v);
        return this;
    }

    public ByteBuf put(byte[] v)
    {
        if(v == null || v.length == 0) {return this;}
        checkCapacity(v.length);
        mBuffer.put(mWriterIdx, v);
        mWriterIdx += v.length;
        return this;
    }

    public ByteBuf putLongArray(long[] v)
    {
        if(v == null || v.length == 0) {
            return this;
        }
        checkCapacity(v.length * 8);
        for(long l : v) {
            mBuffer.putLong(mWriterIdx, l);
            mWriterIdx += 8;
        }
        return this;
    }

    public ByteBuf put(byte[] v, int off, int len)
    {
        if(v == null) {return this;}
        checkCapacity(len);
        mBuffer.put(mWriterIdx, v, off, len);
        mWriterIdx += len;
        return this;
    }

    public ByteBuf put(ByteBuf v)
    {
        int len;
        if(v == null || v == this || (len = v.readableBytes()) == 0) {return this;}
        checkCapacity(len);
        mBuffer.put(mWriterIdx, v.array(), v.readerIdx(), len);
        v.mReaderIdx += len;
        mWriterIdx += len;
        return this;
    }

    public ByteBuf putUTF(String v)
    {
        if(v == null) {
            vPutLength(0);
            return this;
        }
        byte[] s = v.getBytes(StandardCharsets.UTF_8);
        vPutLength(s.length);
        checkCapacity(s.length);
        mBuffer.put(mWriterIdx, s, 0, s.length);
        mWriterIdx += s.length;
        return this;
    }

    public ByteBuf putExactly(ByteBuf v)
    {
        if(v == null || v == this) {return this;}
        int len = Math.min(v.readableBytes(), writableBytes());
        if(len > 0) {
            mBuffer.put(mWriterIdx, v.array(), v.mReaderIdx, len);
            v.mReaderIdx += len;
            mWriterIdx += len;
        }
        return this;
    }

    public void resetReader()
    {
        if(mReaderMark >= 0 && mReaderMark <= mCapacity) {mReaderIdx = mReaderMark;}
        mReaderMark = -1;
    }

    public void resetWriter()
    {
        if(mWriterMark >= 0 && mWriterMark <= mCapacity) {mWriterIdx = mWriterMark;}
        mWriterMark = -1;
    }

    public void expand(int size)
    {
        mBuffer = IoUtil.expandBuffer(mBuffer, size);
        mCapacity += size;
    }

    public void append(ByteBuf other)
    {
        discard();
        if(writableBytes() < other.readableBytes()) {
            expand(other.readableBytes() - writableBytes());
        }
        put(other);
    }

}
