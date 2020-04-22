package com.tgx.chess.knight.cluster.spring.jpa.consensus.dao;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgx.chess.knight.cluster.spring.model.ConsistentEntry;
import com.tgx.chess.knight.endpoint.spring.jpa.model.AuditModel;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

/**
 * @author william.d.zk
 * @date 2020/4/23
 */
@Entity(name = "Consistent")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(indexes = { @Index(name = "consistent_idx_consensus_id", columnList = "consensusId") })
public class ConsistentEntity
        extends
        AuditModel
{
    @Id
    @GeneratedValue(generator = "ZConsistentGenerator")
    @GenericGenerator(name = "ZConsistentGenerator",
                      strategy = "com.tgx.chess.knight.cluster.spring.jpa.generator.ZConsistentGenerator")
    private long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "invalid_at", nullable = false)
    @JsonIgnore
    private Date            invalidAt;
    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private ConsistentEntry payload;
    @Column(updatable = false, nullable = false)
    private long            consensusId;

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

    public ConsistentEntry getPayload()
    {
        return payload;
    }

    public void setPayload(ConsistentEntry payload)
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
}
