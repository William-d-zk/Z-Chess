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

package com.isahl.chess.pawn.endpoint.device.db.local.sqlite.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.bishop.protocol.mqtt.model.data.DeviceSubscribe;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.util.IoUtil;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.queen.io.core.features.model.session.IQoS;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.io.Serial;
import java.util.Map;
import java.util.TreeMap;

import static com.isahl.chess.bishop.protocol.mqtt.model.data.DeviceSubscribe.TYPE_SUBSCRIBES;
import static com.isahl.chess.pawn.endpoint.device.db.PawnConstants.DB_SERIAL_LOCAL_SESSION_ENTITY;

/**
 * @author william.d.zk
 */
@Entity(name = "session_var")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SessionEntity
        extends AuditModel
        implements IValid
{
    @Serial
    private static final long serialVersionUID = -249635102504829625L;

    @Transient
    private DeviceSubscribe mDeviceSubscribe;

    private long       id;
    @JsonIgnore
    private String     willTopic;
    @JsonIgnore
    @Enumerated(EnumType.STRING)
    private IQoS.Level willLevel;
    @JsonIgnore
    private byte[]     willPayload;
    @JsonIgnore
    private boolean    willRetain;
    @JsonIgnore
    private String     subscribes;

    @Transient
    public DeviceSubscribe getDeviceSubscribe()
    {
        return mDeviceSubscribe;
    }

    @Override
    public int serial()
    {
        return DB_SERIAL_LOCAL_SESSION_ENTITY;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    @Id
    public long getId()
    {
        return id;
    }

    @Column(name = "will_level",
            length = 20)
    @Enumerated(EnumType.STRING)
    @JsonIgnore
    public IQoS.Level getWillLevel()
    {
        return willLevel;
    }

    public void setWillLevel(IQoS.Level mWillLevel)
    {
        this.willLevel = mWillLevel;
    }

    @Column(name = "will_topic")
    @JsonIgnore
    public String getWillTopic()
    {
        return willTopic;
    }

    public void setWillTopic(String topic)
    {
        willTopic = topic;
    }

    @Column(name = "will_payload")
    @Type(type = "org.hibernate.type.BinaryType")
    @JsonIgnore
    public byte[] getWillPayload()
    {
        return willPayload;
    }

    public void setWillPayload(byte[] payload)
    {
        willPayload = payload;
    }

    @Column(name = "will_retain")
    public boolean isWillRetain()
    {
        return willRetain;
    }

    public void setWillRetain(boolean flag)
    {
        willRetain = flag;
    }

    public void afterQuery()
    {
        Map<String, IQoS.Level> subscribes = IoUtil.isBlank(this.subscribes) ? new TreeMap<>()
                                                                             : JsonUtil.readValue(this.subscribes,
                                                                                                  TYPE_SUBSCRIBES);
        mDeviceSubscribe = new DeviceSubscribe(subscribes);
        mDeviceSubscribe.setWillTopic(willTopic);
        mDeviceSubscribe.setWillPayload(willPayload);
        mDeviceSubscribe.setWillLevel(willLevel);
        mDeviceSubscribe.setWillRetain(willRetain);
    }

    public void beforeSave()
    {
        if(mDeviceSubscribe != null) {
            subscribes = JsonUtil.writeValueAsString(mDeviceSubscribe.getSubscribes());
        }
        else {
            subscribes = "{}";
        }
    }

    @Column(name = "subscribes",
            columnDefinition = "text")
    public String getSubscribes()
    {
        return subscribes;
    }

    public void setSubscribes(String mSubscribes)
    {
        this.subscribes = mSubscribes;
    }
}
