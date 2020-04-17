/*
 * MIT License
 *
 * Copyright (c) 2016~2020 Z-Chess
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

package com.tgx.chess.knight.raft.model.log;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.io.core.inf.IProtocol;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LogEntry
        implements
        IProtocol
{
    private final static int _LOG_SERIAL = INTERNAL_SERIAL + 2;

    private long   mTerm;
    private long   mIndex;
    private long   mRaftClientId;
    private long   mOrigin;
    private int    mPayloadSerial;
    private byte[] mPayload;

    @JsonIgnore
    private int length;

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public int serial()
    {
        return _LOG_SERIAL;
    }

    @Override
    public int superSerial()
    {
        return INTERNAL_SERIAL;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        this.mTerm = term;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        this.mIndex = index;
    }

    public byte[] getPayload()
    {
        return mPayload;
    }

    public void setPayload(byte[] payload)
    {
        this.mPayload = payload;
    }

    @Override
    public int decode(byte[] data)
    {
        return length = data.length;
    }

    @Override
    public byte[] encode()
    {
        byte[] data = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(data);
        length = data.length;
        return data;
    }

    public int getPayloadSerial()
    {
        return mPayloadSerial;
    }

    public void setPayloadSerial(int payloadSerial)
    {
        mPayloadSerial = payloadSerial;
    }

    public long getRaftClientId()
    {
        return mRaftClientId;
    }

    public void setRaftClientId(long raftClientId)
    {
        mRaftClientId = raftClientId;
    }

    public long getOrigin()
    {
        return mOrigin;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }
}
