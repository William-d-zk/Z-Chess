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

package com.isahl.chess.bishop.protocol.zchat.model.command;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.ZControl;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.queen.io.core.features.model.content.ICommand;

/**
 * @author william.d.zk
 * @date 2019-07-14
 */

public abstract class ZCommand
        extends ZControl
        implements ICommand<ZContext>
{
    private long mMsgId;

    public ZCommand()
    {
        super();
        withId(true);
        mFrameHeader ^= ZFrame.frame_op_code_ctrl;
    }

    public ZCommand(long msgId)
    {
        super();
        setMsgId(msgId);
        mFrameHeader ^= ZFrame.frame_op_code_ctrl;
    }

    @Override
    public void setMsgId(long msgId)
    {
        withId(true);
        mMsgId = msgId;
    }

    @Override
    public long getMsgId()
    {
        return mMsgId;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        input.markReader();
        mAttr = input.get();
        int cmd = input.getUnsigned();
        if(cmd != serial()) {
            throw new ZException("input.cmd[%d]≠self.serial[%d]", cmd, serial());
        }
        if(!isWithId()) {
            throw new ZException("z-chat-command without msg-id?!");
        }
        setMsgId(input.getLong());
        return input.readableBytes();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(getMsgId());
    }
}
