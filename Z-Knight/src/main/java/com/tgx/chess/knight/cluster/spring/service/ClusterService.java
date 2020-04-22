package com.tgx.chess.knight.cluster.spring.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.knight.cluster.ClusterNode;
import com.tgx.chess.knight.raft.IRaftDao;
import com.tgx.chess.knight.raft.config.IClusterConfig;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.service.ClusterCustom;

@Service
public class ClusterService
{

    private final ClusterNode                _ClusterNode;
    private final IClusterConfig             _ClusterConfig;
    private final ConsistentCustom           _ConsistentCustom;
    private final ClusterCustom<ClusterNode> _ClusterCustom;
    private final TimeWheel                  _TimeWheel;
    private final RaftNode<ClusterNode>      _RaftNode;

    @Autowired
    public ClusterService(ClusterNode clusterNode,
                          IClusterConfig clusterConfig,
                          ConsistentCustom consistentCustom,
                          ClusterCustom<ClusterNode> clusterCustom,
                          IRaftDao raftDao)
    {
        _TimeWheel = new TimeWheel();
        _ClusterNode = clusterNode;
        _ClusterConfig = clusterConfig;
        _ConsistentCustom = consistentCustom;
        _ClusterCustom = clusterCustom;
        _RaftNode = new RaftNode<>(_TimeWheel, _ClusterConfig, raftDao, _ClusterNode);
        _ClusterCustom.setRaftNode(_RaftNode);
    }
}
