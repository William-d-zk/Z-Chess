/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2019 Z-Chess                                                
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

package com.tgx.chess.bishop.io.zprotocol.raft;

import com.tgx.chess.bishop.io.zprotocol.ZCommand;
import com.tgx.chess.king.base.util.IoUtil;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
public class X72_RaftVote
        extends
        ZCommand
{
    public final static int COMMAND = 0x72;

    public X72_RaftVote(long msgId)
    {
        super(COMMAND, msgId);
    }

    public X72_RaftVote()
    {
        super(COMMAND, true);
    }

    @Override
    public int getPriority()
    {
        return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
    }

    //candidateId
    private long mPeerId;
    private long mTerm;
    private long mLogIndex;
    private long mLogTerm;
    private long mElector;
    private long mCommit;

    @Override
    public int dataLength()
    {
        return super.dataLength() + 8 * 6;
    }

    @Override
    public int decodec(byte[] data, int pos)
    {
        mPeerId = IoUtil.readLong(data, pos);
        pos += 8;
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mLogIndex = IoUtil.readLong(data, pos);
        pos += 8;
        mLogTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mElector = IoUtil.readLong(data, pos);
        pos += 8;
        mCommit = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mPeerId, data, pos);
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeLong(mLogIndex, data, pos);
        pos += IoUtil.writeLong(mLogTerm, data, pos);
        pos += IoUtil.writeLong(mElector, data, pos);
        pos += IoUtil.writeLong(mCommit, data, pos);
        return pos;
    }

    public long getPeerId()
    {
        return mPeerId;
    }

    public void setPeerId(long peerId)
    {
        this.mPeerId = peerId;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public long getLogIndex()
    {
        return mLogIndex;
    }

    public void setLogIndex(long logIndex)
    {
        mLogIndex = logIndex;
    }

    public long getLogTerm()
    {
        return mLogTerm;
    }

    public void setLogTerm(long logTerm)
    {
        mLogTerm = logTerm;
    }

    public long getElector()
    {
        return mElector;
    }

    public void setElector(long elector)
    {
        mElector = elector;
    }

    public void setCommit(long commit)
    {
        mCommit = commit;
    }

    public long getCommit()
    {
        return mCommit;
    }
}
