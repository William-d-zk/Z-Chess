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

package com.tgx.chess.spring.device.model;

import static com.tgx.chess.king.base.util.IoUtil.isBlank;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2019-08-04
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MessageEntry
        implements
        IStorage
{
    private final static int    MESSAGE_ENTRY_SERIAL = DB_SERIAL + 2;
    private static ObjectMapper JsonMapper           = new ObjectMapper();

    @Override
    public int dataLength()
    {
        return length;
    }

    @Override
    public byte[] encode()
    {
        try {
            byte[] payload = JsonMapper.writer()
                                       .writeValueAsBytes(this);
            length = payload.length;
            return payload;
        }
        catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int decode(byte[] data)
    {
        try {
            JsonNode json = JsonMapper.readTree(data);
            id = json.get("primary_key")
                     .asLong();
            msgId = json.get("msg_id")
                        .asLong();
            origin = json.get("origin")
                         .asLong();
            destination = json.get("destination")
                              .asLong();
            direction = json.get("direction")
                            .asText();
            topic = json.get("topic")
                        .asText();
            owner = json.get("owner")
                        .asText();
            payload = json.get("payload")
                          .asText()
                          .getBytes(StandardCharsets.UTF_8);
            length = data.length;
        }
        catch (IOException e) {
            e.printStackTrace();
        }
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
}
