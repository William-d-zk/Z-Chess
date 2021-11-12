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

package com.isahl.chess.bishop.protocol.ws.zchat.model.command.raft;

import com.isahl.chess.bishop.protocol.ws.zchat.model.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL,
                  serial = 0x73)
public class X73_RaftAccept
        extends ZCommand
{

    public X73_RaftAccept()
    {
        super(true);
    }

    public X73_RaftAccept(long msgId)
    {
        super(msgId);
    }

    private long mFollowerId;
    private long mTerm;
    private long mCatchUp;
    private long mCatchUpTerm;
    private long mLeaderId;

    @Override
    public int length()
    {
        return super.length() + 40;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mFollowerId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mCatchUp = IoUtil.readLong(data, pos);
        pos += 8;
        mCatchUpTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mLeaderId = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mFollowerId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mCatchUp, data, pos);
        pos += IoUtil.writeLong(mCatchUpTerm, data, pos);
        pos += IoUtil.writeLong(mLeaderId, data, pos);
        return pos;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getCatchUp()
    {
        return mCatchUp;
    }

    public void setCatchUp(long catchUp)
    {
        mCatchUp = catchUp;
    }

    public long getCatchUpTerm()
    {
        return mCatchUpTerm;
    }

    public void setCatchUpTerm(long catchUpTerm)
    {
        mCatchUpTerm = catchUpTerm;
    }

    public long getFollowerId()
    {
        return mFollowerId;
    }

    public void setFollowerId(long peerId)
    {
        mFollowerId = peerId;
    }

    public void setLeaderId(long leaderId)
    {
        mLeaderId = leaderId;
    }

    public long getLeaderId()
    {
        return mLeaderId;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return String.format("X71_RaftAccept{follower:%#x@%d, catch_up:%d@%d}",
                             mFollowerId,
                             mTerm,
                             mCatchUp,
                             mCatchUpTerm);
    }
}
