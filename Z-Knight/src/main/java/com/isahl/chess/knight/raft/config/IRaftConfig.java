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

package com.isahl.chess.knight.raft.config;

import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.model.RaftNode;

import java.time.Duration;
import java.util.Map;

/**
 * @author william.d.zk
 */
public interface IRaftConfig
{
    void commit();

    void change(RaftNode delta);

    /**
     * 当前集群的节点列表
     *
     * @return cluster peer topology
     */

    Map<Long, RaftNode> getPeers();

    Map<Long, RaftNode> getNodes();

    /**
     * 集群服务绑定的服务地址 host:port
     *
     * @return local peer bind
     */
    RaftNode getPeerBind();

    boolean isInCongress();

    boolean isClusterMode();

    boolean isGateNode();

    /**
     * 集群标识UID 集群最大容量为 2^14 (16384) 个节点
     *
     * @return cluster uid setting
     * @see ZUID
     */
    ZUID getZUID();

    Duration getElectInSecond();

    Duration getSnapshotInSecond();

    Duration getHeartbeatInSecond();

    Duration getClientSubmitInSecond();

    long getSnapshotMinSize();

    long getSnapshotFragmentMaxSize();

    Map<Long, RaftNode> getGates();

    long getMaxSegmentSize();

    RaftNode findById(long peerId);

    int getSyncBatchMaxSize();

    class Uid
            implements IReset
    {
        private int mNodeId    = -1;
        private int mIdcId     = -1;
        private int mClusterId = -1;
        private int mType      = -1;

        @Override
        public void reset()
        {
            mType = -1;
            mIdcId = -1;
            mClusterId = -1;
            mNodeId = -1;
        }

        public int getNodeId()
        {
            return mNodeId;
        }

        public void setNodeId(int nodeId)
        {
            this.mNodeId = nodeId;
        }

        public int getIdcId()
        {
            return mIdcId;
        }

        public void setIdcId(int idcId)
        {
            this.mIdcId = idcId;
        }

        public int getClusterId()
        {
            return mClusterId;
        }

        public void setClusterId(int clusterId)
        {
            this.mClusterId = clusterId;
        }

        public int getType()
        {
            return mType;
        }

        public void setType(int type)
        {
            this.mType = type;
        }
    }
}
