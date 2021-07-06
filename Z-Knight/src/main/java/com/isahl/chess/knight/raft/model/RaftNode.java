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

package com.isahl.chess.knight.raft.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author william.d.zk
 * @date 2021/06/25
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RaftNode
        implements
        Comparable<RaftNode>
{

    private long      mId;
    private String    mHost;
    private int       mPort;
    private RaftState mState;

    public RaftNode()
    {
        mId = -1;
    }

    public RaftNode(String host,
                    int port,
                    RaftState state)
    {
        mId = -1;
        mHost = host;
        mPort = port;
        mState = state;
    }

    public long getId()
    {
        return mId;
    }

    public void setId(long id)
    {
        mId = id;
    }

    public String getHost()
    {
        return mHost;
    }

    public void setHost(String host)
    {
        mHost = host;
    }

    public int getPort()
    {
        return mPort;
    }

    public void setPort(int port)
    {
        mPort = port;
    }

    public RaftState getState()
    {
        return mState;
    }

    public void setState(String state)
    {
        mState = RaftState.valueOf(state);
    }

    @JsonIgnore
    public void setState(RaftState state)
    {
        mState = state;
    }

    @Override
    public int compareTo(RaftNode o)
    {
        int a = Long.compare(mId, o.mId);
        return a == 0 ? (mHost + ":" + mPort).compareTo(o.mHost + ":" + o.mPort): a;
    }

    @Override
    public String toString()
    {
        return String.format(" RaftNode{%#x,%s:%d,%s}", mId, mHost, mPort, mState);
    }
}
