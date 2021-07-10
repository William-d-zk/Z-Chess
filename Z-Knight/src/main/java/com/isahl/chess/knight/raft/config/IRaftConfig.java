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

package com.isahl.chess.knight.raft.config;

import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.model.RaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.queen.db.inf.IStorage;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * @author william.d.zk
 */
public interface IRaftConfig
{
    /**
     * 当前集群的节点列表
     *
     * @return cluster peer topology
     */

    List<RaftNode> getPeers();

    /**
     * 与其他独region 进行通讯的网关
     *
     * @return gate topology
     */
    List<RaftNode> getGates();

    /**
     * 集群服务绑定的服务地址 host:port
     *
     * @return local peer bind
     */
    RaftNode getPeerBind();

    /**
     * 集群服务绑定的分区节点地址 host:port
     *
     * @return global gate bind
     */
    RaftNode getGateBind();

    /**
     * 集群标识UID 集群最大容量为 2^14 (16384) 个节点
     *
     * @return cluster uid setting
     * @see ZUID
     */
    boolean isInCongress();

    boolean isGateNode();

    Uid getUid();

    ZUID createZUID();

    Duration getElectInSecond();

    Duration getSnapshotInSecond();

    Duration getHeartbeatInSecond();

    Duration getClientSubmitInSecond();

    long getSnapshotMinSize();

    long getSnapshotFragmentMaxSize();

    long getMaxSegmentSize();

    boolean isClusterMode();

    RaftConfig getConfig();

    void update(RaftConfig source) throws IOException;

    /**
     * 调整集群拓扑配置，改变议会成员
     *
     * @param delta     变化量，每次只能有一个node 发生变化，raft协议保障
     * @param operation OP_APPEND,增加集群节点,身份为议员，
     *                  OP_REMOVE,删除当前节点,不再参与选举
     *                  OP_MODIFY,修改peer端口,不能改变节点身份
     *                  注意修改议会成员组成，要求先加入成员,再删除老成员的两步过程。
     */
    void changeTopology(RaftNode delta, IStorage.Operation operation);

    /**
     * ra f
     * 调整多个分区之间通信的端点结构。
     *
     * @param delta     变化网关节点
     * @param operation OP_APPEND,增加网关 节点
     *                  OP_REMOVE,减少网关 节点
     *                  OP_MODIFY,变更gate-listen的端口
     */
    void changeGate(RaftNode delta, IStorage.Operation operation);

    RaftNode findById(long peerId);

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
