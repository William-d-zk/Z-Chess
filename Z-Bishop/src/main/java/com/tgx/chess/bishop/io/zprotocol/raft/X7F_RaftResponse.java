/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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
import com.tgx.chess.queen.event.handler.IoDispatcher;

/**
 * @author william.d.zk
 */
public class X7F_RaftResponse
        extends
        ZCommand
{
    public final static int COMMAND = 0x7F;

    protected X7F_RaftResponse()
    {
        super(COMMAND, true);
    }

    public X7F_RaftResponse(long msgId)
    {
        super(COMMAND, msgId);
    }

    private long mTerm;
    private int  mCode;
    private long mCatchUp;

    @Override
    public int decodec(byte[] data, int pos)
    {
        mTerm = IoUtil.readLong(data, pos);
        pos += 8;
        mCode = data[pos++];
        mCatchUp = IoUtil.readLong(data, pos);
        pos += 8;
        return pos;
    }

    @Override
    public int encodec(byte[] data, int pos)
    {
        pos += IoUtil.writeLong(mTerm, data, pos);
        pos += IoUtil.writeByte(mCode, data, pos);
        pos += IoUtil.writeLong(mCatchUp, data, pos);
        return pos;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        this.mTerm = term;
    }

    public int getCode()
    {
        return mCode;
    }

    public void setCode(int code)
    {
        this.mCode = code;
    }

    public boolean isApply()
    {
        return mCode == Code.SUCCESS.getCode();
    }

    public String getRejectReason()
    {
        return Code.valueOf(mCode)
                   .getDescription();
    }

    public long getCatchUp()
    {
        return mCatchUp;
    }

    public void setCatchUp(long catchUp)
    {
        mCatchUp = catchUp;
    }

    public enum Code
    {
        SUCCESS(0, "success"),
        LOWER_TERM(1, "term < current,reject"),
        INCORRECT_TERM(2, "pre-log-index&pre-log-term inconsistent,reject");

        private final int    _Code;
        private final String _Description;

        Code(int code,
             String des)
        {
            _Code = code;
            _Description = des;
        }

        public int getCode()
        {
            return _Code;
        }

        public String getDescription()
        {
            return _Description;
        }

        static Code valueOf(int code)
        {
            switch (code)
            {
                case 0:
                    return SUCCESS;
                case 1:
                    return LOWER_TERM;
                case 2:
                    return INCORRECT_TERM;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
}
