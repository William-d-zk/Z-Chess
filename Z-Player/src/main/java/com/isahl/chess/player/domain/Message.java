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

package com.isahl.chess.player.domain;

import com.isahl.chess.rook.storage.db.model.AuditModel;
import jakarta.persistence.*;

@Entity
@Table(name = "im_message",
        indexes = { @Index(name = "idx_message_group_time", columnList = "group_id, createdAt"),
                   @Index(name = "idx_message_sender_time", columnList = "sender_id, createdAt"),
                   @Index(name = "idx_message_receiver_time", columnList = "receiver_id, createdAt") })
public class Message
        extends AuditModel
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "receiver_id")
    private Long receiverId;

    @Column(nullable = false, length = 10000)
    private String content;

    @Column(nullable = false)
    private String type = "TEXT";

    @Column(nullable = false)
    private Long sequenceNum;

    @Column(nullable = false)
    private Boolean delivered = false;

    @Column(nullable = false)
    private Boolean recalled = false;

    public Message()
    {
        super();
    }

    public Message(Long senderId, String content, String type)
    {
        super();
        this.senderId = senderId;
        this.content = content;
        this.type = type;
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getGroupId()
    {
        return groupId;
    }

    public void setGroupId(Long groupId)
    {
        this.groupId = groupId;
    }

    public Long getSenderId()
    {
        return senderId;
    }

    public void setSenderId(Long senderId)
    {
        this.senderId = senderId;
    }

    public Long getReceiverId()
    {
        return receiverId;
    }

    public void setReceiverId(Long receiverId)
    {
        this.receiverId = receiverId;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public Long getSequenceNum()
    {
        return sequenceNum;
    }

    public void setSequenceNum(Long sequenceNum)
    {
        this.sequenceNum = sequenceNum;
    }

    public Boolean getDelivered()
    {
        return delivered;
    }

    public void setDelivered(Boolean delivered)
    {
        this.delivered = delivered;
    }

    public Boolean getRecalled()
    {
        return recalled;
    }

    public void setRecalled(Boolean recalled)
    {
        this.recalled = recalled;
    }
}
