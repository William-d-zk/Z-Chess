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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.cluster.raft.IRaftDao;
import com.tgx.chess.cluster.raft.IRaftMachine;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.cluster.raft.model.log.LogEntry;
import com.tgx.chess.cluster.raft.model.log.RaftDao;
import com.tgx.chess.json.JsonUtil;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ICancelable;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftNode
        implements
        IRaftNode
{
    private final Logger                    _Logger        = Logger.getLogger(getClass().getSimpleName());
    private final ZUID                      _ZUid;
    private final IClusterConfig            _ClusterConfig;
    private final QueenManager<ZContext>    _QueenManager;
    private final RaftDao                   _RaftDao;
    private final TimeWheel                 _TimeWheel;
    private final ScheduleHandler<RaftNode> _ElectSchedule, _HeartbeatSchedule, _TickSheSchedule;
    private final RaftGraph                 _RaftGraph;
    private final RaftMachine               _SelfMachine;
    private final List<LogEntry>            _AppendLogList = new LinkedList<>();
    private final Random                    _Random        = new Random();
    private ICancelable                     mElectTask, mHeartbeatTask, mTickTask;

    public RaftNode(TimeWheel timeWheel,
                    IClusterConfig clusterConfig,
                    RaftDao raftDao,
                    QueenManager<ZContext> manager)
    {
        _TimeWheel = timeWheel;
        _ClusterConfig = clusterConfig;
        _QueenManager = manager;
        _ZUid = clusterConfig.createZUID(true);
        _RaftDao = raftDao;
        _ElectSchedule = new ScheduleHandler<>(_ClusterConfig.getElectInSecond()
                                                             .getSeconds(),
                                               RaftNode::startVote);
        _HeartbeatSchedule = new ScheduleHandler<RaftNode>(_ClusterConfig.getHeartbeatInSecond()
                                                                         .getSeconds(),
                                                           true)
        {
            @Override
            public void onCall()
            {
                leaderBroadcast();
            }
        };
        _TickSheSchedule = new ScheduleHandler<>(_ClusterConfig.getHeartbeatInSecond()
                                                               .getSeconds()
                                                 * 2,
                                                 RaftNode::startVote);
        _RaftGraph = new RaftGraph();
        _SelfMachine = new RaftMachine(_ZUid.getPeerId());
        _RaftGraph.append(_SelfMachine);
    }

    private void leaderBroadcast()
    {
        _RaftGraph.getNodeMap()
                  .forEach((k, v) ->
                  {

                      X7E_RaftBroadcast x7e = new X7E_RaftBroadcast(_ZUid.getId());
                      x7e.setPeerId(k);
                      x7e.setTerm(v.getTerm());
                      x7e.setCommit(_SelfMachine.getCommit());
                      x7e.setLeaderId(_ZUid.getPeerId());
                      if (v.getIndex() < _SelfMachine.getIndex()) {
                          List<LogEntry> entryList = new LinkedList<>();
                          for (long i = v.getIndex() + 1, l = _SelfMachine.getIndex(); i <= l; i++) {
                              entryList.add(_RaftDao.getEntry(i));
                          }
                          x7e.setPayload(JsonUtil.writeValueAsBytes(entryList));
                      }
                      ISession<ZContext> session = _QueenManager.findByPrefix(k);
                      _QueenManager.localSend(session, x7e);
                  });
    }

    public void init()
    {
        _Logger.info("raft node init");
        /* _RaftDao 启动的时候已经装载了 snapshot */
        _SelfMachine.setTerm(_RaftDao.getLogMeta()
                                     .getTerm());
        _SelfMachine.setCandidate(_RaftDao.getLogMeta()
                                          .getCandidate());
        _SelfMachine.setCommit(_RaftDao.getLogMeta()
                                       .getCommit());
        _SelfMachine.setApplied(_RaftDao.getLogMeta()
                                        .getApplied());
        // 删除前序日志，只保留snapshot结果
        if (_SelfMachine.getCommit() > 0
            && _RaftDao.getLogMeta()
                       .getStart() <= _SelfMachine.getCommit())
        {
            _RaftDao.truncatePrefix(_SelfMachine.getTerm() + 1);
        }
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

    @Override
    public void load(List<IRaftMessage> snapshot)
    {

    }

    @Override
    public boolean takeSnapshot(IRaftDao snapshot)
    {
        return false;
    }

    @Override
    public IRaftMachine getMachine() {
        return null;
    }

    private void takeSnapshot()
    {
        long localApply;
        long localTerm;
        RaftGraph localGraph = new RaftGraph();
        if (_RaftDao.getTotalSize() < _ClusterConfig.getSnapshotMinSize()) { return; }
        if (_SelfMachine.getApplied() <= _SelfMachine.getCommit()) { return; }//状态迁移未完成
        if (_SelfMachine.getApplied() >= _RaftDao.getStartIndex()
            && _SelfMachine.getApplied() <= _RaftDao.getEndIndex())
        {
            localTerm = _RaftDao.getEntryTerm(_SelfMachine.getApplied());
        }
        localGraph.merge(_RaftDao.getLogMeta()
                                 .getRaftGraph());
        if (takeSnapshot(_RaftDao)) {
            long lastSnapshotIndex = _RaftDao.getSnapshotMeta()
                                             .getCommit();
            if (lastSnapshotIndex > 0 && _RaftDao.getStartIndex() <= lastSnapshotIndex) {
                _RaftDao.truncatePrefix(lastSnapshotIndex + 1);
            }
        }
    }

    /**
     * 此方法会在各种超时处理器中被启用，所以执行线程为TimeWheel.pool中的任意子线程
     */
    private void startVote()
    {
        try {
            Thread.currentThread()
                  .wait(_Random.nextInt(150) + 50);
        }
        catch (InterruptedException e) {
            //ignore
        }
        RaftMachine update = new RaftMachine(_ZUid.getPeerId());
        update.setTerm(_SelfMachine.getTerm() + 1);
        update.setCandidate(_ZUid.getPeerId());
        update.setIndex(_SelfMachine.getIndex());
        if (mTickTask != null) {
            mTickTask.cancel();
        }
    }

    private X7F_RaftResponse success(long peerId)
    {
        X7F_RaftResponse x7f = new X7F_RaftResponse(_ZUid.getId());
        x7f.setTerm(_SelfMachine.getTerm());
        x7f.setCode(X7F_RaftResponse.Code.SUCCESS.getCode());
        x7f.setSession(_QueenManager.findByPrefix(peerId));
        return x7f;
    }

    @Override
    public X7F_RaftResponse reject(long peerId, int code)
    {
        X7F_RaftResponse x7f = new X7F_RaftResponse(_ZUid.getId());
        x7f.setTerm(_SelfMachine.getTerm());
        x7f.setCode(code);
        x7f.setSession(_QueenManager.findByPrefix(peerId));
        return x7f;
    }

    @Override
    public X7F_RaftResponse stepUp(long peerId)
    {
        if (mTickTask != null) {
            mTickTask.cancel();
        }
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        return success(peerId);
    }

    @Override
    public X7F_RaftResponse stepDown(long peerId)
    {
        if (mHeartbeatTask != null) {
            mHeartbeatTask.cancel();
        }
        mTickTask = _TimeWheel.acquire(this, _TickSheSchedule);
        _SelfMachine.setLeader(ZUID.INVALID_PEER_ID);
        _SelfMachine.setCandidate(ZUID.INVALID_PEER_ID);
        return success(peerId);
    }

    @Override
    public X7F_RaftResponse follow(long peerId)
    {
        return success(peerId);
    }

    /**
     * 接收到LEADER的心跳或者日志复写
     * 接收到的日志已经按顺序存入 _AppendLogList
     * 
     * @param peerId
     * @return
     */
    @Override
    public X7F_RaftResponse reTick(long peerId)
    {
        if (mTickTask != null) {
            mTickTask.cancel();
            mTickTask = _TimeWheel.acquire(this, _TickSheSchedule);
        }
        catchUp();
        X7F_RaftResponse x7f = success(peerId);
        x7f.setCatchUp(_SelfMachine.getIndex());
        return x7f;
    }

    @Override
    public X7F_RaftResponse rejectAndStepDown(long peerId, int code) {
        return null;
    }

    @Override
    public void apply(long applied) {

    }

    private void catchUp()
    {
        if (_SelfMachine.getState() == RaftState.LEADER) {
            _Logger.warning("leader needn't catch up any log.");
            return;
        }
        IT_APPEND:
        {
            for (Iterator<LogEntry> it = _AppendLogList.iterator(); it.hasNext();) {
                LogEntry entry = it.next();
                if (_RaftDao.getEndIndex() < entry.getIndex()) {
                    long newEndIndex = _RaftDao.append(entry);
                    if (newEndIndex != entry.getIndex()) {
                        _Logger.warning("conflict{ self expect index %d,entry index %d }",
                                        newEndIndex,
                                        entry.getIndex());
                        break IT_APPEND;
                    }
                    else {
                        _SelfMachine.setIndex(newEndIndex);
                    }
                }
                else {
                    LogEntry old = _RaftDao.getEntry(entry.getIndex());
                    if (old != null && old.getTerm() != entry.getTerm()) {
                        _Logger.warning("log conflict OT:%d <-> NT%d I:%d",
                                        old.getTerm(),
                                        entry.getTerm(),
                                        entry.getIndex());
                        /*
                           此处存在一个可优化点，将此冲突对应的 term 下所有的index 都注销，向leader反馈
                           上一个term.end_index,能有效减少以-1进行删除的数据量。此种优化的意义不是很大。
                        */
                        _RaftDao.truncateSuffix(entry.getIndex() - 1);
                        _SelfMachine.setIndex(_RaftDao.getEndIndex());
                    }
                    else {
                        /*
                            1.old ==  null entry.index 处于 未正常管控的位置
                            启动协商之后是不会出现此类情况的。
                            2.old.term==entry.term已经完成了日志存储，不再重复append 
                         */
                        _SelfMachine.setIndex(entry.getIndex());
                    }
                }
                it.remove();
            }
            return;
        }
        _AppendLogList.clear();
    }

    @Override
    public void applyAndResponse(X7F_RaftResponse response)
    {
        if (response == null) return;//ignore

        response.setCatchUp(_SelfMachine.getCommit());
    }

}
