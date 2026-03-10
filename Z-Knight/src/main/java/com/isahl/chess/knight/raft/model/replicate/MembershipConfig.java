/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.knight.raft.model.replicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.knight.raft.model.MembershipChangeManager;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

import java.io.Serial;
import java.util.Arrays;

/**
 * 成员变更配置 - 用于持久化成员变更状态
 * 
 * Joint Consensus 配置格式:
 * - phase: 变更阶段 (IDLE, PREPARING, JOINT, COMMITTING, CONFIRMED, ROLLBACK, FAILED)
 * - transactionId: 变更事务ID
 * - oldPeers: 旧配置节点列表
 * - newPeers: 新配置节点列表
 * - leader: Leader节点ID
 * - index: 配置变更日志索引
 * - term: 配置变更日志任期
 */
@ISerialGenerator(parent = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL,
                  serial = IProtocol.CLUSTER_KNIGHT_RAFT_SERIAL + 10)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MembershipConfig
        extends BaseMeta
{
    @Serial
    private static final long serialVersionUID = 501154084882121307L;

    /**
     * 变更阶段
     */
    private int    mPhase = MembershipChangeManager.Phase.IDLE.ordinal();
    
    /**
     * 变更事务ID
     */
    private long   mTransactionId = 0;
    
    /**
     * 旧配置节点列表
     */
    private long[] mOldPeers = new long[0];
    
    /**
     * 新配置节点列表
     */
    private long[] mNewPeers = new long[0];
    
    /**
     * Leader节点ID
     */
    private long   mLeader = 0;
    
    /**
     * 配置变更日志索引
     */
    private long   mIndex = 0;
    
    /**
     * 配置变更日志任期
     */
    private long   mTerm = 0;
    
    /**
     * 错误信息（如果变更失败）
     */
    private String mErrorMessage = "";

    public MembershipConfig()
    {
        super();
    }

    public MembershipConfig(ByteBuf input)
    {
        super(input);
    }

    @JsonCreator
    public MembershipConfig(
            @JsonProperty("phase")
            int phase,
            @JsonProperty("transaction_id")
            long transactionId,
            @JsonProperty("old_peers")
            long[] oldPeers,
            @JsonProperty("new_peers")
            long[] newPeers,
            @JsonProperty("leader")
            long leader,
            @JsonProperty("index")
            long index,
            @JsonProperty("term")
            long term,
            @JsonProperty("error_message")
            String errorMessage)
    {
        super();
        mPhase = phase;
        mTransactionId = transactionId;
        mOldPeers = oldPeers != null ? oldPeers : new long[0];
        mNewPeers = newPeers != null ? newPeers : new long[0];
        mLeader = leader;
        mIndex = index;
        mTerm = term;
        mErrorMessage = errorMessage != null ? errorMessage : "";
    }

    @Override
    public int length()
    {
        return 4 + // phase
               8 + // transactionId
               4 + mOldPeers.length * 8 + // oldPeers (length + data)
               4 + mNewPeers.length * 8 + // newPeers (length + data)
               8 + // leader
               8 + // index
               8 + // term
               4 + mErrorMessage.getBytes().length + // errorMessage (length + data)
               super.length();
    }

    @Override
    public ByteBuf suffix(ByteBuf output)
    {
        super.suffix(output);
        output.putInt(mPhase);
        output.putLong(mTransactionId);
        
        // oldPeers
        output.putInt(mOldPeers.length);
        for(long peer : mOldPeers) {
            output.putLong(peer);
        }
        
        // newPeers
        output.putInt(mNewPeers.length);
        for(long peer : mNewPeers) {
            output.putLong(peer);
        }
        
        output.putLong(mLeader);
        output.putLong(mIndex);
        output.putLong(mTerm);
        
        // errorMessage
        byte[] errorBytes = mErrorMessage.getBytes();
        output.putInt(errorBytes.length);
        output.put(errorBytes);
        
        return output;
    }

    @Override
    public int prefix(ByteBuf input)
    {
        int remain = super.prefix(input);
        mPhase = input.getInt();
        remain -= 4;
        mTransactionId = input.getLong();
        remain -= 8;
        
        // oldPeers
        int oldPeersLen = input.getInt();
        remain -= 4;
        mOldPeers = new long[oldPeersLen];
        for(int i = 0; i < oldPeersLen; i++) {
            mOldPeers[i] = input.getLong();
            remain -= 8;
        }
        
        // newPeers
        int newPeersLen = input.getInt();
        remain -= 4;
        mNewPeers = new long[newPeersLen];
        for(int i = 0; i < newPeersLen; i++) {
            mNewPeers[i] = input.getLong();
            remain -= 8;
        }
        
        mLeader = input.getLong();
        remain -= 8;
        mIndex = input.getLong();
        remain -= 8;
        mTerm = input.getLong();
        remain -= 8;
        
        // errorMessage
        int errorLen = input.getInt();
        remain -= 4;
        if(errorLen > 0) {
            byte[] errorBytes = new byte[errorLen];
            input.get(errorBytes);
            mErrorMessage = new String(errorBytes);
            remain -= errorLen;
        }
        else {
            mErrorMessage = "";
        }
        
        return remain;
    }

    @Override
    public void reset()
    {
        mPhase = MembershipChangeManager.Phase.IDLE.ordinal();
        mTransactionId = 0;
        mOldPeers = new long[0];
        mNewPeers = new long[0];
        mLeader = 0;
        mIndex = 0;
        mTerm = 0;
        mErrorMessage = "";
        flush();
    }

    // Getters and Setters

    public MembershipChangeManager.Phase getPhase()
    {
        int ordinal = mPhase;
        MembershipChangeManager.Phase[] phases = MembershipChangeManager.Phase.values();
        if(ordinal >= 0 && ordinal < phases.length) {
            return phases[ordinal];
        }
        return MembershipChangeManager.Phase.IDLE;
    }

    public void setPhase(MembershipChangeManager.Phase phase)
    {
        mPhase = phase.ordinal();
    }

    public long getTransactionId()
    {
        return mTransactionId;
    }

    public void setTransactionId(long transactionId)
    {
        mTransactionId = transactionId;
    }

    public long[] getOldPeers()
    {
        return mOldPeers;
    }

    public void setOldPeers(long[] oldPeers)
    {
        mOldPeers = oldPeers != null ? oldPeers : new long[0];
    }

    public long[] getNewPeers()
    {
        return mNewPeers;
    }

    public void setNewPeers(long[] newPeers)
    {
        mNewPeers = newPeers != null ? newPeers : new long[0];
    }

    public long getLeader()
    {
        return mLeader;
    }

    public void setLeader(long leader)
    {
        mLeader = leader;
    }

    public long getIndex()
    {
        return mIndex;
    }

    public void setIndex(long index)
    {
        mIndex = index;
    }

    public long getTerm()
    {
        return mTerm;
    }

    public void setTerm(long term)
    {
        mTerm = term;
    }

    public String getErrorMessage()
    {
        return mErrorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        mErrorMessage = errorMessage != null ? errorMessage : "";
    }

    /**
     * 从变更事务创建配置
     */
    public static MembershipConfig fromTransaction(MembershipChangeManager.ChangeTransaction tx, long index, long term)
    {
        MembershipConfig config = new MembershipConfig();
        config.setPhase(tx.getPhase());
        config.setTransactionId(tx.getId());
        config.setOldPeers(tx.getOldPeers());
        config.setNewPeers(tx.getNewPeers());
        config.setLeader(tx.getLeader());
        config.setIndex(index);
        config.setTerm(term);
        config.setErrorMessage(tx.getErrorMessage());
        return config;
    }

    @Override
    public String toString()
    {
        return String.format("MembershipConfig{phase=%s, txId=%d, leader=%#x, index=%d@%d, old=%s, new=%s}",
                             getPhase(), mTransactionId, mLeader, mIndex, mTerm,
                             Arrays.toString(mOldPeers), Arrays.toString(mNewPeers));
    }
}
