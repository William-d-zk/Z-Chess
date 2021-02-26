/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

package com.isahl.chess.pawn.cluster.spring.jpa.consensus.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.isahl.chess.knight.cluster.model.ConsistentProtocol;
import com.isahl.chess.rook.storage.jpa.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * @author william.d.zk
 * 
 * @date 2020/4/23
 */
@Entity(name = "Consistent")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(indexes = {@Index(name = "consistent_idx_consensus_id", columnList = "consensusId")})
public class ConsistentEntity
        extends
        AuditModel
{
    @Id
    @GeneratedValue(generator = "ZConsistentGenerator")
    @GenericGenerator(name = "ZConsistentGenerator",
                      strategy = "com.isahl.chess.pawn.cluster.spring.jpa.generator.ZConsistentGenerator")
    private long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invalid_at", nullable = false)
    @JsonIgnore
    private Date               invalidAt;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ConsistentProtocol payload;
    @Column(updatable = false, nullable = false)
    private long               consensusId;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public Date getInvalidAt()
    {
        return invalidAt;
    }

    public void setInvalidAt(Date invalidAt)
    {
        this.invalidAt = invalidAt;
    }

    public ConsistentProtocol getPayload()
    {
        return payload;
    }

    public void setPayload(ConsistentProtocol payload)
    {
        this.payload = payload;
    }

    public long getConsensusId()
    {
        return consensusId;
    }

    public void setConsensusId(long consensusId)
    {
        this.consensusId = consensusId;
    }

    private final static int SERIAL = AUDIT_MODEL_SERIAL + 1000;

    @Override
    public int serial()
    {
        return SERIAL;
    }
}
