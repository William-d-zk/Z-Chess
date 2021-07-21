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

package com.isahl.chess.knight.raft;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZCommand;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.*;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.IValid;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.schedule.ScheduleHandler;
import com.isahl.chess.king.base.schedule.TimeWheel;
import com.isahl.chess.king.base.schedule.inf.ICancelable;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.inf.IRaftMachine;
import com.isahl.chess.knight.raft.inf.IRaftMapper;
import com.isahl.chess.knight.raft.inf.IRaftMessage;
import com.isahl.chess.knight.raft.model.*;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.io.core.inf.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.isahl.chess.king.topology.ZUID.INVALID_PEER_ID;
import static com.isahl.chess.knight.raft.inf.IRaftMachine.INDEX_NAN;
import static com.isahl.chess.knight.raft.inf.IRaftMachine.MIN_START;
import static com.isahl.chess.knight.raft.model.RaftCode.*;
import static com.isahl.chess.knight.raft.model.RaftState.*;
import static java.lang.Math.min;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftPeer<M extends IClusterPeer & IClusterTimer>
        implements IValid
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final ZUID                         _ZUid;
    private final IRaftConfig                  _RaftConfig;
    private final M                            _ClusterPeer;
    private final IRaftMapper                  _RaftMapper;
    private final TimeWheel                    _TimeWheel;
    private final RaftGraph                    _RaftGraph;
    private final RaftMachine                  _SelfMachine;
    private final Queue<LogEntry>              _LogQueue = new LinkedList<>();
    private final Random                       _Random   = new Random();
    private final long                         _SnapshotFragmentMaxSize;
    private final ScheduleHandler<RaftPeer<M>> _ElectSchedule, _HeartbeatSchedule, _TickSchedule;

    private ICancelable mElectTask, mHeartbeatTask, mTickTask;

    public RaftPeer(TimeWheel timeWheel, IRaftConfig raftConfig, IRaftMapper raftDao, M manager)
    {
        _TimeWheel = timeWheel;
        _RaftConfig = raftConfig;
        _ClusterPeer = manager;
        _ZUid = raftConfig.createZUID();
        _RaftMapper = raftDao;
        _ElectSchedule = new ScheduleHandler<>(_RaftConfig.getElectInSecond(), RaftPeer::stepDown);
        _HeartbeatSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond(), RaftPeer::heartbeat);
        _TickSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond()
                                                         .multipliedBy(2), RaftPeer::startVote);
        _RaftGraph = new RaftGraph();
        _SelfMachine = new RaftMachine(_ZUid.getPeerId());
        _RaftGraph.append(_SelfMachine);
        _SnapshotFragmentMaxSize = _RaftConfig.getSnapshotFragmentMaxSize();
    }

    private void heartbeat()
    {
        _ClusterPeer.timerEvent(_SelfMachine.createLeader());
        _Logger.debug("leader heartbeat");
    }

    private void init()
    {
        /* _RaftDao 启动的时候已经装载了 snapshot */
        _SelfMachine.setTerm(_RaftMapper.getLogMeta()
                                        .getTerm());
        _SelfMachine.setCandidate(_RaftMapper.getLogMeta()
                                             .getCandidate());
        _SelfMachine.setCommit(_RaftMapper.getLogMeta()
                                          .getCommit());
        _SelfMachine.setApplied(_RaftMapper.getLogMeta()
                                           .getApplied());
        _SelfMachine.setIndex(_RaftMapper.getLogMeta()
                                         .getIndex());
        _SelfMachine.setIndexTerm(_RaftMapper.getLogMeta()
                                             .getIndexTerm());
        _SelfMachine.setPeerSet(_RaftMapper.getLogMeta()
                                           .getPeerSet());
        _SelfMachine.setGateSet(_RaftMapper.getLogMeta()
                                           .getGateSet());
        /* 初始化时，match_index == applied */
        _SelfMachine.setMatchIndex(_SelfMachine.getApplied());
        if(_SelfMachine.getPeerSet() == null) {
            /* 首次启动或删除本地状态机重启,仅需要连接node_id < self.node_id的peer */
            _RaftMapper.loadDefaultGraphSet();
            _SelfMachine.setPeerSet(_RaftMapper.getLogMeta()
                                               .getPeerSet());
            _SelfMachine.setGateSet(_RaftMapper.getLogMeta()
                                               .getGateSet());
        }
        if(!_RaftConfig.isInCongress()) {
            _Logger.info("single mode , ignore state listen");
            return;
        }
        _TimeWheel.acquire(_RaftMapper,
                           new ScheduleHandler<>(_RaftConfig.getSnapshotInSecond(), true, this::takeSnapshot));
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        _Logger.info("raft node init -> %s", _SelfMachine);
    }

    public void start()
    {
        init();
        if(_RaftConfig.isClusterMode()) {
            // 启动集群连接
            if(_SelfMachine.getPeerSet() != null) {
                for(RaftNode remote : _SelfMachine.getPeerSet()) {
                    long peerId = remote.getId();
                    // Graph中 装入所有需要管理的集群状态机，self已经装入了，此处不再重复
                    if(peerId != _SelfMachine.getPeerId()) {
                        _RaftGraph.append(new RaftMachine(peerId));
                    }
                    // 仅连接NodeId<自身的节点
                    if(peerId < _SelfMachine.getPeerId()) {
                        try {
                            _ClusterPeer.setupPeer(remote.getHost(), remote.getPort());
                            _Logger.info("->peer : %s:%d", remote.getHost(), remote.getPort());
                        }
                        catch(Exception e) {
                            _Logger.warning("peer connect error: %s:%d", e, remote.getHost(), remote.getPort());
                        }
                    }
                }
            }
            if(_SelfMachine.getGateSet() != null) {
                for(RaftNode remote : _SelfMachine.getGateSet()) {
                    long gateId = remote.getId();
                    if(_ZUid.isTheGate(gateId)) {
                        try {
                            _ClusterPeer.setupGate(remote.getGateHost(), remote.getGatePort());
                            _Logger.info("->gate : %s:%d", remote.getGateHost(), remote.getGatePort());
                        }
                        catch(Exception e) {
                            _Logger.warning("gate connect error: %s:%d", e, remote.getGateHost(), remote.getGatePort());
                        }

                    }
                }
            }
            _Logger.info("raft-node : %#x start", _SelfMachine.getPeerId());
        }
    }

    public IRaftConfig getRaftConfig()
    {
        return _RaftConfig;
    }

    public void reinstallGraph()
    {

    }

    public void installSnapshot(List<IRaftMessage> snapshot)
    {

    }

    public void takeSnapshot(IRaftMapper snapshot)
    {

        long localTerm;
        if(_RaftMapper.getTotalSize() < _RaftConfig.getSnapshotMinSize()) {
            _Logger.debug("snapshot size less than threshold");
            return;
        }
        // 状态迁移未完成
        if(_SelfMachine.getApplied() <= _SelfMachine.getCommit()) {
            _Logger.debug(" applied < commit sync is running");
            return;
        }
        if(_SelfMachine.getApplied() >= _RaftMapper.getStartIndex() &&
           _SelfMachine.getApplied() <= _RaftMapper.getEndIndex())
        {
            localTerm = _RaftMapper.getEntryTerm(_SelfMachine.getApplied());
            _RaftMapper.updateSnapshotMeta(_SelfMachine.getApplied(), localTerm);
            _Logger.debug("take snapshot");
        }
        long lastSnapshotIndex = _RaftMapper.getSnapshotMeta()
                                            .getCommit();
        if(lastSnapshotIndex > 0 && _RaftMapper.getStartIndex() <= lastSnapshotIndex) {
            _RaftMapper.truncatePrefix(lastSnapshotIndex + 1);
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
        /*
         * 关闭TickTask,此时执行容器可能为ElectTask 或 TickTask自身
         * 由于Elect.timeout << Tick.timeout,此处不应出现Tick无法
         * 关闭的，或关闭异常。同时cancel 配置了lock 防止意外出现。
         */
        tickCancel();
        try {
            long wait = _Random.nextInt(150) + 100;
            Thread.sleep(wait);
            _Logger.debug("random wait for %d mills", wait);
        }
        catch(InterruptedException e) {
            // ignore
        }
        _ClusterPeer.timerEvent(_SelfMachine.createCandidate());
    }

    private X71_RaftBallot ballot()
    {
        X71_RaftBallot vote = new X71_RaftBallot(_ZUid.getId());
        vote.setElectorId(_SelfMachine.getPeerId());
        vote.setTerm(_SelfMachine.getTerm());
        vote.setIndex(_SelfMachine.getIndex());
        vote.setCandidateId(_SelfMachine.getCandidate());
        return vote;
    }

    private X73_RaftAccept accept()
    {
        X73_RaftAccept accept = new X73_RaftAccept(_ZUid.getId());
        accept.setFollowerId(_SelfMachine.getPeerId());
        accept.setTerm(_SelfMachine.getTerm());
        accept.setCatchUp(_SelfMachine.getIndex());
        accept.setLeaderId(_SelfMachine.getLeader());
        accept.setCommit(_SelfMachine.getCommit());
        return accept;
    }

    private X74_RaftReject reject(RaftCode raftCode, long rejectTo)
    {
        X74_RaftReject reject = new X74_RaftReject(_ZUid.getId());
        reject.setPeerId(_SelfMachine.getPeerId());
        reject.setTerm(_SelfMachine.getTerm());
        reject.setIndex(_SelfMachine.getIndex());
        reject.setIndexTerm(_SelfMachine.getIndexTerm());
        reject.setReject(rejectTo);
        reject.setCode(raftCode.getCode());
        reject.setState(_SelfMachine.getState()
                                    .getCode());
        return reject;
    }

    private IControl[] rejectAndVote(long rejectTo, ISessionManager manager)
    {
        vote4me();
        List<ZCommand> votes = createVotes(_SelfMachine, manager, Function.identity());
        votes.add(reject(RaftCode.OBSOLETE, rejectTo));
        return votes.toArray(IControl[]::new);
    }

    private X71_RaftBallot stepUp(long candidate, long term)
    {
        tickCancel();
        _SelfMachine.beElector(candidate, term, _RaftMapper);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("[follower → elector] %s", _SelfMachine);
        return ballot();
    }

    private void stepDown(long term)
    {
        if(_SelfMachine.getState()
                       .getCode() > FOLLOWER.getCode())
        {
            _Logger.debug("step down [%s → follower] %s", _SelfMachine.getState(), _SelfMachine);
            _SelfMachine.follow(term, _RaftMapper);
            mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        }
        else { _Logger.warning("step down [ignore],state has already changed to FOLLOWER"); }
    }

    private void stepDown()
    {
        _ClusterPeer.timerEvent(_SelfMachine.createFollower());
    }

    private IControl follow(long peerId, long term, long commit, long preIndex, long preIndexTerm)
    {
        tickCancel();
        _SelfMachine.follow(peerId, term, commit, _RaftMapper);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        _Logger.debug("follower %#x @%d => %d", peerId, term, commit);
        if(catchUp(preIndex, preIndexTerm)) {
            _Logger.debug("follower appended from %#x@%d-^%d=> %s", peerId, term, commit, _SelfMachine);
            return accept();
        }
        else {
            _Logger.debug("roll back =>%s ", _SelfMachine);
            return reject(CONFLICT, peerId);
        }
    }

    private void vote4me()
    {
        tickCancel();
        _SelfMachine.beCandidate(_RaftMapper);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("follower => candidate %s", _SelfMachine);
    }

    private void beLeader()
    {
        electCancel();
        _SelfMachine.beLeader(_RaftMapper);
        _Logger.info("be leader=>%s", _SelfMachine);
        mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
    }

    private void tickCancel()
    {
        if(mTickTask != null) {
            _Logger.debug("cancel tick");
            mTickTask.cancel();
        }
    }

    private void leadCancel()
    {
        if(mHeartbeatTask != null) {
            _Logger.debug("cancel heartbeat");
            mHeartbeatTask.cancel();
        }
    }

    private void electCancel()
    {
        if(mElectTask != null) {
            _Logger.debug("cancel elect");
            mElectTask.cancel();
        }
    }

    private IControl[] lowTerm(long term, long peerId)
    {
        return _SelfMachine.getTerm() > term ? new X74_RaftReject[]{ reject(LOWER_TERM, peerId) } : null;
    }

    private boolean highTerm(long term)
    {
        if(term > _SelfMachine.getTerm()) {
            _Logger.debug("new term > self [%d > %d]", term, _SelfMachine.getTerm());
            leadCancel();
            electCancel();
            return true;
        }
        return false;
    }

    public IPair elect(long term, long index, long indexTerm, long candidate, long commit, ISessionManager manager)
    {
        IControl[] response = lowTerm(term, candidate);
        if(response != null) { return new Pair<>(response, null); }
        if(highTerm(term) || _SelfMachine.getState() == FOLLOWER) {
            if(_SelfMachine.getIndex() <= index && _SelfMachine.getIndexTerm() <= indexTerm &&
               _SelfMachine.getCommit() <= commit)
            {
                _Logger.debug("new term [follower → elector %d ] | candidate: %#x", term, candidate);
                X71_RaftBallot vote = stepUp(candidate, term);
                return new Pair<>(new X71_RaftBallot[]{ vote }, null);
            }
            else {
                _Logger.debug("less than me; reject and [ follower → candidate] | mine:%d@%d > in:%d@%d",
                              _SelfMachine.getIndex(),
                              _SelfMachine.getIndexTerm(),
                              index,
                              indexTerm);
                return new Pair<>(rejectAndVote(candidate, manager), null);
            }
        }
        // elector|leader|candidate,one of these states ，candidate != INDEX_NAN 不需要重复判断
        else if(_SelfMachine.getCandidate() != candidate) {
            _Logger.debug("already vote [elector ×] | vote for:%#x not ♂ %#x", _SelfMachine.getCandidate(), candidate);
            return new Pair<>(new X74_RaftReject[]{ reject(ALREADY_VOTE, candidate) }, null);
        }
        return null;
    }

    public IPair ballot(long term, long index, long elector, long candidate, ISessionManager manager)
    {
        IControl[] response = lowTerm(term, elector);
        if(response != null) { return new Pair<>(response, null); }
        if(highTerm(term)) {
            _Logger.warning("high term of ballot; %d->%d");
            stepDown(term);
            return null;
        }
        RaftMachine peerMachine = getMachine(elector, term);
        if(peerMachine == null) { return null; }
        peerMachine.setIndex(index);
        peerMachine.setCandidate(candidate);
        peerMachine.setState(FOLLOWER);
        if(_SelfMachine.getState() == CANDIDATE &&
           _RaftGraph.isMajorAcceptCandidate(_SelfMachine.getPeerId(), _SelfMachine.getTerm()))
        {
            beLeader();
            return new Pair<>(createBroadcasts(manager, Function.identity()).toArray(IControl[]::new), null);
        }
        return null;
    }

    public IPair append(long term, long preIndex, long preIndexTerm, long leader, long leaderCommit)
    {
        IControl[] response = lowTerm(term, leader);
        if(response != null) { return new Pair<>(response, null); }
        if(highTerm(term) || _SelfMachine.getState() != LEADER) {
            _Logger.debug("follow leader %#x leader_commit: %d", leader, leaderCommit);
            return new Pair<>(new IControl[]{ follow(leader, term, leaderCommit, preIndex, preIndexTerm) }, null);
        }
        // leader
        else if(_SelfMachine.getCandidate() != leader) {
            return new Pair<>(new IControl[]{ reject(ILLEGAL_STATE, leader) }, null);
        }
        return null;
    }

    private RaftMachine getMachine(long peerId, long term)
    {
        RaftMachine peerMachine = _RaftGraph.getMachine(peerId);
        if(peerMachine == null) {
            _Logger.warning("peer %#x is not found", peerId);
            return null;
        }
        peerMachine.setTerm(term);
        return peerMachine;
    }

    /**
     * follower → leader
     *
     * @return first : broadcast to peers
     * second : resp to origin
     */
    public IPair onAccept(long peerId, long term, long index, long leader, ISessionManager manager)
    {
        RaftMachine peerMachine = getMachine(peerId, term);
        if(peerMachine == null) { return null; }
        peerMachine.setState(FOLLOWER);
        peerMachine.setLeader(leader);
        peerMachine.setMatchIndex(index);
        /*
         * follower.match_index > leader.commit 完成半数 match 之后只触发一次 leader commit
         */
        long nextCommit = _SelfMachine.getCommit() + 1;
        if(peerMachine.getMatchIndex() > _SelfMachine.getCommit() &&
           _RaftGraph.isMajorAcceptLeader(_SelfMachine.getPeerId(), _SelfMachine.getTerm(), nextCommit))
        {
            // peerMachine.getIndex() > _SelfMachine.getCommit()时，raftLog 不可能为 null
            _SelfMachine.commit(nextCommit, _RaftMapper);
            _Logger.debug("leader commit: %d @%d", nextCommit, _SelfMachine.getTerm());
            LogEntry raftLog = _RaftMapper.getEntry(nextCommit);
            X77_RaftNotify x77 = createNotify(raftLog);
            if(raftLog.getClient() != _SelfMachine.getPeerId()) {
                // leader -> follower -> client
                ISession cSession = manager.findSessionByPrefix(raftLog.getClient());
                if(cSession != null) {
                    return new Pair<>(new IControl[]{ x77 }, x77);
                }
            }
            /*
             * leader -> client
             * 作为client 收到 notify
             * x77 投递给notify-custom
             */
            return new Pair<>(null, x77);
        }
        return null;
    }

    public IPair onReject(long peerId, long term, long index, long indexTerm, int code, int state)
    {
        if(highTerm(term)) {
            stepDown(term);
            return null;
        }
        RaftMachine peerMachine = getMachine(peerId, term);
        if(peerMachine == null) { return null; }
        peerMachine.setIndex(index);
        peerMachine.setIndexTerm(indexTerm);
        peerMachine.setCandidate(INVALID_PEER_ID);
        peerMachine.setLeader(INVALID_PEER_ID);
        peerMachine.setState(RaftState.valueOf(state));
        switch(RaftCode.valueOf(code)) {
            case CONFLICT:
                if(_SelfMachine.getState() == LEADER) {
                    _Logger.debug("follower %#x,match failed,rollback %d",
                                  peerMachine.getPeerId(),
                                  peerMachine.getIndex());
                    return new Pair<>(new X72_RaftAppend[]{ createAppend(peerMachine, 1) }, null);
                }
                else {
                    _Logger.warning("self %#x is old leader & send logs=> %#x,next-index wasn't catchup");
                    // Ignore
                }
                break;
            case ILLEGAL_STATE:
                // Ignore
                if(_SelfMachine.getState() == CANDIDATE) {
                    _Logger.debug("my term is over,the leader will be %#x and receive x7E_raftBroadcast then step_down",
                                  peerMachine.getPeerId());
                }
                break;
            case ALREADY_VOTE:
                if(_SelfMachine.getState() == CANDIDATE &&
                   _RaftGraph.isMinorReject(_SelfMachine.getPeerId(), _SelfMachine.getTerm()))
                {
                    break;
                }
            case OBSOLETE:
                if(_SelfMachine.getState()
                               .getCode() > FOLLOWER.getCode())
                {
                    electCancel();
                    stepDown(_SelfMachine.getTerm());
                }
                break;
        }
        return null;
    }

    /**
     * leader → follower
     *
     * @return first : broadcast to peers
     * second : resp to origin
     */
    public IPair onDirect(long peerId, long term, long index, long leader, ISessionManager manager)
    {
        RaftMachine peerMachine = getMachine(peerId, term);
        if(peerMachine == null) { return null; }

        return null;
    }

    public List<LogEntry> diff()
    {
        long start = _SelfMachine.getApplied();
        long commit = _SelfMachine.getCommit();
        long current = _SelfMachine.getIndex();
        if(start >= commit) {
            return null;
        }
        else {
            long end = min(current, commit);
            List<LogEntry> result = new ArrayList<>((int) (end - start));
            for(long i = start; i <= end; i++) {
                LogEntry entry = _RaftMapper.getEntry(i);
                if(entry != null) { result.add(entry); }
            }
            return result.isEmpty() ? null : result;
        }
    }

    private boolean catchUp(long preIndex, long preIndexTerm)
    {
        if(_SelfMachine.getState() == LEADER) {
            _Logger.warning("leader needn't catch up any log.");
            _LogQueue.clear();
            return false;
        }
        ITERATE_APPEND:
        {
            if(_LogQueue.isEmpty()) {
                /*
                 * 1:follower 未将自身的index 同步给leader，next_index = -1
                 * 2:follower index == leader.index 此时需要确认 双边的index_term是否一致
                 * 需要注意当 index
                 */
                if(_SelfMachine.getIndex() == 0 && preIndex == 0) {
                    // 初始态，raft-machine 中不包含任何 log 记录
                    _Logger.debug("no log append");
                }
                else if(_SelfMachine.getIndex() < preIndex) {
                    /*
                     * 1.follower 未将自身index 同步给leader，所以leader 仅发送了一个当前heartbeat
                     * 后续执行success() → response 会将index 同步给leader。
                     * no action => ignore
                     * 2.leader 已经获知follower的需求 但是 self.index < leader.start
                     * 需要follower先完成snapshot的安装才能正常同步。
                     */
                    _Logger.debug("leader doesn't know my next  || leader hasn't log,need to install snapshot");
                    break ITERATE_APPEND;
                }
                else if(_SelfMachine.getIndex() == preIndex) {
                    if(_SelfMachine.getIndexTerm() == preIndexTerm) {
                        // follower 完成与Leader 的同步
                        _SelfMachine.apply(_RaftMapper);
                        _Logger.debug("log synchronized ");
                    }
                    else {
                        /*
                         * 1.follower.index_term < pre_index_term
                         * 说明follower.index 在 index_term 写入后，未接收到commit就离线了，且leader未能在任期内完成commit
                         * 便触发了新的选举。最终在follower 离线时重新写入了 log.index@pre_index_term
                         * 2.follower.index_term > pre_index_term
                         * 此时集群一定经历了一次不可逆的数据损失。
                         */
                        long newEndIndex = preIndex - 1;
                        LogEntry rollback = _RaftMapper.truncateSuffix(newEndIndex);
                        if(rollback != null) {
                            _SelfMachine.rollBack(rollback.getIndex(), rollback.getTerm(), _RaftMapper);
                            _SelfMachine.apply(_RaftMapper);
                            _Logger.debug("machine rollback %d@%d", rollback.getIndex(), rollback.getTerm());
                        }
                        else if(newEndIndex == 0) {
                            _SelfMachine.reset();
                            _Logger.debug("machine reset in heartbeat");
                        }
                        else if(newEndIndex >= _RaftMapper.getStartIndex()) {
                            _Logger.fetal("lost data, manual recover");
                        }
                        break ITERATE_APPEND;
                    }
                }
            }
            else {
                if(preIndex == _SelfMachine.getIndex() && preIndexTerm == _SelfMachine.getIndexTerm()) {
                    for(Iterator<LogEntry> it = _LogQueue.iterator(); it.hasNext(); ) {
                        LogEntry entry = it.next();
                        if(_RaftMapper.appendLog(entry)) {
                            _SelfMachine.appendLog(entry.getIndex(), entry.getTerm(), _RaftMapper);
                            _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                        }
                        it.remove();
                    }
                }
                else {
                    _Logger.warning("conflict  self: %d@%d <==> leader: %d@%d",
                                    _SelfMachine.getIndex(),
                                    _SelfMachine.getIndexTerm(),
                                    preIndex,
                                    preIndexTerm);
                    LogEntry old = _RaftMapper.getEntry(preIndex);
                    if(old == null || old.getTerm() != preIndexTerm) {
                        if(old == null) {
                            _Logger.warning("log %d miss,self %s", preIndex, _SelfMachine);
                        }
                        /*
                         * 此处存在一个可优化点，将此冲突对应的 term 下所有的index 都注销，向leader反馈
                         * 上一个term.end_index,能有效减少以-1进行删除的数据量。此种优化的意义不是很大。
                         */
                        long newEndIndex = preIndex - 1;
                        LogEntry rollback = _RaftMapper.truncateSuffix(newEndIndex);
                        if(rollback != null) {
                            _SelfMachine.rollBack(rollback.getIndex(), rollback.getTerm(), _RaftMapper);
                            _SelfMachine.apply(_RaftMapper);
                            _Logger.debug("machine rollback %d@%d", rollback.getIndex(), rollback.getTerm());
                        }
                        else if(newEndIndex == 0) {
                            // 回归起点
                            _SelfMachine.reset();
                            _Logger.debug("machine reset in append");
                        }
                        else if(newEndIndex >= _RaftMapper.getStartIndex()) {
                            /*
                             * 回滚异常，但是不会出现 newEndIndex < _RaftDao.start 的情况
                             * 否则集群中的大多数都是错误的数据，在此之前一定出现了大规模集群失败
                             * 且丢失正确的数据的情况,从而导致了，follower本地需要回滚到snapshot之前，
                             */
                            _Logger.fetal("cluster failed over flow,lost data. manual recover ");
                        }
                        break ITERATE_APPEND;
                    }
                    else {
                        /*
                         * old != null && old.term == pre_index_term && old.index == pre_index
                         * 1.old == null entry.index 处于 未正常管控的位置
                         * 启动协商之后是不会出现此类情况的。
                         * 2.old.term==entry.term已经完成了日志存储，不再重复append
                         */
                        _Logger.fetal(" pre_index & pre_index_term already check, impossible go in here ");
                    }
                }
                // 接收了Leader 追加的日志
                _SelfMachine.apply(_RaftMapper);
            }
            _Logger.debug("catch up %d@%d", _SelfMachine.getIndex(), _SelfMachine.getIndexTerm());
            return true;
            // end of IT_APPEND
        }
        _LogQueue.clear();
        return false;
    }

    public void turnToFollower(RaftMachine update)
    {
        if((_SelfMachine.getState() == ELECTOR || _SelfMachine.getState() == CANDIDATE) &&
           update.getState() == FOLLOWER && update.getTerm() >= _SelfMachine.getTerm())
        {
            _Logger.debug("elect time out");
            stepDown(update.getTerm());
            return;
        }
        _Logger.warning("elect time out event [ignore]");
    }

    public <T> List<T> checkVoteState(RaftMachine update, ISessionManager manager, Function<ZCommand, T> mapper)
    {
        if(update.getTerm() == _SelfMachine.getTerm() + 1 && update.getIndex() == _SelfMachine.getIndex() &&
           update.getIndexTerm() == _SelfMachine.getIndexTerm() && update.getCandidate() == _SelfMachine.getPeerId() &&
           update.getCommit() == _SelfMachine.getCommit() && update.getState() == CANDIDATE &&
           _SelfMachine.getState() == FOLLOWER)
        {
            vote4me();
            return createVotes(update, manager, mapper);
        }
        _Logger.warning("check vote failed; now: %s", _SelfMachine);
        return null;
    }

    public <T> List<T> checkLogAppend(RaftMachine update, ISessionManager manager, Function<ZCommand, T> mapper)
    {
        if(_SelfMachine.getState() == LEADER && _SelfMachine.getPeerId() == update.getPeerId() &&
           _SelfMachine.getTerm() >= update.getTerm() && _SelfMachine.getIndex() >= update.getIndex() &&
           _SelfMachine.getIndexTerm() >= update.getIndexTerm() && _SelfMachine.getCommit() >= update.getCommit())
        {
            _Logger.debug("keep lead =>%s", _SelfMachine);
            mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
            return createBroadcasts(manager, mapper);
        }
        // state change => ignore
        _Logger.warning("check leader broadcast failed; now:%s", _SelfMachine);
        return null;
    }

    public void appendLogs(List<LogEntry> entryList)
    {
        if(entryList == null || entryList.isEmpty()) { return; }
        // offer all
        _LogQueue.addAll(entryList);
    }

    public <T extends IConsistent, R> List<R> newLocalLogEntry(T request,
                                                               long client,
                                                               ISessionManager manager,
                                                               Function<ZCommand, R> mapper)
    {
        byte[] payload = request.encode();
        List<R> cmds = createLeaderLogEntry(request.serial(), payload, client, request.getOrigin(), manager, mapper);
        if(cmds == null) {
            stepDown();
            return null;
        }
        else { return cmds; }
    }

    public IPair newLeaderLogEntry(int subSerial,
                                   byte[] payload,
                                   long client,
                                   long origin,
                                   ISessionManager manager,
                                   ISession cSession)
    {
        List<ZCommand> cmds = createLeaderLogEntry(subSerial, payload, client, origin, manager, Function.identity());
        if(cmds != null && !cmds.isEmpty()) {
            X76_RaftResp x76 = raftResp(SUCCESS, client, origin, subSerial, payload);
            x76.setSession(cSession);
            return new Pair<>(cmds.toArray(ZCommand[]::new), x76);
        }
        else {
            X76_RaftResp x76 = raftResp(WAL_FAILED, client, origin, subSerial, payload);
            x76.setSession(cSession);
            return new Pair<>(null, x76);
        }
    }

    public X76_RaftResp raftResp(RaftCode code, long client, long origin, int subSerial, byte[] payload)
    {
        X76_RaftResp x76 = new X76_RaftResp();
        x76.setClientId(client);
        x76.setOrigin(origin);
        x76.setSubSerial(subSerial);
        x76.setPayload(payload);
        x76.setCode((byte) code.getCode());
        return x76;
    }

    private <R> List<R> createLeaderLogEntry(int subSerial,
                                             byte[] payload,
                                             long client,
                                             long origin,
                                             ISessionManager manager,
                                             Function<ZCommand, R> mapper)
    {
        _Logger.debug("create new raft log");
        LogEntry newEntry = new LogEntry(_SelfMachine.getTerm(),
                                         _SelfMachine.getIndex() + 1,
                                         client,
                                         origin,
                                         subSerial,
                                         payload);
        newEntry.setOperation(IStorage.Operation.OP_INSERT);
        if(_RaftMapper.appendLog(newEntry)) {
            _SelfMachine.appendLog(newEntry.getIndex(), newEntry.getTerm(), _RaftMapper);
            _Logger.debug("leader appended log %d@%d", newEntry.getIndex(), newEntry.getTerm());
            return createBroadcasts(manager, mapper);
        }
        _Logger.fetal("RAFT WAL failed!");
        return null;
    }

    private <T> List<T> createVotes(IRaftMachine update, ISessionManager manager, Function<ZCommand, T> mapper)
    {
        return _RaftGraph.getNodeMap()
                         .keySet()
                         .stream()
                         .filter(peerId->peerId != _SelfMachine.getPeerId())
                         .map(peerId->{
                             ISession session = manager.findSessionByPrefix(peerId);
                             if(session != null) {
                                 X70_RaftVote x70 = new X70_RaftVote(_ZUid.getId());
                                 x70.setElectorId(peerId);
                                 x70.setCandidateId(update.getPeerId());
                                 x70.setTerm(update.getTerm());
                                 x70.setIndex(update.getIndex());
                                 x70.setIndexTerm(update.getIndexTerm());
                                 x70.setCommit(update.getCommit());
                                 x70.setSession(session);
                                 return x70;
                             }
                             _Logger.debug("elector :%#x session has not found", peerId);
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .map(mapper)
                         .collect(Collectors.toList());
    }

    private <T> List<T> createBroadcasts(ISessionManager manager, Function<ZCommand, T> mapper)
    {
        return _RaftGraph.getNodeMap()
                         .values()
                         .stream()
                         .filter(other->other.getPeerId() != _SelfMachine.getPeerId())
                         .map(follower->{
                             X72_RaftAppend x72 = createAppend(follower);
                             ISession session = manager.findSessionByPrefix(x72.getFollower());
                             if(session == null) {
                                 _Logger.warning("not found peerId:%#x session", x72.getFollower());
                                 return null;
                             }
                             else {
                                 x72.setSession(session);
                                 return x72;
                             }
                         })
                         .filter(Objects::nonNull)
                         .map(mapper)
                         .collect(Collectors.toList());
    }

    private X72_RaftAppend createAppend(RaftMachine follower)
    {
        return createAppend(follower, -1);
    }

    private X72_RaftAppend createAppend(RaftMachine follower, int limit)
    {
        X72_RaftAppend x72 = new X72_RaftAppend(_ZUid.getId());
        x72.setLeaderId(_SelfMachine.getPeerId());
        x72.setTerm(_SelfMachine.getTerm());
        x72.setCommit(_SelfMachine.getCommit());
        // 先初始化为leader最新的的log数据
        x72.setPreIndex(_SelfMachine.getIndex());
        x72.setPreIndexTerm(_SelfMachine.getIndexTerm());
        x72.setFollower(follower.getPeerId());
        CHECK:
        {
            if(follower.getIndex() == _SelfMachine.getIndex() || follower.getIndex() == INDEX_NAN) {
                // follower 已经同步 或 follower.next 未知
                break CHECK;
            }
            // follower.index < self.index
            List<LogEntry> entryList = new LinkedList<>();
            long preIndex = follower.getIndex();
            long preIndexTerm = follower.getIndexTerm();
            long next = preIndex + 1;
            if(next >= MIN_START) {
                // 存有数据的状态
                LogEntry nextLog;
                nextLog = _RaftMapper.getEntry(next);
                if(nextLog == null) {
                    _Logger.warning("leader truncate prefix，%#x wait for installing snapshot", follower.getPeerId());
                    break CHECK;
                }
                else {
                    x72.setPreIndex(preIndex);
                    x72.setPreIndexTerm(preIndexTerm);
                }
            }
            else {
                /*
                 * follower.index == 0
                 * follower是以空数据状态启动。
                 */
                x72.setPreIndex(0);
                x72.setPreIndexTerm(0);
            }
            for(long l = _SelfMachine.getIndex(), payload_size = 0;
                next <= l && payload_size < _SnapshotFragmentMaxSize; next++) {
                if(limit > 0 && entryList.size() >= limit) {
                    break;
                }
                LogEntry nextLog = _RaftMapper.getEntry(next);
                follower.setIndex(nextLog.getIndex());
                follower.setIndexTerm(nextLog.getTerm());
                entryList.add(nextLog);
                payload_size += nextLog.dataLength();
            }
            x72.setPayload(JsonUtil.writeValueAsBytes(entryList));
        }
        return x72;
    }

    public boolean isInCongress()
    {
        return _RaftConfig.isInCongress();
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    public long generateId()
    {
        return _ZUid.getId();
    }

    public long getPeerId()
    {
        return _ZUid.getPeerId();
    }

    private X77_RaftNotify createNotify(LogEntry raftLog)
    {
        X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
        x77.setSubSerial(raftLog.getSubSerial());
        x77.setPayload(raftLog.getPayload());
        x77.setOrigin(raftLog.getOrigin());
        return x77;
    }

    public RaftNode getLeader()
    {
        if(getMachine().getLeader() != INVALID_PEER_ID) {
            return _RaftConfig.findById(getMachine().getLeader());
        }
        return null;
    }

}
