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

package com.isahl.chess.bishop.io.ws.zchat.factory;

import com.isahl.chess.bishop.io.ws.zchat.ZContext;
import com.isahl.chess.bishop.io.ws.zchat.model.ZCommand;
import com.isahl.chess.bishop.io.ws.zchat.model.command.raft.*;
import com.isahl.chess.queen.io.core.features.model.content.ICommand;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class ZClusterFactory
        implements ICommand.Factory<ICommand, IFrame, ZContext>
{
    @Override
    public ICommand create(IFrame frame, ZContext context)
    {
        return create(frame.payload()[1] & 0xFF, frame.payload(), context);
    }

    @Override
    public ICommand create(int serial, byte[] input, ZContext context)
    {
        ZCommand cmd = switch(serial) {
            case X70_RaftVote.COMMAND -> new X70_RaftVote();
            case X71_RaftBallot.COMMAND -> new X71_RaftBallot();
            case X72_RaftAppend.COMMAND -> new X72_RaftAppend();
            case X73_RaftAccept.COMMAND -> new X73_RaftAccept();
            case X74_RaftReject.COMMAND -> new X74_RaftReject();
            case X75_RaftReq.COMMAND -> new X75_RaftReq();
            case X76_RaftResp.COMMAND -> new X76_RaftResp();
            case X77_RaftNotify.COMMAND -> new X77_RaftNotify();
            case X78_RaftChange.COMMAND -> new X78_RaftChange();
            case X79_RaftAdjudge.COMMAND -> new X79_RaftAdjudge();
            default -> null;
        };
        if(cmd != null) {cmd.decode(input, context);}
        return cmd;
    }

}
