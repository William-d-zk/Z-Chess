/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.knight.raft.config;

import java.time.Duration;
import java.util.List;

import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.topology.ZUID;

/**
 * @author william.d.zk
 */
public interface IRaftConfig
{
    IPair getPeerTest();

    IPair getGateTest();

    /**
     * 当前集群的节点列表
     * 
     * @return
     */

    List<IPair> getPeers();

    /**
     * 与其他独region 进行通讯的网关
     * 
     * @return
     */
    List<IPair> getGates();

    /**
     * 集群服务绑定的服务地址 host:port
     * 
     * @return
     */
    IPair getBind();

    /**
     * 集群标识UID 集群最大容量为 2^14 (16384) 个节点
     * 
     * @see ZUID
     * @return
     */
    Uid getUid();

    boolean isInCongress();

    ZUID createZUID();

    Duration getElectInSecond();

    Duration getSnapshotInSecond();

    Duration getHeartbeatInSecond();

    Duration getClientSubmitInSecond();

    long getSnapshotMinSize();

    long getSnapshotFragmentMaxSize();

    boolean isClusterMode();

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
