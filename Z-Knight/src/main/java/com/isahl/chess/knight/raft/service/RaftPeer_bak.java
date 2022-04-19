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

import com.isahl.chess.bishop.protocol.zchat.model.command.raft.*;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.features.functions.IOperator;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.knight.raft.config.IRaftConfig;
import com.isahl.chess.knight.raft.features.IRaftControl;
import com.isahl.chess.knight.raft.features.IRaftMapper;
import com.isahl.chess.knight.raft.features.IRaftService;
import com.isahl.chess.knight.raft.model.*;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.knight.raft.model.replicate.LogMeta;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.cluster.IClusterTimer;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.lmax.disruptor.RingBuffer;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.*;
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
public class RaftPeer_bak
        implements IValid,
                   IRaftService,
                   IClusterTimer
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final ZUID            _ZUid;
    private final IRaftConfig     _RaftConfig;
    private final IRaftMapper     _RaftMapper;
    private final TimeWheel       _TimeWheel;
    private final RaftGraph       _SelfGraph;
    private final RaftMachine     _SelfMachine;
    private final RaftGraph       _JointGraph;
    private final RaftMachine     _JointMachine;
    private final Queue<LogEntry> _RecvLogQueue = new LinkedList<>();
    private final Random          _Random       = new Random();
    private final long            _SnapshotFragmentMaxSize;

    /*
     * key(Long) → msgId
     * value(X75_RaftReq) → request
     */
    private final Map<Long, X75_RaftReq>        _Cached = new TreeMap<>();
    private final ScheduleHandler<RaftPeer_bak> _ElectSchedule, _HeartbeatSchedule, _TickSchedule, _CleanSchedule;

    private IClusterNode mClusterNode;
    private RaftState    mState = FOLLOWER;
    private ICancelable  mElectTask, mHeartbeatTask, mTickTask;

    public RaftPeer_bak(TimeWheel timeWheel, IRaftConfig raftConfig, IRaftMapper raftMapper)
    {
        _TimeWheel = timeWheel;
        _RaftConfig = raftConfig;
        _ZUid = raftConfig.getZUID();
        _RaftMapper = raftMapper;
        _ElectSchedule = new ScheduleHandler<>(_RaftConfig.getElectInSecond(), RaftPeer_bak::stepDown);
        _HeartbeatSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond(), RaftPeer_bak::heartbeat);
        _TickSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond()
                                                         .multipliedBy(2), RaftPeer_bak::restart);
        _CleanSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond(), true, RaftPeer_bak::cleanCache);
        _SelfGraph = new RaftGraph();
        _JointGraph = new RaftGraph();
        _SelfMachine = new RaftMachine(_ZUid.getPeerId());
        _JointMachine = new RaftMachine(_ZUid.getPeerId());
        _SelfGraph.append(_SelfMachine);
        _JointGraph.append(_JointMachine);
        _SnapshotFragmentMaxSize = _RaftConfig.getSnapshotFragmentMaxSize();
    }

    private void heartbeat()
    {
        trigger(_SelfMachine.createLeader());
    }

    private void cleanCache()
    {
        for(Iterator<Map.Entry<Long, X75_RaftReq>> it = _Cached.entrySet()
                                                               .iterator(); it.hasNext(); ) {
            Map.Entry<Long, X75_RaftReq> entry = it.next();
            if(entry.getValue().tCached) {it.remove();}
            else {entry.getValue().tCached = true;}
        }
    }

    public void start(final IClusterNode _Node)
    {
        LogMeta meta = _RaftMapper.getLogMeta();
        _SelfMachine.setTerm(meta.getTerm());
        _SelfMachine.commit(meta.getCommit());
        _SelfMachine.accept(meta.getApplied());
        _SelfMachine.index(meta.getIndex());
        _SelfMachine.indexTerm(meta.getIndexTerm());
        _SelfMachine.matchIndex(meta.getApplied());
        mClusterNode = _Node;
        if(_RaftConfig.isClusterMode()) {
            // 启动集群连接
            installGraph(_SelfGraph, _RaftConfig.getPeers(), _RaftConfig.getGates());
            // 周期性检查 leader 是否存在
            mTickTask = _TimeWheel.acquire(RaftPeer_bak.this, _TickSchedule);
            _SelfMachine.state(_RaftConfig.isInCongress() ? FOLLOWER : CLIENT);
            _Logger.info("raft node init -> %s", _SelfMachine);
        }
        else {
            _SelfMachine.state(CLIENT);
            _Logger.info("not in congress, single model!");
        }
    }

    private void installGraph(RaftGraph main, Iterable<RaftNode> peers, Iterable<RaftNode> gates)
    {
        if(peers != null) {
            for(RaftNode remote : peers) {
                long peerId = remote.getId();
                // Graph中 装入所有需要管理的集群状态机，self已经装入了，此处不再重复
                if(peerId != _SelfMachine.peer()) {
                    main.append(new RaftMachine(peerId));
                }
                // 仅连接NodeId<自身的节点
                if(peerId < _SelfMachine.peer()) {
                    try {
                        mClusterNode.setupPeer(remote.getHost(), remote.getPort());
                        _Logger.info("->peer : %s:%d", remote.getHost(), remote.getPort());
                    }
                    catch(Exception e) {
                        _Logger.warning("peer connect error: %s:%d", e, remote.getHost(), remote.getPort());
                    }
                }
            }
        }
        if(gates != null) {
            for(RaftNode remote : gates) {
                long gateId = remote.getId();
                if(_ZUid.isTheGate(gateId)) {
                    try {
                        mClusterNode.setupGate(remote.getGateHost(), remote.getGatePort());
                        _Logger.info("->gate : %s:%d", remote.getGateHost(), remote.getGatePort());
                    }
                    catch(Exception e) {
                        _Logger.warning("gate connect error: %s:%d", e, remote.getGateHost(), remote.getGatePort());
                    }
                }
            }
        }
    }

    public void installSnapshot(List<IRaftControl> snapshot)
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
        if(_SelfMachine.accept() <= _SelfMachine.commit()) {
            _Logger.debug(" applied < commit sync is running");
            return;
        }
        if(_SelfMachine.accept() >= _RaftMapper.getStartIndex() &&
           _SelfMachine.accept() <= _RaftMapper.getEndIndex())
        {
            localTerm = _RaftMapper.getEntryTerm(_SelfMachine.accept());
            _RaftMapper.updateSnapshotMeta(_SelfMachine.accept(), localTerm);
            _Logger.debug("take snapshot");
        }
        long lastSnapshotIndex = _RaftMapper.getSnapshotMeta()
                                            .getCommit();
        if(lastSnapshotIndex > 0 && _RaftMapper.getStartIndex() <= lastSnapshotIndex) {
            _RaftMapper.truncatePrefix(lastSnapshotIndex + 1);
            _Logger.debug("snapshot truncate prefix %d", lastSnapshotIndex);
        }

    }

    private void restart()
    {
        /*
         * 关闭TickTask,此时执行容器可能为ElectTask 或 TickTask自身
         * 由于Elect.timeout << Tick.timeout,此处不应出现Tick无法
         * 关闭的，或关闭异常。同时cancel 配置了lock 防止意外出现。
         */
        tickCancel();
        if(_SelfMachine.state() == CLIENT) {

        }

    }

    /**
     * 此方法会在各种超时处理器中被启用，所以执行线程为TimeWheel.pool中的任意子线程
     */
    private void startVote()
    {

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

    private X71_RaftBallot ballot(long msgId)
    {
        X71_RaftBallot vote = new X71_RaftBallot(msgId);
        vote.setElector(_SelfMachine.peer());
        vote.setTerm(_SelfMachine.term());
        vote.setIndex(_SelfMachine.index());
        vote.setIndexTerm(_SelfMachine.indexTerm());
        vote.setCandidate(_SelfMachine.candidate());
        return vote;
    }

    private X73_RaftAccept accept(long msgId)
    {
        X73_RaftAccept accept = new X73_RaftAccept(msgId);
        accept.setFollower(_SelfMachine.peer());
        accept.setTerm(_SelfMachine.term());
        accept.setCatchUp(_SelfMachine.index());
        accept.setCatchUpTerm(_SelfMachine.indexTerm());
        accept.setLeader(_SelfMachine.leader());
        return accept;
    }

    private X74_RaftReject reject(RaftCode raftCode, long rejectTo, long msgId)
    {
        X74_RaftReject reject = new X74_RaftReject(msgId);
        reject.setPeer(_SelfMachine.peer());
        reject.setTerm(_SelfMachine.term());
        reject.setIndex(_SelfMachine.index());
        reject.setIndexTerm(_SelfMachine.indexTerm());
        reject.setReject(rejectTo);
        reject.setCode(raftCode.getCode());
        reject.setState(_SelfMachine.state()
                                    .getCode());
        return reject;
    }

    private List<ITriple> rejectThenVote(IManager manager)
    {
        vote4me();
        return createVotes(manager);
    }

    private X71_RaftBallot stepUp(long term, long candidate, long msgId)
    {
        tickCancel();
        _SelfMachine.beElector(candidate, term, _RaftMapper);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("[follower → elector] %s", _SelfMachine);
        return ballot(msgId);
    }

    private void stepDown(long term)
    {
        if(_SelfMachine.state()
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

    private IProtocol follow(long term, long leader, long commit, long preIndex, long preIndexTerm, long msgId)
    {
        tickCancel();
        _SelfMachine.follow(term, leader, _RaftMapper);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        if(catchUp(preIndex, preIndexTerm)) {
            _SelfMachine.commit(commit, _RaftMapper);
            return accept(msgId);
        }
        else {
            _Logger.debug("catch up failed reject → %#x ", leader);
            return reject(CONFLICT, leader, msgId);
        }
    }

    private void vote4me()
    {
        tickCancel();
        _SelfMachine.beCandidate(_RaftMapper);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("vote4me follower → candidate %s", _SelfMachine.toSimple());
    }

    private void beLeader()
    {
        electCancel();
        _SelfMachine.beLeader(_RaftMapper);
        mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
        _Logger.info("be leader → %s", _SelfMachine.toSimple());
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

    private boolean highTerm(long term)
    {
        return term > _SelfMachine.term();
    }

    private boolean lowTerm(long term)
    {
        return term < _SelfMachine.term();
    }

    public ITriple onVote(X70_RaftVote x70, IManager manager, ISession session)
    {
        RaftMachine peerMachine = getMachine(x70.getCandidate(), x70.getTerm());
        if(peerMachine == null) {return null;}
        peerMachine.index(x70.getIndex());
        peerMachine.indexTerm(x70.getIndexTerm());
        peerMachine.commit(x70.getAccept());
        peerMachine.candidate(x70.getCandidate());
        peerMachine.state(CANDIDATE);
        if(lowTerm(x70.getTerm())) {// reject election term < my term
            _Logger.debug("elect {low term: reject %#x,mine:[%d@%d] candidate:[%d@%d]}",
                          x70.getCandidate(),
                          _SelfMachine.index(),
                          _SelfMachine.term(),
                          peerMachine.index(),
                          peerMachine.term());
            return Triple.of(reject(LOWER_TERM, x70.getCandidate(), x70.getMsgId()).with(session), null, SINGLE);
        }
        if(highTerm(x70.getTerm())) { // term > my term
            _Logger.debug("elect {step down [%#x → follower]}", _SelfMachine.peer());
            stepDown(x70.getTerm());
        }
        if(_SelfMachine.state() == FOLLOWER) {
            //@formatter:off
            if(_SelfMachine.index() <= x70.getIndex() &&
               _SelfMachine.indexTerm() <= x70.getIndexTerm() &&
               _SelfMachine.commit() <= x70.getAccept())
            //@formatter:on
            {
                _Logger.debug("new term [ %d ] follower[%#x] → elector | candidate: %#x",
                              x70.getTerm(),
                              _SelfMachine.peer(),
                              x70.getCandidate());
                IProtocol ballot = stepUp(x70.getTerm(), x70.getCandidate(), x70.getMsgId());
                return Triple.of(ballot.with(session), null, SINGLE);
            }
            else {
                _Logger.debug(
                        "less than me; reject[%s]  mine:[%d@%d] greater than candidate:[%d@%d] then vote[ follower → candidate] |",
                        OBSOLETE,
                        _SelfMachine.index(),
                        _SelfMachine.term(),
                        x70.getIndex(),
                        x70.getIndexTerm());
                return Triple.of(rejectThenVote(manager), null, BATCH);
            }
        }
        // elector|leader|candidate,one of these states ，candidate != INDEX_NAN 不需要重复判断
        else if(_SelfMachine.candidate() != x70.getCandidate()) {
            _Logger.debug("already vote [elector ×] | vote for:%#x not ♂ %#x",
                          _SelfMachine.candidate(),
                          x70.getCandidate());
            return Triple.of(reject(ALREADY_VOTE, x70.getCandidate(), x70.getMsgId()).with(session), null, SINGLE);
        }
        return null;
    }

    public ITriple onBallot(X71_RaftBallot x71, IManager manager, ISession session)
    {
        RaftMachine peerMachine = getMachine(x71.getElector(), x71.getTerm());
        if(peerMachine == null) {return null;}
        peerMachine.index(x71.getIndex());
        peerMachine.indexTerm(x71.getIndexTerm());
        peerMachine.commit(x71.getCommit());
        peerMachine.candidate(_SelfMachine.peer());
        peerMachine.state(ELECTOR);
        //@formatter:off
        if(_SelfMachine.state() == CANDIDATE &&
           _SelfMachine.term() ==  x71.getTerm() &&
           _SelfGraph.isMajorAcceptCandidate(_SelfMachine.peer(), _SelfMachine.term()))
        //@formatter:on
        { //term == my term
            beLeader();
            return Triple.of(createAppends(manager), null, BATCH);
        }
        else if(highTerm(x71.getTerm())) { //term > my term
            _Logger.debug("ballot {step down [%s → follower] %s}", _SelfMachine.state(), _SelfMachine.toSimple());
            stepDown(x71.getTerm());
        }
        else { //term < my term
            _Logger.debug("ballot {reject %#x,mine:[%d@%d] candidate:[%d@%d]}",
                          x71.getElector(),
                          _SelfMachine.index(),
                          _SelfMachine.term(),
                          peerMachine.index(),
                          peerMachine.term());
            return Triple.of(reject(LOWER_TERM, x71.getElector(), x71.getMsgId()).with(session), null, SINGLE);
        }
        return null;
    }

    public ITriple onAppend(X72_RaftAppend x72, ISession session)
    {
        RaftMachine peerMachine = getMachine(x72.getLeader(), x72.getTerm());
        if(peerMachine == null) {return null;}
        peerMachine.index(x72.getPreIndex());
        peerMachine.indexTerm(x72.getPreIndexTerm());
        peerMachine.commit(x72.getCommit());
        peerMachine.candidate(_SelfMachine.peer());
        peerMachine.state(LEADER);
        if(lowTerm(x72.getTerm())) {
            _Logger.debug("append {low term from:[%#x]}", x72.getTerm());
            return Triple.of(reject(LOWER_TERM, x72.getLeader(), x72.getMsgId()).with(session), null, SINGLE);
        }
        if(highTerm(x72.getTerm())) {
            _Logger.debug(" → append {step down [%s → follower] %s}", _SelfMachine.state(), _SelfMachine.toSimple());
            stepDown(x72.getTerm());
        }
        //term == my term
        if(_SelfMachine.state()
                       .getCode() < LEADER.getCode())
        {
            //follower | elector
            if(x72.payload() != null) {
                ListSerial<LogEntry> logs = new ListSerial<>(LogEntry::new);
                logs.decode(x72.subEncoded());
                receiveLogs(logs);
            }
            return Triple.of(follow(x72.getTerm(),
                                    x72.getLeader(),
                                    x72.getCommit(),
                                    x72.getPreIndex(),
                                    x72.getPreIndexTerm(),
                                    x72.getMsgId()).with(session), null, SINGLE);
        }
        // leader
        else {
            _Logger.warning("state:[%s], leader[%#x] → the other [%#x] ",
                            _SelfMachine.state(),
                            _SelfMachine.candidate(),
                            x72.getLeader());
            return Triple.of(reject(SPLIT_CLUSTER, x72.getLeader(), x72.getMsgId()).with(session), null, SINGLE);
        }
    }

    private RaftMachine getMachine(long peerId, long term)
    {
        RaftMachine peerMachine = _SelfGraph.get(peerId);
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
     * @return result triple
     * fst : broadcast to peer
     * snd : leader handle x79,adjudge
     * trd : operator type 「 SINGLE/BATCH 」
     */
    public ITriple onAccept(X73_RaftAccept x73, IManager manager)
    {
        RaftMachine peerMachine = getMachine(x73.getFollower(), x73.getTerm());
        if(peerMachine == null) {return null;}
        peerMachine.state(FOLLOWER);
        peerMachine.index(x73.getCatchUp());
        peerMachine.indexTerm(x73.getCatchUpTerm());
        peerMachine.accept(x73.getCatchUp());
        peerMachine.matchIndex(x73.getCatchUp());
        peerMachine.leader(_SelfMachine.peer());
        peerMachine.candidate(_SelfMachine.peer());
        /*
         * follower.match_index > leader.commit 完成半数 match 之后只触发一次 leader commit
         */
        long nextCommit = _SelfMachine.commit() + 1;
        if(peerMachine.accept() > _SelfMachine.commit() &&
           _SelfGraph.isMajorAcceptLeader(_SelfMachine.peer(), _SelfMachine.term(), nextCommit))
        {
            // peerMachine.getIndex() > _SelfMachine.getCommit()时，raftLog 不可能为 null
            _SelfMachine.commit(nextCommit, _RaftMapper);
            _Logger.debug("leader commit: %d @%d", nextCommit, _SelfMachine.term());
            LogEntry raftLog = _RaftMapper.getEntry(nextCommit);
            if(raftLog.getClient() != _SelfMachine.peer()) {
                // leader → follower → client
                ISession client = manager.findSessionByPrefix(raftLog.getClient());
                if(client != null) {
                    return Triple.of(createNotify(raftLog).with(client), null, SINGLE);
                }
            }
            else {
                // leader → client 
                return Triple.of(null, createNotify(raftLog), NULL);
            }
        }
        return null;
    }

    public ITriple onReject(X74_RaftReject x74, ISession session)
    {
        RaftMachine peerMachine = getMachine(x74.getPeer(), x74.getTerm());
        if(peerMachine == null) {return null;}
        peerMachine.index(x74.getIndex());
        peerMachine.indexTerm(x74.getIndexTerm());
        peerMachine.candidate(INVALID_PEER_ID);
        peerMachine.leader(INVALID_PEER_ID);
        peerMachine.state(RaftState.valueOf(x74.getState()));
        if(highTerm(x74.getTerm())) { //term > my term
            _Logger.debug(" → reject {step down [%s → follower]}", _SelfMachine.state());
            stepDown(x74.getTerm());
        }
        else { // term == my term
            // term < my term 是不存在的情况
            STEP_DOWN:
            {
                switch(RaftCode.valueOf(x74.getCode())) {
                    case CONFLICT -> {
                        if(_SelfMachine.state() == LEADER) {
                            _Logger.debug("follower %#x,match failed,rollback %d@%d",
                                          peerMachine.peer(),
                                          peerMachine.index(),
                                          peerMachine.indexTerm());
                            return Triple.of(createAppend(peerMachine, 1).with(session), null, SINGLE);
                        }
                        else {
                            _Logger.warning("self %#x is old leader & send logs → %#x,next-index wasn't catchup",
                                            _SelfMachine.peer(),
                                            peerMachine.peer());
                            // Ignore
                        }
                    }
                    case SPLIT_CLUSTER -> {
                        if(_SelfMachine.state() == LEADER) {
                            _Logger.debug("other leader:[%#x]", peerMachine.peer());
                        }
                    }
                    case ALREADY_VOTE -> {
                        if(_SelfMachine.state() == CANDIDATE) {
                            _Logger.debug("elector[%#x] has vote", peerMachine.peer());
                        }
                    }
                    case OBSOLETE -> {
                        if(_SelfMachine.state()
                                       .getCode() > FOLLOWER.getCode())
                        {
                            stepDown(_SelfMachine.term());
                        }
                        break STEP_DOWN;
                    }
                }
                if(_SelfGraph.isMajorReject(_SelfMachine.peer(), _SelfMachine.term())) {
                    stepDown(_SelfMachine.term());
                }
            }
        }
        return null;
    }

    public List<LogEntry> diff()
    {
        long start = _SelfMachine.accept();
        long commit = _SelfMachine.commit();
        long current = _SelfMachine.index();
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
            if(_SelfMachine.index() == preIndex && _SelfMachine.indexTerm() == preIndexTerm) {
            /* follower 与 Leader 状态一致
               开始同步接受缓存在_LogQueue中的数据
             */
                for(Iterator<LogEntry> it = _RecvLogQueue.iterator(); it.hasNext(); ) {
                    LogEntry entry = it.next();
                    if(_RaftMapper.append(entry)) {
                        _SelfMachine.append(entry.getIndex(), entry.getTerm(), _RaftMapper);
                        _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                    }
                    it.remove();
                }
                break CHECK;
            }
            else if(_SelfMachine.index() == 0 && preIndex == 0) {
                // 初始态，raft-machine 中不包含任何 log 记录
                _Logger.info("self machine is empty");
                break CHECK;
            }
            else if(_SelfMachine.index() < preIndex) {
                /*
                 * 1.follower 未将自身index 同步给leader，所以leader 发送了一个最新状态过来
                 * 后续执行accept() → response 会将index 同步给leader。
                 * 2.leader 已经获知follower的需求 但是 self.index < leader.start
                 * 需要follower先完成snapshot的安装才能正常同步。
                 */
                _Logger.debug("leader doesn't know my next  || leader hasn't log,need to install snapshot");
            }
            else if(_SelfMachine.index() > preIndex) {
                /* 此时_SelfMachine.term 与 leader 持平
                 * 新进入集群的节点数据更多，需要重新选举
                 */
                if(_SelfMachine.indexTerm() >= preIndexTerm) {
                    _Logger.warning("peer [%#x] join into cluster has history,some data has lost!!",
                                    _SelfMachine.peer());
                }
                else {//_SelfMachine.getIndexTerm() < preIndexTerm
                    if(rollback(_RecvLogQueue, preIndex)) {
                        break CHECK;
                    }
                }
            }
            else { // _SelfMachine.index == preIndex && _SelfMachine.indexTerm!=preIndexTerm
                if(rollback(_RecvLogQueue, preIndex)) {
                    break CHECK;
                }
            }
            _RecvLogQueue.clear();
            return false;
        }
        _RecvLogQueue.clear();
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
                    _SelfMachine.accept(_RaftMapper);
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
            //@formatter:off
            if(_SelfMachine.state()
                           .getCode() > FOLLOWER.getCode() &&
               _SelfMachine.state()
                           .getCode() < LEADER.getCode() &&
               update.term() >= _SelfMachine.term())
            //@formatter:on
            {
                _Logger.debug("elect time out → turn to follower");
                stepDown(update.term());
                break CHECK;
            }
            _Logger.warning("state %s event [ignore]", _SelfMachine.state());
        }
        return null;
    }

    public List<ITriple> checkVoteState(RaftMachine update, IManager manager)
    {
        //@formatter:off
        if(update.term() > _SelfMachine.term() &&
           update.index() == _SelfMachine.index() &&
           update.indexTerm() == _SelfMachine.indexTerm() &&
           update.candidate() == _SelfMachine.peer() &&
           update.commit() == _SelfMachine.commit() &&
           _SelfMachine.state() == FOLLOWER)
        //@formatter:on
        {
            vote4me();
            return createVotes(manager);
        }
        _Logger.warning("check vote failed; now: %s", _SelfMachine);
        return null;
    }

    public List<ITriple> checkLogAppend(RaftMachine update, IManager manager)
    {
        //@formatter:off
        if(_SelfMachine.state() == LEADER &&
           _SelfMachine.peer() == update.peer() &&
           _SelfMachine.term() >= update.term() &&
           _SelfMachine.index() >= update.index() &&
           _SelfMachine.indexTerm() >= update.indexTerm() &&
           _SelfMachine.commit() >= update.commit())
        //@formatter:on
        {
            mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
            return createAppends(manager);
        }
        // state change => ignore
        _Logger.warning("check leader heartbeat failed; now:%s", _SelfMachine);
        return null;
    }

    private void receiveLogs(List<LogEntry> logList)
    {
        if(logList == null || logList.isEmpty()) {return;}
        _RecvLogQueue.addAll(logList);
    }

    public List<ITriple> onSubmit(byte[] payload, IManager manager, long origin)
    {
        switch(_SelfMachine.state()) {
            case LEADER -> {
                List<ITriple> responses = append(payload, _SelfMachine.peer(), origin, manager);
                if(responses == null) {
                    stepDown(_SelfMachine.term());
                    return null;
                }
                else {return responses;}
            }
            case FOLLOWER -> {
                ISession session = manager.findSessionByPrefix(_SelfMachine.leader());
                if(session != null) {
                    _Logger.debug(" client[%#x]→ follower[%#x]→ leader[%#x] x75 ",
                                  origin,
                                  _SelfMachine.peer(),
                                  _SelfMachine.leader());
                    X75_RaftReq x75 = new X75_RaftReq(generateId());
                    x75.withSub(payload);
                    x75.setOrigin(origin);
                    x75.setClientId(_SelfMachine.peer());
                    x75.with(session);
                    return Collections.singletonList(Triple.of(x75, session, session.getEncoder()));
                }
                else {
                    _Logger.fetal("Leader connection miss,wait for reconnecting");
                }
            }
            default -> {
                _Logger.fetal("cluster is electing slef[%#x]→ %s", _SelfMachine.peer(), _SelfMachine.state());
                return null;
            }
        }
        return null;
    }

    /**
     * @param manager session manager
     * @param session raft client session
     * @return response in RaftCustom
     * response. first [response → cluster]
     * response. second [response → raft client]
     * response. third [operator-type]
     */
    public ITriple onRequest(X75_RaftReq x75, IManager manager, ISession session)
    {

        if(_SelfMachine.isEqualState(LEADER)) {
            List<ITriple> appends = append(x75.payload(), x75.getClientId(), x75.origin(), manager);
            if(appends != null && !appends.isEmpty()) {
                return Triple.of(appends,
                                 response(SUCCESS, x75.getClientId(), x75.origin(), x75.getMsgId()).with(session),
                                 BATCH);
            }
            else {
                return Triple.of(null,
                                 response(WAL_FAILED, x75.getClientId(), x75.origin(), x75.getMsgId()).with(session),
                                 NULL);
            }
        }
        else {
            return Triple.of(null,
                             response(ILLEGAL_STATE, x75.getClientId(), x75.origin(), x75.getMsgId()).with(session),
                             NULL);
        }
    }

    public ITriple onResponse(X76_RaftResp x76)
    {
        long msgId = x76.getMsgId();
        X75_RaftReq x75 = _Cached.remove(msgId);
        if(x75 != null && x76.getCode() != SUCCESS.getCode()) {
            //x76 在异常返回的时候才需要返回x75的内容
            x76.withSub(x75.payload());
        }
        return Triple.of(null, x76, NULL);
    }

    public ITriple onNotify(X77_RaftNotify x77, IManager manager)
    {
        LogEntry entry = getLogEntry(x77.getIndex());
        if(entry != null) {
            return Triple.of(null, x77.withSub(entry.payload()), NULL);
        }
        return null;
    }

    private X76_RaftResp response(RaftCode code, long client, long origin, long reqId)
    {
        X76_RaftResp x76 = new X76_RaftResp(reqId);
        x76.setClientId(client);
        x76.setOrigin(origin);
        x76.setCode(code.getCode());
        return x76;
    }

    private List<ITriple> append(byte[] payload, long client, long origin, IManager manager)
    {
        _Logger.debug("leader append new log");
        LogEntry newEntry = new LogEntry(_SelfMachine.term(), _SelfMachine.index() + 1, client, origin, payload);
        if(_RaftMapper.append(newEntry)) {
            _SelfMachine.append(newEntry.getIndex(), newEntry.getTerm(), _RaftMapper);
            _SelfMachine.accept(_RaftMapper);
            _Logger.debug("leader appended log %d@%d", newEntry.getIndex(), newEntry.getTerm());
            return createAppends(manager);
        }
        _Logger.fetal("RAFT WAL failed!");
        return null;
    }

    private List<ITriple> createVotes(IManager manager)
    {
        return _SelfGraph.getPeers()
                         .keySet()
                         .stream()
                         .filter(peerId->peerId != _SelfMachine.peer())
                         .map(peerId->{
                             ISession session = manager.findSessionByPrefix(peerId);
                             if(session == null) {
                                 _Logger.debug("elector :%#x session has not found", peerId);
                                 return null;
                             }
                             X70_RaftVote x70 = new X70_RaftVote(_ZUid.getId());
                             x70.with(session);
                             x70.setElector(peerId);
                             x70.setCandidate(_SelfMachine.peer());
                             x70.setTerm(_SelfMachine.term());
                             x70.setIndex(_SelfMachine.index());
                             x70.setIndexTerm(_SelfMachine.indexTerm());
                             x70.setAccept(_SelfMachine.commit());
                             return x70;

                         })
                         .filter(Objects::nonNull)
                         .map(this::map)
                         .collect(Collectors.toList());
    }

    private List<ITriple> createAppends(IManager manager)
    {
        return _SelfGraph.getPeers()
                         .entrySet()
                         .stream()
                         .filter(e->e.getKey() != _SelfMachine.peer())
                         .map(e->{
                             ISession session = manager.findSessionByPrefix(e.getKey());
                             _Logger.debug("leader → follower [ %#x ]:%s", e.getKey(), session);
                             return session == null ? null : createAppend(e.getValue()).with(session);
                         })
                         .filter(Objects::nonNull)
                         .map(this::map)
                         .collect(Collectors.toList());
    }

    private X72_RaftAppend createAppend(RaftMachine follower)
    {
        return createAppend(follower, -1);
    }

    private X72_RaftAppend createAppend(RaftMachine follower, int limit)
    {
        X72_RaftAppend x72 = new X72_RaftAppend(_ZUid.getId());
        x72.setLeader(_SelfMachine.peer());
        x72.setTerm(_SelfMachine.term());
        x72.setCommit(_SelfMachine.commit());
        x72.setPreIndex(_SelfMachine.index());
        x72.setPreIndexTerm(_SelfMachine.indexTerm());
        x72.setFollower(follower.peer());
        CHECK:
        {
            long preIndex = follower.index();
            long preIndexTerm = follower.indexTerm();
            if(preIndex == _SelfMachine.index() || preIndex == INDEX_NAN) {
                // follower 已经同步 或 follower.next 未知
                break CHECK;
            }
            // preIndex < self.index && preIndex >= 0
            ListSerial<LogEntry> entryList = new ListSerial<>(LogEntry::new);
            long next = preIndex + 1;//next >= 1
            if(next > MIN_START) {
                if(next > _SelfMachine.index()) {
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
                                        peerId(),
                                        follower.peer(),
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
            for(long end = _SelfMachine.index(), payloadSize = 0;
                next <= end && payloadSize < _SnapshotFragmentMaxSize; next++) {
                if(limit > 0 && entryList.size() >= limit) {
                    break;
                }
                LogEntry nextLog = _RaftMapper.getEntry(next);
                _Logger.debug("leader → follower:%s", nextLog);
                entryList.add(nextLog);
                payloadSize += nextLog.sizeOf();
            }
            x72.withSub(entryList);
        }
        return x72;
    }

    private Triple<IProtocol, ISession, IPipeEncoder> map(IProtocol source)
    {
        // source 一定持有 session 在上一步完成了这个操作。
        return Triple.of(source,
                         source.session(),
                         source.session()
                               .getEncoder());
    }

    @Override
    public boolean isInCongress()
    {
        return _RaftConfig.isInCongress();
    }

    @Override
    public long peerId()
    {
        return _SelfMachine.peer();
    }

    @Override
    public long generateId()
    {
        return _ZUid.getId();
    }

    @Override
    public RaftNode getLeader()
    {
        if(_SelfMachine.leader() != INVALID_PEER_ID) {
            return _RaftConfig.findById(_SelfMachine.leader());
        }
        return null;
    }

    @Override
    public void changeTopology(RaftNode delta)
    {

        final RingBuffer<QEvent> _ConsensusApiEvent = mClusterNode.getPublisher(IOperator.Type.CLUSTER_TOPOLOGY);
        final ReentrantLock _ConsensusApiLock = mClusterNode.getLock(IOperator.Type.CLUSTER_TOPOLOGY);
        _ConsensusApiLock.lock();
        try {
            long sequence = _ConsensusApiEvent.next();
            try {
                QEvent event = _ConsensusApiEvent.get(sequence);
                event.produce(IOperator.Type.CLUSTER_TOPOLOGY, Pair.of(this, delta), null);
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
        final RingBuffer<QEvent> _ConsensusEvent = mClusterNode.getPublisher(IOperator.Type.CLUSTER_TIMER);
        final ReentrantLock _ConsensusLock = mClusterNode.getLock(IOperator.Type.CLUSTER_TIMER);
        /*
        通过 Schedule thread-pool 进行 timer 执行, 排队执行。
         */
        _ConsensusLock.lock();
        try {
            long sequence = _ConsensusEvent.next();
            try {
                QEvent event = _ConsensusEvent.get(sequence);
                event.produce(IOperator.Type.CLUSTER_TIMER, Pair.of(content, null), null);
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
        x79.setOrigin(raftLog.origin());
        x79.setIndex(raftLog.getIndex());
        x79.setClient(raftLog.getClient());
        x79.withSub(raftLog.payload());
        return x79;
    }

    private X77_RaftNotify createNotify(LogEntry raftLog)
    {
        X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
        x77.setOrigin(raftLog.origin());
        x77.setIndex(raftLog.getIndex());
        x77.setClient(raftLog.getClient());
        x77.withSub(raftLog.payload());
        return x77;
    }

}
