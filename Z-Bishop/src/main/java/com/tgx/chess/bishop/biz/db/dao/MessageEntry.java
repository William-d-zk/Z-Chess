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

package com.tgx.chess.bishop.biz.db.dao;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2019-08-04
 */
public class MessageEntry
        implements
        IStorage
{
    private final static int MESSAGE_ENTRY_SERIAL = DB_SERIAL + 2;

    @Override
    public int dataLength()
    {
        return getOriginLength()
               + getTargetLength()
               + getMsgIdLength()
               + getOperationLength()
               + getStrategyLength()
               + getTopicLength()
               + getPayloadLength();
    }

    @Override
    public int serial()
    {
        return MESSAGE_ENTRY_SERIAL;
    }

    private int getOriginLength()
    {
        return 8;
    }

    private int getTargetLength()
    {
        return 8;
    }

    private int getMsgIdLength()
    {
        return 8;
    }

    private int getOperationLength()
    {
        return 1;
    }

    private int getTopicLength()
    {
        return isBlank(topic) ? 0
                              : topic.getBytes(StandardCharsets.UTF_8).length;
    }

    private int getPayloadLength()
    {
        return payload == null ? 0
                               : payload.length;
    }

    private int getStrategyLength()
    {
        return 1;
    }

    private long      origin, target;
    private long      msgId;
    private String    direction;
    private Operation operation = Operation.OP_NULL;
    private Strategy  strategy  = Strategy.RETAIN;
    private String    topic;
    private byte[]    payload;

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

    public Operation getOperation()
    {
        return operation;
    }

    @Override
    public Strategy getStrategy()
    {
        return strategy;
    }

    @Override
    public void setStrategy(Strategy strategy)
    {
        Objects.requireNonNull(strategy);
        this.strategy = strategy;
    }

    public long getOrigin()
    {
        return origin;
    }

    public void setOrigin(long origin)
    {
        this.origin = origin;
    }

    public long getTarget()
    {
        return target;
    }

    public void setTarget(long target)
    {
        this.target = target;
    }

    public String getDirection()
    {
        return direction;
    }

    public void setDirection(String direction)
    {
        this.direction = direction;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public byte[] getPayload()
    {
        return payload;
    }
}
