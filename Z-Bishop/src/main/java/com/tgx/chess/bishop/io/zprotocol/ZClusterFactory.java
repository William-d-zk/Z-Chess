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

package com.tgx.chess.bishop.io.zprotocol;

import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.zprotocol.raft.X70_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X71_RaftBallot;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftAppend;
import com.tgx.chess.bishop.io.zprotocol.raft.X73_RaftAccept;
import com.tgx.chess.bishop.io.zprotocol.raft.X74_RaftReject;
import com.tgx.chess.bishop.io.zprotocol.raft.X75_RaftRequest;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;

/**
 * @author william.d.zk
 * 
 * @date 2019-05-08
 */
public class ZClusterFactory
        implements
        ICommandFactory<ZCommand, WsFrame>
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
            case X70_RaftVote.COMMAND:
                return new X70_RaftVote();
            case X71_RaftBallot.COMMAND:
                return new X71_RaftBallot();
            case X72_RaftAppend.COMMAND:
                return new X72_RaftAppend();
            case X73_RaftAccept.COMMAND:
                return new X73_RaftAccept();
            case X74_RaftReject.COMMAND:
                return new X74_RaftReject();
            case X75_RaftRequest.COMMAND:
                return new X75_RaftRequest();
            case X76_RaftNotify.COMMAND:
                return new X76_RaftNotify();
            default:
                return null;
        }
    }

}
