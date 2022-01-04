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
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.IOException;
import java.io.RandomAccessFile;

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

    @JsonIgnore
    protected RandomAccessFile mFile;

    public void flush()
    {
        try {
            mFile.seek(0);
            byte[] toWrite = encode().array();
            _Logger.info("write meta %s,size:%d,{%s}",
                         getClass().getSimpleName(),
                         toWrite.length,
                         JsonUtil.writeValueAsString(this));
            mFile.write(IoUtil.variableLength(toWrite.length));
            mFile.write(toWrite);
            mFile.getFD()
                 .sync();
        }
        catch(IOException e) {
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

    @SuppressWarnings("unchecked")
    protected <T extends BaseMeta> T from(RandomAccessFile file)
    {
        mFile = file;
        return (T) this;
    }

}
