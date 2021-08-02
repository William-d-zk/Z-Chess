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
import com.isahl.chess.rook.storage.jpa.model.AuditModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serial;

import static com.isahl.chess.pawn.endpoint.device.jpa.PawnConstants.DB_SERIAL_LOCAL_MSG_ENTITY;

@Entity(name = "msg_var")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BrokerMsgEntity
        extends AuditModel
{
    @Serial
    private static final long serialVersionUID = -7454808074804600508L;

    private int    mId;
    private String mTopic;
    private int    mTopicAlias;
    private byte[] mContent;

    @Id
    public int getId()
    {
        return mId;
    }

    @Column(length = 511,
            nullable = false,
            updatable = false,
            name = "topic")
    public String getTopic()
    {
        return mTopic;
    }

    @Column(name = "topic_alias")
    public int getTopicAlias()
    {
        return mTopicAlias;
    }

    @Column(name = "content")
    public byte[] getContent()
    {
        return mContent;
    }

    public void setId(int id)
    {
        mId = id;
    }

    public void setTopic(String topic)
    {
        mTopic = topic;
    }

    public void setTopicAlias(int alias)
    {
        mTopicAlias = alias;
    }

    public void setContent(byte[] content)
    {
        mContent = content;
    }

    @Override
    public int serial()
    {
        return DB_SERIAL_LOCAL_MSG_ENTITY;
    }

}
