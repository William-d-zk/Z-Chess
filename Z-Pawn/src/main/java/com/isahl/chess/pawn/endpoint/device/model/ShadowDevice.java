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

package com.isahl.chess.pawn.endpoint.device.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.pawn.endpoint.device.jpa.model.DeviceSubscribe;
import com.isahl.chess.pawn.endpoint.device.jpa.model.MessageBody;
import com.isahl.chess.pawn.endpoint.device.jpa.model.ShadowEntity;
import com.isahl.chess.pawn.endpoint.device.jpa.model.Subscribe;

import java.io.Serial;
import java.io.Serializable;
import java.util.Queue;

/**
 * @author william.d.zk
 * @date 2021/5/9
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowDevice
        implements
        Serializable
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    private final long               _DeviceId;
    private final DeviceSubscribe    _Subscribes;
    private final Queue<MessageBody> _MsgQueue;
    private final Subscribe          _WillSubscribe;
    private final byte[]             _WillPayload;
    private final boolean            _WillRetain;
    private final String             _Username;

    @JsonCreator
    public ShadowDevice(@JsonProperty("device_id") long deviceId,
                        @JsonProperty("subscribes") DeviceSubscribe subscribes,
                        @JsonProperty("msg_queue") Queue<MessageBody> msgQueue,
                        @JsonProperty("will_subscribe") Subscribe willSubscribe,
                        @JsonProperty("will_payload") byte[] willPayload,
                        @JsonProperty("will_retain") boolean willRetain,
                        @JsonProperty("username") String username)
    {
        _DeviceId = deviceId;
        _Subscribes = subscribes;
        _MsgQueue = msgQueue;
        _WillSubscribe = willSubscribe;
        _WillPayload = willPayload;
        _WillRetain = willRetain;
        _Username = username;
    }

    @JsonIgnore
    public long getDeviceId()
    {
        return _DeviceId;
    }

    public DeviceSubscribe getSubscribes()
    {
        return _Subscribes;
    }

    public Queue<MessageBody> getMsgQueue()
    {
        return _MsgQueue;
    }

    public Subscribe getWillSubscribe()
    {
        return _WillSubscribe;
    }

    public byte[] getWillPayload()
    {
        return _WillPayload;
    }

    public boolean isWillRetain()
    {
        return _WillRetain;
    }

    public ShadowEntity convert()
    {
        ShadowEntity entity = new ShadowEntity();
        entity.setUsername(_Username);
        entity.setDeviceId(_DeviceId);
        entity.setWillPayload(_WillPayload);
        entity.setWillSubscribe(_WillSubscribe);
        return entity;
    }

}
