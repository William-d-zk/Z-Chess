/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE    
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER         
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE  
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.cluster.raft.model;

import java.util.List;
import java.util.function.Consumer;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.cluster.raft.IRaftDao;
import com.tgx.chess.cluster.raft.IRaftMachine;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.cluster.raft.model.log.LogEntry;
import com.tgx.chess.cluster.raft.model.log.RaftDao;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.queen.db.inf.IStorage;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftNode
        implements
        IRaftNode,
        IRaftMachine
{
    private final Logger                    _Logger = Logger.getLogger(getClass().getSimpleName());
    private final ZUID                      _ZUid;
    private final IClusterConfig            _ClusterConfig;
    private final RaftDao                   _RaftDao;
    private final TimeWheel                 _TimeWheel;
    private final ScheduleHandler<RaftNode> _ElectSchedule, _HeartBeatSchedule;
    private final RaftGraph                 _RaftGraph;

    /**
     * 状态
     */
    private RaftState state = RaftState.FOLLOWER;
    /**
     * 当前选举的轮次
     * 有效值 > 0
     */
    private long      term  = 0;
    /**
     * 候选人 有效值 非0，很可能是负值
     * 
     * @see ZUID
     */
    private long      candidate;
    /**
     * 已知的leader
     * 有效值 非0，很可能是负值
     * 
     * @see ZUID
     */
    private long      leader;
    /**
     * 已被提交的日志ID
     * 有效值 非0，很可能是负值
     * 48bit
     * 
     * @see ZUID
     */
    private long      commit;
    /**
     * 已被状态机采纳的纪录ID
     * 必须 <= commit
     * 48bit
     * 
     * @see ZUID
     */
    private long      apply;

    public RaftNode(TimeWheel timeWheel,
                    IClusterConfig clusterConfig,
                    RaftDao raftDao,
                    Consumer<RaftNode> consumer)
    {
        _TimeWheel = timeWheel;
        _ClusterConfig = clusterConfig;
        _ZUid = clusterConfig.createZUID(true);
        _RaftDao = raftDao;
        _ElectSchedule = new ScheduleHandler<>(_ClusterConfig.getElectInSecond()
                                                             .getSeconds(),
                                               consumer);
        _HeartBeatSchedule = new ScheduleHandler<RaftNode>(_ClusterConfig.getHeartBeatInSecond()
                                                                         .getSeconds(),
                                                           true)
        {
            @Override
            public void onCall()
            {
                heartbeat();
            }
        };
        _RaftGraph = new RaftGraph();
        _RaftGraph.append(_ZUid.getPeerId(), term, apply, candidate, leader);
    }

    private void heartbeat()
    {
        RaftMessage message = new RaftMessage();
        message.setPrimaryKey(_ZUid.getId());
        message.setPeerId(_ZUid.getPeerId());
        message.setTerm(term);
        message.setRaftState(RaftState.LEADER);
        message.setStrategy(IStorage.Strategy.RETAIN);
        message.setCommit(commit);
        
    }

    public void init()
    {
        _Logger.info("raft node init");
        /* _RaftDao 启动的时候已经装载了 snapshot */
        term = _RaftDao.getLogMeta()
                       .getTerm();
        candidate = _RaftDao.getLogMeta()
                            .getCandidate();
        commit = _RaftDao.getSnapshotMeta()
                         .getLastIncludedIndex();
        // 删除前序日志，只保留snapshot结果
        if (commit > 0
            && _RaftDao.getLogMeta()
                       .getFirstLogIndex() <= commit)
        {
            _RaftDao.truncatePrefix(term + 1);
        }
        apply = commit;
        //启动snapshot定时回写计时器
        _TimeWheel.acquire(this,
                           new ScheduleHandler<RaftNode>(_ClusterConfig.getSnapshotInSecond()
                                                                       .getSeconds(),
                                                         true)
                           {
                               @Override
                               public void onCall()
                               {
                                   takeSnapshot();
                               }
                           });
        _RaftDao.updateAll();
    }

    private void reset()
    {
        leader = 0;
        candidate = 0;
        term = 0;
        commit = 0;
        apply = 0;
    }

    @Override
    public RaftState getState()
    {
        return state;
    }

    @Override
    public long getTerm()
    {
        return term;
    }

    @Override
    public long getElector()
    {
        return candidate;
    }

    @Override
    public long getLeader()
    {
        return leader;
    }

    @Override
    public long getCommitIndex()
    {
        return commit;
    }

    @Override
    public long getLastAppliedIndex()
    {
        return apply;
    }

    @Override
    public void apply(IRaftMessage msg)
    {

    }

    @Override
    public void load(List<IRaftMessage> snapshot)
    {

    }

    @Override
    public boolean takeSnapshot(IRaftDao snapshot)
    {
        return false;
    }

    private void takeSnapshot()
    {
        long localApply;
        long localTerm;
        RaftGraph localGraph = new RaftGraph();
        if (_RaftDao.getTotalSize() < _ClusterConfig.getSnapshotMinSize()) { return; }
        if (apply <= commit) { return; }//状态迁移未完成
        localApply = apply;
        if (apply >= _RaftDao.getFirstLogIndex() && apply <= _RaftDao.getLastLogIndex()) {
            localTerm = _RaftDao.getEntryTerm(apply);
        }
        localGraph.merge(_RaftDao.getLogMeta()
                                 .getRaftGraph());
        if (takeSnapshot(_RaftDao)) {
            long lastSnapshotIndex = _RaftDao.getSnapshotMeta()
                                             .getLastIncludedIndex();
            if (lastSnapshotIndex > 0 && _RaftDao.getFirstLogIndex() <= lastSnapshotIndex) {
                _RaftDao.truncatePrefix(lastSnapshotIndex + 1);
            }
        }
    }

    void applyRaftGraph(LogEntry logEntry)
    {

        RaftGraph raftGraph = logEntry.getRaftGraph();
        _RaftGraph.apply(_ZUid.getPeerId(), logEntry.getIndex(), raftGraph);
    }

    void stepDown(long newTerm)
    {
        if (term > newTerm) { throw new IllegalStateException("state machine error"); }
        if (term < newTerm) {
            term = newTerm;
            leader = 0;
            candidate = 0;
            _RaftDao.updateLogMeta(term, commit, candidate);
        }
        state = RaftState.FOLLOWER;
        _HeartBeatSchedule.cancel();
        _TimeWheel.acquire(this, _ElectSchedule);
    }

    void appendLog(RaftMessage leaderMessage)
    {

    }

}
