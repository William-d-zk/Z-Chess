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

package com.isahl.chess.pawn.endpoint.device.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.BinarySerial;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.pawn.endpoint.device.api.db.model.ShadowEntity;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Queue;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * @date 2021/5/9
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.BIZ_PLAYER_API_SERIAL)
public class ShadowDevice
        extends BinarySerial
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    private final long                      _DeviceId;
    private final ListSerial<IThread.Topic> _Subscribes;
    private final ListSerial<MessageBody>   _MsgQueue;
    private final IThread.Topic             _WillSubscribe;
    private final byte[]                    _WillPayload;
    private final String                    _Username;
    private final String                    _SN;

    private transient long                      mDeviceId;
    private transient ListSerial<IThread.Topic> mSubscribes;
    private transient ListSerial<MessageBody>   mMsgQueue;
    private transient IThread.Topic             mWillSubscribe;
    private transient byte[]                    mWillPayload;
    private transient String                    mUsername;
    private transient String                    mSN;

    @JsonCreator
    public ShadowDevice(
            @JsonProperty("device_id")
                    long deviceId,
            @JsonProperty("sn")
                    String sn,
            @JsonProperty("subscribes")
                    List<IThread.Topic> subscribes,
            @JsonProperty("msg_queue")
                    Queue<MessageBody> msgQueue,
            @JsonProperty("will_subscribe")
                    IThread.Topic willSubscribe,
            @JsonProperty("will_payload")
                    byte[] willPayload,
            @JsonProperty("username")
                    String username)
    {
        _DeviceId = deviceId;
        _Subscribes = new ListSerial<>(subscribes, IThread.Topic::new);
        _MsgQueue = new ListSerial<>(msgQueue, MessageBody::new);
        _WillSubscribe = willSubscribe;
        _WillPayload = willPayload;
        _Username = username;
        _SN = sn;
    }

    public ShadowDevice(ByteBuf input)
    {
        super(input);
        _DeviceId = mDeviceId;
        _Subscribes = mSubscribes;
        _MsgQueue = mMsgQueue;
        _WillSubscribe = mWillSubscribe;
        _WillPayload = mWillPayload;
        _Username = mUsername;
        _SN = mSN;
    }

    public long getDeviceId()
    {
        return _DeviceId;
    }

    public List<IThread.Topic> getSubscribes()
    {
        return _Subscribes;
    }

    public Queue<MessageBody> getMsgQueue()
    {
        return _MsgQueue;
    }

    public IThread.Topic getWillSubscribe()
    {
        return _WillSubscribe;
    }

    public byte[] getWillPayload()
    {
        return _WillPayload;
    }

    public String getSn()
    {
        return _SN;
    }

    public String getUsername()
    {
        return _Username;
    }

    @Override
    public int length()
    {
        return super.length() + 8 + // device_id
               _Subscribes.sizeOf() +  // subscribes.size_of
               _MsgQueue.sizeOf() + // msg-queue
               _WillSubscribe.sizeOf() + // will-subscribe
               vSizeOf(_WillPayload == null ? 0 : _WillPayload.length) + // will-payload
               vSizeOf(_Username.getBytes(StandardCharsets.UTF_8).length) + // username-length
               vSizeOf(_SN.getBytes(StandardCharsets.UTF_8).length);// sn-length
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mDeviceId = input.getLong();
        remain -= 8;
        mSubscribes = new ListSerial<>(input, IThread.Topic::new);
        remain -= mSubscribes.sizeOf();
        mMsgQueue = new ListSerial<>(input, MessageBody::new);
        remain -= mMsgQueue.sizeOf();
        mWillSubscribe = new IThread.Topic(input);
        remain -= mWillSubscribe.sizeOf();
        int pl = input.vLength();
        mWillPayload = new byte[pl];
        input.get(mWillPayload);
        remain -= vSizeOf(pl);
        int ul = input.vLength();
        mUsername = input.readUTF(ul);
        remain -= vSizeOf(ul);
        int sl = input.vLength();
        mSN = input.readUTF(sl);
        remain -= vSizeOf(sl);
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        return super.suffix(output)
                    .putLong(_DeviceId)
                    .put(_Subscribes.encode())
                    .put(_MsgQueue.encode())
                    .put(_WillSubscribe.encode())
                    .put(_WillPayload)
                    .putUTF(_Username)
                    .putUTF(_SN);
    }

    public ShadowEntity convert()
    {
        ShadowEntity entity = new ShadowEntity();
        entity.setUsername(_Username);
        entity.setDeviceId(_DeviceId);
        entity.setWillPayload(_WillPayload);
        entity.setWillSubscribe(_WillSubscribe);
        entity.setSn(_SN);
        return entity;
    }

}
