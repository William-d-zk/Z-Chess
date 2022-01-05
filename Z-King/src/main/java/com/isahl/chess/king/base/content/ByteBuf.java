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

/**
 * @author william.d.zk
 * @date 2021-12-06
 */
public class ByteBuf
{

    public static ByteBuf allocate(int size)
    {
        return new ByteBuf(size, false);
    }

    public ByteBuf(int size, boolean isDirect)
    {
        _Direct = isDirect;
        if(size > 0) {buffer = _Direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);}
        capacity = size;
    }

    public ByteBuf(byte[] bytes)
    {
        _Direct = false;
        if(bytes != null && bytes.length > 0) {
            buffer = ByteBuffer.wrap(bytes);
            capacity = bytes.length;
        }
        writerIdx = capacity;
    }

    public ByteBuf(ByteBuf exist)
    {
        _Direct = exist._Direct;
        capacity = exist.capacity;
        buffer = exist.buffer;
        readerIdx = exist.readerIdx;
        writerIdx = exist.writerIdx;
    }

    public ByteBuf()
    {
        _Direct = false;
    }

    protected     ByteBuffer buffer;
    protected     int        capacity;
    protected     int        readerIdx;
    protected     int        writerIdx;
    private       int        readerMark = -1;
    private       int        writerMark = -1;
    private final boolean    _Direct;

    public int capacity()
    {
        return capacity;
    }

    public static ByteBuf wrap(byte[] bytes)
    {
        return new ByteBuf(bytes);
    }

    public ByteBuffer toReadBuffer()
    {
        return buffer.slice(readerIdx, readableBytes());
    }

    public ByteBuffer toWriteBuffer()
    {
        return buffer.slice(writerIdx, writableBytes());
    }

    public ByteBuf discard()
    {
        if(readerIdx > 0) {
            int gap = writerIdx - readerIdx;
            buffer.put(0, buffer, readerIdx, gap);
            writerIdx -= readerIdx;
            readerIdx = 0;
            readerMark = writerMark = -1;
        }
        return this;
    }

    public ByteBuf discardOnHalf()
    {
        return writableBytes() < (capacity() << 1) ? discard() : this;
    }

    public ByteBuf clear()
    {
        writerIdx = readerIdx = 0;
        buffer.clear();
        readerMark = writerMark = -1;
        return this;
    }

    public byte[] array()
    {
        return capacity > 0 ? buffer.array() : null;
    }

    public int writableBytes()
    {
        return capacity - writerIdx;
    }

    public int readableBytes()
    {
        return writerIdx - readerIdx;
    }

    public ByteBuf skip(int length)
    {
        checkOffset(length);
        readerIdx += length;
        return this;
    }

    public ByteBuf seek(int length)
    {
        checkCapacity(length);
        writerIdx += length;
        return this;
    }

    public int readerIdx() {return readerIdx;}

    public int writerIdx() {return writerIdx;}

    public ByteBuf copy()
    {
        ByteBuf newBuf = new ByteBuf(capacity, _Direct);
        newBuf.buffer.put(buffer.array())
                     .clear();
        newBuf.writerIdx = writerIdx;
        newBuf.readerIdx = readerIdx;
        return newBuf;
    }

    public boolean isReadable()
    {
        return readerIdx < writerIdx;
    }

    public boolean isWritable()
    {
        return writerIdx < capacity;
    }

    public byte peek(int offset)
    {
        checkOffset(offset + 1);
        return buffer.get(readerIdx + offset);
    }

    public byte[] peekAll(int offset)
    {
        int remain = readableBytes();
        if(remain > 0) {
            byte[] p = new byte[remain];
            buffer.get(readerIdx + offset, p);
            return p;
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
        return buffer.getShort(readerIdx + offset);
    }

    public int peekUnsignedShort(int offset)
    {
        return peekShort(offset) & 0xFFFF;
    }

    public byte get()
    {
        checkOffset(1);
        return buffer.get(readerIdx++);
    }

    public int getUnsigned()
    {
        return get() & 0xFF;
    }

    public short getShort()
    {
        checkOffset(2);
        short v = buffer.getShort(readerIdx);
        readerIdx += 2;
        return v;
    }

    public int getUnsignedShort()
    {
        return getShort() & 0xFFFF;
    }

    public int getInt()
    {
        checkOffset(4);
        int v = buffer.getInt(readerIdx);
        readerIdx += 4;
        return v;
    }

    public long getLong()
    {
        checkOffset(8);
        long v = buffer.getLong(readerIdx);
        readerIdx += 8;
        return v;
    }

    public long peekLong(int offset)
    {
        checkOffset(offset + 8);
        return buffer.getLong(readerIdx + offset);
    }

    public void get(byte[] dst)
    {
        checkOffset(dst.length);
        buffer.get(readerIdx, dst);
        readerIdx += dst.length;
    }

    public void get(byte[] dst, int off, int len)
    {
        checkOffset(len);
        buffer.get(readerIdx, dst, off, len);
        readerIdx += len;
    }

    public String readUTF(int len)
    {
        if(len > 0) {
            checkOffset(len);
            String str = new String(buffer.array(), readerIdx, len, StandardCharsets.UTF_8);
            readerIdx += len;
            return str;
        }
        return "";
    }

    public String readLine()
    {
        if(isReadable()) {
            int offset = 0;
            int remain = readableBytes();
            while(offset < remain) {
                if(buffer.get(readerIdx + offset++) == '\n') {
                    return readUTF(offset).replace("\r", "")
                                          .replace("\n", "");
                }
            }
        }
        return null;
    }

    public boolean isOffsetReadable(int offset)
    {
        return readerIdx + offset <= writerIdx;
    }

    private void checkOffset(int offset)
    {
        if(!isOffsetReadable(offset)) {
            throw new ZException("read out of bounds");
        }
    }

    public boolean isCapacityWritable(int capacity)
    {
        return writerIdx + capacity <= this.capacity;
    }

    private void checkCapacity(int capacity)
    {
        if(!isCapacityWritable(capacity)) {
            throw new ZException("write out of bounds");
        }
    }

    private void checkPosition(int position)
    {
        if(position < 0 || position > capacity) {
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

    public int vLength(int offset)
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

    public static int maxLength()
    {
        return 268435455;
    }

    public void markReader()
    {
        readerMark = readerIdx;
    }

    public void markWriter()
    {
        writerMark = writerIdx;
    }

    public int readerMark()
    {
        return readerMark;
    }

    public int writerMark()
    {
        return writerMark;
    }

    public ByteBuf put(byte v)
    {
        checkCapacity(1);
        buffer.put(writerIdx++, v);
        return this;
    }

    public ByteBuf put(int v, int pos)
    {
        checkPosition(pos);
        buffer.put(pos, (byte) v);
        return this;
    }

    public ByteBuf put(int v)
    {
        checkCapacity(1);
        buffer.put(writerIdx++, (byte) v);
        return this;
    }

    public ByteBuf putShort(short v)
    {
        checkCapacity(2);
        buffer.putShort(writerIdx, v);
        writerIdx += 2;
        return this;
    }

    public ByteBuf putInt(int v)
    {
        checkCapacity(4);
        buffer.putInt(writerIdx, v);
        writerIdx += 4;
        return this;
    }

    public ByteBuf putLong(long v)
    {
        checkCapacity(8);
        buffer.putLong(writerIdx, v);
        writerIdx += 8;
        return this;
    }

    public ByteBuf put(byte[] v)
    {
        if(v == null || v.length == 0) {return this;}
        checkCapacity(v.length);
        buffer.put(writerIdx, v);
        writerIdx += v.length;
        return this;
    }

    public ByteBuf put(byte[] v, int off, int len)
    {
        if(v == null) {return this;}
        checkCapacity(len);
        buffer.put(writerIdx, v, off, len);
        writerIdx += len;
        return this;
    }

    public ByteBuf put(ByteBuf v)
    {
        int len;
        if(v == null || v == this || (len = v.readableBytes()) == 0) {return this;}
        checkCapacity(len);
        buffer.put(writerIdx, v.array(), v.readerIdx(), len);
        v.readerIdx += len;
        writerIdx += len;
        return this;
    }

    public ByteBuf putUTF(String v)
    {
        if(v == null) {return this;}
        byte[] s = v.getBytes(StandardCharsets.UTF_8);
        vPutLength(s.length);
        checkCapacity(s.length);
        buffer.put(writerIdx, s, 0, s.length);
        writerIdx += s.length;
        return this;
    }

    public ByteBuf putExactly(ByteBuf v)
    {
        if(v == null || v == this) {return this;}
        int len = Math.min(v.readableBytes(), writableBytes());
        if(len > 0) {
            buffer.put(writerIdx, v.array(), v.readerIdx, len);
            v.readerIdx += len;
            writerIdx += len;
        }
        return this;
    }

    public void resetReader()
    {
        if(readerMark >= 0 && readerMark <= capacity) {readerIdx = readerMark;}
        readerMark = -1;
    }

    public void resetWriter()
    {
        if(writerMark >= 0 && writerMark <= capacity) {writerIdx = writerMark;}
        writerMark = -1;
    }

    public void expand(int size)
    {
        buffer = IoUtil.expandBuffer(buffer, size);
        capacity += size;
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
