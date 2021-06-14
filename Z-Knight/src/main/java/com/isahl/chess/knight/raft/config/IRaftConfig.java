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

import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.topology.ZUID;

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

    List<IPair> getPeers();

    /**
     * 与其他独region 进行通讯的网关
     * 
     * @return gate topology
     */
    List<IPair> getGates();

    /**
     * 集群服务绑定的服务地址 host:port
     *
     * @return local peer bind
     */
    IPair getPeerBind();

    /**
     * 集群服务绑定的分区节点地址 host:port
     * 
     * @return global gate bind
     */
    IPair getGateBind();

    /**
     * 集群标识UID 集群最大容量为 2^14 (16384) 个节点
     * 
     * @see ZUID
     * 
     * @return cluster uid setting
     */
    Uid getUid();

    boolean isInCongress();

    boolean isGateNode();

    boolean isBeGate();

    ZUID createZUID();

    Duration getElectInSecond();

    Duration getSnapshotInSecond();

    Duration getHeartbeatInSecond();

    Duration getClientSubmitInSecond();

    long getSnapshotMinSize();

    long getSnapshotFragmentMaxSize();

    boolean isClusterMode();

    void update(IRaftConfig source);

    class Uid
    {
        private int nodeId;
        private int idcId;
        private int clusterId;
        private int type;

        public int getNodeId()
        {
            return nodeId;
        }

        public void setNodeId(int nodeId)
        {
            this.nodeId = nodeId;
        }

        public int getIdcId()
        {
            return idcId;
        }

        public void setIdcId(int idcId)
        {
            this.idcId = idcId;
        }

        public int getClusterId()
        {
            return clusterId;
        }

        public void setClusterId(int clusterId)
        {
            this.clusterId = clusterId;
        }

        public int getType()
        {
            return type;
        }

        public void setType(int type)
        {
            this.type = type;
        }
    }
}
