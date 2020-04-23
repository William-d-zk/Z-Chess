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

package com.tgx.chess.bishop.io.zprotocol;

import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X75_RaftRequest;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftResult;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class ZClusterFactory
        implements
        ICommandFactory<ZContext,
                        ZCommand,
                        WsFrame>
{
    @Override
    public ZCommand create(WsFrame frame)
    {
        return create(frame.getPayload()[1] & 0xFF);
    }

    @Override
    public ZCommand create(int serial)
    {

        switch (serial)
        {
            case X72_RaftVote.COMMAND:
                return new X72_RaftVote();
            case X75_RaftRequest.COMMAND:
                return new X75_RaftRequest();
            case X76_RaftResult.COMMAND:
                return new X76_RaftResult();
            case X7E_RaftBroadcast.COMMAND:
                return new X7E_RaftBroadcast();
            case X7F_RaftResponse.COMMAND:
                return new X7F_RaftResponse();
            default:
                return null;
        }
    }

}
