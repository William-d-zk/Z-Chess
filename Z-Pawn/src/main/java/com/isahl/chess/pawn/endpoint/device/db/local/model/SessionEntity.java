/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.db.local.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.pawn.endpoint.device.db.legacy.LegacyBinaryType;
import com.isahl.chess.pawn.endpoint.device.model.DeviceClient;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Type;

import java.io.Serial;

/**
 * @author william.d.zk
 */
@Entity(name = "session_var")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class SessionEntity
        extends AuditModel
        implements IValid
{
    @Serial
    private static final long serialVersionUID = -249635102504829625L;

    @Transient
    private DeviceClient mClient;

    public void setId(long id)
    {
        pKey = id;
    }

    @Id
    public long getId()
    {
        return pKey;
    }

    @Column(name = "device_client")
    @Type(LegacyBinaryType.class)
    public void setDeviceClient(byte[] data)
    {
        mClient = data == null ? null : new DeviceClient(ByteBuf.wrap(data));
    }

    @Column(name = "device_client")
    @Type(LegacyBinaryType.class)
    public byte[] getDeviceClient()
    {
        return mClient == null ? null : mClient.encoded();
    }

    public DeviceClient client()
    {
        return mClient;
    }

    public void update(DeviceClient client)
    {
        mClient = client;
    }

}
