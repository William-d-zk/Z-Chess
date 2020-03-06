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

package com.tgx.chess.cluster.raft.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.json.JsonUtil;
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RaftMessage
        implements
        IRaftMessage
{
    private final static int RAFT_MESSAGE_SERIAL = DB_SERIAL + 3;

    private IRaftNode.RaftState raftState;
    private long                peerId;
    private long                term;
    private long                msgId;

    @JsonIgnore
    private int       length;
    @JsonIgnore
    private Operation operation = Operation.OP_NULL;
    @JsonIgnore
    private Strategy  strategy  = Strategy.RETAIN;

    @Override
    public byte[] encode()
    {
        byte[] payload = JsonUtil.writeValue(this);
        Objects.requireNonNull(payload);
        length = payload.length;
        return payload;
    }

    @Override
    public int decode(byte[] data)
    {
        RaftMessage json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        msgId = json.msgId;
        peerId = json.peerId;
        term = json.getTerm();
        raftState = json.getState();
        length = data.length;
        return length;
    }

    @Override
    public long getNode()
    {
        return peerId;
    }

    @Override
    public long getTerm()
    {
        return term;
    }

    @Override
    public IRaftNode.RaftState getState()
    {
        return raftState;
    }

    @Override
    public long getPrimaryKey()
    {
        return msgId;
    }

    @Override
    public void setPrimaryKey(long key)
    {
        msgId = key;
    }

    @Override
    public void setOperation(Operation op)
    {
        operation = op;
    }

    @Override
    public Operation getOperation()
    {
        return operation;
    }

    @Override
    public void setStrategy(Strategy strategy)
    {
        this.strategy = strategy;
    }

    @Override
    public Strategy getStrategy()
    {
        return strategy;
    }

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public int serial()
    {
        return RAFT_MESSAGE_SERIAL;
    }
}
