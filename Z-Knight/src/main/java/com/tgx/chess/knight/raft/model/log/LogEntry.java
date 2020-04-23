/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.io.core.inf.IProtocol;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class LogEntry
        implements
        IProtocol
{
    private final static int _LOG_SERIAL = INTERNAL_SERIAL + 2;

    private final long   _Term;
    private final long   _Index;
    private final long   _RaftClientId;
    private final long   _Origin;
    private final int    _PayloadSerial;
    private final byte[] _Payload;

    @JsonIgnore
    private transient byte[] tData;

    @JsonIgnore
    private int length;

    @Override
    public int dataLength()
    {
        return length;
    }

    @JsonCreator
    public LogEntry(@JsonProperty("term") long term,
                    @JsonProperty("index") long index,
                    @JsonProperty("raft_client_id") long raftClientId,
                    @JsonProperty("origin") long origin,
                    @JsonProperty("payload_serial") int payloadSerial,
                    @JsonProperty("payload") byte[] payload)
    {
        _Term = term;
        _Index = index;
        _RaftClientId = raftClientId;
        _Origin = origin;
        _PayloadSerial = payloadSerial;
        _Payload = payload;
        encode();
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
        return _Term;
    }

    public long getIndex()
    {
        return _Index;
    }

    public byte[] getPayload()
    {
        return _Payload;
    }

    @Override
    public int decode(byte[] data)
    {
        return length = data.length;
    }

    @Override
    public byte[] encode()
    {
        if (tData != null) { return tData; }
        tData = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(tData);
        length = tData.length;
        return tData;
    }

    public int getPayloadSerial()
    {
        return _PayloadSerial;
    }

    public long getRaftClientId()
    {
        return _RaftClientId;
    }

    public long getOrigin()
    {
        return _Origin;
    }

    @Override
    public String toString()
    {
        return "LogEntry{"
               + "_Term="
               + _Term
               + ", _Index="
               + _Index
               + ", _RaftClientId="
               + _RaftClientId
               + ", _Origin="
               + _Origin
               + ", _PayloadSerial="
               + _PayloadSerial
               + ", _Payload="
               + IoUtil.bin2Hex(_Payload)
               + '}';
    }
}
