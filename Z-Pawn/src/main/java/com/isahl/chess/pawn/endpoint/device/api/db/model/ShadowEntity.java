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
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2021/5/9
 */
@Entity(name = "shadow")
@TypeDef(name = "jsonb",
         typeClass = JsonBinaryType.class)
@Table(schema = "z_chess_pawn")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class ShadowEntity
        extends AuditModel
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    @Id
    @GeneratedValue
    @Column(name = "shadow_id")
    @JsonIgnore
    private long                      shadowId;
    @Column(name = "device_id",
            unique = true)
    private long                      deviceId;
    @Type(type = "jsonb")
    @Column(name = "subscribes",
            columnDefinition = "jsonb")
    private ListSerial<IThread.Topic> subscribes;
    @Type(type = "jsonb")
    @Column(name = "will_subscribe",
            columnDefinition = "jsonb")
    private IThread.Topic             willSubscribe;
    @Column(name = "will_payload")
    @Type(type = "org.hibernate.type.BinaryType")
    private byte[]                    willPayload;
    @Column(length = 64,
            name = "username")
    private String                    username;
    @Column(length = 128,
            name = "sn")
    private String                    sn;

    public ShadowEntity(ByteBuf input)
    {
        super(input);
    }

    public ShadowEntity()
    {
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

    public IThread.Topic getWillSubscribe()
    {
        return willSubscribe;
    }

    public void setWillSubscribe(IThread.Topic willSubscribe)
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

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public List<IThread.Topic> getSubscribes()
    {
        return subscribes;
    }

    public void setSubscribes(List<IThread.Topic> subscribes)
    {
        this.subscribes = new ListSerial<>(subscribes, IThread.Topic::new);
    }

    public String getSn()
    {
        return sn;
    }

    public void setSn(String sn)
    {
        this.sn = sn;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        shadowId = input.getLong();
        deviceId = input.getLong();
        int sl = input.vLength();
        sn = input.readUTF(sl);
        remain -= 16 + vSizeOf(sl);
        int ul = input.vLength();
        username = input.readUTF(ul);
        remain -= vSizeOf(ul);
        subscribes = new ListSerial<>(input, IThread.Topic::new);
        remain -= subscribes.sizeOf();
        willSubscribe = new IThread.Topic(input);
        remain -= willSubscribe.sizeOf();
        int pl = input.vLength();
        if(pl > 0) {
            willPayload = new byte[pl];
            input.get(willPayload);
            remain -= vSizeOf(pl);
        }
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(shadowId)
                    .putLong(deviceId)
                    .putUTF(sn)
                    .putUTF(username)
                    .put(subscribes.encode())
                    .put(willSubscribe.encode())
                    .vPut(willPayload);
    }

    @Override
    public int length()
    {
        return super.length() + //
               8 + // shadow-id
               8 + // device-id
               vSizeOf(sn.getBytes(StandardCharsets.UTF_8).length) + // sn.length
               subscribes.sizeOf() + // subscribes.size_of
               willSubscribe.sizeOf() + // will-subscribe.size_of
               vSizeOf(willPayload == null ? 0 : willPayload.length);  // will-payload.size_of
    }
}
