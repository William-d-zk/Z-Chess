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

package com.isahl.chess.pawn.endpoint.device.api.db.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.bishop.protocol.mqtt.model.data.DeviceSubscribe;
import com.isahl.chess.bishop.protocol.mqtt.model.data.SubscribeEntry;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serial;

import static com.isahl.chess.pawn.endpoint.device.db.PawnConstants.DB_SERIAL_REMOTE_SHADOW_ENTITY;

/**
 * @author william.d.zk
 * @date 2021/5/9
 */
@Entity(name = "shadow")
@TypeDef(name = "jsonb",
         typeClass = JsonBinaryType.class)
@Table(schema = "z_chess_pawn")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ShadowEntity
        extends AuditModel
        implements IStorage
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    @Id
    @GeneratedValue
    @Column(name = "shadow_id")
    @JsonIgnore
    private long            shadowId;
    @Column(name = "device_id",
            unique = true)
    private long            deviceId;
    @Type(type = "jsonb")
    @Column(name = "subscribes",
            columnDefinition = "jsonb")
    private DeviceSubscribe subscribes;
    @Type(type = "jsonb")
    @Column(name = "will_subscribe",
            columnDefinition = "jsonb")
    private SubscribeEntry  willSubscribe;
    @Column(name = "will_payload")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[]          willPayload;
    @Column(length = 32,
            name = "username")
    private String          username;

    public ShadowEntity()
    {
    }

    public ShadowEntity(long deviceId)
    {
        this.deviceId = deviceId;
    }

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

    public SubscribeEntry getWillSubscribe()
    {
        return willSubscribe;
    }

    public void setWillSubscribe(SubscribeEntry willSubscribe)
    {
        this.willSubscribe = willSubscribe;
    }

    public byte[] getWillPayload()
    {
        return willPayload;
    }

    public void setWillPayload(byte[] willPayload)
    {
        this.willPayload = willPayload;
    }

    @Transient
    private Operation mOperation = Operation.OP_NULL;

    @Override
    public long primaryKey()
    {
        return shadowId;
    }

    @Override
    public long foreignKey()
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
        return DB_SERIAL_REMOTE_SHADOW_ENTITY;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public DeviceSubscribe getSubscribes()
    {
        return subscribes;
    }

    public void setSubscribes(DeviceSubscribe subscribes)
    {
        this.subscribes = subscribes;
    }
}
