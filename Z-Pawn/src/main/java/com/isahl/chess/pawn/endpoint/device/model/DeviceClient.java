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

package com.isahl.chess.pawn.endpoint.device.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.model.BinarySerial;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.routes.IThread;

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

/**
 * @author william.d.zk
 * {@code @date} 2021/5/9
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.BIZ_PLAYER_API_SERIAL)
public class DeviceClient
        extends BinarySerial
{

    @Serial
    private static final long serialVersionUID = -6248323684179351633L;

    private long                      mDeviceId;
    private String                    mUsername;
    private String                    mNotice;
    private IThread.Topic             mWillContent;
    private ListSerial<IThread.Topic> mSubscribes;
    private long                      mKeepAlive;
    private long                      mInvalidAt;
    private boolean                   mClean;

    private final transient Map<Long, IProtocol> tIdentifierReceivedMap = new HashMap<>();
    private final transient Map<Long, IProtocol> tIdentifierSendingMap  = new HashMap<>();

    @JsonCreator
    public DeviceClient(
            @JsonProperty("device_id")
            long deviceId,
            @JsonProperty("username")
            String username,
            @JsonProperty("notice")
            String notice)

    {
        mDeviceId = deviceId;
        mSubscribes = new ListSerial<>(IThread.Topic::new);
        mUsername = username;
        mNotice = notice;
    }

    public DeviceClient(long deviceId)
    {
        mDeviceId = deviceId;
        mSubscribes = new ListSerial<>(IThread.Topic::new);
    }

    public DeviceClient(ByteBuf input)
    {
        super(input);
    }

    public long getDeviceId()
    {
        return mDeviceId;
    }

    public List<IThread.Topic> getSubscribes()
    {
        return mSubscribes;
    }

    public IThread.Topic getWillContent()
    {
        return mWillContent;
    }

    public String getNotice()
    {
        return mNotice;
    }

    public String getUsername()
    {
        return mUsername;
    }

    public long getKeepAlive()
    {
        return mKeepAlive;
    }

    public long getInvalidAt()
    {
        return mInvalidAt;
    }

    public boolean isClean()
    {
        return mClean;
    }

    public Map<Long, IProtocol> identifierReceivedMap()
    {
        return tIdentifierReceivedMap;
    }

    public Map<Long, IProtocol> identifierSendingMap()
    {
        return tIdentifierSendingMap;
    }

    public void setSubscribes(List<IThread.Topic> subscribes)
    {
        mSubscribes.clear();
        mSubscribes.addAll(subscribes);
    }

    public void setWillContent(IThread.Topic content)
    {
        mWillContent = content;
    }

    public void setKeepAlive(long duration)
    {
        mKeepAlive = duration;
    }

    public void setInvalidAt(long timestamp)
    {
        mInvalidAt = timestamp;
    }

    public void setClean(boolean clean)
    {
        mClean = clean;
    }

    public void of(DeviceEntity device)
    {
        mUsername = device.getUsername();
        mNotice = device.getNotice();
    }

    @Override
    public int length()
    {
        return super.length() + // pyaload
               8 + // device_id
               8 + // keep alive
               8 + // invalid at
               1 + // clean
               mSubscribes.sizeOf() +  // subscribes.size_of
               vSizeOf(mUsername == null ? 0 : mUsername.getBytes(StandardCharsets.UTF_8).length) + // username-length
               vSizeOf(mNotice == null ? 0 : mNotice.getBytes(StandardCharsets.UTF_8).length) +// sn-length
               (mWillContent == null ? 0 : mWillContent.sizeOf()); // will-content
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mDeviceId = input.getLong();
        remain -= 8;
        mKeepAlive = input.getLong();
        remain -= 8;
        mInvalidAt = input.getLong();
        remain -= 8;
        mClean = input.get() != 0;
        remain -= 1;
        mSubscribes = new ListSerial<>(input, IThread.Topic::new);
        remain -= mSubscribes.sizeOf();
        int ul = input.vLength();
        mUsername = input.readUTF(ul);
        remain -= vSizeOf(ul);
        int sl = input.vLength();
        mNotice = input.readUTF(sl);
        remain -= vSizeOf(sl);
        if(remain > 0) {
            mWillContent = new IThread.Topic(input);
            remain -= mWillContent.sizeOf();
        }
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mDeviceId)
                      .putLong(mKeepAlive)
                      .putLong(mInvalidAt)
                      .put(mClean ? 1 : 0)
                      .put(mSubscribes.encode())
                      .putUTF(mUsername)
                      .putUTF(mNotice);
        if(mWillContent != null) {
            output.put(mWillContent.encode());
        }
        return output;
    }

}
