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

package com.isahl.chess.pawn.endpoint.device.jpa.local.sqlite.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.king.base.schedule.inf.ICancelable;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serial;

import static com.isahl.chess.pawn.endpoint.device.jpa.PawnConstants.DB_SERIAL_LOCAL_MSG_ENTITY;

@Entity(name = "msg_var")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BrokerMsgEntity
        extends AuditModel
        implements ICancelable
{
    @Serial
    private static final long serialVersionUID = -7454808074804600508L;

    private String  id;
    private int     msgId;
    private long    target;
    private long    origin;
    private String  topic;
    private int     topicAlias;
    private byte[]  content;
    @Transient
    private boolean valid = true;

    @Id
    public String getId()
    {
        return id;
    }

    @Column(name = "msg_id")
    public int getMsgId()
    {
        return msgId;
    }

    @Column(name = "origin")
    public long getOrigin()
    {
        return origin;
    }

    @Column(name = "target")
    public long getTarget()
    {
        return target;
    }

    @Column(length = 511,
            nullable = false,
            updatable = false,
            name = "topic")
    public String getTopic()
    {
        return topic;
    }

    @Column(name = "topic_alias")
    public int getTopicAlias()
    {
        return topicAlias;
    }

    @Column(name = "content")
    @Type(type = "org.hibernate.type.BinaryType")
    public byte[] getContent()
    {
        return content;
    }

    @Transient
    public boolean isValid()
    {
        return valid;
    }

    @Transient
    public boolean isInvalid()
    {
        return !valid;
    }

    public void cancel()
    {
        valid = false;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setMsgId(int msgId)
    {
        this.msgId = msgId;
    }

    public void setOrigin(long origin)
    {
        this.origin = origin;
    }

    public void setTarget(long target)
    {
        this.target = target;
    }

    public void setTopic(String topic)
    {
        this.topic = topic;
    }

    public void setTopicAlias(int alias)
    {
        topicAlias = alias;
    }

    public void setContent(byte[] content)
    {
        this.content = content;
    }

    @Override
    public int serial()
    {
        return DB_SERIAL_LOCAL_MSG_ENTITY;
    }

    public final static String BROKER_PRIMARY_FORMAT   = "%#20x-%7d-B";
    public final static String RECEIVER_PRIMARY_FORMAT = "%#20x-%7d-R";
}
