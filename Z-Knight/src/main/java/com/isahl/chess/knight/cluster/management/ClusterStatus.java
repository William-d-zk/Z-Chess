/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.knight.cluster.management;

import java.util.HashMap;
import java.util.Map;

/**
 * 集群状态信息
 * 
 * @author william.d.zk
 */
public class ClusterStatus
{
    private int clusterSize;
    private int quorumSize;
    private String leaderId;
    private long lastElectionTime;
    private long commitIndex;
    private Map<String, ClusterNode.NodeStatus> nodeStatusMap = new HashMap<>();
    private Map<String, Long> replicationLag = new HashMap<>();
    
    public int getClusterSize()
    {
        return clusterSize;
    }
    
    public void setClusterSize(int clusterSize)
    {
        this.clusterSize = clusterSize;
    }
    
    public int getQuorumSize()
    {
        return quorumSize;
    }
    
    public void setQuorumSize(int quorumSize)
    {
        this.quorumSize = quorumSize;
    }
    
    public String getLeaderId()
    {
        return leaderId;
    }
    
    public void setLeaderId(String leaderId)
    {
        this.leaderId = leaderId;
    }
    
    public long getLastElectionTime()
    {
        return lastElectionTime;
    }
    
    public void setLastElectionTime(long lastElectionTime)
    {
        this.lastElectionTime = lastElectionTime;
    }
    
    public long getCommitIndex()
    {
        return commitIndex;
    }
    
    public void setCommitIndex(long commitIndex)
    {
        this.commitIndex = commitIndex;
    }
    
    public Map<String, ClusterNode.NodeStatus> getNodeStatusMap()
    {
        return nodeStatusMap;
    }
    
    public void setNodeStatusMap(Map<String, ClusterNode.NodeStatus> nodeStatusMap)
    {
        this.nodeStatusMap = nodeStatusMap;
    }
    
    public Map<String, Long> getReplicationLag()
    {
        return replicationLag;
    }
    
    public void setReplicationLag(Map<String, Long> replicationLag)
    {
        this.replicationLag = replicationLag;
    }
}
