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
import com.isahl.chess.knight.raft.features.IRaftMachine;
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
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_APPEND;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_MODIFY;
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
    private final Logger          _Logger       = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
    private final ZUID            _ZUid;
    private final IRaftConfig     _RaftConfig;
    private final IRaftMapper     _RaftMapper;
    private final TimeWheel       _TimeWheel;
    private final RaftGraph       _SelfGraph;
    private final IRaftMachine    _SelfMachine;
    private final RaftGraph       _JointGraph;
    private final Queue<LogEntry> _RecvLogQueue = new LinkedList<>();
    private final Random          _Random       = new Random();
    private final long            _SnapshotFragmentMaxSize;
    private final int             _SyncBatchMaxSize;

    /*
     * key(Long) → msgId
     * value(X75_RaftReq) → request
     */
    private final Map<Long, X75_RaftReq>    _Cached = new TreeMap<>();
    private final ScheduleHandler<RaftPeer> _ElectSchedule, _HeartbeatSchedule, _TickSchedule, _CleanSchedule;

    private IClusterNode mClusterNode;
    private ICancelable  mElectTask, mHeartbeatTask, mTickTask;

    public RaftPeer(TimeWheel timeWheel, IRaftConfig raftConfig, IRaftMapper raftMapper)
    {
        _TimeWheel = timeWheel;
        _RaftConfig = raftConfig;
        _ZUid = raftConfig.getZUID();
        _RaftMapper = raftMapper;
        _ElectSchedule = new ScheduleHandler<>(_RaftConfig.getElectInSecond(), RaftPeer::stepDown);
        _TickSchedule = new ScheduleHandler<>(_RaftConfig.getElectInSecond()
                                                         .multipliedBy(2), RaftPeer::restart);
        _HeartbeatSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond(), RaftPeer::heartbeat);
        _CleanSchedule = new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond()
                                                          .multipliedBy(2), true, RaftPeer::cleanCache);
        _SelfGraph = RaftGraph.create();
        _JointGraph = RaftGraph.create();
        _SelfMachine = RaftMachine.createBy(_ZUid.getPeerId(), OP_MODIFY);
        _SelfGraph.append(_SelfMachine);
        _JointGraph.append(_SelfMachine);
        _SnapshotFragmentMaxSize = _RaftConfig.getSnapshotFragmentMaxSize();
        _SyncBatchMaxSize = _RaftConfig.getSyncBatchMaxSize();
    }

    private IRaftMachine updateMachine(IRaftMachine machine, int state)
    {
        RaftMachine update = RaftMachine.createBy(machine.peer(), OP_APPEND);
        update.from(update);
        update.state(state);
        return update;
    }

    private void heartbeat()
    {
        if(_SelfMachine.isInState(LEADER)) {
            trigger(updateMachine(_SelfMachine, _SelfMachine.state()));
        }
        else {
            beatCancel();
        }
    }

    private void cleanCache()
    {
        for(Iterator<Map.Entry<Long, X75_RaftReq>> it = _Cached.entrySet()
                                                               .iterator(); it.hasNext(); ) {
            Map.Entry<Long, X75_RaftReq> entry = it.next();
            if(entry.getValue().tCached) {it.remove();}
            else {
                entry.getValue().tCached = true;
                //TODO clean cache, 回收利用
            }
        }
    }

    public void start(final IClusterNode _Node)
    {
        LogMeta meta = _RaftMapper.getLogMeta();
        _SelfMachine.term(meta.getTerm());
        _SelfMachine.commit(meta.getCommit());
        _SelfMachine.accept(meta.getApplied());
        _SelfMachine.index(meta.getIndex());
        _SelfMachine.indexTerm(meta.getIndexTerm());
        _SelfMachine.matchIndex(meta.getApplied());
        mClusterNode = _Node;
        if(_RaftConfig.isClusterMode()) {
            // 启动集群连接
            graphInit(_SelfGraph,
                      // _RaftConfig.getPeers().keySet() 是议会的成员
                      _RaftConfig.getPeers()
                                 .keySet(),
                      _RaftConfig.getNodes()
                                 .keySet());
            if(_RaftConfig.isInCongress()) {
                _SelfMachine.state(FOLLOWER.getCode());
                // 周期性检查 leader 是否存在
                mTickTask = _TimeWheel.acquire(RaftPeer.this, _TickSchedule);
            }
            else {
                _SelfMachine.state(CLIENT.getCode());
            }
            // 如果 self 是 client 此时是全连接 proposer, 如果是议员则直接执行全连接
            graphUp(_SelfGraph.getPeers(), _RaftConfig.getNodes());
            _Logger.info("raft node init -> %s", _SelfMachine);
        }
        else {
            _SelfMachine.state(OUTSIDE.getCode());
            _Logger.info("not in cluster, single model!");
        }
    }

    private void graphInit(RaftGraph graph, Collection<Long> congress, Collection<Long> nodes)
    {
        for(long node : nodes) {
            // Graph中 装入所有需要管理的集群状态机，self已经装入了，此处不再重复
            IRaftMachine machine = RaftMachine.createBy(node, OP_MODIFY);
            // nodes 至少是配置图谱中的一员，所以
            machine.state(congress.contains(node) ? FOLLOWER.getCode() : CLIENT.getCode());
            graph.append(machine);
        }
    }

    private void graphUp(Map<Long, IRaftMachine> peers, Map<Long, RaftNode> nodes)
    {
        peers.forEach((peer, machine)->{
            // 仅连接peer<自身的节点
            if(peer < _SelfMachine.peer()) {
                RaftNode remote = nodes.get(peer);
                try {
                    mClusterNode.setupPeer(remote.getHost(), remote.getPort());
                    _Logger.info("setup, connect [peer : %s:%d]", remote.getHost(), remote.getPort());
                }
                catch(Exception e) {
                    _Logger.warning("peer connect error: %s:%d", e, remote.getHost(), remote.getPort());
                }
            }
        });
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
        if(_SelfMachine.accept() >= _RaftMapper.getStartIndex() && _SelfMachine.accept() <= _RaftMapper.getEndIndex()) {
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
        _Logger.debug("peer[%s], restart", _SelfMachine.state());
        if(_SelfMachine.isInState(CLIENT)) {
            _Logger.info("peer[%#] → CLIENT, don't join congress", peerId());
        }
        else if(_SelfMachine.isInState(JOINT)) {

        }
        else {
            startVote();
        }
    }

    /**
     * 此方法会在各种超时处理器中被启用，所以执行线程为TimeWheel.pool中的任意子线程
     */
    private void startVote()
    {
        try {
            long wait = _Random.nextInt(70) + 50;
            Thread.sleep(wait);
            _Logger.debug("random wait for %d mills, then vote", wait);
        }
        catch(InterruptedException e) {
            // ignore
        }
        trigger(updateMachine(_SelfMachine, (byte) (CANDIDATE.getCode() | _SelfMachine.state())));
    }

    private Collection<Long> vote4me(long term)
    {
        tickCancel();
        updateTerm(term);
        _SelfMachine.candidate(_SelfMachine.peer());
        _SelfMachine.leader(INVALID_PEER_ID);
        _SelfMachine.state(CANDIDATE.getCode());
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("vote4me follower → candidate %s", _SelfMachine.toPrimary());
        return RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph)
                        .keySet();
    }

    private void updateTerm(long term)
    {
        _SelfMachine.term(term);
        _RaftMapper.updateTerm(term);
    }

    private List<ITriple> createVotes(Collection<Long> peers, IManager manager)
    {
        return peers.stream()
                    .filter(peer->peer != _SelfMachine.peer())
                    .map(peer->{
                        ISession session = manager.findSessionByPrefix(peer);
                        if(session == null) {
                            _Logger.debug("elector :%#x session has not found", peer);
                            return null;
                        }
                        X70_RaftVote x70 = new X70_RaftVote(_ZUid.getId());
                        x70.with(session);
                        x70.setElector(peer);
                        x70.setCandidate(_SelfMachine.peer());
                        x70.setTerm(_SelfMachine.term());
                        x70.setIndex(_SelfMachine.index());
                        x70.setIndexTerm(_SelfMachine.indexTerm());
                        x70.setAccept(_SelfMachine.accept());
                        x70.setCommit(_SelfMachine.commit());
                        return x70;
                    })
                    .filter(Objects::nonNull)
                    .map(this::map)
                    .collect(Collectors.toList());
    }

    private List<ITriple> rejectThenVote(long rejectTo, long msgId, IManager manager, ISession session)
    {
        List<ITriple> broadcast = createVotes(vote4me(_SelfMachine.term()), manager);
        X74_RaftReject x74 = reject(RaftCode.OBSOLETE, rejectTo, msgId);
        broadcast.add(Triple.of(x74, session, session.getEncoder()));
        return broadcast;
    }

    public List<ITriple> vote4me(IRaftMachine update, IManager manager)
    {
        //@formatter:off
        if(_SelfMachine.isInState(FOLLOWER)&&
           update.index() <= _SelfMachine.index() &&
           update.term() > _SelfMachine.term() &&
           update.accept() <= _SelfMachine.accept() &&
           update.candidate() == _SelfMachine.peer())
        //@formatter:on
        {
            return createVotes(vote4me(update.term()), manager);
        }
        _Logger.warning("check vote failed; now: %s", _SelfMachine);
        return null;
    }

    public ITriple onVote(X70_RaftVote x70, IManager manager, ISession session)
    {
        IRaftMachine machine = getMachine(_SelfGraph, x70.getCandidate());
        if(machine != null) {
            machine.term(x70.getTerm());
            machine.index(x70.getIndex());
            machine.indexTerm(x70.getIndexTerm());
            machine.commit(x70.getCommit());
            machine.accept(x70.getAccept());
            machine.candidate(x70.getCandidate());
            machine.state(CANDIDATE.getCode());
            if(lowTerm(x70.getTerm())) {
                // reject election term < my term
                _Logger.debug("elect {low term: reject %#x,mine:[%d@%d[%d]] candidate:[%d@%d[%d]]}",
                              x70.getCandidate(),
                              _SelfMachine.index(),
                              _SelfMachine.indexTerm(),
                              _SelfMachine.term(),
                              machine.index(),
                              machine.indexTerm(),
                              machine.term());
                return Triple.of(reject(LOWER_TERM, x70.getCandidate(), x70.getMsgId()).with(session), null, SINGLE);
            }
            if(highTerm(x70.getTerm())) {
                // term > my term
                _Logger.debug("elect {step down [%#x → follower]}", _SelfMachine.peer());
                stepDown(x70.getTerm());
            }
            if(_SelfMachine.isInState(FOLLOWER)) {
                //@formatter:off
                if(_SelfMachine.commit() > x70.getCommit() ||
                   _SelfMachine.accept() > x70.getAccept() ||
                   _SelfMachine.indexTerm() > x70.getIndexTerm() ||
                   _SelfMachine.index() > x70.getIndex()
                )
                //@formatter:on
                {
                    _Logger.debug(
                            "less than me; reject[%s]  mine:[ %d@%d,a:%d | c:%d ] > candidate:[ %d@%d, a:%d | c:%d ] then vote4me[ follower → candidate ] ",
                            OBSOLETE,
                            _SelfMachine.index(),
                            _SelfMachine.indexTerm(),
                            _SelfMachine.accept(),
                            _SelfMachine.commit(),
                            x70.getIndex(),
                            x70.getIndexTerm(),
                            x70.getAccept(),
                            x70.getCommit());
                    return Triple.of(rejectThenVote(x70.getCandidate(), x70.getMsgId(), manager, session), null, BATCH);
                }
                else {
                    _Logger.debug("new term [ %d ] follower [ %#x ] → elector | candidate:[ %#x ]",
                                  x70.getTerm(),
                                  _SelfMachine.peer(),
                                  x70.getCandidate());
                    IProtocol ballot = stepUp(x70.getTerm(), x70.getCandidate(), x70.getMsgId());
                    return Triple.of(ballot.with(session), null, SINGLE);
                }
            }
            // elector|leader|candidate,one of these states ，candidate != INDEX_NAN 不需要重复判断
            else if(_SelfMachine.candidate() != x70.getCandidate()) {
                _Logger.debug("already vote [elector ×] | vote for:[ %#x not ♂ %#x ]",
                              _SelfMachine.candidate(),
                              x70.getCandidate());
                return Triple.of(reject(ALREADY_VOTE, x70.getCandidate(), x70.getMsgId()).with(session), null, SINGLE);
            }
        }
        return null;
    }

    public ITriple onBallot(X71_RaftBallot x71, IManager manager, ISession session)
    {
        IRaftMachine machine = getMachine(_SelfGraph, x71.getElector());
        if(machine != null) {
            machine.term(x71.getTerm());
            machine.index(x71.getIndex());
            machine.indexTerm(x71.getIndexTerm());
            machine.accept(x71.getAccept());
            machine.commit(x71.getCommit());
            machine.candidate(_SelfMachine.peer());
            machine.state(ELECTOR.getCode());
            //@formatter:off
            if(_SelfMachine.isInState( CANDIDATE )&&
               _SelfMachine.term() ==  x71.getTerm() &&
               _SelfGraph.isMajorAccept(_SelfMachine.peer(), _SelfMachine.term())
            )
            //@formatter:on
            {
                //term == my term
                lead();
                return Triple.of(createAppends(_SelfGraph.getPeers(), manager), null, BATCH);
            }
            else if(highTerm(x71.getTerm())) {
                //term > my term
                _Logger.debug("ballot {step down [%s → follower] %s}", _SelfMachine.state(), _SelfMachine.toPrimary());
                stepDown(x71.getTerm());
            }
            else {
                //term < my term
                _Logger.debug("ballot {reject %#x,mine:[%d@%d] candidate:[%d@%d]}",
                              x71.getElector(),
                              _SelfMachine.index(),
                              _SelfMachine.term(),
                              machine.index(),
                              machine.term());
                return Triple.of(reject(LOWER_TERM, x71.getElector(), x71.getMsgId()).with(session), null, SINGLE);
            }
        }
        return null;
    }

    public ITriple onAppend(X72_RaftAppend x72, ISession session)
    {
        if(lowTerm(x72.getTerm())) {
            //leader's term < my term
            _Logger.warning("old leader rise %#x @%d, reject}", x72.getLeader(), x72.getTerm());
            return Triple.of(reject(LOWER_TERM, x72.getLeader(), x72.getMsgId()).with(session), null, SINGLE);
        }
        if(highTerm(x72.getTerm())) {
            _Logger.debug(" → append {step down [%s → follower] %s}", _SelfMachine.state(), _SelfMachine.toPrimary());
            stepDown(x72.getTerm());
        }
        //term == my term
        switch(RaftState.valueOf(_SelfMachine.state())) {
            case LEADER -> {
                _Logger.warning("state:[%s], leader[%#x] → the other [%#x] ",
                                _SelfMachine.state(),
                                _SelfMachine.leader(),
                                x72.getLeader());
                return Triple.of(reject(SPLIT_CLUSTER, x72.getLeader(), x72.getMsgId()).with(session), null, SINGLE);
            }
            case FOLLOWER, ELECTOR -> {
                if(x72.payload() != null) {
                    ListSerial<LogEntry> logs = new ListSerial<>(LogEntry::new);
                    logs.decode(x72.subEncoded());
                    _RecvLogQueue.addAll(logs);
                }
                IRaftMachine machine = getMachine(_SelfGraph, x72.getLeader());
                if(machine != null) {
                    machine.term(x72.getTerm());
                    machine.index(x72.getPreIndex());
                    machine.indexTerm(x72.getPreIndexTerm());
                    machine.accept(x72.getAccept());
                    machine.commit(x72.getCommit());
                    machine.candidate(x72.getLeader());
                    machine.leader(x72.getLeader());
                    machine.state(LEADER.getCode());
                    return follow(x72.getTerm(),
                                  x72.getLeader(),
                                  x72.getCommit(),
                                  x72.getPreIndex(),
                                  x72.getPreIndexTerm(),
                                  x72.getMsgId(),
                                  session);
                }
            }
            default -> _Logger.warning("illegal state :%s", _SelfMachine.state());
        }
        return null;
    }

    public ITriple onReject(X74_RaftReject x74, ISession session)
    {
        IRaftMachine machine = getMachine(_SelfGraph, x74.getPeer());
        if(machine != null) {
            machine.term(x74.getTerm());
            machine.index(x74.getIndex());
            machine.indexTerm(x74.getIndexTerm());
            machine.accept(x74.getAccept());
            machine.commit(x74.getCommit());
            machine.candidate(x74.getCandidate());
            machine.leader(x74.getLeader());
            machine.state(x74.getState());
            if(highTerm(x74.getTerm())) {
                //peer's term > my term
                _Logger.debug(" → reject {step down [%s → follower]}", _SelfMachine.state());
                stepDown(x74.getTerm());
            }
            else {
                // peer's term == my term
                // peer's term <  my term 是不存在的情况
                STEP_DOWN:
                {
                    switch(RaftCode.valueOf(x74.getCode())) {
                        case CONFLICT -> {
                            // follower 持有的 log 纪录 index@term 与leader 投送的不一致，后续进行覆盖同步
                            if(_SelfMachine.isInState(LEADER)) {
                                _Logger.debug("follower %#x,match failed,rollback %d@%d",
                                              machine.peer(),
                                              machine.index(),
                                              machine.indexTerm());
                                return Triple.of(createAppend(machine,
                                                              min((int) (_SelfMachine.commit() - machine.index()),
                                                                  _SyncBatchMaxSize)).with(session), null, SINGLE);
                            }
                            else {
                                _Logger.warning("self %#x is old leader & send logs → %#x,next-index wasn't catchup",
                                                _SelfMachine.peer(),
                                                machine.peer());
                                // Ignore
                            }
                        }
                        case SPLIT_CLUSTER -> {
                            if(_SelfMachine.isInState(LEADER)) {
                                _Logger.debug("other leader:[%#x]", machine.peer());
                            }
                        }
                        case ALREADY_VOTE -> {
                            if(_SelfMachine.isInState(CANDIDATE)) {
                                _Logger.debug("elector[%#x] has vote", machine.peer());
                            }
                        }
                        case OBSOLETE -> {
                            if(_SelfMachine.state() > FOLLOWER.getCode()) {
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
        }
        return null;
    }

    /**
     * follower → leader
     *
     * @return result triple
     * fst : broadcast to peer
     * snd : leader handle x77 → notify client
     * trd : operator type 「 SINGLE/BATCH 」
     */
    public ITriple onAccept(X73_RaftAccept x73, IManager manager)
    {
        IRaftMachine machine = getMachine(_SelfGraph, x73.getFollower());
        if(machine != null) {
            machine.state(FOLLOWER.getCode());
            machine.term(x73.getTerm());
            machine.index(x73.getCatchUp());
            machine.indexTerm(x73.getCatchUpTerm());
            machine.accept(x73.getCatchUp());
            machine.matchIndex(x73.getCatchUp());
            machine.commit(x73.getCommit());
            machine.leader(x73.getLeader());
            machine.candidate(x73.getLeader());
            /*
             * member.accept > leader.commit 完成半数 match 之后只触发一次 leader commit
             */
            long next = _SelfMachine.commit() + 1;
            if(machine.accept() >= next && _SelfGraph.isMajorAccept(next)) {
                _SelfMachine.commit(next, _RaftMapper);
                _Logger.debug("leader commit: %d@%d", next, _SelfMachine.term());
                LogEntry logEntry = _RaftMapper.getEntry(next);
                if(logEntry.getClient() != _SelfMachine.peer()) {
                    // leader → client → device
                    ISession session = manager.findSessionByPrefix(logEntry.getClient());
                    if(session != null) {
                        return Triple.of(createNotify(logEntry).with(session), null, SINGLE);
                    }
                }
                else {
                    // leader ≡ client → device
                    return Triple.of(null, createNotify(logEntry), NULL);
                }
            }
            else {
                // machine.accept < next → 已经执行过 self commit 过, 无须重复 commit
                // !_SelfGraph.isMajorAccept(next) → 未满足 commit 条件, 不执行 commit
                _Logger.debug("member %#x, catchup:%d → %d", machine.peer(), machine.accept(), _SelfMachine.accept());
            }
        }
        return null;
    }

    private IRaftMachine getMachine(RaftGraph graph, long peerId)
    {
        IRaftMachine machine = graph.get(peerId);
        if(machine == null) {
            _Logger.warning("peer %#x is not found", peerId);
            return null;
        }
        return machine;
    }

    private X71_RaftBallot ballot(long msgId)
    {
        X71_RaftBallot vote = new X71_RaftBallot(msgId);
        vote.setElector(_SelfMachine.peer());
        vote.setTerm(_SelfMachine.term());
        vote.setIndex(_SelfMachine.index());
        vote.setIndexTerm(_SelfMachine.indexTerm());
        vote.setAccept(_SelfMachine.accept());
        vote.setCommit(_SelfMachine.commit());
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
        reject.setAccept(_SelfMachine.accept());
        reject.setCommit(_SelfMachine.commit());
        reject.setReject(rejectTo);
        reject.setCandidate(_SelfMachine.candidate());
        reject.setLeader(_SelfMachine.leader());
        reject.setCode(raftCode.getCode());
        reject.setState(_SelfMachine.state());
        return reject;
    }

    private X71_RaftBallot stepUp(long term, long candidate, long msgId)
    {
        tickCancel();
        updateTerm(term);
        _SelfMachine.state(ELECTOR.getCode());
        _SelfMachine.leader(INVALID_PEER_ID);
        _SelfMachine.candidate(candidate);
        mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
        _Logger.debug("[follower → elector] %s", _SelfMachine.toPrimary());
        return ballot(msgId);
    }

    private void stepDown(long term)
    {

        if(_SelfMachine.state() > FOLLOWER.getCode()) {
            beatCancel();
            electCancel();
            updateTerm(term);
            _SelfMachine.candidate(INVALID_PEER_ID);
            _SelfMachine.leader(INVALID_PEER_ID);
            _SelfMachine.state(FOLLOWER.getCode());
            mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        }
        else {_Logger.warning("step down [ignore],state has already changed to FOLLOWER");}
    }

    private void lead()
    {
        electCancel();
        _SelfMachine.state(LEADER.getCode());
        _SelfMachine.leader(_SelfMachine.peer());
        mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
        _Logger.info("be leader → %s", _SelfMachine.toPrimary());
    }

    private void stepDown()
    {
        trigger(updateMachine(_SelfMachine, FOLLOWER.getCode()));
    }

    private ITriple follow(long term,
                           long leader,
                           long commit,
                           long preIndex,
                           long preIndexTerm,
                           long msgId,
                           ISession session)
    {
        tickCancel();
        _SelfMachine.follow(term, leader, _RaftMapper);
        mTickTask = _TimeWheel.acquire(this, _TickSchedule);
        if(catchUp(preIndex, preIndexTerm)) {
            IProtocol notify = null;
            if(preIndex <= commit) {
                ListSerial<LogEntry> sub = new ListSerial<>(LogEntry::new);
                for(long i = preIndex; i <= commit; i++) {
                    _SelfMachine.commit(i);
                    sub.add(_RaftMapper.getEntry(i));
                }
                notify = createNotify(sub);
            }
            return Triple.of(accept(msgId).with(session), notify, SINGLE);
        }
        else {
            _Logger.debug("catch up failed reject → %#x ", leader);
            return Triple.of(reject(CONFLICT, leader, msgId).with(session), null, SINGLE);
        }
    }

    private void tickCancel()
    {
        if(mTickTask != null) {
            mTickTask.cancel();
        }
    }

    private void beatCancel()
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

    public LogEntry getLogEntry(long index)
    {
        _Logger.debug("peer get log entry: %d", index);
        return _RaftMapper.getEntry(index);
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
            /* follower 与 Leader 依据 index@term 进行对齐
               开始同步接受缓存在_LogQueue中的数据
             */
                for(Iterator<LogEntry> it = _RecvLogQueue.iterator(); it.hasNext(); ) {
                    LogEntry entry = it.next();
                    if(_RaftMapper.append(entry)) {
                        _SelfMachine.append(entry.getIndex(), entry.getTerm(), _RaftMapper);
                        _SelfMachine.accept(entry.getIndex());
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
                _Logger.debug("leader doesn't know my next  || leader hasn't log, need to install snapshot");
            }
            else if(rollback(_RecvLogQueue, preIndex)) {
                // _SelfMachine.index >= preIndex
                break CHECK;
            }
            _RecvLogQueue.clear();
            return false;
        }
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
                // preIndex == 1 && queue.empty → reject
            }
            else {
                //刚好follow是空的, leader投过来的数据就接收了
                LogEntry entry;
                // mapper.append false的状态包含了, _LogQueue.empty的情况
                while(_RaftMapper.append(entry = _LogQueue.poll())) {
                    //mapper.append 包含了preIndex ==1 && entry.index ==1 && newEndIndex == 0
                    assert entry != null;
                    _SelfMachine.append(entry.getIndex(), entry.getTerm(), _RaftMapper);
                    _SelfMachine.accept(_RaftMapper);
                    _Logger.debug("follower catch up %d@%d", entry.getIndex(), entry.getTerm());
                }
                return true;
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
                // rollback == null
                _Logger.fetal("lost data → reset machine & mapper ");
                _SelfMachine.reset();
                _RaftMapper.reset();
            }
        }
        return false;
    }

    public List<ITriple> turnDown(IRaftMachine update)
    {
        CHECK:
        {
            //@formatter:off
            if(_SelfMachine.state() > FOLLOWER.getCode() &&
               _SelfMachine.state() < LEADER.getCode() &&
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

    public List<ITriple> logAppend(IRaftMachine update, IManager manager)
    {
        //@formatter:off
        if(_SelfMachine.peer() == update.peer() &&
           _SelfMachine.term() >= update.term() &&
           _SelfMachine.index() >= update.index() &&
           _SelfMachine.indexTerm() >= update.indexTerm() &&
           _SelfMachine.commit() >= update.commit()
          )
        //@formatter:on
        {
            mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
            return createAppends(RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager);
        }
        // state change => ignore
        _Logger.warning("check leader heartbeat failed; now:%s", _SelfMachine);
        return null;
    }

    public List<ITriple> onSubmit(byte[] payload, IManager manager, long origin)
    {
        switch(RaftState.valueOf(_SelfMachine.state())) {
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

        if(_SelfMachine.isInState(LEADER)) {
            List<ITriple> appends = append(x75.payload(), x75.getClientId(), x75.origin(), manager);
            if(appends != null && !appends.isEmpty()) {
                return Triple.of(appends, response(SUCCESS, x75.getClientId(), x75.getMsgId()).with(session), BATCH);
            }
            else {
                return Triple.of(null, response(WAL_FAILED, x75.getClientId(), x75.getMsgId()).with(session), NULL);
            }
        }
        else {
            return Triple.of(null, response(ILLEGAL_STATE, x75.getClientId(), x75.getMsgId()).with(session), NULL);
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

    public ITriple onNotify(X77_RaftNotify x77)
    {
        LogEntry entry = x77.deserializeSub(LogEntry::new);
        if(entry != null) {
            return Triple.of(null, x77.withSub(entry.payload()), NULL);
        }
        return null;
    }

    /*
        1. 转换leader 状态为 joint union 状态
        2. 向 old_graph 和 new_graph 同时发送状态信息
        3.
     */
    public ITriple onJoint(X78_RaftModify x78, IManager manager)
    {
        long[] peers = x78.getNewGraph();
        long leader = x78.getLeader();
        X76_RaftResp x76 = new X76_RaftResp();
        x76.setClientId(x78.getClient());
        x76.setCode(SUCCESS.getCode());
        x76.setMsgId(x78.getMsgId());
        RaftState state = RaftState.valueOf(_SelfMachine.state());
        switch(state) {
            case LEADER -> {
                //old_graph
                _SelfMachine.state(JOINT.getCode());
                for(long peer : peers) {_JointGraph.append(RaftMachine.createBy(peer, OP_MODIFY));}
                List<IProtocol> joints = new LinkedList<>();
                RaftGraph.join(leader, _SelfGraph, _JointGraph)
                         .forEach((peer, machine)->{
                             machine.state(JOINT.getCode());
                             ISession session = manager.findSessionByPrefix(peer);
                             if(session != null) {
                                 X78_RaftModify copy = x78.copy();
                                 copy.with(session);
                                 copy.setMsgId(_ZUid.getId());
                                 joints.add(copy);
                             }
                         });

                return Triple.of(joints, x76, BATCH);
            }
            case CLIENT, OUTSIDE -> {
                for(long peer : peers) {
                    if(peer == _SelfMachine.peer()) {
                        _SelfMachine.state(JOINT.getCode());
                        _SelfMachine.leader(leader);
                    }
                    _JointGraph.append(RaftMachine.createBy(peer, OP_MODIFY));
                }
            }
            case FOLLOWER -> {
                if(_SelfMachine.leader() == leader) {
                    _SelfMachine.state(JOINT.getCode());
                    return Triple.of(x76, null, SINGLE);
                }
                //else 选举态,返回失败
                _Logger.warning("expect:{FOLLOW→%#x},from:%#x; illegal state", _SelfMachine.leader(), leader);
            }
            default -> _Logger.warning("illegal state: [%s]", state); //ignore ELECTOR, CANDIDATE, GATE
        }
        x76.setCode(ILLEGAL_STATE.getCode());
        return Triple.of(x76, null, SINGLE);
    }

    private X76_RaftResp response(RaftCode code, long client, long reqId)
    {
        X76_RaftResp x76 = new X76_RaftResp(reqId);
        x76.setClientId(client);
        x76.setCode(code.getCode());
        return x76;
    }

    private List<ITriple> append(byte[] payload, long client, long origin, IManager manager)
    {
        _Logger.debug("leader append new log");
        LogEntry newEntry = new LogEntry(_SelfMachine.index() + 1, _SelfMachine.term(), client, origin, payload);
        if(_RaftMapper.append(newEntry)) {
            _SelfMachine.append(newEntry.getIndex(), newEntry.getTerm(), _RaftMapper);
            _SelfMachine.accept(_RaftMapper);
            _Logger.debug("leader appended log %d@%d", newEntry.getIndex(), newEntry.getTerm());
            return createAppends(RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager);
        }
        _Logger.fetal("RAFT WAL failed!");
        return null;
    }

    private List<ITriple> createAppends(Map<Long, IRaftMachine> peers, IManager manager)
    {
        return peers.entrySet()
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

    private X72_RaftAppend createAppend(IRaftMachine acceptor)
    {
        return createAppend(acceptor, -1);
    }

    private X72_RaftAppend createAppend(IRaftMachine acceptor, int limit)
    {
        X72_RaftAppend x72 = new X72_RaftAppend(_ZUid.getId());
        x72.setLeader(_SelfMachine.peer());
        x72.setTerm(_SelfMachine.term());
        x72.setCommit(_SelfMachine.commit());
        x72.setPreIndex(_SelfMachine.index());
        x72.setPreIndexTerm(_SelfMachine.indexTerm());
        x72.setFollower(acceptor.peer());
        CHECK:
        {
            long preIndex = acceptor.index();
            long preIndexTerm = acceptor.indexTerm();
            if(preIndex == _SelfMachine.index() || preIndex == INDEX_NAN) {
                // acceptor 已经同步 或 acceptor.next 未知
                break CHECK;
            }
            // preIndex < self.index && preIndex >= 0
            ListSerial<LogEntry> entryList = new ListSerial<>(LogEntry::new);
            long next = preIndex + 1;//next >= 1
            if(next > MIN_START) {
                if(next > _SelfMachine.index()) {
                    //acceptor.index > leader.index, 后续用leader的数据进行覆盖
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
                                        acceptor.peer(),
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
                _Logger.debug("leader → acceptor:%s", nextLog);
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

    private <T> void trigger(T delta, IOperator.Type type)
    {
        final RingBuffer<QEvent> _Publisher = mClusterNode.getPublisher(type);
        final ReentrantLock _Lock = mClusterNode.getLock(type);
        _Lock.lock();
        try {
            long sequence = _Publisher.next();
            try {
                QEvent event = _Publisher.get(sequence);
                event.produce(IOperator.Type.CLUSTER_TOPOLOGY, Pair.of(this, delta), null);
            }
            finally {
                _Publisher.publish(sequence);
            }
        }
        finally {
            _Lock.unlock();
        }
    }

    @Override
    public void changeTopology(RaftNode delta)
    {
        trigger(delta, IOperator.Type.CLUSTER_TOPOLOGY);
    }

    @Override
    public <T extends IStorage> void trigger(T content)
    {
        trigger(content, IOperator.Type.CLUSTER_TIMER);
    }

    @Override
    public Collection<RaftNode> getTopology()
    {
        return _RaftConfig.getPeers()
                          .values();
    }

    private X77_RaftNotify createNotify(LogEntry... raftLog)
    {
        X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
        ListSerial<LogEntry> logs = new ListSerial<>(LogEntry::new);
        logs.addAll(List.of(raftLog));
        x77.withSub(logs);
        return x77;
    }

    private X77_RaftNotify createNotify(ListSerial<LogEntry> logs)
    {
        X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
        x77.withSub(logs);
        return x77;
    }

}
