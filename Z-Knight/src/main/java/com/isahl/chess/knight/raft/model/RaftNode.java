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

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.message.InnerProtocol;

import java.io.Serial;
import java.nio.charset.StandardCharsets;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2021/06/25
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL)
public class RaftNode
        extends InnerProtocol
        implements Comparable<RaftNode>
{
    @Serial
    private static final long serialVersionUID = 7634053506664147831L;

    private String mHost;
    private int    mPort;
    private String mGateHost;
    private int    mGatePort;
    private byte   mState;

    public RaftNode()
    {
        super(Operation.OP_MODIFY, Strategy.RETAIN);
    }

    public RaftNode(String host, int port, RaftState state)
    {
        this();
        mHost = host;
        mPort = port;
        mState = state.getCode();
    }

    public RaftNode(ByteBuf input)
    {
        super(input);
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putShort((short) getPort())
                      .putShort((short) getGatePort())
                      .put(mState);
        output.vPut(hostBytes())
              .vPut(gateHostBytes());
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        setPort(input.getUnsignedShort());
        setGatePort(input.getUnsignedShort());
        setState(input.get());
        int hl = input.vLength();
        setHost(input.readUTF(hl));
        int gl = input.vLength();
        setGateHost(input.readUTF(gl));
        return remain - 5 - vSizeOf(hl) - vSizeOf(gl);
    }

    @Override
    public int length()
    {
        int length = 2 + // port
                     2 + // gate port
                     1;  // state
        length += vSizeOf(IoUtil.isBlank(getHost()) ? 0 : getHost().length());
        length += vSizeOf(IoUtil.isBlank(getGateHost()) ? 0 : getGateHost().length());
        return length + super.length();
    }

    public long getId()
    {
        return primaryKey();
    }

    public void setId(long id)
    {
        pKey = id;
    }

    public String getHost()
    {
        return mHost;
    }

    public byte[] hostBytes()
    {
        return IoUtil.isBlank(getHost()) ? null : getHost().getBytes(StandardCharsets.UTF_8);
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

    public boolean isInState(RaftState state)
    {
        return (mState & state.getCode()) != 0;
    }

    public void setState(byte state)
    {
        mState = state;
    }

    public String getGateHost()
    {
        return mGateHost;
    }

    public byte[] gateHostBytes()
    {
        return IoUtil.isBlank(getGateHost()) ? null : getGateHost().getBytes(StandardCharsets.UTF_8);
    }

    public void setGateHost(String host)
    {
        mGateHost = host;
    }

    public int getGatePort()
    {
        return mGatePort;
    }

    public void setGatePort(int port)
    {
        mGatePort = port;
    }

    /*
     * 对比时，仅比较 id 与 host 忽略 port
     * 不得在相同的host中绑定多个port，禁止这种操作
     */
    @Override
    public int compareTo(RaftNode o)
    {
        int a = Long.compare(primaryKey(), o.primaryKey());
        return a == 0 ? mHost.compareTo(o.mHost) : a;
    }

    @Override
    public String toString()
    {
        return String.format(" RaftNode{%#x,%s:%d,@ %s | %s:%d }",
                             primaryKey(),
                             mHost,
                             mPort,
                             mState,
                             mGateHost,
                             mGatePort);
    }
}
