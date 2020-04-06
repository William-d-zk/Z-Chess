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

import static com.tgx.chess.bishop.ZUID.INVALID_PEER_ID;
import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.CANDIDATE;
import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.ELECTOR;
import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.FOLLOWER;
import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.LEADER;
import static com.tgx.chess.cluster.raft.model.RaftCode.ALREADY_VOTE;
import static com.tgx.chess.cluster.raft.model.RaftCode.ILLEGAL_STATE;
import static com.tgx.chess.cluster.raft.model.RaftCode.INCORRECT_TERM;
import static com.tgx.chess.cluster.raft.model.RaftCode.LOWER_TERM;
import static com.tgx.chess.cluster.raft.model.RaftCode.OBSOLETE;
import static com.tgx.chess.cluster.raft.model.RaftCode.SPLIT_CLUSTER;
import static com.tgx.chess.cluster.raft.model.RaftCode.SUCCESS;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CLUSTER_LOCAL;
import static com.tgx.chess.queen.event.inf.IOperator.Type.CONSENSUS_ELECT;
import static java.lang.Math.min;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.cluster.raft.IRaftDao;
import com.tgx.chess.cluster.raft.IRaftMachine;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.cluster.raft.model.log.LogEntry;
import com.tgx.chess.cluster.raft.model.log.RaftDao;
import com.tgx.chess.json.JsonUtil;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ICancelable;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftNode<T extends ISessionManager<ZContext> & IActivity<ZContext> & IClusterPeer>
        implements
        IRaftNode
{
    private final Logger                       _Logger        = Logger.getLogger(getClass().getSimpleName());
    private final ZUID                         _ZUID;
    private final IClusterConfig               _ClusterConfig;
    private final T                            _SessionManager;
    private final RaftDao                      _RaftDao;
    private final TimeWheel                    _TimeWheel;
    private final ScheduleHandler<RaftNode<T>> _ElectSchedule, _HeartbeatSchedule, _TickSchedule;
    private final RaftGraph                    _RaftGraph;
    private final RaftMachine                  _SelfMachine;
    private final Queue<LogEntry>              _AppendLogList = new LinkedList<>();
    private final Random                       _Random        = new Random();
    private ICancelable                        mElectTask, mHeartbeatTask, mTickTask;

    public RaftNode(TimeWheel timeWheel,
                    IClusterConfig clusterConfig,
                    RaftDao raftDao,
                    T manager)
    {
        _TimeWheel = timeWheel;
        _ClusterConfig = clusterConfig;
        _SessionManager = manager;
        _ZUID = clusterConfig.createZUID(true);
        _RaftDao = raftDao;
        _ElectSchedule = new ScheduleHandler<>(_ClusterConfig.getElectInSecond(), RaftNode::startVote, this);
        _HeartbeatSchedule = new ScheduleHandler<>(_ClusterConfig.getHeartbeatInSecond(),
                                                   true,
                                                   RaftNode::leaderBroadcast,
                                                   this);
        _TickSchedule = new ScheduleHandler<>(_ClusterConfig.getHeartbeatInSecond()
                                                            .multipliedBy(2),
                                              RaftNode::startVote,
                                              this);
        _RaftGraph = new RaftGraph();
        _SelfMachine = new RaftMachine(_ZUID.getPeerId());
        _RaftGraph.append(_SelfMachine);
    }

    private void leaderBroadcast()
    {
        _RaftGraph.getNodeMap()
                  .forEach((k, v) ->
                  {
                      X7E_RaftBroadcast x7e = new X7E_RaftBroadcast(_ZUID.getId());
                      x7e.setPeerId(k);
                      x7e.setTerm(_SelfMachine.getTerm());
                      x7e.setCommit(_SelfMachine.getCommit());
                      x7e.setLeaderId(_SelfMachine.getPeerId());
                      x7e.setPreIndex(_SelfMachine.getIndex());
                      x7e.setPreIndexTerm(_SelfMachine.getIndexTerm());
                      if (v.getIndex() < _SelfMachine.getIndex()) {
                          List<LogEntry> entryList = new LinkedList<>();
                          for (long i = v.getIndex() + 1, l = _SelfMachine.getIndex(); i <= l; i++) {
                              entryList.add(_RaftDao.getEntry(i));
                          }
                          x7e.setPayload(JsonUtil.writeValueAsBytes(entryList));
                      }
                      ISession<ZContext> session = _SessionManager.findSessionByPrefix(k);
                      _SessionManager.send(session, CLUSTER_LOCAL, x7e);
                  });
    }

    public void init() throws IOException
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
        _SelfMachine.setIndex(_RaftDao.getLogMeta()
                                      .getIndex());
        _SelfMachine.setIndexTerm(_RaftDao.getLogMeta()
                                          .getTerm());
        _SelfMachine.setPeerSet(_RaftDao.getLogMeta()
                                        .getPeerSet());
        _SelfMachine.setGateSet(_RaftDao.getLogMeta()
                                        .getGateSet());
        if (_SelfMachine.getPeerSet() == null && _SelfMachine.getGateSet() == null) {
            /*首次启动或删除本地状态机重启,仅需要连接node_id < self.node_id的peer*/
            List<IPair> peers = _ClusterConfig.getPeers();
            if (peers != null) {
                for (int i = 0, size = peers.size(); i < size; i++) {
                    IPair pair = peers.get(i);
                    _SelfMachine.appendPeer(new Triple<>(_ZUID.getPeerId(i), pair.getFirst(), pair.getSecond()));
                }
                _RaftDao.getLogMeta()
                        .setPeerSet(_SelfMachine.getPeerSet());
            }
            List<IPair> gates = _ClusterConfig.getGates();
            if (gates != null) {
                for (int i = 0, size = gates.size(); i < size; i++) {
                    IPair pair = gates.get(i);
                    _SelfMachine.appendGate(new Triple<>(_ZUID.getClusterId(i), pair.getFirst(), pair.getSecond()));
                }
                _RaftDao.getLogMeta()
                        .setGateSet(_SelfMachine.getGateSet());
            }
        }
        //启动snapshot定时回写计时器
        _TimeWheel.acquire(_RaftDao,
                           new ScheduleHandler<>(_ClusterConfig.getSnapshotInSecond(),
                                                 true,
                                                 this::takeSnapshot,
                                                 _RaftDao));
        _RaftDao.updateAll();
        //启动集群连接
        if (_SelfMachine.getPeerSet() != null) {
            for (ITriple remote : _SelfMachine.getPeerSet()) {
                long peerId = remote.getFirst();
                //Graph中转装入所有需要管理的集群状态机，self已经装入了，此处不再重复
                if (peerId != _SelfMachine.getPeerId()) {
                    _RaftGraph.append(new RaftMachine(peerId));
                }
                //仅连接NodeId<自身的节点
                if (peerId < _SelfMachine.getPeerId()) {
                    _SessionManager.addPeer(new Pair<>(remote.getSecond(), remote.getThird()));
                }
            }
        }
        if (_SelfMachine.getGateSet() != null) {
            for (ITriple remote : _SelfMachine.getGateSet()) {
                long gateId = remote.getFirst();
                if (gateId != _ZUID.getClusterId()) {
                    _SessionManager.addGate(new Pair<>(remote.getSecond(), remote.getThird()));
                }
            }
        }
        //初始化为FOLLOW 状态，等待LEADER的HEARTBEAT 
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
    }

    @Override
    public void load(List<IRaftMessage> snapshot)
    {

    }

    @Override
    public void takeSnapshot(IRaftDao snapshot)
    {
        long localTerm;
        if (_RaftDao.getTotalSize() < _ClusterConfig.getSnapshotMinSize()) {
            _Logger.info("snapshot size less than threshold");
            return;
        }
        //状态迁移未完成
        if (_SelfMachine.getApplied() <= _SelfMachine.getCommit()) {
            _Logger.info(" applied < commit sync is running");
            return;
        }
        if (_SelfMachine.getApplied() >= _RaftDao.getStartIndex()
            && _SelfMachine.getApplied() <= _RaftDao.getEndIndex())
        {
            localTerm = _RaftDao.getEntryTerm(_SelfMachine.getApplied());
            _RaftDao.updateSnapshotMeta(_SelfMachine.getApplied(), localTerm);
            _Logger.info("take snapshot");
        }
        long lastSnapshotIndex = _RaftDao.getSnapshotMeta()
                                         .getCommit();
        if (lastSnapshotIndex > 0 && _RaftDao.getStartIndex() <= lastSnapshotIndex) {
            _RaftDao.truncatePrefix(lastSnapshotIndex + 1);
            _Logger.info("snapshot truncate prefix %d", lastSnapshotIndex);
        }

    }

    @Override
    public IRaftMachine getMachine()
    {
        return _SelfMachine;
    }

    /**
     * 此方法会在各种超时处理器中被启用，所以执行线程为TimeWheel.pool中的任意子线程
     */
    private void startVote()
    {
        /*关闭TickTask,此时执行容器可能为ElectTask 或 TickTask自身
          由于Elect.timeout << Tick.timeout,此处不应出现Tick无法
          关闭的，或关闭异常。同时cancel 配置了lock 防止意外出现。
        */
        if (mTickTask != null) {
            mTickTask.cancel();
        }
        try {
            Thread.sleep(_Random.nextInt(150) + 50);
        }
        catch (InterruptedException e) {
            //ignore
        }
        _SelfMachine.setState(CANDIDATE);
        X72_RaftVote x72 = new X72_RaftVote(_ZUID.getId());
        x72.setTerm(_SelfMachine.increaseTerm());
        x72.setPeerId(_SelfMachine.getPeerId());
        x72.setLogIndex(_SelfMachine.getIndex());
        x72.setLogTerm(_SelfMachine.getIndexTerm());
        _RaftGraph.getNodeMap()
                  .forEach((k, v) ->
                  {
                      if (k != _SelfMachine.getPeerId()) {
                          ISession<ZContext> session = _SessionManager.findSessionByPrefix(k);
                          _SessionManager.send(session, CONSENSUS_ELECT, x72);
                      }
                  });
        _Logger.info("start vote self {%s}", _SelfMachine.toString());
    }

    private X7F_RaftResponse success(long peerId)
    {
        X7F_RaftResponse response = new X7F_RaftResponse(_ZUID.getId());
        response.setPeerId(peerId);
        response.setTerm(_SelfMachine.getTerm());
        response.setCode(SUCCESS.getCode());
        return response;
    }

    @Override
    public X7F_RaftResponse reject(long peerId, int code)
    {
        X7F_RaftResponse response = new X7F_RaftResponse(_ZUID.getId());
        response.setPeerId(peerId);
        response.setTerm(_SelfMachine.getTerm());
        response.setCode(code);
        return response;
    }

    @Override
    public X7F_RaftResponse stepUp(long peerId)
    {
        _SelfMachine.setState(ELECTOR);
        _SelfMachine.setCandidate(peerId);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        return success(peerId);
    }

    @Override
    public X7F_RaftResponse stepDown(long peerId)
    {
        _SelfMachine.setState(FOLLOWER);
        _SelfMachine.setLeader(INVALID_PEER_ID);
        _SelfMachine.setCandidate(INVALID_PEER_ID);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        return success(peerId);
    }

    @Override
    public X7F_RaftResponse follow(long peerId)
    {
        _SelfMachine.setState(FOLLOWER);
        _SelfMachine.setLeader(peerId);
        _SelfMachine.setCandidate(INVALID_PEER_ID);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
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
        tickCancel();
        X7F_RaftResponse response = catchUp() ? success(peerId)
                                              : reject(peerId, INCORRECT_TERM.getCode());
        response.setCatchUp(_SelfMachine.getIndex());
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        return response;
    }

    private void tickCancel()
    {
        if (mTickTask != null) mTickTask.cancel();
    }

    private void heartbeatCancel()
    {
        if (mHeartbeatTask != null) mHeartbeatTask.cancel();
    }

    @Override
    public X7F_RaftResponse rejectAndStepDown(long peerId, int code)
    {
        _SelfMachine.setState(FOLLOWER);
        _SelfMachine.setLeader(INVALID_PEER_ID);
        _SelfMachine.setCandidate(INVALID_PEER_ID);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        return reject(peerId, code);
    }

    @Override
    public X7F_RaftResponse merge(IRaftMachine update)
    {
        X7F_RaftResponse response = null;
        APPLY:
        {
            //RaftGraph中不存LEARNER 节点所以集群广播和RPC时不涉及LEARNER
            if (_SelfMachine.getTerm() > update.getTerm()) {
                response = reject(update.getPeerId(), LOWER_TERM.getCode());
                break APPLY;
            }
            if (update.getTerm() > _SelfMachine.getTerm()) {
                _SelfMachine.setTerm(update.getTerm());
                tickCancel();
                heartbeatCancel();
                //* ->follower -> elector
                if (_SelfMachine.getIndex() <= update.getIndex()
                    && _SelfMachine.getIndexTerm() <= update.getIndexTerm()
                    && _SelfMachine.getCommit() <= update.getCommit()
                    && update.getState() == CANDIDATE)
                {
                    response = stepUp(update.getPeerId());
                }
                else if ((_SelfMachine.getIndex() > update.getIndex()
                          || _SelfMachine.getIndexTerm() > update.getIndexTerm())
                         && update.getState() == CANDIDATE)
                {
                    response = rejectAndStepDown(update.getPeerId(), OBSOLETE.getCode());
                    break APPLY;
                }
                else if (update.getState() == LEADER) {
                    response = follow(update.getPeerId());
                }
                //else ignore { follower elector 都不会成为update.getState的结果，所以此处忽略}
            }
            else switch (_SelfMachine.getState())
            {
                case ELECTOR:
                    if (update.getState() == CANDIDATE && _SelfMachine.getCandidate() != update.getCandidate()) {
                        response = reject(update.getPeerId(), ALREADY_VOTE.getCode());
                        break APPLY;
                    }
                    if (update.getState() == LEADER) {
                        response = follow(update.getPeerId());
                    }
                    break;
                case FOLLOWER:
                    if (update.getState() == CANDIDATE) {
                        if (_SelfMachine.getIndex() <= update.getIndex()
                            && _SelfMachine.getIndexTerm() <= update.getIndexTerm())
                        {
                            response = stepUp(update.getPeerId());
                        }
                        else {
                            response = reject(update.getPeerId(), OBSOLETE.getCode());
                            break APPLY;
                        }
                    }
                    else if (update.getState() == LEADER) {
                        response = reTick(update.getPeerId());
                    }
                    else {
                        response = reject(update.getPeerId(), ILLEGAL_STATE.getCode());
                        break APPLY;
                    }
                    break;
                case LEADER:
                    if (update.getState() == LEADER) {
                        if (update.getPeerId() != _SelfMachine.getPeerId()) {
                            response = rejectAndStepDown(update.getPeerId(), SPLIT_CLUSTER.getCode());
                        }
                        else {
                            _Logger.warning("check raft graph & broadcast leader self->self");
                        }
                    }
                    else {
                        response = reject(update.getPeerId(), ILLEGAL_STATE.getCode());
                    }
                    break APPLY;
                case LEARNER:
                    break;
            }

            if (update.getState() == LEADER && _SelfMachine.getCommit() < update.getCommit()) {
                _SelfMachine.setCommit(min(update.getCommit(), update.getIndex()));
            }
        }
        if (response != null) {
            response.setCatchUp(_SelfMachine.getCommit());
        }
        return response;
    }

    @Override
    public void apply(long applied)
    {

    }

    private boolean catchUp()
    {
        if (_SelfMachine.getState() == LEADER) {
            _Logger.warning("leader needn't catch up any log.");
            return false;
        }
        IT_APPEND:
        {
            for (Iterator<LogEntry> it = _AppendLogList.iterator(); it.hasNext();) {
                LogEntry entry = it.next();
                if (_RaftDao.append(entry)) {
                    _SelfMachine.setIndex(entry.getIndex());
                    _SelfMachine.setIndexTerm(entry.getTerm());
                }
                else {
                    LogEntry old = _RaftDao.getEntry(entry.getIndex());
                    if (old == null || old.getTerm() != entry.getTerm()) {
                        if (old != null) {
                            _Logger.warning("log conflict OT:%d <-> NT:%d I:%d",
                                            old.getTerm(),
                                            entry.getTerm(),
                                            entry.getIndex());
                        }
                        else {
                            _Logger.warning("log %d miss", entry.getIndex());
                        }
                        /*
                           此处存在一个可优化点，将此冲突对应的 term 下所有的index 都注销，向leader反馈
                           上一个term.end_index,能有效减少以-1进行删除的数据量。此种优化的意义不是很大。
                        */
                        long newEndIndex = entry.getIndex() - 1;
                        LogEntry logEntry = _RaftDao.truncateSuffix(newEndIndex);
                        if (logEntry != null) {
                            _SelfMachine.setIndex(newEndIndex);
                            _SelfMachine.setIndexTerm(logEntry.getTerm());
                        }
                        break IT_APPEND;
                    }
                    else {
                        /*
                            1.old ==  null entry.index 处于 未正常管控的位置
                            启动协商之后是不会出现此类情况的。
                            2.old.term==entry.term已经完成了日志存储，不再重复append 
                         */
                        _SelfMachine.setIndex(entry.getIndex());
                        _SelfMachine.setIndexTerm(entry.getTerm());
                    }
                }
                it.remove();
            }
            return true;
        }
        _AppendLogList.clear();
        return false;
    }

    @Override
    public boolean checkVoteState(X72_RaftVote x72)
    {
        return x72.getPeerId() == _SelfMachine.getPeerId()
               && x72.getTerm() == _SelfMachine.getTerm()
               && x72.getLogIndex() == _SelfMachine.getIndex()
               && _SelfMachine.getState() == ELECTOR;
    }

    @Override
    public boolean checkLogAppend(X7E_RaftBroadcast x7e)
    {
        return _SelfMachine.getState() == LEADER
               && _SelfMachine.getTerm() == x7e.getTerm()
               && _SelfMachine.getCommit() >= x7e.getCommit()
               && _SelfMachine.getPeerId() == x7e.getLeaderId();
    }

    @Override
    public void appendLogs(List<LogEntry> entryList)
    {
        //offer all
        _AppendLogList.addAll(entryList);
    }
}
