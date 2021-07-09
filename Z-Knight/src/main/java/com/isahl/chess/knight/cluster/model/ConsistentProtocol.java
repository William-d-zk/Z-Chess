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

package com.isahl.chess.knight.cluster.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.bishop.io.json.JsonProtocol;
import com.isahl.chess.queen.io.core.inf.INotify;

import java.nio.charset.StandardCharsets;

/**
 * @author william.d.zk
 * @date 2020/4/25
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConsistentProtocol
        extends JsonProtocol
        implements INotify
{
    public final static int     _SERIAL = CONSISTENT_SERIAL + 1;
    private final       byte[]  _Content;
    private final       boolean _Public;
    private final       long    _Origin;
    private final       long    _Zuid;
    @JsonIgnore
    private transient   byte[]  tData;

    @JsonCreator
    public ConsistentProtocol(@JsonProperty("content") byte[] content,
                              @JsonProperty("public") boolean all,
                              @JsonProperty("zuid") long zuid,
                              @JsonProperty("origin") long origin)
    {
        _Content = content;
        _Public = all;
        _Origin = origin;
        _Zuid = zuid;
    }

    @Override
    public int serial()
    {
        return _SERIAL;
    }

    @Override
    public int superSerial()
    {
        return CONSISTENT_SERIAL;
    }

    @Override
    public byte[] encode()
    {
        if(tData != null) { return tData; }
        tData = super.encode();
        return tData;
    }

    public byte[] getContent()
    {
        return _Content;
    }

    public long getZuid()
    {
        return _Zuid;
    }

    @Override
    public long getOrigin()
    {
        return _Origin;
    }

    @Override
    public boolean isAll()
    {
        return _Public;
    }

    @Override
    public String toString()
    {
        return String.format("ConsistentProtocol{content:%s,public:%s,origin:%#x,zuid:%#x||%d}",
                             new String(_Content, StandardCharsets.UTF_8),
                             _Public,
                             _Origin,
                             _Zuid,
                             _Zuid);
    }
}
