/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.king.base.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.schedule.Status;

/**
 * @author william.d.zk
 * 
 * @date 2020/7/7
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ZProgress
        implements
        Serializable
{
    private final long _Size;

    private Progress progress = Progress.NA;
    private Status   status   = Status.CREATED;
    private long     count;
    private boolean  exist;

    @JsonCreator
    public ZProgress(@JsonProperty("size") long size)
    {
        _Size = size;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public long getCount()
    {
        return count;
    }

    public void setCount(long count)
    {
        this.count = count;
    }

    public long getSize()
    {
        return _Size;
    }

    public boolean isExist()
    {
        return exist;
    }

    public void setExist(boolean exist)
    {
        this.exist = exist;
    }

    public Progress getProgress()
    {
        return progress;
    }

    public void setProgress(Progress progress)
    {
        this.progress = progress;
    }
}
