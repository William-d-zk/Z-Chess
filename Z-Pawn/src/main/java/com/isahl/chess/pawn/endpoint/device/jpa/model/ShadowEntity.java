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

package com.isahl.chess.pawn.endpoint.device.jpa.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import java.io.Serial;

/**
 * @author william.d.zk
 * @date 2021/5/9
 */
@Entity(name = "shadow")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowEntity
        extends
        AuditModel
        implements
        IStorage
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    @Id
    @GeneratedValue
    @Column(name = "shadow_id")
    private long        shadowId;
    @OneToOne(targetEntity = DeviceEntity.class, cascade = CascadeType.ALL)
    private long        deviceId;
    @Type(type = "jsonb")
    @Column(name = "will_subscribe", columnDefinition = "jsonb")
    private Subscribe   willSubscribe;
    @Column(name = "will_payload", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private MessageBody willPayload;

    public long getShadowId()
    {
        return shadowId;
    }

    public void setShadowId(long shadowId)
    {
        this.shadowId = shadowId;
    }

    public long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(long deviceId)
    {
        this.deviceId = deviceId;
    }

    public Subscribe getWillSubscribe()
    {
        return willSubscribe;
    }

    public void setWillSubscribe(Subscribe willSubscribe)
    {
        this.willSubscribe = willSubscribe;
    }

    public MessageBody getWillPayload()
    {
        return willPayload;
    }

    public void setWillPayload(MessageBody willPayload)
    {
        this.willPayload = willPayload;
    }

    @Transient
    private Operation mOperation = Operation.OP_NULL;

    @Override
    public long primaryKey()
    {
        return deviceId;
    }

    @Override
    public Operation operation()
    {
        return mOperation;
    }

    public void setOperation(Operation operation)
    {
        mOperation = operation;
    }

    @Override
    public Strategy strategy()
    {
        return Strategy.RETAIN;
    }

    @Override
    public int serial()
    {
        return SHADOW_ENTITY_SERIAL;
    }

    private final static int SHADOW_ENTITY_SERIAL = AUDIT_MODEL_SERIAL + 3;

}
