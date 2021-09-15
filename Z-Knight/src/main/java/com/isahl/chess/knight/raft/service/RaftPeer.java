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

package com.isahl.chess.knight.raft.service;

import com.isahl.chess.bishop.io.ws.zchat.model.ZCommand;
import com.isahl.chess.bishop.io.ws.zchat.model.command.raft.*;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.IPair;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.features.IRaftMachine;
import com.isahl.chess.knight.raft.features.IRaftMapper;
import com.isahl.chess.knight.raft.features.IRaftMessage;
import com.isahl.chess.knight.raft.features.IRaftService;
import com.isahl.chess.knight.raft.model.*;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.tasks.features.ILocalPublisher;
import com.isahl.chess.queen.io.core.features.cluster.IClusterTimer;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISessionManager;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.lmax.disruptor.RingBuffer;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.isahl.chess.king.env.ZUID.INVALID_PEER_ID;
import static com.isahl.chess.knight.raft.features.IRaftMachine.INDEX_NAN;
import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;
import static com.isahl.chess.knight.raft.model.RaftCode.*;
import static com.isahl.chess.knight.raft.model.RaftState.*;
import static java.lang.Math.min;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftPeer
        implements IValid,
                   IRaftService,
                   IClusterTimer
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final ZUID                      _ZUid;
    private final IRaftConfig               _RaftConfig;
    private final IRaftMapper               _RaftMapper;
    private final TimeWheel                 _TimeWheel;
    private final RaftGraph                 _RaftGraph;
    private final RaftMachine               _SelfMachine;
    private final Queue<LogEntry>           _LogQueue = new LinkedList<>();
    private final Random                    _Random   = new Random();
    private final long                      _SnapshotFragmentMaxSize;
    private final ScheduleHandler<RaftPeer> _ElectSchedule, _HeartbeatSchedule, _TickSchedule;

    private ILocalPublisher mClusterPublisher;
    private ICancelable     mElectTask, mHeartbeatTask, mTickTask;

    public RaftPeer(TimeWheel timeWheel, IRaftConfig raftConfig, IRaftMapper raftMapper)
    {
        _TimeWheel = timeWheel;
        _RaftConfig = raftConfig;
        _ZUid = raftConfig.createZUID();
        _RaftMapper = raftMapper;
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
        trigger(_SelfMachine.createLeader());
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

    public void start(final IClusterNode _Node)
    {
        init();
        mClusterPublisher = _Node;
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
                            _Node.setupPeer(remote.getHost(), remote.getPort());
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
                            _Node.setupGate(remote.getGateHost(), remote.getGatePort());
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
        trigger(_SelfMachine.createCandidate());
    }

    private X71_RaftBallot ballot()
    {
        X71_RaftBallot vote = new X71_RaftBallot(_ZUid.getId());
        vote.setElectorId(_SelfMachine.getPeerId());
        vote.setTerm(_SelfMachine.getTerm());
        vote.setIndex(_SelfMachine.getIndex());
        vote.setIndexTerm(_SelfMachine.getIndexTerm());
        vote.setCandidateId(_SelfMachine.getCandidate());
        return vote;
    }

    private X73_RaftAccept accept()
    {
        X73_RaftAccept accept = new X73_RaftAccept(_ZUid.getId());
        accept.setFollowerId(_SelfMachine.getPeerId());
        accept.setTerm(_SelfMachine.getTerm());
        accept.setCatchUp(_SelfMachine.getIndex());
        accept.setCatchUpTerm(_SelfMachine.getIndexTerm());
        accept.setLeaderId(_SelfMachine.getLeader());
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

    private IControl[] rejectThenVote(ISessionManager manager)
    {

        vote4me();
        return createVotes(manager, Function.identity()).toArray(IControl[]::new);
    }

    private X71_RaftBallot stepUp(long term, long candidate)
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
            leadCancel();
            electCancel();
            _SelfMachine.beFollower(term, _RaftMapper);
            mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        }
        else {_Logger.warning("step down [ignore],state has already changed to FOLLOWER");}
    }

    private void stepDown()
    {
        trigger(_SelfMachine.createFollower());
    }

    private IControl follow(long term, long leader, long commit, long preIndex, long preIndexTerm)
    {
        tickCancel();
        _SelfMachine.follow(term, leader, _RaftMapper);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        if(catchUp(preIndex, preIndexTerm)) {
            _SelfMachine.commit(commit, _RaftMapper);
            return accept();
        }
        else {
            _Logger.debug("catch up failed reject → %#x ", leader);
            return reject(CONFLICT, leader);
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
        mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
        _Logger.info("be leader=>%s", _SelfMachine);
    }

    private void tickCancel()
    {
        if(mTickTask != null) {
            mTickTask.cancel();
        }
    }

    private void leadCancel()
    {
        if(mHeartbeatTask != null) {
            mHeartbeatTask.cancel();
        }
    }

    private void electCancel()
    {
        if(mElectTask != null) {
            mElectTask.cancel();
        }
    }

    private IControl[] lowTerm(long term, long peerId)
    {
        return _SelfMachine.getTerm() > term ? new X74_RaftReject[]{ reject(LOWER_TERM, peerId) } : null;
    }

    private boolean highTerm(long term)
    {
        return term > _SelfMachine.getTerm();
    }

    public IPair elect(long term, long index, long indexTerm, long candidate, long commit, ISessionManager manager)
    {
        RaftMachine peerMachine = getMachine(candidate, term);
        if(peerMachine == null) {return null;}
        peerMachine.setIndex(index);
        peerMachine.setIndexTerm(indexTerm);
        peerMachine.setCommit(commit);
        peerMachine.setCandidate(candidate);
        peerMachine.setState(CANDIDATE);
        IControl[] response = lowTerm(term, candidate);
        if(response != null) {return new Pair<>(response, null);}
        if(highTerm(term)) {
            _Logger.debug("elect {step down [%s → follower]}", _SelfMachine.getState());
            stepDown(term);
        }
        if(_SelfMachine.getState() == FOLLOWER) {
            if(_SelfMachine.getIndex() <= index && _SelfMachine.getIndexTerm() <= indexTerm &&
               _SelfMachine.getCommit() <= commit)
            {
                _Logger.debug("new term [ %d ] follower → elector | candidate: %#x", term, candidate);
                X71_RaftBallot vote = stepUp(term, candidate);
                return new Pair<>(new X71_RaftBallot[]{ vote }, null);
            }
            else {
                _Logger.debug(
                        "less than me; reject[%s]  mine:%d@%d > candidate:%d@%d then vote[ follower → candidate] |",
                        OBSOLETE,
                        _SelfMachine.getIndex(),
                        _SelfMachine.getIndexTerm(),
                        index,
                        indexTerm);
                return new Pair<>(rejectThenVote(manager), null);
            }
        }
        // elector|leader|candidate,one of these states ，candidate != INDEX_NAN 不需要重复判断
        else if(_SelfMachine.getCandidate() != candidate) {
            _Logger.debug("already vote [elector ×] | vote for:%#x not ♂ %#x", _SelfMachine.getCandidate(), candidate);
            return new Pair<>(new X74_RaftReject[]{ reject(ALREADY_VOTE, candidate) }, null);
        }
        return null;
    }

    public IPair ballot(long term,
                        long elector,
                        long catchUpIndex,
                        long catchUpTerm,
                        long commit,
                        ISessionManager manager)
    {
        RaftMachine peerMachine = getMachine(elector, term);
        if(peerMachine == null) {return null;}
        peerMachine.setIndex(catchUpIndex);
        peerMachine.setIndexTerm(catchUpTerm);
        peerMachine.setCommit(commit);
        peerMachine.setCandidate(_SelfMachine.getPeerId());
        peerMachine.setState(ELECTOR);
        IControl[] response = lowTerm(term, elector);
        if(response != null) {return new Pair<>(response, null);}
        if(highTerm(term)) {
            _Logger.debug("ballot {step down [%s → follower]}", _SelfMachine.getState());
            stepDown(term);
        }
        else if(_SelfMachine.getState() == CANDIDATE &&
                _RaftGraph.isMajorAcceptCandidate(_SelfMachine.getPeerId(), _SelfMachine.getTerm()))
        {
            beLeader();
            return new Pair<>(createAppends(manager, Function.identity()).toArray(IControl[]::new), null);
        }
        return null;
    }

    public IPair onResponse(long term, long preIndex, long preIndexTerm, long leader, long commit)
    {
        RaftMachine peerMachine = getMachine(leader, term);
        if(peerMachine == null) {return null;}
        peerMachine.setIndex(preIndex);
        peerMachine.setIndexTerm(preIndexTerm);
        peerMachine.setCommit(commit);
        peerMachine.setCandidate(_SelfMachine.getPeerId());
        peerMachine.setState(LEADER);
        IControl[] response = lowTerm(term, leader);
        if(response != null) {return new Pair<>(response, null);}
        if(highTerm(term)) {
            _Logger.debug(" → append {step down [%s → follower]}", _SelfMachine.getState());
            stepDown(term);
        }
        if(_SelfMachine.getState()
                       .getCode() < LEADER.getCode())
        {
            return new Pair<>(new IControl[]{ follow(term, leader, commit, preIndex, preIndexTerm) }, null);
        }
        // leader
        else {
            _Logger.warning("state:[%s], leader[%#x] → the other [%#x] ",
                            _SelfMachine.getState(),
                            _SelfMachine.getCandidate(),
                            leader);
            return new Pair<>(new IControl[]{ reject(SPLIT_CLUSTER, leader) }, null);
        }
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

    public LogEntry getLogEntry(long index)
    {
        _Logger.debug("peer get log entry: %d", index);
        return _RaftMapper.getEntry(index);
    }

    /**
     * follower → leader
     *
     * @return result- pair
     * first : broadcast to peers
     * second : resp to origin
     */
    public IPair onAccept(long term, long index, long indexTerm, long follower, ISessionManager manager)
    {
        RaftMachine peerMachine = getMachine(follower, term);
        if(peerMachine == null) {return null;}
        peerMachine.setState(FOLLOWER);
        peerMachine.setIndex(index);
        peerMachine.setIndexTerm(indexTerm);
        peerMachine.setApplied(index);
        peerMachine.setMatchIndex(index);
        peerMachine.setLeader(_SelfMachine.getPeerId());
        peerMachine.setCandidate(_SelfMachine.getPeerId());
        /*
         * follower.match_index > leader.commit 完成半数 match 之后只触发一次 leader commit
         */
        long nextCommit = _SelfMachine.getCommit() + 1;
        if(peerMachine.getApplied() > _SelfMachine.getCommit() &&
           _RaftGraph.isMajorAcceptLeader(_SelfMachine.getPeerId(), _SelfMachine.getTerm(), nextCommit))
        {
            // peerMachine.getIndex() > _SelfMachine.getCommit()时，raftLog 不可能为 null
            _SelfMachine.commit(nextCommit, _RaftMapper);
            _Logger.debug("leader commit: %d @%d", nextCommit, _SelfMachine.getTerm());
            LogEntry raftLog = _RaftMapper.getEntry(nextCommit);
            X79_RaftAdjudge x79 = createAdjudge(raftLog);
            if(raftLog.getClient() != _SelfMachine.getPeerId()) {
                // leader -> follower -> client
                ISession raftClient = manager.findSessionByPrefix(raftLog.getClient());
                if(raftClient != null) {
                    return new Pair<>(new IControl[]{ createNotify(raftLog, raftClient) }, x79);
                }
            }
            return new Pair<>(null, x79);
        }
        return null;
    }

    public IPair onReject(long term, long index, long indexTerm, long from, int code, int state)
    {
        RaftMachine peerMachine = getMachine(from, term);
        if(peerMachine == null) {return null;}
        peerMachine.setIndex(index);
        peerMachine.setIndexTerm(indexTerm);
        peerMachine.setCandidate(INVALID_PEER_ID);
        peerMachine.setLeader(INVALID_PEER_ID);
        peerMachine.setState(RaftState.valueOf(state));
        if(highTerm(term)) {
            _Logger.debug(" → reject {step down [%s → follower]}", _SelfMachine.getState());
            stepDown(term);
        }
        else {
            STEP_DOWN:
            {
                switch(RaftCode.valueOf(code)) {
                    case CONFLICT:
                        if(_SelfMachine.getState() == LEADER) {
                            _Logger.debug("follower %#x,match failed,rollback %d@%d",
                                          peerMachine.getPeerId(),
                                          peerMachine.getIndex(),
                                          peerMachine.getIndexTerm());
                            return new Pair<>(new X72_RaftAppend[]{ createAppend(peerMachine, 1) }, null);
                        }
                        else {
                            _Logger.warning("self %#x is old leader & send logs → %#x,next-index wasn't catchup",
                                            _SelfMachine.getPeerId(),
                                            peerMachine.getPeerId());
                            // Ignore
                        }
                        break;
                    case SPLIT_CLUSTER:
                        if(_SelfMachine.getState() == LEADER) {
                            _Logger.debug("other leader:[%#x]", peerMachine.getPeerId());
                        }
                        break;
                    case ALREADY_VOTE:
                        if(_SelfMachine.getState() == CANDIDATE) {
                            _Logger.debug("elector[%#x] has vote", peerMachine.getPeerId());
                        }
                        break;
                    case OBSOLETE:
                        if(_SelfMachine.getState()
                                       .getCode() > FOLLOWER.getCode())
                        {
                            stepDown(_SelfMachine.getTerm());
                        }
                        break STEP_DOWN;
                }
                if(_RaftGraph.isMajorReject(_SelfMachine.getPeerId(), _SelfMachine.getTerm())) {
                    stepDown(_SelfMachine.getTerm());
                }
            }
        }
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
                if(entry != null) {result.add(entry);}
            }
            return result.isEmpty() ? null : result;
        }
    }

    private boolean catchUp(long preIndex, long preIndexTerm)
    {
        CHECK:
        {
            if(_SelfMachine.getIndex() == preIndex && _SelfMachine.getIndexTerm() == preIndexTerm) {
            /* follower 与 Leader 状态一致
               开始同步接受缓存在_LogQueue中的数据
             */
                for(Iterator<LogEntry> it = _LogQueue.iterator(); it.hasNext(); ) {
                    LogEntry entry = it.next();
                    if(_RaftMapper.append(entry)) {
                        _SelfMachine.append(entry.getIndex(), entry.getTerm(), _RaftMapper);
                        _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                    }
                    it.remove();
                }
                break CHECK;
            }
            else if(_SelfMachine.getIndex() == 0 && preIndex == 0) {
                // 初始态，raft-machine 中不包含任何 log 记录
                _Logger.info("self machine is empty");
                break CHECK;
            }
            else if(_SelfMachine.getIndex() < preIndex) {
                /*
                 * 1.follower 未将自身index 同步给leader，所以leader 发送了一个最新状态过来
                 * 后续执行accept() → response 会将index 同步给leader。
                 * 2.leader 已经获知follower的需求 但是 self.index < leader.start
                 * 需要follower先完成snapshot的安装才能正常同步。
                 */
                _Logger.debug("leader doesn't know my next  || leader hasn't log,need to install snapshot");
            }
            else if(_SelfMachine.getIndex() > preIndex) {
                /* 此时_SelfMachine.term 与 leader 持平
                 * 新进入集群的节点数据更多，需要重新选举
                 */
                if(_SelfMachine.getIndexTerm() >= preIndexTerm) {
                    _Logger.warning("peer [%#x] join into cluster has history,some data has lost!!",
                                    _SelfMachine.getPeerId());
                }
                else {//_SelfMachine.getIndexTerm() < preIndexTerm
                    if(rollback(_LogQueue, preIndex)) {
                        break CHECK;
                    }
                }
            }
            else { // _SelfMachine.index == preIndex && _SelfMachine.indexTerm!=preIndexTerm
                if(rollback(_LogQueue, preIndex)) {
                    break CHECK;
                }
            }
            _LogQueue.clear();
            return false;
        }
        _LogQueue.clear();
        return true;
    }

    private boolean rollback(final Queue<LogEntry> _LogQueue, long preIndex)
    {
        long newEndIndex = preIndex - 1;
        if(newEndIndex <= 0) {
            //preIndex == 1 || preIndex ==0
            _SelfMachine.reset();
            _RaftMapper.reset();
            if(_LogQueue.isEmpty()) {
                if(preIndex == 0) {
                    _Logger.debug("empty leader → clean!");
                    return true;
                }
                // preIndex == 1 → reject
            }
            else {//刚好follow是空的，leader投过来的数据就接收了
                LogEntry entry = _LogQueue.poll();
                if(_RaftMapper.append(entry)) {
                    //mapper.append 包含了preIndex ==1 && entry.index ==1 && newEndIndex == 0
                    _SelfMachine.append(entry.getIndex(), entry.getTerm(), _RaftMapper);
                    _SelfMachine.apply(_RaftMapper);
                    _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                    return true;
                }
            }
        }
        else {
            // newEndIndex >= MIN_START(1)
            LogEntry rollback = _RaftMapper.truncateSuffix(newEndIndex);
            if(rollback != null) {
                _SelfMachine.rollBack(rollback.getIndex(), rollback.getTerm(), _RaftMapper);
                _Logger.debug("machine rollback %d@%d", rollback.getIndex(), rollback.getTerm());
            }
            else if(newEndIndex >= _RaftMapper.getStartIndex()) {
                _Logger.fetal("lost data → reset ");
                _SelfMachine.reset();
                _RaftMapper.reset();
            }
        }
        return false;
    }

    public List<ITriple> turnToFollower(RaftMachine update)
    {
        CHECK:
        {
            if(_SelfMachine.getState()
                           .getCode() > FOLLOWER.getCode() && _SelfMachine.getState()
                                                                          .getCode() < LEADER.getCode() &&
               update.getTerm() >= _SelfMachine.getTerm())
            {
                _Logger.debug("elect time out");
                stepDown(update.getTerm());
                break CHECK;
            }
            _Logger.warning("elect time out event [ignore]");
        }
        return null;
    }

    public <T> List<T> checkVoteState(RaftMachine update, ISessionManager manager, Function<ZCommand, T> mapper)
    {
        if(update.getTerm() > _SelfMachine.getTerm() && update.getIndex() == _SelfMachine.getIndex() &&
           update.getIndexTerm() == _SelfMachine.getIndexTerm() && update.getCandidate() == _SelfMachine.getPeerId() &&
           update.getCommit() == _SelfMachine.getCommit() && _SelfMachine.getState() == FOLLOWER)
        {
            vote4me();
            return createVotes(manager, mapper);
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
            mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
            return createAppends(manager, mapper);
        }
        // state change => ignore
        _Logger.warning("check leader heartbeat failed; now:%s", _SelfMachine);
        return null;
    }

    public void receiveLogs(List<LogEntry> logList)
    {
        if(logList == null || logList.isEmpty()) {return;}
        _LogQueue.addAll(logList);
    }

    public <T extends IConsistent, R> List<R> onImmediate(T request,
                                                          ISessionManager manager,
                                                          Function<ZCommand, R> mapper)
    {
        byte[] payload = request.encode();
        List<R> cmds = append(request.serial(),
                              payload,
                              _SelfMachine.getPeerId(),
                              request.getOrigin(),
                              manager,
                              mapper);
        if(cmds == null) {
            stepDown(_SelfMachine.getTerm());
            return null;
        }
        else {return cmds;}
    }

    /**
     * @param subSerial  payload-z_protocol-serial
     * @param payload    payload data
     * @param client     raft client peer-id
     * @param origin     "device\request"-session-index
     * @param manager    session manager
     * @param raftClient raft client session
     * @return response in RaftCustom
     * pair.first [response → cluster]
     * pair.second [response → raft client]
     */
    public IPair onRequest(int subSerial,
                           byte[] payload,
                           long client,
                           long origin,
                           ISessionManager manager,
                           ISession raftClient)
    {
        List<ZCommand> cmds = append(subSerial, payload, client, origin, manager, Function.identity());
        if(cmds != null && !cmds.isEmpty()) {
            X76_RaftResp x76 = raftResp(SUCCESS, client, origin, subSerial, payload);
            x76.putSession(raftClient);
            return new Pair<>(cmds.toArray(ZCommand[]::new), x76);
        }
        else {
            X76_RaftResp x76 = raftResp(WAL_FAILED, client, origin, subSerial, payload);
            x76.putSession(raftClient);
            return new Pair<>(null, x76);
        }
    }

    public X76_RaftResp raftResp(RaftCode code, long client, long origin, int subSerial, byte[] payload)
    {
        X76_RaftResp x76 = new X76_RaftResp();
        x76.setClientId(client);
        x76.setOrigin(origin);
        x76.setSubSerial(subSerial);
        x76.putPayload(payload);
        x76.setCode((byte) code.getCode());
        return x76;
    }

    private <R> List<R> append(int subSerial,
                               byte[] content,
                               long client,
                               long origin,
                               ISessionManager manager,
                               Function<ZCommand, R> mapper)
    {
        _Logger.debug("leader append new log");
        LogEntry newEntry = new LogEntry(_SelfMachine.getTerm(),
                                         _SelfMachine.getIndex() + 1,
                                         client,
                                         origin,
                                         subSerial,
                                         content);
        newEntry.setOperation(IStorage.Operation.OP_INSERT);
        if(_RaftMapper.append(newEntry)) {
            _SelfMachine.append(newEntry.getIndex(), newEntry.getTerm(), _RaftMapper);
            _SelfMachine.apply(_RaftMapper);
            _Logger.debug("leader appended log %d@%d", newEntry.getIndex(), newEntry.getTerm());
            return createAppends(manager, mapper);
        }
        _Logger.fetal("RAFT WAL failed!");
        return null;
    }

    private <T> List<T> createVotes(ISessionManager manager, Function<ZCommand, T> mapper)
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
                                 x70.setCandidateId(_SelfMachine.getPeerId());
                                 x70.setTerm(_SelfMachine.getTerm());
                                 x70.setIndex(_SelfMachine.getIndex());
                                 x70.setIndexTerm(_SelfMachine.getIndexTerm());
                                 x70.setCommit(_SelfMachine.getCommit());
                                 x70.putSession(session);
                                 return x70;
                             }
                             _Logger.debug("elector :%#x session has not found", peerId);
                             return null;
                         })
                         .filter(Objects::nonNull)
                         .map(mapper)
                         .collect(Collectors.toList());
    }

    private <T> List<T> createAppends(ISessionManager manager, Function<ZCommand, T> mapper)
    {
        return _RaftGraph.getNodeMap()
                         .values()
                         .stream()
                         .filter(other->other.getPeerId() != _SelfMachine.getPeerId())
                         .map(follower->{
                             ISession session = manager.findSessionByPrefix(follower.getPeerId());
                             if(session == null) {
                                 _Logger.warning("not found follower:%#x session", follower.getPeerId());
                                 return null;
                             }
                             else {
                                 X72_RaftAppend x72 = createAppend(follower);
                                 x72.putSession(session);
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
        x72.setPreIndex(_SelfMachine.getIndex());
        x72.setPreIndexTerm(_SelfMachine.getIndexTerm());
        x72.setFollower(follower.getPeerId());
        CHECK:
        {
            long preIndex = follower.getIndex();
            long preIndexTerm = follower.getIndexTerm();
            if(preIndex == _SelfMachine.getIndex() || preIndex == INDEX_NAN) {
                // follower 已经同步 或 follower.next 未知
                break CHECK;
            }
            // preIndex < self.index && preIndex >= 0
            List<LogEntry> entryList = new LinkedList<>();
            long next = preIndex + 1;//next >= 1
            if(next > MIN_START) {
                if(next > _SelfMachine.getIndex()) {
                    //follower.index > leader.index, 后续用leader的数据进行覆盖
                    break CHECK;
                }
                // 存有数据的状态
                LogEntry matched;
                if((matched = _RaftMapper.getEntry(preIndex)) == null) {
                    _Logger.warning("match point %d@%d lost", preIndex, preIndexTerm);
                    break CHECK;
                }
                else {
                    if(matched.getIndex() == preIndex && matched.getTerm() == preIndexTerm) {
                        x72.setPreIndex(preIndex);
                        x72.setPreIndexTerm(preIndexTerm);
                    }
                    else {
                        _Logger.warning("matched %#x vs %#x no consistency;%d@%d",
                                        getPeerId(),
                                        follower.getPeerId(),
                                        preIndex,
                                        preIndexTerm);
                        break CHECK;
                    }
                }
            }
            else { // preIndex == 0
                /*
                 * follower是以空数据状态启动。
                 */
                x72.setPreIndex(0);
                x72.setPreIndexTerm(0);
            }
            for(long end = _SelfMachine.getIndex(), payloadSize = 0;
                next <= end && payloadSize < _SnapshotFragmentMaxSize; next++) {
                if(limit > 0 && entryList.size() >= limit) {
                    break;
                }
                LogEntry nextLog = _RaftMapper.getEntry(next);
                entryList.add(nextLog);
                payloadSize += nextLog.dataLength();
            }
            if(!entryList.isEmpty()) {
                /* 此处不检查 entryList.isEmpty() 也不会有有影响, 是因为
                 * JsonUtil 对ObjectMapper做了设定, 不包含Empty项目
                 */
                x72.putPayload(JsonUtil.writeValueAsBytes(entryList));
            }
        }
        return x72;
    }

    public boolean isInCongress()
    {
        return _RaftConfig.isInCongress();
    }

    public long getPeerId()
    {
        return _SelfMachine.getPeerId();
    }

    @Override
    public long generateId()
    {
        return _ZUid.getId();
    }

    @Override
    public RaftNode getLeader()
    {
        if(getMachine().getLeader() != INVALID_PEER_ID) {
            return _RaftConfig.findById(getMachine().getLeader());
        }
        return null;
    }

    @Override
    public void changeTopology(RaftNode delta, IStorage.Operation operation)
    {
        _RaftConfig.changeTopology(delta, operation);
        final RingBuffer<QEvent> _ConsensusApiEvent = mClusterPublisher.getPublisher(IOperator.Type.CLUSTER_TOPOLOGY);
        final ReentrantLock _ConsensusApiLock = mClusterPublisher.getLock(IOperator.Type.CLUSTER_TOPOLOGY);
        _ConsensusApiLock.lock();
        try {
            long sequence = _ConsensusApiEvent.next();
            try {
                QEvent event = _ConsensusApiEvent.get(sequence);
                event.produce(IOperator.Type.CLUSTER_TOPOLOGY, new Pair<>(delta, operation), null);
            }
            finally {
                _ConsensusApiEvent.publish(sequence);
            }
        }
        finally {
            _ConsensusApiLock.unlock();
        }
    }

    @Override
    public <T extends IStorage> void trigger(T content)
    {
        final RingBuffer<QEvent> _ConsensusEvent = mClusterPublisher.getPublisher(IOperator.Type.CLUSTER_TIMER);
        final ReentrantLock _ConsensusLock = mClusterPublisher.getLock(IOperator.Type.CLUSTER_TIMER);
        /*
        通过 Schedule thread-pool 进行 timer 执行, 排队执行。
         */
        _ConsensusLock.lock();
        try {
            long sequence = _ConsensusEvent.next();
            try {
                QEvent event = _ConsensusEvent.get(sequence);
                event.produce(IOperator.Type.CLUSTER_TIMER, new Pair<>(content, null), null);
            }
            finally {
                _ConsensusEvent.publish(sequence);
            }
        }
        finally {
            _ConsensusLock.unlock();
        }
    }

    @Override
    public List<RaftNode> getTopology()
    {
        return _RaftConfig.getPeers();
    }

    private X79_RaftAdjudge createAdjudge(LogEntry raftLog)
    {
        X79_RaftAdjudge x79 = new X79_RaftAdjudge(_ZUid.getId());
        x79.setOrigin(raftLog.getOrigin());
        x79.setIndex(raftLog.getIndex());
        x79.setClient(raftLog.getClient());
        x79.setSubSerial(raftLog.subSerial());
        x79.putPayload(raftLog.getContent());
        return x79;
    }

    private X77_RaftNotify createNotify(LogEntry raftLog, ISession session)
    {
        X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
        x77.setOrigin(raftLog.getOrigin());
        x77.setIndex(raftLog.getIndex());
        x77.setClient(raftLog.getClient());
        x77.putSession(session);
        return x77;
    }
}
