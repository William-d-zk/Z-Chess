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
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.knight.raft.model;

import static com.tgx.chess.knight.raft.IRaftMachine.INDEX_NAN;
import static com.tgx.chess.knight.raft.IRaftMachine.MIN_START;
import static com.tgx.chess.knight.raft.RaftState.CANDIDATE;
import static com.tgx.chess.knight.raft.RaftState.FOLLOWER;
import static com.tgx.chess.knight.raft.RaftState.LEADER;
import static com.tgx.chess.knight.raft.model.RaftCode.ALREADY_VOTE;
import static com.tgx.chess.knight.raft.model.RaftCode.CONFLICT;
import static com.tgx.chess.knight.raft.model.RaftCode.ILLEGAL_STATE;
import static com.tgx.chess.knight.raft.model.RaftCode.LOWER_TERM;
import static com.tgx.chess.knight.raft.model.RaftCode.OBSOLETE;
import static com.tgx.chess.knight.raft.model.RaftCode.SUCCESS;
import static java.lang.Math.min;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.inf.IValid;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.schedule.ICancelable;
import com.tgx.chess.king.base.schedule.ScheduleHandler;
import com.tgx.chess.king.base.schedule.TimeWheel;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.knight.raft.IRaftDao;
import com.tgx.chess.knight.raft.IRaftMachine;
import com.tgx.chess.knight.raft.IRaftMessage;
import com.tgx.chess.knight.raft.RaftState;
import com.tgx.chess.knight.raft.config.IRaftConfig;
import com.tgx.chess.knight.raft.model.log.LogEntry;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IClusterTimer;
import com.tgx.chess.queen.io.core.inf.IConsistentProtocol;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftNode<T extends IActivity<ZContext> & IClusterPeer & IClusterTimer>
        implements
        IValid
{
    private final Logger                       _Logger         = Logger.getLogger("cluster.knight."
                                                                                  + getClass().getSimpleName());
    private final ZUID                         _ZUID;
    private final IRaftConfig                  _ClusterConfig;
    private final T                            _ClusterPeer;
    private final IRaftDao                     _RaftDao;
    private final TimeWheel                    _TimeWheel;
    private final ScheduleHandler<RaftNode<T>> _ElectSchedule, _HeartbeatSchedule, _TickSchedule;
    private final RaftGraph                    _RaftGraph;
    private final RaftMachine                  _SelfMachine;
    private final Queue<LogEntry>              _AppendLogQueue = new LinkedList<>();
    private final Random                       _Random         = new Random();
    private ICancelable                        mElectTask, mHeartbeatTask, mTickTask;

    public RaftNode(TimeWheel timeWheel,
                    IRaftConfig clusterConfig,
                    IRaftDao raftDao,
                    T manager)
    {
        _TimeWheel = timeWheel;
        _ClusterConfig = clusterConfig;
        _ClusterPeer = manager;
        _ZUID = clusterConfig.createZUID();
        _RaftDao = raftDao;
        _ElectSchedule = new ScheduleHandler<>(_ClusterConfig.getElectInSecond(), RaftNode::stepDown, this);
        _HeartbeatSchedule = new ScheduleHandler<>(_ClusterConfig.getHeartbeatInSecond(), RaftNode::heartbeat, this);
        _TickSchedule = new ScheduleHandler<>(_ClusterConfig.getHeartbeatInSecond()
                                                            .multipliedBy(2),
                                              RaftNode::startVote,
                                              this);
        _RaftGraph = new RaftGraph();
        _SelfMachine = new RaftMachine(_ZUID.getPeerId());
        _RaftGraph.append(_SelfMachine);
    }

    private void heartbeat()
    {
        _ClusterPeer.timerEvent(_SelfMachine.createLeader());
        _Logger.debug("leader heartbeat");
    }

    public void init() throws IOException
    {
        if (!_ClusterConfig.isClusterModel()) {
            _Logger.info("single model skip init raft node");
            return;
        }
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
        _SelfMachine.setPeerSet(_RaftDao.getLogMeta()
                                        .getPeerSet());
        _SelfMachine.setGateSet(_RaftDao.getLogMeta()
                                        .getGateSet());
        _SelfMachine.setMatchIndex(_SelfMachine.getIndex());
        if (_SelfMachine.getIndex() >= _RaftDao.getStartIndex()) {
            _SelfMachine.setIndexTerm(_RaftDao.getEntryTerm(_SelfMachine.getIndex()));
        }
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
                    _ClusterPeer.addPeer(new Pair<>(remote.getSecond(), remote.getThird()));
                }
            }
        }
        if (_SelfMachine.getGateSet() != null) {
            for (ITriple remote : _SelfMachine.getGateSet()) {
                long gateId = remote.getFirst();
                if (gateId != _ZUID.getClusterId()) {
                    _ClusterPeer.addGate(new Pair<>(remote.getSecond(), remote.getThird()));
                }
            }
        }
        //初始化为FOLLOWER 状态，等待LEADER的HEARTBEAT
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        _Logger.debug("raft node init -> %s", _SelfMachine);
    }

    public void load(List<IRaftMessage> snapshot)
    {

    }

    public void takeSnapshot(IRaftDao snapshot)
    {
        long localTerm;
        if (_RaftDao.getTotalSize() < _ClusterConfig.getSnapshotMinSize()) {
            _Logger.debug("snapshot size less than threshold");
            return;
        }
        //状态迁移未完成
        if (_SelfMachine.getApplied() <= _SelfMachine.getCommit()) {
            _Logger.debug(" applied < commit sync is running");
            return;
        }
        if (_SelfMachine.getApplied() >= _RaftDao.getStartIndex()
            && _SelfMachine.getApplied() <= _RaftDao.getEndIndex())
        {
            localTerm = _RaftDao.getEntryTerm(_SelfMachine.getApplied());
            _RaftDao.updateSnapshotMeta(_SelfMachine.getApplied(), localTerm);
            _Logger.debug("take snapshot");
        }
        long lastSnapshotIndex = _RaftDao.getSnapshotMeta()
                                         .getCommit();
        if (lastSnapshotIndex > 0 && _RaftDao.getStartIndex() <= lastSnapshotIndex) {
            _RaftDao.truncatePrefix(lastSnapshotIndex + 1);
            _Logger.debug("snapshot truncate prefix %d", lastSnapshotIndex);
        }

    }

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
        tickCancel();
        try {
            long wait = _Random.nextInt(150) + 100;
            Thread.sleep(wait);
            _Logger.debug("random wait for %d mills", wait);
        }
        catch (InterruptedException e) {
            //ignore
        }
        _ClusterPeer.timerEvent(_SelfMachine.createCandidate());
    }

    private X7F_RaftResponse success()
    {
        return response(SUCCESS.getCode());
    }

    private X7F_RaftResponse response(int code)
    {
        X7F_RaftResponse response = new X7F_RaftResponse(_ZUID.getId());
        response.setCode(code);
        response.setPeerId(_SelfMachine.getPeerId());
        response.setTerm(_SelfMachine.getTerm());
        response.setCatchUp(_SelfMachine.getIndex());
        response.setCatchUpTerm(_SelfMachine.getIndexTerm());
        response.setCandidate(_SelfMachine.getCandidate());
        return response;
    }

    private X7F_RaftResponse reject(RaftCode raftCode)
    {
        return response(raftCode.getCode());
    }

    private X7F_RaftResponse rejectAndStepDown(long term)
    {
        _SelfMachine.follow(term, _RaftDao);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        return reject(RaftCode.SPLIT_CLUSTER);
    }

    @SuppressWarnings("unchecked")
    private IControl<ZContext>[] rejectAndVote(long term)
    {
        _SelfMachine.follow(term, _RaftDao);
        vote4me();
        Stream<IControl<ZContext>> stream = Stream.of(reject(RaftCode.OBSOLETE));
        return Stream.concat(stream, createVotes(_SelfMachine))
                     .toArray(IControl[]::new);
    }

    private X7F_RaftResponse stepUp(long peerId, long term)
    {
        _SelfMachine.beElector(peerId, term, _RaftDao);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("[follower → elector] %s", _SelfMachine);
        return success();
    }

    private void stepDown()
    {
        _SelfMachine.follow(_RaftDao);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        _Logger.debug("[* → follower] %s", _SelfMachine);
    }

    private X7F_RaftResponse follow(long peerId, long term, long commit, long preIndex, long preIndexTerm)
    {
        _Logger.debug("follower %#x @%d => %d", peerId, term, commit);
        _SelfMachine.follow(peerId, term, commit, _RaftDao);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        if (catchUp(preIndex, preIndexTerm)) {
            _Logger.debug("follower appended from %#x@%d-^%d=> %s", peerId, term, commit, _SelfMachine);
            return success();
        }
        else {
            _Logger.debug("roll back =>%s ", _SelfMachine);
            return reject(CONFLICT);
        }
    }

    private void vote4me()
    {
        _SelfMachine.beCandidate(_RaftDao);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("follower => candidate %s", _SelfMachine);
    }

    private void beLeader()
    {
        _SelfMachine.beLeader(_RaftDao);
        _Logger.debug("be leader=>%s", _SelfMachine);
    }

    private void tickCancel()
    {
        if (mTickTask != null) {
            _Logger.debug("cancel tick");
            mTickTask.cancel();
        }
    }

    private void heartbeatCancel()
    {
        if (mHeartbeatTask != null) {
            _Logger.debug("cancel heartbeat");
            mHeartbeatTask.cancel();
        }
    }

    private void electCancel()
    {
        if (mElectTask != null) {
            _Logger.debug("cancel elect");
            mElectTask.cancel();
        }
    }

    public IControl<ZContext>[] merge(IRaftMachine update)
    {
        //RaftGraph中不存LEARNER节点,所以集群广播和RPC时不涉及LEARNER
        if (_SelfMachine.getTerm() > update.getTerm()) { return new X7F_RaftResponse[] { reject(LOWER_TERM) }; }
        if (update.getTerm() > _SelfMachine.getTerm()) {
            tickCancel();
            heartbeatCancel();
            electCancel();
            // * -> follower -> elector X72_RaftVote
            if (update.getState() == CANDIDATE) {
                if (_SelfMachine.getIndex() <= update.getIndex()
                    && _SelfMachine.getIndexTerm() <= update.getIndexTerm())
                {
                    X7F_RaftResponse response = stepUp(update.getPeerId(), update.getTerm());
                    _Logger.debug("new term %d [follower → elector] | candidate: %#x",
                                  _SelfMachine.getTerm(),
                                  _SelfMachine.getCandidate());
                    return new X7F_RaftResponse[] { response };
                }
                else {
                    _Logger.debug("less than me; reject and [%s → candidate] | mine:%d@%d > in:%d@%d",
                                  _SelfMachine.getState(),
                                  _SelfMachine.getIndex(),
                                  _SelfMachine.getIndexTerm(),
                                  update.getIndex(),
                                  update.getIndexTerm());
                    return rejectAndVote(update.getTerm());
                }
            }//X7E_RaftBroadcast
            else if (update.getState() == LEADER) {
                _Logger.debug("follow leader %#x leader_commit: %d", update.getPeerId(), update.getCommit());
                return new X7F_RaftResponse[] { follow(update.getPeerId(),
                                                       update.getTerm(),
                                                       update.getCommit(),
                                                       update.getIndex(),
                                                       update.getIndexTerm()) };
            }
            //else ignore { follower elector 都不会成为update.getState的结果，所以此处忽略}
            else {
                _Logger.fetal("merge illegal state %s", update.getState());
                return new X7F_RaftResponse[] { reject(ILLEGAL_STATE) };
            }
        }
        //_SelfMachine.getTerm == update.getTerm
        else switch (_SelfMachine.getState())
        {
            case FOLLOWER:
                if (update.getState() == CANDIDATE) {
                    if (_SelfMachine.getIndex() <= update.getIndex()
                        && _SelfMachine.getIndexTerm() <= update.getIndexTerm())
                    {
                        tickCancel();
                        return new X7F_RaftResponse[] { stepUp(update.getPeerId(), update.getTerm()) };
                    }
                    else {
                        _Logger.debug("less than me,follower reject | mine:%d@%d > in:%d@%d",
                                      _SelfMachine.getIndex(),
                                      _SelfMachine.getIndexTerm(),
                                      update.getIndex(),
                                      update.getIndexTerm());
                        return new X7F_RaftResponse[] { reject(OBSOLETE) };
                    }
                }
                else if (update.getState() == LEADER) {
                    tickCancel();
                    return new X7F_RaftResponse[] { follow(update.getPeerId(),
                                                           update.getTerm(),
                                                           update.getCommit(),
                                                           update.getIndex(),
                                                           update.getIndexTerm()) };
                }
                else {
                    return new X7F_RaftResponse[] { reject(ILLEGAL_STATE) };
                }
            case ELECTOR:
                if (update.getState() == CANDIDATE) {
                    //不重置elect-timer
                    if (_SelfMachine.getCandidate() != update.getPeerId()) {
                        _Logger.debug("already vote [elector ×] | vote for:%#x not ♂ %#x",
                                      _SelfMachine.getCandidate(),
                                      update.getCandidate());
                        return new X7F_RaftResponse[] { reject(ALREADY_VOTE) };
                    }
                    else {
                        _Logger.warning("duplicate already vote for %#x@%d", update.getPeerId(), update.getTerm());
                        //response => null ignore
                    }
                }
                if (update.getState() == LEADER) {
                    electCancel();
                    return new X7F_RaftResponse[] { follow(update.getPeerId(),
                                                           update.getTerm(),
                                                           update.getCommit(),
                                                           update.getIndex(),
                                                           update.getIndexTerm()) };
                }
                break;
            case CANDIDATE:
                if (update.getState() == CANDIDATE) {
                    if (_SelfMachine.getIndex() < update.getIndex()
                        || _SelfMachine.getIndexTerm() < update.getIndexTerm())
                    {
                        _Logger.warning("other %#x better; candidate => elector");
                        electCancel();
                        return new X7F_RaftResponse[] { stepUp(update.getPeerId(), update.getTerm()) };
                    }
                    else if (_SelfMachine.getIndex() > update.getIndex()
                             || _SelfMachine.getIndexTerm() > update.getIndexTerm())
                    {
                        return new X7F_RaftResponse[] { reject(OBSOLETE) };
                    }
                    else return new X7F_RaftResponse[] { reject(ALREADY_VOTE) };
                }
                else if (update.getState() == LEADER) {
                    electCancel();
                    return new X7F_RaftResponse[] { follow(update.getPeerId(),
                                                           update.getTerm(),
                                                           update.getCommit(),
                                                           update.getIndex(),
                                                           update.getIndexTerm()) };
                }
                break;
            case LEADER:
                if (update.getState() == LEADER) {
                    if (update.getPeerId() != _SelfMachine.getPeerId()) {
                        _Logger.fetal("add new peer more than old-graph-size / 2");
                        heartbeatCancel();
                        return new X7F_RaftResponse[] { rejectAndStepDown(update.getTerm()) };
                    }
                    else {
                        _Logger.warning("check raft graph & broadcast leader self->self");
                        break;
                    }
                }
                else if (update.getState() == CANDIDATE) {
                    _Logger.warning("same term %d,late recv X72 from %#x", update.getTerm(), update.getPeerId());
                    return new X7F_RaftResponse[] { reject(ALREADY_VOTE) };
                }
            case LEARNER:
                break;
        }
        return null;
    }

    public IPair onResponse(long peerId,
                            long term,
                            long index,
                            long indexTerm,
                            long candidate,
                            RaftState state,
                            RaftCode code,
                            ISessionManager<ZContext> manager)
    {
        RaftMachine peerMachine = _RaftGraph.getMachine(peerId);
        if (peerMachine == null) {
            _Logger.warning("peer %#x is not found", peerId);
            return null;
        }
        peerMachine.setIndex(index);
        peerMachine.setIndexTerm(indexTerm);
        peerMachine.setTerm(term);
        peerMachine.setState(state);
        peerMachine.setCandidate(candidate);
        switch (code)
        {
            case SUCCESS:
                if (_SelfMachine.getState() == CANDIDATE) {
                    //granted
                    if (_RaftGraph.isMajorAcceptCandidate(_SelfMachine.getPeerId(), _SelfMachine.getTerm())) {
                        electCancel();
                        beLeader();
                        return new Pair<>(createBroadcasts().toArray(X7E_RaftBroadcast[]::new), null);
                    }
                }
                else if (_SelfMachine.getState() == LEADER) {
                    peerMachine.setMatchIndex(index);
                    X7E_RaftBroadcast x7e = null;
                    if (peerMachine.getIndex() < _SelfMachine.getIndex()) {
                        //append log -> follower
                        x7e = createBroadcast(peerMachine);
                        x7e.setSession(manager.findSessionByPrefix(peerId));
                    }
                    LogEntry raftLog = _RaftDao.getEntry(index);
                    if (peerMachine.getIndex() > _SelfMachine.getCommit()
                        && _RaftGraph.isMajorAcceptLeader(_SelfMachine.getPeerId(), _SelfMachine.getTerm(), index))
                    {
                        //peerMachine.getIndex() > _SelfMachine.getCommit()时，raftLog 不可能为 null
                        _SelfMachine.commit(index, _RaftDao);
                        _Logger.debug("consensus done->");
                        X76_RaftNotify x76 = createNotify(raftLog);
                        if (raftLog.isNotifyAll()) {
                            if (x7e != null) {
                                return new Pair<>(Stream.concat(Stream.of(x7e), createNotifyStream(manager, raftLog))
                                                        .toArray(IControl[]::new),
                                                  raftLog.getRaftClientId() == _SelfMachine.getPeerId() ? x76//leader -> client
                                                                                                        : null);//leader -> follower -> client
                            }
                            else {
                                return new Pair<>(createNotifyStream(manager, raftLog).toArray(X76_RaftNotify[]::new),
                                                  raftLog.getRaftClientId() == _SelfMachine.getPeerId() ? x76 //leader -> client
                                                                                                        : null);//leader -> follower -> client
                            }
                        }
                        else if (raftLog.getRaftClientId() != _SelfMachine.getPeerId()) {
                            //leader -> follower -> client
                            ISession<ZContext> followerSession = manager.findSessionByPrefix(raftLog.getRaftClientId());
                            return x7e != null && followerSession != null ? new Pair<>(new IControl[] { x7e,
                                                                                                        x76 },
                                                                                       null)
                                                                          : followerSession != null ? new Pair<>(new X76_RaftNotify[] { x76 },
                                                                                                                 null)
                                                                                                    : null;
                        }
                        else {
                            //leader -> client
                            return x7e != null ? new Pair<>(new X7E_RaftBroadcast[] { x7e }, x76)
                                               : new Pair<>(null, x76);

                        }
                    }
                }
                break;
            case LOWER_TERM:
                tickCancel();
                heartbeatCancel();
                electCancel();
                _Logger.debug("self.term %d < response.term %d => step_down",
                              _SelfMachine.getTerm(),
                              peerMachine.getTerm());
                stepDown();
                break;
            case CONFLICT:
                if (_SelfMachine.getState() == LEADER) {
                    _Logger.debug("follower %#x,match failed,rollback %d",
                                  peerMachine.getPeerId(),
                                  peerMachine.getIndex());
                    return new Pair<>(new X7E_RaftBroadcast[] { createBroadcast(peerMachine, 1) }, null);
                }
                else {
                    _Logger.warning("self %#x is old leader & send logs=> %#x,next-index wasn't catchup");
                    //Ignore
                }
                break;
            case ILLEGAL_STATE:
                //Ignore 
                if (_SelfMachine.getState() == CANDIDATE) {
                    _Logger.debug("my term is over,the leader will be %#x and receive x7E_raftBroadcast then step_down",
                                  peerMachine.getPeerId());
                }
                break;
            case ALREADY_VOTE:
                if (_SelfMachine.getState() == CANDIDATE
                    && _RaftGraph.isMinorReject(_SelfMachine.getPeerId(), _SelfMachine.getTerm()))
                {
                    break;
                }
            case OBSOLETE:
                if (_SelfMachine.getState() == CANDIDATE) {
                    electCancel();
                    stepDown();
                }
                break;
        }
        return null;
    }

    public List<LogEntry> diff()
    {
        long start = _SelfMachine.getApplied();
        long commit = _SelfMachine.getCommit();
        long current = _SelfMachine.getIndex();
        if (start >= commit) {
            return null;
        }
        else {
            long end = min(current, commit);
            List<LogEntry> result = new ArrayList<>((int) (end - start));
            for (long i = start; i <= end; i++) {
                LogEntry entry = _RaftDao.getEntry(i);
                if (entry != null) result.add(entry);
            }
            return result.isEmpty() ? null
                                    : result;
        }
    }

    private boolean catchUp(long preIndex, long preIndexTerm)
    {
        if (_SelfMachine.getState() == LEADER) {
            _Logger.warning("leader needn't catch up any log.");
            _AppendLogQueue.clear();
            return false;
        }
        ITERATE_APPEND:
        {
            if (_AppendLogQueue.isEmpty()) {
                /*
                1:follower 未将自身的index 同步给leader，next_index = -1
                2:follower index == leader.index 此时需要确认 双边的index_term是否一致
                需要注意当 index 
                 */
                if (_SelfMachine.getIndex() == 0 && preIndex == 0) {
                    //初始态，raft-machine 中不包含任何 log 记录
                    _Logger.debug("no log append");
                }
                else if (_SelfMachine.getIndex() < preIndex) {
                    /*
                    1.follower 未将自身index 同步给leader，所以leader 仅发送了一个当前heartbeat
                    后续执行success() → response 会将index 同步给leader。
                    no action => ignore
                    2.leader 已经获知follower的需求 但是 self.index < leader.start
                    需要follower先完成snapshot的安装才能正常同步。
                    */
                    _Logger.debug("leader doesn't know my next  || leader hasn't log,need to install snapshot");
                }
                else if (_SelfMachine.getIndex() == preIndex) {
                    if (_SelfMachine.getIndexTerm() == preIndexTerm) {
                        // follower 完成与Leader 的同步
                        _SelfMachine.apply(_RaftDao);
                        _Logger.debug("log synchronized ");
                    }
                    else {
                        /*
                         1.follower.index_term < pre_index_term 
                         说明follower.index 在 index_term 写入后，未接收到commit就离线了，且leader未能在任期内完成commit 
                         便触发了新的选举。最终在follower 离线时重新写入了 log.index@pre_index_term
                         2.follower.index_term > pre_index_term
                         此时集群一定经历了一次不可逆的数据损失。
                         */
                        long newEndIndex = preIndex - 1;
                        LogEntry rollback = _RaftDao.truncateSuffix(newEndIndex);
                        if (rollback != null) {
                            _SelfMachine.appendLog(rollback.getIndex(), rollback.getTerm(), _RaftDao);
                            _SelfMachine.apply(_RaftDao);
                            _Logger.debug("machine rollback %d@%d", rollback.getIndex(), rollback.getTerm());
                        }
                        else if (newEndIndex == 0) {
                            _SelfMachine.reset();
                            _Logger.debug("machine reset in heartbeat");
                        }
                        else if (newEndIndex >= _RaftDao.getStartIndex()) {
                            _Logger.fetal("lost data, manual recover");
                        }
                        break ITERATE_APPEND;
                    }
                }

            }
            else {
                if (preIndex == _SelfMachine.getIndex() && preIndexTerm == _SelfMachine.getIndexTerm()) {
                    for (Iterator<LogEntry> it = _AppendLogQueue.iterator(); it.hasNext();) {
                        LogEntry entry = it.next();
                        if (_RaftDao.appendLog(entry)) {
                            _SelfMachine.appendLog(entry.getIndex(), entry.getTerm(), _RaftDao);
                            _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                        }
                        it.remove();
                    }
                }
                else {
                    _Logger.debug("conflict  self: %d@%d <==> leader: %d@%d",
                                  _SelfMachine.getIndex(),
                                  _SelfMachine.getIndexTerm(),
                                  preIndex,
                                  preIndexTerm);
                    LogEntry old = _RaftDao.getEntry(preIndex);
                    if (old == null || old.getTerm() != preIndexTerm) {
                        if (old == null) {
                            _Logger.debug("log %d miss,self %s", preIndex, _SelfMachine);
                        }
                        /*
                           此处存在一个可优化点，将此冲突对应的 term 下所有的index 都注销，向leader反馈
                           上一个term.end_index,能有效减少以-1进行删除的数据量。此种优化的意义不是很大。
                        */
                        long newEndIndex = preIndex - 1;
                        LogEntry rollback = _RaftDao.truncateSuffix(newEndIndex);
                        if (rollback != null) {
                            _SelfMachine.appendLog(rollback.getIndex(), rollback.getTerm(), _RaftDao);
                            _SelfMachine.apply(_RaftDao);
                            _Logger.debug("machine rollback %d@%d", rollback.getIndex(), rollback.getTerm());
                        }
                        else if (newEndIndex == 0) {
                            //回归起点
                            _SelfMachine.reset();
                            _Logger.debug("machine reset in append");
                        }
                        else if (newEndIndex >= _RaftDao.getStartIndex()) {
                            /*
                            回滚异常，但是不会出现 newEndIndex < _RaftDao.start 的情况
                            否则集群中的大多数都是错误的数据，在此之前一定出现了大规模集群失败
                            且丢失正确的数据的情况,从而导致了，follower本地需要回滚到snapshot之前，
                            */
                            _Logger.fetal("cluster failed over flow,lost data. manual recover ");
                        }
                        break ITERATE_APPEND;
                    }
                    else {
                        /*
                            old != null && old.term == pre_index_term && old.index == pre_index
                            1.old ==  null entry.index 处于 未正常管控的位置
                            启动协商之后是不会出现此类情况的。
                            2.old.term==entry.term已经完成了日志存储，不再重复append 
                        */
                        _Logger.fetal(" pre_index & pre_index_term already check, impossible go in here ");
                    }
                }
                //接收了Leader 追加的日志
                _SelfMachine.apply(_RaftDao);
            }
            _Logger.debug("catch up %d@%d", _SelfMachine.getIndex(), _SelfMachine.getIndexTerm());
            return true;
            //end of IT_APPEND
        }
        _AppendLogQueue.clear();
        return false;
    }

    public List<X72_RaftVote> checkVoteState(RaftMachine update)
    {
        if (update.getPeerId() == _SelfMachine.getPeerId()
            && update.getTerm() == _SelfMachine.getTerm() + 1
            && update.getIndex() == _SelfMachine.getIndex()
            && update.getIndexTerm() == _SelfMachine.getIndexTerm()
            && update.getCandidate() == _SelfMachine.getPeerId()
            && update.getCommit() == _SelfMachine.getCommit()
            && _SelfMachine.getState() == FOLLOWER
            && update.getState() == CANDIDATE)
        {
            vote4me();
            return createVotes(update).collect(Collectors.toList());
        }
        _Logger.warning("check vote failed; now: %s", _SelfMachine);
        return null;
    }

    public List<X7E_RaftBroadcast> checkLogAppend(RaftMachine update)
    {
        if (_SelfMachine.getState() == LEADER
            && _SelfMachine.getPeerId() == update.getPeerId()
            && _SelfMachine.getTerm() >= update.getTerm()
            && _SelfMachine.getIndex() >= update.getIndex()
            && _SelfMachine.getIndexTerm() >= update.getIndexTerm()
            && _SelfMachine.getCommit() >= update.getCommit())
        {
            if (_RaftGraph.isMajorAcceptLeader(_SelfMachine.getPeerId(),
                                               _SelfMachine.getTerm(),
                                               _SelfMachine.getIndex()))
            {
                _Logger.debug("keep lead =>%s", _SelfMachine);
                return createBroadcasts().collect(Collectors.toList());
            }
            else {
                _Logger.debug("no response enough");
                stepDown();
            }
        }
        //state change => ignore
        _Logger.warning("check leader broadcast failed; now:%s", _SelfMachine);
        return null;
    }

    public void appendLogs(List<LogEntry> entryList)
    {
        //offer all
        _AppendLogQueue.addAll(entryList);
    }

    public Stream<X7E_RaftBroadcast> newLogEntry(IConsistentProtocol request, long raftClientId, long origin)
    {
        return newLogEntry(request.serial(), request.encode(), raftClientId, origin, request.isNotifyAll());
    }

    public Stream<X7E_RaftBroadcast> newLogEntry(int requestSerial,
                                                 byte[] requestData,
                                                 long raftClientId,
                                                 long origin,
                                                 boolean notifyAll)
    {
        _Logger.debug("create new raft log");
        LogEntry newEntry = new LogEntry(_SelfMachine.getTerm(),
                                         _SelfMachine.getIndex() + 1,
                                         raftClientId,
                                         origin,
                                         requestSerial,
                                         requestData,
                                         notifyAll);
        _Logger.debug("%s", newEntry);
        if (_RaftDao.appendLog(newEntry)) {
            heartbeatCancel();
            _SelfMachine.appendLog(newEntry.getIndex(), newEntry.getTerm(), _RaftDao);
            _Logger.debug("leader appended log %d@%d", newEntry.getIndex(), newEntry.getTerm());
            return createBroadcasts();
        }
        _Logger.fetal("Raft WAL failed!");
        throw new IllegalStateException("Raft WAL failed!");
    }

    private Stream<X72_RaftVote> createVotes(IRaftMachine update)
    {
        return _RaftGraph.getNodeMap()
                         .keySet()
                         .stream()
                         .filter(peerId -> peerId != _SelfMachine.getPeerId())
                         .map(peerId ->
                         {
                             X72_RaftVote x72 = new X72_RaftVote(_ZUID.getId());
                             x72.setPeerId(update.getPeerId());
                             x72.setTerm(update.getTerm());
                             x72.setLogIndex(update.getIndex());
                             x72.setLogTerm(update.getIndexTerm());
                             x72.setElector(peerId);
                             x72.setCommit(update.getCommit());
                             return x72;
                         });
    }

    private Stream<X7E_RaftBroadcast> createBroadcasts()
    {
        mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
        return _RaftGraph.getNodeMap()
                         .values()
                         .stream()
                         .filter(other -> other.getPeerId() != _SelfMachine.getPeerId())
                         .map(this::createBroadcast);
    }

    private X7E_RaftBroadcast createBroadcast(RaftMachine follower)
    {
        return createBroadcast(follower, -1);
    }

    private X7E_RaftBroadcast createBroadcast(RaftMachine follower, int limit)
    {
        X7E_RaftBroadcast x7e = new X7E_RaftBroadcast(_ZUID.getId());
        x7e.setPeerId(_SelfMachine.getPeerId());
        x7e.setTerm(_SelfMachine.getTerm());
        x7e.setCommit(_SelfMachine.getCommit());
        //先初始化为leader最新的的log数据
        x7e.setPreIndex(_SelfMachine.getIndex());
        x7e.setPreIndexTerm(_SelfMachine.getIndexTerm());
        x7e.setFollower(follower.getPeerId());
        follower.setMatchIndex(INDEX_NAN);//match_index 重置
        if (follower.getIndex() == _SelfMachine.getIndex() || follower.getIndex() == INDEX_NAN) {
            //follower 已经同步 或 follower.next 未知
            return x7e;
        }
        //follower.index < self.index
        List<LogEntry> entryList = new LinkedList<>();
        final long _Index = follower.getIndex();
        LogEntry nextLog;
        nextLog = _RaftDao.getEntry(_Index >= MIN_START ? _Index
                                                        : 0);
        if (nextLog == null) {
            if (_Index >= MIN_START) {
                _Logger.warning("leader truncate prefix，%#x wait for installing snapshot", follower.getPeerId());
            }
            return x7e;
        }
        else {
            x7e.setPreIndex(nextLog.getIndex());
            x7e.setPreIndexTerm(nextLog.getTerm());
        }
        for (long i = nextLog.getIndex() + 1, l = _SelfMachine.getIndex(); i <= l; i++) {
            if (limit > 0 && entryList.size() >= limit) {
                break;
            }
            entryList.add(_RaftDao.getEntry(i));
        }
        x7e.setPayload(JsonUtil.writeValueAsBytes(entryList));
        return x7e;
    }

    public boolean isClusterModel()
    {
        return _ClusterConfig.isClusterModel();
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    private X76_RaftNotify createNotify(LogEntry raftLog)
    {
        X76_RaftNotify x76 = new X76_RaftNotify(_ZUID.getId());
        x76.setPayloadSerial(raftLog.getPayloadSerial());
        x76.setPayload(raftLog.getPayload());
        x76.setNotifyAll(raftLog.isNotifyAll());
        x76.setOrigin(raftLog.getOrigin());
        return x76;
    }

    private Stream<X76_RaftNotify> createNotifyStream(ISessionManager<ZContext> manager, LogEntry raftLog)
    {
        return _RaftGraph.getNodeMap()
                         .values()
                         .stream()
                         .filter(node -> node.getPeerId() != _SelfMachine.getPeerId())
                         .map(machine ->
                         {
                             ISession<ZContext> followerSession = manager.findSessionByPrefix(machine.getPeerId());
                             if (followerSession != null) {
                                 X76_RaftNotify x76 = createNotify(raftLog);
                                 x76.setSession(followerSession);
                                 return x76;
                             }
                             else {
                                 _Logger.debug("not found %#x 's session", machine.getPeerId());
                                 return null;
                             }
                         })
                         .filter(Objects::nonNull);
    }
}
