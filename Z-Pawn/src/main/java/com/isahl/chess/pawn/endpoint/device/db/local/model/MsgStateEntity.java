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

import static com.isahl.chess.king.base.content.ByteBuf.vSizeOf;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.pawn.endpoint.device.db.legacy.LegacyBinaryType;
import com.isahl.chess.rook.storage.db.model.AuditModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import org.hibernate.annotations.Type;

@Entity(name = "msg_var")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@ISerialGenerator(parent = ISerial.STORAGE_ROOK_DB_SERIAL)
public class MsgStateEntity
        extends AuditModel
        implements ICancelable
{
    @Serial
    private static final long serialVersionUID = -7454808074804600508L;

    @Transient
    private String  mId;
    @Transient
    private long    mMsgId;
    @Transient
    private long    mTarget;
    @Transient
    private long    mOrigin;
    @Transient
    private String  mTopic;
    @Transient
    private int     mTopicAlias;
    @Transient
    private boolean mValid = true;

    @Override
    public int length()
    {
        return super.length() + // audit-model.length
               8 + // msg-id
               8 + // origin
               8 + // target
               4 + // topic-alias
               vSizeOf(mId.getBytes(StandardCharsets.UTF_8).length) + // id.length
               vSizeOf(mTopic.getBytes(StandardCharsets.UTF_8).length); // topic-length
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mMsgId = input.getLong();
        mOrigin = input.getLong();
        mTarget = input.getLong();
        mTopicAlias = input.getInt();
        remain -= 28;
        int tl = input.vLength();
        mTopic = input.readUTF(tl);
        remain -= vSizeOf(tl);
        int il = input.vLength();
        mId = input.readUTF(il);
        remain -= vSizeOf(il);
        return remain;
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        output = super.suffix(output)
                      .putLong(mMsgId)
                      .putLong(mOrigin)
                      .putLong(mTarget)
                      .putInt(mTopicAlias);
        output.vPut(mTopic.getBytes(StandardCharsets.UTF_8));
        output.vPut(mId.getBytes(StandardCharsets.UTF_8));
        return output;
    }

    @Id
    public String getId()
    {
        return mId;
    }

    @Column(name = "msg_id")
    public long getMsgId()
    {
        return mMsgId;
    }

    @Column
    public long getOrigin()
    {
        return mOrigin;
    }

    @Column
    public long getTarget()
    {
        return mTarget;
    }

    @Column(length = 511,
            nullable = false,
            updatable = false)
    public String topic()
    {
        return mTopic;
    }

    @Column(name = "topic_alias")
    public int getTopicAlias()
    {
        return mTopicAlias;
    }

    @Column
    @Type(LegacyBinaryType.class)
    public byte[] getContent()
    {
        return payload();
    }

    @Transient
    public boolean isValid()
    {
        return mValid;
    }

    @Transient
    public boolean isInvalid()
    {
        return !mValid;
    }

    public void cancel()
    {
        mValid = false;
    }

    public void setId(String id)
    {
        mId = id;
    }

    public void setMsgId(long msgId)
    {
        mMsgId = msgId;
    }

    public void setOrigin(long origin)
    {
        mOrigin = origin;
    }

    public void setTarget(long target)
    {
        mTarget = target;
    }

    public void setTopic(String topic)
    {
        mTopic = topic;
    }

    public void setTopicAlias(int alias)
    {
        mTopicAlias = alias;
    }

    public void setContent(byte[] content) {withSub(content);}

    public final static String BROKER_PRIMARY_FORMAT   = "%#20x-%7d-B";
    public final static String RECEIVER_PRIMARY_FORMAT = "%#20x-%7d-R";

    public MsgStateEntity()
    {
        super();
    }

    public MsgStateEntity(ByteBuf input)
    {
        super(input);
    }
}
