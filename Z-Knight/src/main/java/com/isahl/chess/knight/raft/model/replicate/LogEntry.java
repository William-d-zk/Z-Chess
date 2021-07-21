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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.bishop.io.json.JsonProtocol;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.io.core.inf.ITraceable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LogEntry
        extends JsonProtocol
        implements ITraceable,
                   IStorage
{
    private final static int _LOG_SERIAL = INTERNAL_SERIAL + 2;

    private final long   _Term;
    private final long   _Index;
    private final long   _Client;//raft-client-id
    private final long   _Origin;//request-id → client
    private final int    _SubSerial;
    private final byte[] _Payload;

    @JsonIgnore
    private Operation mOperation = Operation.OP_INSERT;

    @JsonIgnore
    private transient byte[] tData;

    @JsonCreator
    public LogEntry(
            @JsonProperty("term")
                    long term,
            @JsonProperty("index")
                    long index,
            @JsonProperty("client")
                    long client,
            @JsonProperty("origin")
                    long origin,
            @JsonProperty("sub_serial")
                    int subSerial,
            @JsonProperty("payload")
                    byte[] payload)
    {
        _Term = term;
        _Index = index;
        _Client = client;
        _Origin = origin;
        _SubSerial = subSerial;
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
    public byte[] encode()
    {
        if(tData != null) { return tData; }
        tData = super.encode();
        return tData;
    }

    public int getSubSerial()
    {
        return _SubSerial;
    }

    public long getClient()
    {
        return _Client;
    }

    @Override
    public long getOrigin()
    {
        return _Origin;
    }

    @Override
    public String toString()
    {
        return String.format("raft_log{ %d@%d, from:%#x, origin:%#x, payload-serial:%#x, payload-size:%d }",
                             _Index,
                             _Term,
                             _Client,
                             _Origin,
                             _SubSerial,
                             _Payload == null ? 0 : _Payload.length);
    }

    @Override
    @JsonIgnore
    public long primaryKey()
    {
        return _Index;
    }

    @JsonIgnore
    public void setOperation(Operation op)
    {
        mOperation = op;
    }

    @JsonIgnore
    @Override
    public Operation operation()
    {
        return mOperation;
    }

    @JsonIgnore
    public Strategy strategy()
    {
        return Strategy.RETAIN;
    }
}
