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

package com.tgx.chess.knight.endpoint.spring.device.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2019-08-04
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class MessageEntry
        implements
        IStorage
{
    private final static int MESSAGE_ENTRY_SERIAL = DB_SERIAL + 2;

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public byte[] encode()
    {
        byte[] payload = JsonUtil.writeValueAsBytes(this);
        Objects.requireNonNull(payload);
        length = payload.length;
        return payload;
    }

    @Override
    public int decode(byte[] data)
    {
        MessageEntry json = JsonUtil.readValue(data, getClass());
        Objects.requireNonNull(json);
        id = json.getPrimaryKey();
        msgId = json.getMsgId();
        origin = json.getOrigin();
        destination = json.getDestination();
        direction = json.getDirection();
        topic = json.getTopic();
        owner = json.getOwner();
        payload = json.getPayload();
        cmd = json.getCmd();
        length = data.length;
        return length;
    }

    @Override
    public int serial()
    {
        return MESSAGE_ENTRY_SERIAL;
    }

    private long   origin, destination;
    private long   msgId;
    private long   id;
    private String direction;
    private String topic;
    private String owner;
    private byte[] payload;
    private int    cmd;

    @JsonIgnore
    private int       length;
    @JsonIgnore
    private Operation operation = Operation.OP_NULL;
    @JsonIgnore
    private Strategy  strategy  = Strategy.RETAIN;

    @Override
    public long getPrimaryKey()
    {
        return id;
    }

    @Override
    public void setPrimaryKey(long key)
    {
        id = key;
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

    public long getDestination()
    {
        return destination;
    }

    public void setDestination(long destination)
    {
        this.destination = destination;
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

    public void setOwner(String owner)
    {
        this.owner = owner;
    }

    public String getOwner()
    {
        return owner;
    }

    public long getMsgId()
    {
        return msgId;
    }

    public void setMsgId(long msgId)
    {
        this.msgId = msgId;
    }

    public int getCmd()
    {
        return cmd;
    }

    public void setCmd(int cmd)
    {
        this.cmd = cmd;
    }

}
