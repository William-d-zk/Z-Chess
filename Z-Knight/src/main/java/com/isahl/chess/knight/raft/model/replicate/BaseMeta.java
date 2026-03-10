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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public abstract class BaseMeta
        extends InnerProtocol
        implements IReset
{
    protected final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    public BaseMeta(Operation operation, Strategy strategy)
    {
        super(operation, strategy);
    }

    public BaseMeta()
    {
        super();
    }

    public BaseMeta(ByteBuf input)
    {
        super(input);
    }

    @JsonIgnore
    protected RandomAccessFile mFile;

    public void flush()
    {
        flush(true); // 默认强制 fsync
    }

    /**
     * 刷盘操作
     * @param fsync 是否强制 fsync 到磁盘
     */
    public void flush(boolean fsync)
    {
        try {
            byte[] toWrite = encoded();
            FileChannel channel = mFile.getChannel();
            
            // 使用内存映射写入
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, SERIAL_POS, toWrite.length);
            _Logger.info("write meta %s,size:%d,fsync=%s,{%s}",
                         getClass().getSimpleName(),
                         toWrite.length,
                         fsync,
                         JsonUtil.writeValueAsString(this));
            mapped.put(toWrite);
            
            // 强制刷盘确保数据落盘
            if(fsync) {
                mapped.force();
                channel.force(true); // 强制刷盘，包含文件元数据
            }
        }
        catch(IOException e) {
            _Logger.fetal("flush meta failed: %s", e);
            e.printStackTrace();
        }
    }

    public void close()
    {
        flush();
        try {
            mFile.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void ofFile(RandomAccessFile file)
    {
        mFile = file;
    }

    public static <T extends BaseMeta> T from(RandomAccessFile file, IoFactory<T> factory) throws IOException
    {
        if(factory == null || file.length() == 0) {return null;}
        ByteBuffer mapped = file.getChannel()
                                .map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        T t = factory.create(ByteBuf.wrap(mapped));
        t.mFile = file;
        return t;
    }

}
