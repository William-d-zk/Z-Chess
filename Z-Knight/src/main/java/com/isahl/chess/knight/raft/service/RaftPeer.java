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

import static com.isahl.chess.king.base.disruptor.features.functions.OperateType.*;
import static com.isahl.chess.king.env.ZUID.INVALID_PEER_ID;
import static com.isahl.chess.knight.raft.features.IRaftMachine.INDEX_NAN;
import static com.isahl.chess.knight.raft.features.IRaftMachine.MIN_START;
import static com.isahl.chess.knight.raft.model.RaftCode.*;
import static com.isahl.chess.knight.raft.model.RaftState.*;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_APPEND;
import static com.isahl.chess.queen.db.model.IStorage.Operation.OP_MODIFY;
import static java.lang.Math.min;

import com.isahl.chess.bishop.protocol.zchat.model.command.raft.*;
import com.isahl.chess.king.base.cron.ScheduleHandler;
import com.isahl.chess.king.base.cron.TimeWheel;
import com.isahl.chess.king.base.cron.features.ICancelable;
import com.isahl.chess.king.base.disruptor.features.functions.OperateType;
import com.isahl.chess.king.base.features.IValid;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
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
import com.isahl.chess.knight.raft.model.replicate.SnapshotEntry;
import com.isahl.chess.knight.raft.model.replicate.SnapshotMeta;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.model.QEvent;
import com.isahl.chess.queen.io.core.features.cluster.IClusterTimer;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.lmax.disruptor.RingBuffer;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftPeer implements IValid, IRaftService, IClusterTimer {
  private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());
  private final ZUID _ZUid;
  private final IRaftConfig _RaftConfig;
  private final IRaftMapper _RaftMapper;
  private final TimeWheel _TimeWheel;
  private final RaftGraph _SelfGraph;
  private final IRaftMachine _SelfMachine;
  private final RaftGraph _JointGraph;
  private final Queue<LogEntry> _RecvLogQueue = new LinkedList<>();
  private final SecureRandom _Random = new SecureRandom();
  private final long _SnapshotFragmentMaxSize;
  private final int _SyncBatchMaxSize;

  /*
   * key(Long) → msgId
   * value(X75_RaftReq) → request
   */
  private final ScheduleHandler<RaftPeer> _ElectSchedule, _HeartbeatSchedule, _TickSchedule;
  private final ScheduleHandler<RaftPeer> _PreVoteSchedule;

  private IClusterNode mClusterNode;
  private ICancelable mElectTask, mHeartbeatTask, mTickTask, mPreVoteTask;

  // Pre-vote 状态跟踪
  private final Map<Long, Boolean> _PreVoteReceived = new ConcurrentHashMap<>();
  private volatile boolean _PreVoteInProgress = false;

  // Snapshot 安装状态
  private final ReentrantLock _SnapshotLock = new ReentrantLock();
  private volatile boolean _InstallingSnapshot = false;
  private long _SnapshotLeader = INVALID_PEER_ID;

  // ReadIndex 等待队列 (commitIndex -> List<readId>)
  private final Map<Long, List<ReadIndexRequest>> _ReadIndexWaiters = new ConcurrentHashMap<>();
  private long _LastReadId = 0;

  // CheckQuorum 机制
  private final ScheduleHandler<RaftPeer> _CheckQuorumSchedule;
  private ICancelable mCheckQuorumTask;
  private volatile long _LastQuorumCheckTime = 0;
  private final AtomicInteger _ActivePeers = new AtomicInteger(0);

  // 成员变更管理器
  private final MembershipChangeManager _MembershipChangeManager = new MembershipChangeManager();
  private ICancelable mMembershipChangeTimeoutTask;
  private final ScheduleHandler<RaftPeer> _MembershipChangeTimeoutSchedule;

  // Leader 转让管理器
  private final LeadershipTransferManager _LeadershipTransferManager =
      new LeadershipTransferManager();
  private ICancelable mTransferTimeoutTask;
  private final ScheduleHandler<RaftPeer> _TransferTimeoutSchedule;

  // Pipeline 复制管理器
  private final PipelineReplicationManager _PipelineManager;
  private volatile boolean mPipelineEnabled = true;

  // Lease 读管理器
  private final LeaseManager _LeaseManager = new LeaseManager();
  private volatile boolean mLeaseReadEnabled = true;

  // Learner 节点管理器
  private final LearnerManager _LearnerManager = new LearnerManager("_Learners_");
  private volatile boolean mLearnerReplicationEnabled = true;

  public RaftPeer(TimeWheel timeWheel, IRaftConfig raftConfig, IRaftMapper raftMapper) {
    _TimeWheel = timeWheel;
    _RaftConfig = raftConfig;
    _ZUid = raftConfig.getZUID();
    _RaftMapper = raftMapper;
    _ElectSchedule = new ScheduleHandler<>(_RaftConfig.getElectInSecond(), RaftPeer::stepDown);
    _TickSchedule =
        new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond().multipliedBy(2), RaftPeer::start);
    _HeartbeatSchedule =
        new ScheduleHandler<>(_RaftConfig.getHeartbeatInSecond(), RaftPeer::heartbeat);
    _PreVoteSchedule =
        new ScheduleHandler<>(
            _RaftConfig.getElectInSecond().multipliedBy(2), RaftPeer::preVoteTimeout);
    _CheckQuorumSchedule =
        new ScheduleHandler<>(
            _RaftConfig.getHeartbeatInSecond().multipliedBy(5), RaftPeer::checkQuorum);
    _MembershipChangeTimeoutSchedule =
        new ScheduleHandler<>(
            _RaftConfig.getHeartbeatInSecond().multipliedBy(10),
            RaftPeer::onMembershipChangeTimeout);
    _TransferTimeoutSchedule =
        new ScheduleHandler<>(
            _RaftConfig.getHeartbeatInSecond().multipliedBy(2),
            RaftPeer::onLeadershipTransferTimeout);
    _SelfGraph = RaftGraph.create("_Self_");
    _JointGraph = RaftGraph.create("_Joint_");
    _SelfMachine = RaftMachine.createBy(_ZUid.getPeerId(), OP_MODIFY);
    _SelfGraph.append(_SelfMachine);
    _JointGraph.append(_SelfMachine);
    _SnapshotFragmentMaxSize = _RaftConfig.getSnapshotFragmentMaxSize();
    _SyncBatchMaxSize = _RaftConfig.getSyncBatchMaxSize();
    _PipelineManager =
        new PipelineReplicationManager(
            _RaftConfig.getPipelineMaxInflight(), _RaftConfig.getPipelineInflightTimeoutMs());
  }

  private RaftMachine updateMachine(IRaftMachine machine, RaftState state) {
    RaftMachine update = RaftMachine.createBy(machine.peer(), OP_APPEND);
    update.from(machine);
    update.approve(state);
    return update;
  }

  private IRaftMachine beCandidate() {
    RaftMachine update = updateMachine(_SelfMachine, CANDIDATE);
    update.term(update.term() + 1);
    update.candidate(_SelfMachine.peer());
    return update;
  }

  private void heartbeat() {
    if (_SelfMachine.isInState(LEADER)) {
      timeTrigger(updateMachine(_SelfMachine, LEADER));
    } else {
      beatCancel();
    }
  }

  public void start(final IClusterNode _Node) {
    LogMeta meta = _RaftMapper.getLogMeta();
    _SelfMachine.term(meta.getTerm());
    _SelfMachine.commit(meta.getCommit());
    _SelfMachine.accept(meta.getAccept());
    _SelfMachine.index(meta.getIndex());
    _SelfMachine.indexTerm(meta.getIndexTerm());
    _SelfMachine.matchIndex(meta.getAccept());
    mClusterNode = _Node;
    if (_RaftConfig.isClusterMode()) {
      // 初始化集群角色
      graphInit(
          _SelfGraph,
          // _RaftConfig.getPeers().keySet() 是议会的成员
          _RaftConfig.getPeers().keySet(),
          // _RaftConfig.getNodes().keySet() 是集群节点成员，包括议会成员
          _RaftConfig.getNodes().keySet());
      // 启动集群连接
      graphUp(_SelfGraph.getPeers(), _RaftConfig.getNodes());

      // 恢复成员变更状态
      restoreMembershipChangeState();

      if (_RaftConfig.isInCongress()) {
        _SelfMachine.approve(FOLLOWER);
        // 延迟一个'投票周期'，等待议会完成连接。
        mTickTask =
            _TimeWheel.acquire(
                RaftPeer.this,
                new ScheduleHandler<>(_RaftConfig.getElectInSecond(), RaftPeer::start));
      } else {
        _SelfMachine.approve(CLIENT);
      }
      _Logger.debug("raft node init -> %s", _SelfMachine);
    } else {
      _SelfMachine.outside();
      _Logger.debug("not in cluster, single model!");
    }
  }

  private void graphInit(RaftGraph graph, Collection<Long> congress, Collection<Long> nodes) {
    for (long node : nodes) {
      // Graph中 装入所有需要管理的集群状态机，self已经装入了，此处不再重复
      IRaftMachine machine = RaftMachine.createBy(node, OP_MODIFY);
      // nodes 至少是配置图谱中的一员，所以
      machine.approve(congress.contains(node) ? FOLLOWER : CLIENT);
      graph.append(machine);
    }
  }

  private void graphUp(Map<Long, IRaftMachine> peers, Map<Long, RaftNode> nodes) {
    peers.forEach(
        (peer, machine) -> {
          // 仅连接 peer < self.peer 的节点
          if (peer < _SelfMachine.peer()) {
            RaftNode remote = nodes.get(peer);
            try {
              mClusterNode.setupPeer(remote.getHost(), remote.getPort());
              _Logger.debug("setup, connect [peer : %s:%d]", remote.getHost(), remote.getPort());
            } catch (Exception e) {
              _Logger.warning("peer connect error: %s:%d", e, remote.getHost(), remote.getPort());
            }
          }
        });
  }

  public void installSnapshot(List<IRaftControl> snapshot) {}

  /**
   * Leader 发送 Snapshot 给 Follower
   *
   * @param peer 目标节点 ID
   * @param machine 目标节点状态机
   * @param session 目标节点会话
   * @return 发送的 snapshot 消息列表
   */
  private List<ITriple> sendSnapshot(long peer, IRaftMachine machine, ISession session) {
    _Logger.info("sending snapshot to follower[%#x], session=%s", peer, session);

    SnapshotMeta meta = _RaftMapper.getSnapshotMeta();
    long lastIncludeIndex = meta.getCommit();
    long lastIncludeTerm = meta.getTerm();

    if (lastIncludeIndex <= 0) {
      _Logger.warning("no valid snapshot available for peer[%#x]", peer);
      return null;
    }

    SnapshotEntry snapshot = _RaftMapper.getSnapshot();
    byte[] data = snapshot != null ? snapshot.content() : null;
    long totalSize = data != null ? data.length : 0;

    _Logger.debug(
        "snapshot: lastIndex=%d@%d, size=%d", lastIncludeIndex, lastIncludeTerm, totalSize);

    List<ITriple> fragments = new LinkedList<>();
    long offset = 0;
    int fragmentSize = (int) Math.min(_SnapshotFragmentMaxSize, totalSize);

    while (offset < totalSize) {
      int remaining = (int) (totalSize - offset);
      int currentFragmentSize = Math.min(fragmentSize, remaining);
      byte[] fragmentData = new byte[currentFragmentSize];
      System.arraycopy(data, (int) offset, fragmentData, 0, currentFragmentSize);

      X7D_RaftSnapshot x7d = new X7D_RaftSnapshot();
      x7d.with(session);
      x7d.leader(_SelfMachine.peer());
      x7d.term(_SelfMachine.term());
      x7d.lastIncludeIndex(lastIncludeIndex);
      x7d.lastIncludeTerm(lastIncludeTerm);
      x7d.offset(offset);
      x7d.totalSize(totalSize);
      x7d.done(offset + currentFragmentSize >= totalSize);
      x7d.data(fragmentData);

      fragments.add(Triple.of(x7d, session, session.encoder()));

      _Logger.debug(
          "snapshot fragment: offset=%d, size=%d, done=%s",
          offset, currentFragmentSize, x7d.done());

      offset += currentFragmentSize;
    }

    // 更新 machine 的索引状态
    machine.index(lastIncludeIndex);
    machine.indexTerm(lastIncludeTerm);
    machine.matchIndex(lastIncludeIndex);

    _Logger.info("sent %d snapshot fragments to peer[%#x]", fragments.size(), peer);
    return fragments;
  }

  public void takeSnapshot(IRaftMapper snapshot) {
    long localTerm;
    if (_RaftMapper.getTotalSize() < _RaftConfig.getSnapshotMinSize()) {
      _Logger.debug("snapshot size less than threshold");
      return;
    }
    // 状态迁移未完成
    if (_SelfMachine.accept() <= _SelfMachine.commit()) {
      _Logger.debug(" applied < commit sync is running");
      return;
    }
    if (_SelfMachine.accept() >= _RaftMapper.getStartIndex()
        && _SelfMachine.accept() <= _RaftMapper.getEndIndex()) {
      localTerm = _RaftMapper.getEntryTerm(_SelfMachine.accept());
      _RaftMapper.updateSnapshotMeta(_SelfMachine.accept(), localTerm);
      _Logger.debug("take snapshot");
    }
    long lastSnapshotIndex = _RaftMapper.getSnapshotMeta().getCommit();
    if (lastSnapshotIndex > 0 && _RaftMapper.getStartIndex() <= lastSnapshotIndex) {
      _RaftMapper.truncatePrefix(lastSnapshotIndex + 1);
      _Logger.debug("snapshot truncate prefix %d", lastSnapshotIndex);
    }
  }

  private void start() {
    /*
     * 关闭TickTask,此时执行容器可能为ElectTask 或 TickTask自身
     * 由于Elect.timeout << Tick.timeout,此处不应出现Tick无法
     * 关闭的，或关闭异常。同时cancel 配置了lock 防止意外出现。
     */
    tickCancel();
    if (_SelfMachine.isLessThanState(FOLLOWER)) {
      _Logger.debug("peer[%#] → CLIENT/OUTSIDE, don't join congress", _SelfMachine.peer());
    } else {
      _Logger.debug("peer[ %#x ], start vote", _SelfMachine.peer());
      startVote();
    }
  }

  /** 此方法会在各种超时处理器中被启用，所以执行线程为TimeWheel.pool中的任意子线程 */
  private void startVote() {
    try {
      long wait = _Random.nextInt(70) + 50;
      Thread.sleep(wait);
      _Logger.debug("random wait for %d mills, then vote", wait);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // 恢复中断状态
      _Logger.warning("Thread interrupted during random wait");
    }
    timeTrigger(beCandidate());
  }

  private Map<Long, IRaftMachine> vote4me(long term) {
    // Learner 节点不参与选举
    if (_SelfMachine.isInState(RaftState.LEARNER)) {
      _Logger.debug("Learner node does not participate in election");
      return null;
    }

    tickCancel();
    updateTerm(term);
    _SelfMachine.leader(INVALID_PEER_ID);
    _SelfMachine.candidate(_SelfMachine.peer());
    mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
    _Logger.debug("vote4me %s", _SelfMachine.toPrimary());
    return RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph);
  }

  private void updateTerm(long term) {
    _SelfMachine.term(term);
    _RaftMapper.updateTerm(term);
  }

  private List<ITriple> createVotes(Map<Long, IRaftMachine> peers, IManager manager) {
    return peers != null && !peers.isEmpty()
        ? peers.keySet().stream()
            .filter(peer -> peer != _SelfMachine.peer())
            .map(
                peer -> {
                  ISession session = manager.fairLoadSessionByPrefix(peer);
                  if (session == null) {
                    _Logger.warning("elector :%#x session has not found", peer);
                    return null;
                  }
                  _Logger.debug("vote4me to %s", session);
                  X70_RaftVote x70 = new X70_RaftVote(_ZUid.getId());
                  x70.with(session);
                  x70.peer(peer);
                  x70.candidate(_SelfMachine.peer());
                  x70.term(_SelfMachine.term());
                  x70.index(_SelfMachine.index());
                  x70.indexTerm(_SelfMachine.indexTerm());
                  x70.accept(_SelfMachine.accept());
                  x70.commit(_SelfMachine.commit());
                  return x70;
                })
            .filter(Objects::nonNull)
            .map(this::map)
            .collect(Collectors.toList())
        : new LinkedList<>();
  }

  private List<ITriple> rejectThenVote(
      long rejectTo, long msgId, IManager manager, ISession session) {
    List<ITriple> broadcast = createVotes(vote4me(_SelfMachine.term()), manager);
    X74_RaftReject x74 = reject(RaftCode.OBSOLETE, rejectTo, msgId);
    broadcast.add(Triple.of(x74, session, session.encoder()));
    return broadcast;
  }

  public List<ITriple> vote4me(IRaftMachine update, IManager manager) {
    if (_SelfMachine.isInState(FOLLOWER)
        && update.index() <= _SelfMachine.index()
        && update.term() > _SelfMachine.term()
        && update.accept() <= _SelfMachine.accept()
        && update.candidate() == _SelfMachine.peer()) {
      return createVotes(vote4me(update.term()), manager);
    }
    _Logger.warning("check vote failed; now: %s\n%s", _SelfMachine, update);
    return null;
  }

  private void fromRecord(IRaftRecord record, RaftGraph graph, long peer) {
    IRaftMachine machine = getMachine(graph, peer);
    if (machine != null) {
      machine.term(record.term());
      machine.index(record.index());
      machine.indexTerm(record.indexTerm());
      machine.commit(record.commit());
      machine.accept(record.accept());
      machine.matchIndex(record.index());
      machine.candidate(record.candidate());
    }
  }

  /**
   * @return triple, fst:集群分发消息体,snd: → link ,分发消息是单体还是集合
   */
  private ITriple checkTerm(IRaftRecord record, long msgId, ISession session) {
    if (lowTerm(record.term())) {
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "{low term: reject %#x, mine:[%d@%d(%d)] from:[%d@%d(%d)]}",
            record.peer(),
            _SelfMachine.index(),
            _SelfMachine.indexTerm(),
            _SelfMachine.term(),
            record.index(),
            record.indexTerm(),
            record.term());
      }
      return Triple.of(reject(LOWER_TERM, record.peer(), msgId).with(session), null, SINGLE);
    }
    if (highTerm(record.term())) {
      // term > my term
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "{step down [%#x → follower] high term [ %d > %d ]}",
            _SelfMachine.peer(), record.term(), _SelfMachine.term());
      }
      stepDown(record.term());
    }
    return null;
  }

  public ITriple onVote(X70_RaftVote x70, IManager manager, ISession session) {
    fromRecord(x70, _SelfGraph, x70.candidate());
    // 联合一致
    if (_SelfMachine.isInState(JOINT)) {
      fromRecord(x70, _JointGraph, x70.candidate());
    }
    ITriple reject = checkTerm(x70, x70.msgId(), session);
    if (reject != null) {
      return reject;
    } else if (_SelfMachine.isInState(FOLLOWER)) {
      // x70.term > my term → my term = x70.term
      if (_SelfMachine.commit() > x70.commit()
          || _SelfMachine.accept() > x70.accept()
          || _SelfMachine.indexTerm() > x70.indexTerm()
          || _SelfMachine.index() > x70.index()) {
        if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
          _Logger.debug(
              "less than me; reject[%s]  mine:[ %d@%d,a:%d | c:%d ] > candidate:[ %d@%d, a:%d | c:%d ] then vote4me[ follower → candidate ] ",
              OBSOLETE,
              _SelfMachine.index(),
              _SelfMachine.indexTerm(),
              _SelfMachine.accept(),
              _SelfMachine.commit(),
              x70.index(),
              x70.indexTerm(),
              x70.accept(),
              x70.commit());
        }
        return Triple.of(
            rejectThenVote(x70.candidate(), x70.msgId(), manager, session), null, BATCH);
      } else {
        // 投票给候选人
        if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
          _Logger.debug(
              "new term [ %d ] follower [ %#x ] → elector | candidate:[ %#x ]",
              x70.term(), _SelfMachine.peer(), x70.candidate());
        }
        IProtocol ballot = stepUp(x70.term(), x70.candidate(), x70.msgId());
        return Triple.of(ballot.with(session), null, SINGLE);
      }
    }
    // elector|leader|candidate,one of these states ，candidate != INDEX_NAN 不需要重复判断
    else if (_SelfMachine.candidate() != x70.candidate()) {
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "already vote [elector ×] | vote for:[ %#x not ♂ %#x ]",
            _SelfMachine.candidate(), x70.candidate());
      }
      return Triple.of(
          reject(ALREADY_VOTE, x70.candidate(), x70.msgId()).with(session), null, SINGLE);
    } else {
      // 重复投票给相同的候选人
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "same vote [elector x] | vote for:[%#x ♂ %#x]",
            _SelfMachine.candidate(), x70.candidate());
      }
      IProtocol ballot = ballot(x70.msgId());
      return Triple.of(ballot.with(session), null, SINGLE);
    }
  }

  public ITriple onBallot(X71_RaftBallot x71, IManager manager, ISession session) {
    fromRecord(x71, _SelfGraph, x71.peer());
    if (_SelfMachine.isInState(JOINT)) {
      fromRecord(x71, _JointGraph, x71.peer());
    }
    ITriple reject = checkTerm(x71, x71.msgId(), session);
    if (reject != null) {
      return reject;
    } else {
      boolean condition =
          _SelfMachine.isInState(CANDIDATE)
              && _SelfMachine.term() == x71.term()
              && _SelfGraph.isMajorAccept(_SelfMachine.peer(), _SelfMachine.term());
      boolean joint =
          _SelfMachine.isInState(JOINT)
              && _JointGraph.isMajorAccept(_SelfMachine.peer(), _SelfMachine.term());
      if (joint && condition || !_SelfMachine.isInState(JOINT) && condition) {
        // term == my term
        lead();
        if (joint) {
          return Triple.of(
              followersAppend(
                  RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager),
              null,
              BATCH);
        } else {
          return Triple.of(followersAppend(_SelfGraph.getPeers(), manager), null, BATCH);
        }
      }
    }
    return null;
  }

  public ITriple onAppend(X72_RaftAppend x72, ISession session) {
    ITriple reject = checkTerm(x72, x72.msgId(), session);
    if (reject != null) {
      return reject;
    }
    // term == my term
    RaftState state = RaftState.valueOf(_SelfMachine.state());
    switch (state) {
      case LEADER -> {
        _Logger.warning(
            "state:[%s], leader[%#x] → the other [%#x] ",
            state, _SelfMachine.leader(), x72.leader());
        return Triple.of(
            reject(SPLIT_CLUSTER, x72.leader(), x72.msgId()).with(session), null, SINGLE);
      }
      case FOLLOWER, ELECTOR -> {
        if (x72.payload() != null) {
          _RecvLogQueue.addAll(x72.deserializeSub(ListSerial._Factory(LogEntry::new)));
        }
        fromRecord(x72, _SelfGraph, x72.leader());
        if (_SelfMachine.isInState(JOINT)) {
          fromRecord(x72, _JointGraph, x72.leader());
        }
        return follow(
            x72.term(),
            x72.leader(),
            x72.commit(),
            x72.index(),
            x72.indexTerm(),
            x72.msgId(),
            session);
      }
      default -> _Logger.warning("illegal state :%s", RaftState.roleOf(_SelfMachine.state()));
    }
    return null;
  }

  public ITriple onReject(X74_RaftReject x74, ISession session) {
    fromRecord(x74, _SelfGraph, x74.peer());
    if (_SelfMachine.isInState(JOINT)) {
      fromRecord(x74, _JointGraph, x74.peer());
    }
    IRaftMachine machine = getMachine(_SelfGraph, x74.peer());

    // Pipeline 模式：处理拒绝，回退 inflight 窗口
    if (mPipelineEnabled && _SelfMachine.isInState(LEADER)) {
      long newNextIndex = _PipelineManager.onReject(x74.peer(), x74.index(), x74.indexTerm());
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "Pipeline: received reject from %#x, newNextIndex=%d", x74.peer(), newNextIndex);
      }
    }

    if (highTerm(x74.term())) {
      // peer's term > my term
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            " → reject {step down [%s → follower]}", RaftState.roleOf(_SelfMachine.state()));
      }
      stepDown(x74.term());
    } else {
      // peer's term == my term
      // peer's term <  my term 是不存在的情况
      STEP_DOWN:
      {
        switch (RaftCode.valueOf(x74.code())) {
          case CONFLICT -> {
            // follower 持有的 log 纪录 index@term 与leader 投送的不一致，后续进行覆盖同步
            if (_SelfMachine.isInState(LEADER)) {
              if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
                _Logger.debug(
                    "follower %#x,match failed,rollback %d@%d",
                    x74.peer(), x74.index(), x74.indexTerm());
              }

              // Pipeline 模式：使用回退后的 nextIndex 重新发送
              boolean usePipelineRetry =
                  mPipelineEnabled && _PipelineManager.getWindow(x74.peer()) != null;
              IProtocol append =
                  machine != null
                      ? createAppend(
                              machine,
                              min(
                                  (int) (_SelfMachine.commit() - machine.index()),
                                  _SyncBatchMaxSize),
                              usePipelineRetry)
                          .with(session)
                      : null;

              if (_SelfMachine.isInState(JOINT)) {
                IRaftMachine jMachine = getMachine(_JointGraph, x74.peer());
                IProtocol jAppend =
                    jMachine != null
                        ? createAppend(
                                jMachine,
                                min(
                                    (int) (_SelfMachine.commit() - jMachine.index()),
                                    _SyncBatchMaxSize))
                            .with(session)
                        : null;
                if (append != null && jAppend != null) {
                  return Triple.of(List.of(append, jAppend), null, BATCH);
                } else if (jAppend != null) {
                  return Triple.of(jAppend, null, SINGLE);
                }
              }
              if (append != null) {
                return Triple.of(append, null, SINGLE);
              }
            } else {
              _Logger.warning(
                  "self %#x is old leader & send logs → %#x,next-index wasn't catchup",
                  _SelfMachine.peer(), x74.peer());
              // Ignore
            }
          }
          case SPLIT_CLUSTER -> {
            if (_SelfMachine.isInState(LEADER)) {
              if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
                _Logger.debug("other leader:[%#x]", x74.leader());
              }
            }
          }
          case ALREADY_VOTE -> {
            if (_SelfMachine.isInState(CANDIDATE)) {
              _Logger.debug("elector[%#x] has vote", x74.peer());
            }
          }
          case OBSOLETE -> {
            if (_SelfMachine.isGreaterThanState(FOLLOWER)) {
              stepDown(_SelfMachine.term());
            }
            break STEP_DOWN;
          }
        }
        if (_SelfGraph.isMajorReject(_SelfMachine.peer(), _SelfMachine.term())) {
          stepDown(_SelfMachine.term());
        }
      }
    }
    return null;
  }

  /**
   * follower → leader
   *
   * @return result triple fst : broadcast to peer snd : leader handle x77 → notify client trd :
   *     operator type 「 SINGLE/BATCH 」
   */
  public ITriple onAccept(X73_RaftAccept x73, IManager manager) {
    fromRecord(x73, _SelfGraph, x73.peer());
    if (_SelfMachine.isInState(JOINT)) {
      fromRecord(x73, _JointGraph, x73.peer());
    }

    // Pipeline 模式：更新 inflight 窗口
    if (mPipelineEnabled && _SelfMachine.isInState(LEADER)) {
      _PipelineManager.onAck(x73.peer(), x73.accept());

      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "Pipeline: received accept from %#x, index=%d, inflight=%d",
            x73.peer(), x73.accept(), _PipelineManager.getInflightCount(x73.peer()));
      }

      // 继续 Pipeline 复制（如果窗口允许且还有更多日志）
      continuePipelineReplication(x73.peer(), manager);
    }

    /*
     * member.accept > leader.commit 完成半数 match 之后只触发一次 leader commit
     */
    long next = _SelfMachine.commit() + 1;
    boolean condition = _SelfMachine.isInState(LEADER) && _SelfGraph.isMajorAccept(next);
    boolean joint = _SelfMachine.isInState(JOINT) && _JointGraph.isMajorAccept(next);

    // Lease 续期：收到多数派确认时续期
    if (mLeaseReadEnabled && _SelfMachine.isInState(LEADER)) {
      if (condition && (joint || !_SelfMachine.isInState(JOINT))) {
        if (_LeaseManager.renewLease()) {
          _Logger.debug("Lease renewed on majority accept");
        }
      }
    }

    if (x73.accept() >= next && condition && (joint || !_SelfMachine.isInState(JOINT))) {
      _SelfMachine.commit(next, _RaftMapper);
      // 检查并处理等待中的 ReadIndex 请求
      checkReadIndexWaiters();
      LogEntry entry = _RaftMapper.getEntry(next);
      if (entry.client() != _SelfMachine.peer()) {
        // leader → client → device
        ISession session = manager.fairLoadSessionByPrefix(entry.client());
        if (session != null) {
          return Triple.of(createNotify(entry).with(session), createNotify(entry), SINGLE);
        }
      } else {
        // leader ≡ client → device
        return Triple.of(null, createNotify(entry), NULL);
      }
    } else if (x73.accept() < next) { // machine.accept < next → 已经执行过 self commit 过, 无须重复 commit
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "already commit %d, follow[ %#x ] accept: %d commit: %d",
            _SelfMachine.commit(), x73.peer(), x73.accept(), x73.commit());
      }
    } else {
      // !_SelfGraph.isMajorAccept(next) → 未满足 commit 条件, 不执行 commit
      if (_Logger.isEnable(org.slf4j.event.Level.DEBUG)) {
        _Logger.debug(
            "member %#x, catchup:%d → %d", x73.peer(), x73.accept(), _SelfMachine.accept());
      }
    }
    return null;
  }

  private IRaftMachine getMachine(RaftGraph graph, long peer) {
    IRaftMachine machine = graph.get(peer);
    if (machine == null) {
      _Logger.warning("%s:peer %#x is not found", graph.name(), peer);
      return null;
    }
    return machine;
  }

  private X71_RaftBallot ballot(long msgId) {
    X71_RaftBallot vote = new X71_RaftBallot(msgId);
    vote.peer(_SelfMachine.peer());
    vote.term(_SelfMachine.term());
    vote.index(_SelfMachine.index());
    vote.indexTerm(_SelfMachine.indexTerm());
    vote.accept(_SelfMachine.accept());
    vote.commit(_SelfMachine.commit());
    vote.candidate(_SelfMachine.candidate());
    return vote;
  }

  private X73_RaftAccept accept(long msgId) {
    X73_RaftAccept accept = new X73_RaftAccept(msgId);
    accept.peer(_SelfMachine.peer());
    accept.term(_SelfMachine.term());
    accept.index(_SelfMachine.index());
    accept.indexTerm(_SelfMachine.indexTerm());
    accept.leader(_SelfMachine.leader());
    accept.commit(_SelfMachine.commit());
    return accept;
  }

  private X74_RaftReject reject(RaftCode raftCode, long rejectTo, long msgId) {
    X74_RaftReject reject = new X74_RaftReject(msgId);
    reject.peer(_SelfMachine.peer());
    reject.term(_SelfMachine.term());
    reject.index(_SelfMachine.index());
    reject.indexTerm(_SelfMachine.indexTerm());
    reject.accept(_SelfMachine.accept());
    reject.commit(_SelfMachine.commit());
    reject.reject(rejectTo);
    reject.candidate(_SelfMachine.candidate());
    reject.leader(_SelfMachine.leader());
    reject.setCode(raftCode.getCode());
    reject.state(_SelfMachine.state());
    return reject;
  }

  private X71_RaftBallot stepUp(long term, long candidate, long msgId) {
    tickCancel();
    updateTerm(term);
    _SelfMachine.leader(INVALID_PEER_ID);
    _SelfMachine.candidate(candidate);
    _SelfMachine.approve(ELECTOR);
    mElectTask = _TimeWheel.acquire(this, _ElectSchedule);
    _Logger.debug("[follower → elector] %s", _SelfMachine.toPrimary());
    return ballot(msgId);
  }

  private void stepDown(long term) {
    if (_SelfMachine.isGreaterThanState(FOLLOWER)) {
      _Logger.debug("step down:%s → FOLLOWER", RaftState.roleOf(_SelfMachine.state()));
      beatCancel();
      electCancel();
      // 取消 CheckQuorum 任务
      if (mCheckQuorumTask != null) {
        mCheckQuorumTask.cancel();
        mCheckQuorumTask = null;
      }
      updateTerm(term);
      _SelfMachine.leader(INVALID_PEER_ID);
      _SelfMachine.candidate(INVALID_PEER_ID);
      _SelfMachine.approve(FOLLOWER);
      mTickTask = _TimeWheel.acquire(this, _TickSchedule);

    } else {
      _Logger.warning("step down [ignore],state now[%s]", RaftState.roleOf(_SelfMachine.state()));
    }
  }

  private void lead() {
    electCancel();
    _SelfMachine.leader(_SelfMachine.peer());
    mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
    // 启动 CheckQuorum 定时任务
    if (mCheckQuorumTask != null) {
      mCheckQuorumTask.cancel();
    }
    _ActivePeers.set(0);
    _LastQuorumCheckTime = System.currentTimeMillis();
    mCheckQuorumTask = _TimeWheel.acquire(this, _CheckQuorumSchedule);

    // 创建 Leader 租约（租约时长 = 2 * heartbeat 间隔）
    if (mLeaseReadEnabled) {
      long leaseDuration = _RaftConfig.getHeartbeatInSecond().toMillis() * 2;
      _LeaseManager.createLease(_SelfMachine.peer(), _SelfMachine.term(), leaseDuration);
    }

    _Logger.debug("be leader → %s", _SelfMachine.toPrimary());
  }

  private void stepDown() {
    // 使租约失效
    if (mLeaseReadEnabled) {
      _LeaseManager.invalidateLease("Step down");
    }
    timeTrigger(updateMachine(_SelfMachine, FOLLOWER));
  }

  private ITriple follow(
      long term,
      long leader,
      long commit,
      long preIndex,
      long preIndexTerm,
      long msgId,
      ISession session) {
    tickCancel();
    _SelfMachine.follow(term, leader, _RaftMapper);
    mTickTask = _TimeWheel.acquire(this, _TickSchedule);
    if (catchUp(preIndex, preIndexTerm)) {
      List<X77_RaftNotify> notifies = null;
      if (_SelfMachine.commit() < commit) {
        for (long i = preIndex; i <= _SelfMachine.accept(); i++) {
          if (notifies == null) notifies = new LinkedList<>();
          LogEntry entry = _RaftMapper.getEntry(i);
          if (entry != null && entry.client() != _SelfMachine.peer()) {
            notifies.add(createNotify(entry));
          } else if (entry == null) {
            _Logger.warning(
                "entry %d is null,self accept:%d; check segment", i, _SelfMachine.accept());
          }
        }
        _SelfMachine.commit(min(_SelfMachine.accept(), commit), _RaftMapper);
      }
      return Triple.of(accept(msgId).with(session), notifies, SINGLE);
    } else {
      _Logger.debug("catch up failed reject → %#x ", leader);
      return Triple.of(reject(CONFLICT, leader, msgId).with(session), null, SINGLE);
    }
  }

  private void tickCancel() {
    if (mTickTask != null) {
      mTickTask.cancel();
    }
  }

  private void beatCancel() {
    if (mHeartbeatTask != null) {
      mHeartbeatTask.cancel();
    }
  }

  private void electCancel() {
    if (mElectTask != null) {
      mElectTask.cancel();
    }
  }

  private boolean highTerm(long term) {
    return term > _SelfMachine.term();
  }

  private boolean lowTerm(long term) {
    return term < _SelfMachine.term();
  }

  public LogEntry getLogEntry(long index) {
    _Logger.debug("peer get log entry: %d", index);
    return _RaftMapper.getEntry(index);
  }

  public List<LogEntry> diff() {
    long start = _SelfMachine.accept();
    long commit = _SelfMachine.commit();
    long current = _SelfMachine.index();
    if (start >= commit) {
      return null;
    } else {
      long end = min(current, commit);
      List<LogEntry> result = new ArrayList<>((int) (end - start));
      for (long i = start; i <= end; i++) {
        LogEntry entry = _RaftMapper.getEntry(i);
        if (entry != null) {
          result.add(entry);
        }
      }
      return result.isEmpty() ? null : result;
    }
  }

  private boolean catchUp(long preIndex, long preIndexTerm) {
    CHECK:
    {
      if (_SelfMachine.index() == preIndex && _SelfMachine.indexTerm() == preIndexTerm) {
        /*
          follower 与 Leader 依据 index@term 进行对齐
          开始同步接受缓存在_LogQueue中的数据
        */
        for (Iterator<LogEntry> it = _RecvLogQueue.iterator(); it.hasNext(); ) {
          LogEntry entry = it.next();
          if (_RaftMapper.append(entry)) {
            _SelfMachine.accept(entry.index(), entry.term());
            _Logger.debug("follower catch up %d@%d", entry.index(), entry.term());
          }
          it.remove();
        }
        break CHECK;
      } else if (_SelfMachine.index() == 0 && preIndex == 0) {
        // 初始态，raft-machine 中不包含任何 log 记录
        _Logger.debug("self machine is empty");
        break CHECK;
      } else if (_SelfMachine.index() < preIndex) {
        /*
         * 1.follower 未将自身index 同步给leader，所以leader 发送了一个最新状态过来
         * 后续执行accept() → response 会将index 同步给leader。
         * 2.leader 已经获知follower的需求 但是 self.index < leader.start
         * 需要follower先完成snapshot的安装才能正常同步。
         */
        _Logger.debug(
            "leader doesn't know my next  || leader hasn't log, need to install snapshot");
      } else if (rollback(_RecvLogQueue, preIndex)) {
        // _SelfMachine.index >= preIndex
        break CHECK;
      }
      _RecvLogQueue.clear();
      return false;
    }
    return true;
  }

  private boolean rollback(final Queue<LogEntry> _LogQueue, long preIndex) {
    if (preIndex < 1) {
      _Logger.debug("rollback leader empty");
      // preIndex ==0
      _SelfMachine.reset();
      _RaftMapper.reset();
      if (_LogQueue.isEmpty()) {
        _Logger.debug("empty leader → clean!");
        // preIndex == 1 && queue.empty → reject
      } else {
        _Logger.debug("follower empty,accept leader's at all");

        // 刚好follow是空的, leader投过来的数据就接收了
        LogEntry entry;
        // mapper.append false的状态包含了, _LogQueue.empty的情况
        while (_RaftMapper.append(entry = _LogQueue.poll())) {
          assert entry != null; // 其实没用,mapper.append的结果已经保障了entry != null
          _SelfMachine.accept(entry.index(), entry.term());
          _Logger.debug("follower catch up %d@%d", entry.index(), entry.term());
        }
      }
      return true;
    } else {
      _Logger.debug("rollback → leader [%d]", preIndex);
      // preIndex >= MIN_START(1)
      LogEntry rollback = _RaftMapper.truncateSuffix(preIndex);
      if (rollback != null) {
        _SelfMachine.rollBack(rollback.index(), rollback.term(), _RaftMapper);
        _Logger.debug("machine rollback %d@%d", rollback.index(), rollback.term());
        LogEntry entry;
        while (_RaftMapper.append(entry = _LogQueue.poll())) {
          assert entry != null;
          _SelfMachine.accept(entry.index(), entry.term());
          _Logger.debug("follower catch up %d@%d", entry.index(), entry.term());
        }
        return true;
      }
      // rollback == null
      else if (preIndex >= _RaftMapper.getStartIndex()) {
        _Logger.fetal("lost data → reset machine & mapper ");
        _SelfMachine.reset();
        _RaftMapper.reset();
      }
    }
    return false;
  }

  public List<ITriple> turnDown(IRaftMachine update) {
    CHECK:
    {
      if (_SelfMachine.isGreaterThanState(FOLLOWER)
          && _SelfMachine.isLessThanState(LEADER)
          && update.term() >= _SelfMachine.term()) {
        _Logger.debug("elect time out → turn to follower");
        stepDown(update.term());
        break CHECK;
      }
      _Logger.warning("state %s event [ignore]", RaftState.roleOf(_SelfMachine.state()));
    }
    return null;
  }

  public List<ITriple> logAppend(IRaftMachine update, IManager manager) {
    if (_SelfMachine.peer() == update.peer()
        && _SelfMachine.term() >= update.term()
        && _SelfMachine.index() >= update.index()
        && _SelfMachine.indexTerm() >= update.indexTerm()
        && _SelfMachine.commit() >= update.commit()) {
      mHeartbeatTask = _TimeWheel.acquire(this, _HeartbeatSchedule);
      // 使用 Pipeline 模式发送日志
      boolean usePipeline = mPipelineEnabled;
      return followersAppend(
          RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager, usePipeline);
    }
    // state change => ignore
    _Logger.warning("check leader heartbeat failed; now:%s", _SelfMachine);
    return null;
  }

  public List<ITriple> onSubmit(IoSerial request, IManager manager, long origin, int factory) {
    switch (RaftState.valueOf(_SelfMachine.state())) {
      case LEADER -> {
        List<ITriple> responses =
            leaderAppend(request.encoded(), _SelfMachine.peer(), origin, factory, manager);
        if (responses == null) {
          stepDown(_SelfMachine.term());
          return null;
        } else {
          return responses;
        }
      }
      case CLIENT, FOLLOWER -> {
        ISession session = manager.fairLoadSessionByPrefix(_SelfMachine.leader());
        if (session != null) {
          _Logger.debug(
              " [%#x][%s] → leader[%#x] x75, device[%#x] ",
              _SelfMachine.peer(),
              RaftState.roleOf(_SelfMachine.state()),
              _SelfMachine.leader(),
              origin);
          X75_RaftReq x75 = new X75_RaftReq(generateId());
          x75.withSub(request);
          x75.origin(origin);
          x75.factory(factory);
          x75.client(_SelfMachine.peer());
          x75.with(session);
          return Collections.singletonList(Triple.of(x75, session, session.encoder()));
        } else {
          _Logger.fetal("Leader connection miss,wait for reconnecting");
        }
      }
      default -> {
        _Logger.fetal(
            "cluster is electing self[%#x]→ %s",
            _SelfMachine.peer(), RaftState.roleOf(_SelfMachine.state()));
        return null;
      }
    }
    return null;
  }

  public ITriple onRequest(X75_RaftReq x75, IManager manager, ISession session) {
    if (_SelfMachine.isInState(LEADER)) {
      List<ITriple> appends =
          leaderAppend(x75.payload(), x75.client(), x75.origin(), x75.factory(), manager);
      if (appends != null && !appends.isEmpty()) {
        return Triple.of(
            appends,
            response(SUCCESS, x75.client(), x75.msgId(), x75.origin()).with(session),
            BATCH);
      } else {
        return Triple.of(
            null,
            response(WAL_FAILED, x75.client(), x75.msgId(), x75.origin()).with(session),
            NULL);
      }
    } else {
      return Triple.of(
          null,
          response(ILLEGAL_STATE, x75.client(), x75.msgId(), x75.origin()).with(session),
          NULL);
    }
  }

  // client receive x76
  public ITriple onResponse(X76_RaftResp x76) {
    return Triple.of(null, x76, NULL);
  }

  public ITriple onNotify(X77_RaftNotify x77) {
    _Logger.debug("client[%#x] onNotify", _SelfMachine.peer());
    return Triple.of(null, x77, NULL);
  }

  public List<ITriple> onModify(RaftNode delta, IManager manager) {
    X78_RaftModify x78 = new X78_RaftModify(generateId());
    long leader = _SelfMachine.leader();
    _RaftConfig.change(delta);
    x78.newGraph(leader, _RaftConfig.getPeers().keySet());
    RaftState state = RaftState.valueOf(_SelfMachine.state());
    switch (state) {
      case LEADER -> {
        // old_graph
        _SelfMachine.modify();
        List<ITriple> joints = new LinkedList<>();
        long[] peers = x78.getNewGraph();
        for (long peer : peers) {
          _JointGraph.append(RaftMachine.createBy(peer, delta.operation()));
        }
        Map<Long, IRaftMachine> union = RaftGraph.join(leader, _SelfGraph, _JointGraph);
        if (union != null && !union.isEmpty()) {
          union.forEach(
              (peer, machine) -> {
                ISession session = manager.fairLoadSessionByPrefix(peer);
                if (session != null) {
                  X78_RaftModify copy = x78.duplicate();
                  copy.with(session);
                  copy.msgId(_ZUid.getId());
                  joints.add(Triple.of(copy, session, session.encoder()));
                }
              });
          return joints;
        }
      }
      case CLIENT, FOLLOWER -> {
        ISession session = manager.fairLoadSessionByPrefix(leader);
        return List.of(Triple.of(x78, session, session.encoder()));
      }
        // ignore ELECTOR, CANDIDATE, GATE
      default -> _Logger.warning("illegal state: [%s]", state);
    }
    return null;
  }

  /*
     1. 转换leader 状态为 joint union 状态
     2. 向 old_graph 和 new_graph 同时发送状态信息
     3.
  */
  public ITriple onModify(X78_RaftModify x78, IManager manager) {
    long[] peers = x78.getNewGraph();
    long leader = x78.getLeader();
    X7A_RaftJoint x7a = new X7A_RaftJoint();
    x7a.code(SUCCESS.getCode());
    x7a.msgId(x78.msgId());
    x7a.peer(_SelfMachine.peer());
    X7B_RaftConfirm x7c = new X7B_RaftConfirm();
    x7c.peer(_SelfMachine.peer());
    x7c.term(_SelfMachine.term());
    x7c.commit(_SelfMachine.commit());
    x7c.index(_SelfMachine.index());
    x7c.indexTerm(_SelfMachine.indexTerm());
    x7c.leader(_SelfMachine.leader());
    RaftState state = RaftState.valueOf(_SelfMachine.state());
    for (long peer : peers) {
      _JointGraph.append(RaftMachine.createBy(peer, OP_MODIFY));
    }

    // 进入 JOINT 阶段
    if (_MembershipChangeManager.hasActiveChange()) {
      _MembershipChangeManager.enterJointPhase();
      saveMembershipChangeState();
      _Logger.debug("Entered JOINT phase for membership change");
    }

    switch (state) {
      case LEADER -> {
        // old_graph
        _SelfMachine.modify();
        List<IProtocol> joints = new LinkedList<>();
        Map<Long, IRaftMachine> union = RaftGraph.join(leader, _SelfGraph, _JointGraph);
        if (union != null) {
          union.forEach(
              (peer, machine) -> {
                ISession session = manager.fairLoadSessionByPrefix(peer);
                if (session != null) {
                  X78_RaftModify copy = x78.duplicate();
                  copy.with(session);
                  copy.msgId(_ZUid.getId());
                  joints.add(copy);
                }
              });
          return Triple.of(joints, x7a, BATCH);
        }
        x7a.code(GRAPH_CONFIG.getCode());
        return Triple.of(null, x7a, NULL);
      }
      case CLIENT, OUTSIDE -> {
        ITriple confirm = null;
        for (long peer : peers) {
          if (peer == _SelfMachine.peer()) {
            _SelfMachine.reset();
            _SelfMachine.modify(FOLLOWER);
            _SelfMachine.leader(leader);
            confirm = Triple.of(x7c, null, SINGLE);
          }
        }
        if (confirm != null) {
          return confirm;
        }
      }
      case FOLLOWER -> {
        if (_SelfMachine.leader() == leader) {
          _SelfMachine.modify();
          return Triple.of(x7c, null, SINGLE);
        }
        // else 选举态 不改变状态
        _Logger.warning(
            "expect:{ FOLLOW → %#x }, from:%#x ; illegal state", _SelfMachine.leader(), leader);
      }
      default -> {
        // ignore ELECTOR, CANDIDATE, GATE
        _Logger.warning("illegal state: [%s]", state);
        x7a.code(ILLEGAL_STATE.getCode());
        return Triple.of(null, x7a, NULL);
      }
    }
    return null;
  }

  public ITriple onConfirm(X7B_RaftConfirm x7c, IManager manager) {
    if (_SelfMachine.isInState(JOINT)) {
      fromRecord(x7c, _SelfGraph, x7c.peer());
      fromRecord(x7c, _JointGraph, x7c.peer());

      // 确认节点响应
      _MembershipChangeManager.confirmPeer(x7c.peer());

      if (_SelfGraph.isMajorConfirm() && _JointGraph.isMajorConfirm()) {
        _SelfMachine.confirm();
        _SelfGraph.resetTo(_JointGraph);

        // 确认成员变更完成
        confirmMembershipChange();

        Map<Long, IRaftMachine> union =
            RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph);
        if (union != null) {
          return Triple.of(
              union.entrySet().stream()
                  .map(
                      entry -> {
                        X79_RaftConfirm x79 = new X79_RaftConfirm();
                        x79.peer(entry.getKey());
                        x79.code(SUCCESS.getCode());
                        x79.state(entry.getValue().state());
                        ISession ps = manager.fairLoadSessionByPrefix(x79.peer());
                        if (ps != null) {
                          return x79.with(ps);
                        }
                        return null;
                      })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList()),
              null,
              BATCH);
        }
        _Logger.warning("graph union NULL");
      }
    }
    // leader 已经完成 confirm 忽略
    return null;
  }

  public void onConfirm(X79_RaftConfirm x79) {
    if (_SelfMachine.isInState(JOINT)) {
      IRaftMachine leader = getMachine(_JointGraph, x79.peer());
      if (leader != null) {
        leader.confirm();
      }
      _SelfMachine.confirm();
      _SelfGraph.resetTo(_JointGraph);
      _Logger.debug("joint confirm");
    }
  }

  /** 开始成员变更流程（Joint Consensus） 阶段1: COLD → JOINT */
  public List<ITriple> startMembershipChange(RaftNode delta, IManager manager) {
    // 检查是否已有进行中的变更
    if (_MembershipChangeManager.hasActiveChange()) {
      _Logger.warning(
          "Membership change already in progress: %s",
          _MembershipChangeManager.getCurrentTransaction());
      return null;
    }

    // 获取旧配置
    Collection<Long> oldPeers = new ArrayList<>(_SelfGraph.getPeers().keySet());

    // 应用配置变更
    _RaftConfig.change(delta);
    Collection<Long> newPeers = _RaftConfig.getPeers().keySet();

    // 开始变更事务
    long changeId = generateId();
    if (!_MembershipChangeManager.startChange(changeId, _SelfMachine.peer(), oldPeers, newPeers)) {
      _Logger.warning("Failed to start membership change %d", changeId);
      return null;
    }

    // 启动超时检查
    if (mMembershipChangeTimeoutTask != null) {
      mMembershipChangeTimeoutTask.cancel();
    }
    mMembershipChangeTimeoutTask = _TimeWheel.acquire(this, _MembershipChangeTimeoutSchedule);

    _Logger.info("Started membership change %d: old=%s, new=%s", changeId, oldPeers, newPeers);

    // 调用现有的 onModify 逻辑
    return onModify(delta, manager);
  }

  /** 成员变更超时处理 */
  private void onMembershipChangeTimeout() {
    MembershipChangeManager.ChangeTransaction tx = _MembershipChangeManager.getCurrentTransaction();
    if (tx == null) {
      return;
    }

    if (tx.isTimeout()) {
      _Logger.warning("Membership change %d timeout, rolling back", tx.getId());
      rollbackMembershipChange("Timeout after " + tx.getElapsedTime());
    } else {
      // 继续监控
      mMembershipChangeTimeoutTask = _TimeWheel.acquire(this, _MembershipChangeTimeoutSchedule);
    }
  }

  /** 回滚成员变更 */
  private void rollbackMembershipChange(String reason) {
    MembershipChangeManager.ChangeTransaction tx = _MembershipChangeManager.getCurrentTransaction();
    if (tx == null) {
      return;
    }

    // 标记为回滚状态
    _MembershipChangeManager.rollback(reason);

    // 恢复状态
    _SelfMachine.confirm(); // 清除 JOINT 状态
    _JointGraph.resetTo(_SelfGraph); // 恢复旧配置

    // 清理任务
    if (mMembershipChangeTimeoutTask != null) {
      mMembershipChangeTimeoutTask.cancel();
      mMembershipChangeTimeoutTask = null;
    }

    // 持久化回滚状态
    saveMembershipChangeState();

    // 完成事务
    tx = _MembershipChangeManager.complete();
    _Logger.warning("Membership change %d rolled back: %s", tx.getId(), reason);
  }

  /** 确认成员变更完成 阶段2: JOINT → CNEW */
  private void confirmMembershipChange() {
    MembershipChangeManager.ChangeTransaction tx = _MembershipChangeManager.getCurrentTransaction();
    if (tx == null || !_MembershipChangeManager.isInJointPhase()) {
      return;
    }

    // 进入提交阶段
    _MembershipChangeManager.enterCommittingPhase();

    // 确认变更
    _MembershipChangeManager.confirmChange();

    // 清理任务
    if (mMembershipChangeTimeoutTask != null) {
      mMembershipChangeTimeoutTask.cancel();
      mMembershipChangeTimeoutTask = null;
    }

    // 持久化完成状态
    saveMembershipChangeState();

    // 完成事务
    tx = _MembershipChangeManager.complete();
    _Logger.info("Membership change %d confirmed successfully", tx.getId());
  }

  /** 持久化成员变更状态 */
  private void saveMembershipChangeState() {
    MembershipChangeManager.ChangeTransaction tx = _MembershipChangeManager.getCurrentTransaction();
    if (tx != null) {
      var config =
          com.isahl.chess.knight.raft.model.replicate.MembershipConfig.fromTransaction(
              tx, _SelfMachine.index(), _SelfMachine.term());
      _RaftMapper.saveMembershipConfig(config);
    }
  }

  /** 恢复成员变更状态（启动时调用） */
  private void restoreMembershipChangeState() {
    var config = _RaftMapper.getMembershipConfig();
    if (config == null) {
      return;
    }

    MembershipChangeManager.Phase phase = config.getPhase();
    if (phase == MembershipChangeManager.Phase.IDLE
        || phase == MembershipChangeManager.Phase.CONFIRMED
        || phase == MembershipChangeManager.Phase.FAILED) {
      // 无需恢复
      return;
    }

    _Logger.info(
        "Restoring membership change state: phase=%s, txId=%d", phase, config.getTransactionId());

    // 如果处于中间状态，需要回滚
    if (phase == MembershipChangeManager.Phase.PREPARING
        || phase == MembershipChangeManager.Phase.JOINT
        || phase == MembershipChangeManager.Phase.COMMITTING) {
      _Logger.warning("Incomplete membership change detected, rolling back");
      _SelfMachine.confirm();
      _JointGraph.resetTo(_SelfGraph);
      _RaftMapper.resetMembershipConfig();
    }
  }

  /** 获取成员变更管理器 */
  public MembershipChangeManager getMembershipChangeManager() {
    return _MembershipChangeManager;
  }

  /** 获取 Leader 转让管理器 */
  public LeadershipTransferManager getLeadershipTransferManager() {
    return _LeadershipTransferManager;
  }

  /** 获取 Pipeline 复制管理器 */
  public PipelineReplicationManager getPipelineReplicationManager() {
    return _PipelineManager;
  }

  /** 启用/禁用 Pipeline 复制 */
  public void setPipelineEnabled(boolean enabled) {
    mPipelineEnabled = enabled;
    _Logger.info("Pipeline replication %s", enabled ? "enabled" : "disabled");
  }

  /** 检查 Pipeline 复制是否启用 */
  public boolean isPipelineEnabled() {
    return mPipelineEnabled;
  }

  /**
   * 发起 Leader 转让
   *
   * @param targetPeer 目标节点ID，如果为0则自动选择最佳目标
   * @return true 如果成功发起转让
   */
  public boolean transferLeadership(long targetPeer, IManager manager) {
    if (!_SelfMachine.isInState(LEADER)) {
      _Logger.warning("Cannot transfer leadership: not a leader");
      return false;
    }

    // 检查是否已有进行中的转让
    if (_LeadershipTransferManager.hasActiveTransfer()) {
      _Logger.warning("Cannot transfer leadership: another transfer in progress");
      return false;
    }

    // 检查是否正在成员变更
    if (_MembershipChangeManager.hasActiveChange()) {
      _Logger.warning("Cannot transfer leadership: membership change in progress");
      return false;
    }

    // 自动选择最佳目标
    if (targetPeer == 0) {
      targetPeer =
          LeadershipTransferManager.selectBestTarget(
              _SelfGraph.getPeers().values(), _SelfMachine.peer());
      if (targetPeer == 0) {
        _Logger.warning("Cannot find suitable target for leadership transfer");
        return false;
      }
    }

    // 检查目标是否是自己
    if (targetPeer == _SelfMachine.peer()) {
      _Logger.warning("Cannot transfer leadership to self");
      return false;
    }

    // 检查目标是否在集群中
    if (!_SelfGraph.getPeers().containsKey(targetPeer)) {
      _Logger.warning("Target peer %#x not in cluster", targetPeer);
      return false;
    }

    // 开始转让事务
    long transferId = generateId();
    if (!_LeadershipTransferManager.startTransfer(
        transferId, targetPeer, _SelfMachine.peer(), _SelfMachine.term())) {
      _Logger.warning("Failed to start leadership transfer");
      return false;
    }

    // 启动超时检查
    if (mTransferTimeoutTask != null) {
      mTransferTimeoutTask.cancel();
    }
    mTransferTimeoutTask = _TimeWheel.acquire(this, _TransferTimeoutSchedule);

    _Logger.info("Initiating leadership transfer to %#x (transferId=%d)", targetPeer, transferId);

    // 发送转让请求
    return sendTransferRequest(targetPeer, manager);
  }

  /** 发送 Leader 转让请求 */
  private boolean sendTransferRequest(long targetPeer, IManager manager) {
    LeadershipTransferManager.TransferTransaction tx =
        _LeadershipTransferManager.getCurrentTransaction();
    if (tx == null) {
      return false;
    }

    ISession session = manager.fairLoadSessionByPrefix(targetPeer);
    if (session == null) {
      _Logger.warning("Cannot find session to target peer %#x", targetPeer);
      _LeadershipTransferManager.markFailed("Target peer session not found");
      _LeadershipTransferManager.complete();
      return false;
    }

    X81_RaftTransferLeadership x81 = new X81_RaftTransferLeadership(generateId());
    x81.setTargetPeer(targetPeer);
    x81.setLeaderCommit(_SelfMachine.commit());
    x81.setLeaderIndex(_SelfMachine.index());
    x81.setTerm(_SelfMachine.term());
    x81.setTimeoutMs(_LeadershipTransferManager.getDefaultTimeout().toMillis());
    x81.with(session);

    // 发送请求
    mClusterNode.send(session, SINGLE, x81);

    _Logger.debug("Sent transfer request to %#x", targetPeer);
    return true;
  }

  /** 处理 Leader 转让请求（作为目标节点） */
  public ITriple onTransferLeadership(
      X81_RaftTransferLeadership x81, IManager manager, ISession session) {
    long targetPeer = x81.getTargetPeer();

    // 检查是否是自己
    if (targetPeer != _SelfMachine.peer()) {
      _Logger.warning(
          "Transfer request target mismatch: expected %#x, got %#x",
          targetPeer, _SelfMachine.peer());
      return Triple.of(
          createTransferResponse(x81.msgId(), false, "Target mismatch", session), null, SINGLE);
    }

    // 检查当前状态
    if (!_SelfMachine.isInState(FOLLOWER)) {
      _Logger.warning(
          "Cannot accept leadership: not a follower (state=%s)",
          RaftState.roleOf(_SelfMachine.state()));
      return Triple.of(
          createTransferResponse(x81.msgId(), false, "Not a follower", session), null, SINGLE);
    }

    // 检查 term
    if (x81.getTerm() < _SelfMachine.term()) {
      _Logger.warning(
          "Cannot accept leadership: stale term (request=%d, current=%d)",
          x81.getTerm(), _SelfMachine.term());
      return Triple.of(
          createTransferResponse(x81.msgId(), false, "Stale term", session), null, SINGLE);
    }

    // 检查日志是否追上
    long lag = x81.getLeaderIndex() - _SelfMachine.index();
    if (lag > 100) { // 日志落后太多，拒绝
      _Logger.warning("Cannot accept leadership: log lag too large (%d entries)", lag);
      return Triple.of(
          createTransferResponse(
              x81.msgId(), false, "Log lag too large: " + lag + " entries", session),
          null,
          SINGLE);
    }

    _Logger.info("Accepting leadership transfer from %#x", x81.getTerm());

    // 发送接受响应
    ITriple response =
        Triple.of(createTransferResponse(x81.msgId(), true, "Accepted", session), null, SINGLE);

    // 增加 term 并开始选举
    // 注意：这里增加 term 会导致原 Leader step down
    updateTerm(_SelfMachine.term() + 1);
    _SelfMachine.candidate(_SelfMachine.peer());

    // 开始选举
    List<ITriple> votes = vote4me(_SelfMachine, manager);
    if (votes != null && !votes.isEmpty()) {
      List<ITriple> all = new LinkedList<>();
      all.add(response);
      all.addAll(votes);
      return Triple.of(all, null, BATCH);
    }

    return response;
  }

  /** 创建 Leader 转让响应 */
  private X82_RaftTransferLeadershipResp createTransferResponse(
      long msgId, boolean success, String message, ISession session) {
    X82_RaftTransferLeadershipResp x82 = new X82_RaftTransferLeadershipResp(msgId);
    x82.setPeer(_SelfMachine.peer());
    x82.setCode(success ? SUCCESS.getCode() : RaftCode.ILLEGAL_STATE.getCode());
    x82.setIndex(_SelfMachine.index());
    x82.setCommit(_SelfMachine.commit());
    x82.setMessage(message);
    if (session != null) {
      x82.with(session);
    }
    return x82;
  }

  /** 处理 Leader 转让响应（作为原 Leader） */
  public void onTransferLeadershipResp(X82_RaftTransferLeadershipResp x82) {
    LeadershipTransferManager.TransferTransaction tx =
        _LeadershipTransferManager.getCurrentTransaction();
    if (tx == null) {
      return;
    }

    // 检查是否来自目标节点
    if (x82.getPeer() != tx.getTargetPeer()) {
      _Logger.warning("Transfer response from unexpected peer: %#x", x82.getPeer());
      return;
    }

    if (x82.isSuccess()) {
      _Logger.info("Target peer %#x accepted leadership transfer", x82.getPeer());
      _LeadershipTransferManager.markSuccess();

      // 停止发送心跳，让新 Leader 接管
      beatCancel();

      // 增加 term 并 step down
      updateTerm(_SelfMachine.term() + 1);
      stepDown(_SelfMachine.term());
    } else {
      _Logger.warning(
          "Target peer %#x rejected leadership transfer: %s", x82.getPeer(), x82.getMessage());

      // 尝试重试
      if (_LeadershipTransferManager.shouldRetry()) {
        _Logger.info(
            "Retrying leadership transfer (attempt %d/%d)",
            tx.getRetryCount(), _LeadershipTransferManager.getMaxRetries());
        // 重试将在超时处理中触发
      } else {
        _LeadershipTransferManager.markFailed("Target rejected: " + x82.getMessage());
        _LeadershipTransferManager.complete();

        // 取消超时任务
        if (mTransferTimeoutTask != null) {
          mTransferTimeoutTask.cancel();
          mTransferTimeoutTask = null;
        }
      }
    }
  }

  /** Leader 转让超时处理 */
  private void onLeadershipTransferTimeout() {
    LeadershipTransferManager.TransferTransaction tx =
        _LeadershipTransferManager.getCurrentTransaction();
    if (tx == null) {
      return;
    }

    // 检查是否已完成
    if (tx.isTerminal()) {
      _LeadershipTransferManager.complete();
      mTransferTimeoutTask = null;
      return;
    }

    // 检查是否超时
    if (tx.isTimeout()) {
      _Logger.warning("Leadership transfer %d timeout", tx.getId());
      _LeadershipTransferManager.markTimeout();
      _LeadershipTransferManager.complete();
      mTransferTimeoutTask = null;
      return;
    }

    // 重试转让请求
    if (tx.getRetryCount() < _LeadershipTransferManager.getMaxRetries()) {
      _Logger.debug("Retrying leadership transfer request");
      // 重新获取 manager 并重试
      // 这里简化处理，实际需要获取 manager
    }

    // 继续监控
    mTransferTimeoutTask = _TimeWheel.acquire(this, _TransferTimeoutSchedule);
  }

  private X76_RaftResp response(RaftCode code, long client, long reqId, long origin) {
    X76_RaftResp x76 = new X76_RaftResp(reqId);
    x76.client(client);
    x76.origin(origin);
    x76.code(code.getCode());
    return x76;
  }

  private List<ITriple> leaderAppend(
      byte[] payload, long client, long origin, int factory, IManager manager) {
    LogEntry newEntry =
        new LogEntry(
            _SelfMachine.index() + 1, _SelfMachine.term(), client, origin, factory, payload);
    _Logger.debug("leader append new log {%s}", newEntry);
    if (_RaftMapper.append(newEntry)) {
      _SelfMachine.accept(newEntry.index(), newEntry.term());
      _Logger.debug("leader appended log %d@%d", newEntry.index(), newEntry.term());
      return followersAppend(RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager);
    }
    _Logger.fetal("RAFT WAL failed!");
    return null;
  }

  private List<ITriple> followersAppend(Map<Long, IRaftMachine> peers, IManager manager) {
    return followersAppend(peers, manager, false);
  }

  /**
   * 向所有 Followers 发送 AppendEntries
   *
   * @param peers 目标节点列表
   * @param manager Session 管理器
   * @param pipelineMode 是否使用 Pipeline 模式
   */
  private List<ITriple> followersAppend(
      Map<Long, IRaftMachine> peers, IManager manager, boolean pipelineMode) {
    if (peers == null || peers.isEmpty()) {
      return null;
    }
    List<ITriple> results = new LinkedList<>();
    for (Map.Entry<Long, IRaftMachine> e : peers.entrySet()) {
      if (e.getKey() == _SelfMachine.peer()) {
        continue;
      }

      long peerId = e.getKey();
      IRaftMachine peer = e.getValue();

      // Pipeline 模式：检查是否可以发送
      if (mPipelineEnabled && pipelineMode && !_PipelineManager.canSendTo(peerId)) {
        _Logger.debug("Pipeline: skip peer %#x, inflight window full", peerId);
        continue;
      }

      ISession session = manager.fairLoadSessionByPrefix(peerId);
      if (session == null) {
        continue;
      }

      // 初始化 PipelineWindow（如果需要）
      if (mPipelineEnabled && pipelineMode) {
        long nextIdx = _PipelineManager.getNextIndex(peerId);
        if (nextIdx == 0) {
          // 首次使用 Pipeline，初始化窗口
          _PipelineManager.getOrCreateWindow(peerId, peer.index() + 1);
        }
      }

      X72_RaftAppend append = createAppend(peer, -1, pipelineMode);
      if (append == null) {
        // 需要发送 snapshot
        List<ITriple> snapshotTriples = sendSnapshot(peerId, peer, session);
        if (snapshotTriples != null) {
          results.addAll(snapshotTriples);
        }
      } else {
        results.add(map(append.with(session)));

        // Pipeline 模式：记录 inflight
        if (mPipelineEnabled && pipelineMode) {
          recordInflight(append, peerId);
        }
      }
    }

    // 向 Learner 节点复制日志
    if (mLearnerReplicationEnabled && _LearnerManager.hasLearners()) {
      List<ITriple> learnerResults = replicateToLearners(manager, pipelineMode);
      if (learnerResults != null) {
        results.addAll(learnerResults);
      }
    }

    return results.isEmpty() ? null : results;
  }

  /** 向 Learner 节点复制日志 */
  private List<ITriple> replicateToLearners(IManager manager, boolean pipelineMode) {
    List<ITriple> results = new LinkedList<>();

    for (Map.Entry<Long, IRaftMachine> e : _LearnerManager.getAllLearners().entrySet()) {
      long learnerId = e.getKey();
      IRaftMachine learner = e.getValue();

      ISession session = manager.fairLoadSessionByPrefix(learnerId);
      if (session == null) {
        continue;
      }

      X72_RaftAppend append = createAppend(learner, -1, pipelineMode);
      if (append == null) {
        // Learner 需要 snapshot
        List<ITriple> snapshotTriples = sendSnapshot(learnerId, learner, session);
        if (snapshotTriples != null) {
          results.addAll(snapshotTriples);
        }
      } else {
        results.add(map(append.with(session)));
        _Logger.debug("Replicated to learner %#x", learnerId);
      }
    }

    return results.isEmpty() ? null : results;
  }

  /** 记录 inflight 日志 */
  private void recordInflight(X72_RaftAppend append, long peerId) {
    // 通过 index() 方法获取 preIndex，然后推断范围
    // X72_RaftAppend 的 index() 返回 mPreIndex
    long preIndex = append.index();
    long leaderIndex = _SelfMachine.index();

    if (preIndex >= leaderIndex) {
      return; // 没有新日志
    }

    long firstIndex = preIndex + 1;
    long lastIndex = Math.min(leaderIndex, firstIndex + _SyncBatchMaxSize - 1);
    long term = _SelfMachine.term();

    if (firstIndex <= lastIndex) {
      _PipelineManager.recordSendBatch(peerId, firstIndex, lastIndex, term);
      _Logger.debug(
          "Pipeline: recorded inflight for peer %#x, indices [%d, %d]",
          peerId, firstIndex, lastIndex);
    }
  }

  /** 尝试继续 Pipeline 复制（在收到响应后调用） */
  private void continuePipelineReplication(long peerId, IManager manager) {
    if (!mPipelineEnabled || !_SelfMachine.isInState(LEADER)) {
      return;
    }

    if (!_PipelineManager.canSendTo(peerId)) {
      return;
    }

    ISession session = manager.fairLoadSessionByPrefix(peerId);
    if (session == null) {
      return;
    }

    IRaftMachine peer = getMachine(_SelfGraph, peerId);
    if (peer == null) {
      return;
    }

    // 检查是否还有更多日志需要发送
    long nextIndex = _PipelineManager.getNextIndex(peerId);
    if (nextIndex <= _SelfMachine.index()) {
      X72_RaftAppend append = createAppend(peer, -1, true);
      if (append != null) {
        mClusterNode.send(session, SINGLE, append);
        recordInflight(append, peerId);

        _Logger.debug(
            "Pipeline: continued replication to peer %#x, nextIndex=%d", peerId, nextIndex);
      }
    }
  }

  private X72_RaftAppend createAppend(IRaftMachine acceptor) {
    return createAppend(acceptor, -1);
  }

  private X72_RaftAppend createAppend(IRaftMachine acceptor, int limit) {
    return createAppend(acceptor, limit, false);
  }

  /**
   * 创建 AppendEntries 消息
   *
   * @param acceptor 目标节点
   * @param limit 日志条数限制，-1表示无限制
   * @param usePipeline 是否使用 Pipeline 的 nextIndex
   */
  private X72_RaftAppend createAppend(IRaftMachine acceptor, int limit, boolean usePipeline) {
    X72_RaftAppend x72 = new X72_RaftAppend(_ZUid.getId());
    x72.leader(_SelfMachine.peer());
    x72.term(_SelfMachine.term());
    x72.commit(_SelfMachine.commit());
    x72.preIndex(_SelfMachine.index());
    x72.preIndexTerm(_SelfMachine.indexTerm());
    x72.setFollower(acceptor.peer());

    long preIndex = acceptor.index();
    long preIndexTerm = acceptor.indexTerm();

    // Pipeline 模式：使用 PipelineManager 的 nextIndex
    if (mPipelineEnabled && usePipeline) {
      long pipelineNext = _PipelineManager.getNextIndex(acceptor.peer());
      if (pipelineNext > 0) {
        preIndex = pipelineNext - 1;
        preIndexTerm = _RaftMapper.getEntryTerm(preIndex);
      }
    }

    CHECK:
    {
      if (preIndex == _SelfMachine.index() || preIndex == INDEX_NAN) {
        // acceptor 已经同步 或 acceptor.next 未知
        break CHECK;
      }
      // 检查是否需要发送 snapshot
      if (preIndex > 0 && preIndex < _RaftMapper.getStartIndex() - 1) {
        _Logger.info(
            "follower[%#x] preIndex %d < startIndex %d, need snapshot",
            acceptor.peer(), preIndex, _RaftMapper.getStartIndex());
        // 标记需要发送 snapshot，返回 null 表示需要 snapshot
        return null;
      }
      // preIndex < self.index && preIndex >= 0
      ListSerial<LogEntry> entryList = new ListSerial<>(LogEntry::new);
      long next = preIndex + 1; // next >= 1
      if (next > MIN_START) {
        if (next > _SelfMachine.index()) {
          // acceptor.index > leader.index, 后续用leader的数据进行覆盖
          break CHECK;
        }
        // 存有数据的状态
        LogEntry matched;
        if ((matched = _RaftMapper.getEntry(preIndex)) == null) {
          _Logger.warning("match point %d@%d lost", preIndex, preIndexTerm);
          break CHECK;
        } else {
          if (matched.index() == preIndex && matched.term() == preIndexTerm) {
            x72.preIndex(preIndex);
            x72.preIndexTerm(preIndexTerm);
            _Logger.debug(
                "follow[%#x] %d@%d,leader %d@%d",
                acceptor.peer(),
                preIndex,
                preIndexTerm,
                _SelfMachine.index(),
                _SelfMachine.indexTerm());
          } else {
            _Logger.warning(
                "matched %#x vs %#x no consistency;%d@%d",
                peerId(), acceptor.peer(), preIndex, preIndexTerm);
            break CHECK;
          }
        }
      } else {
        /*
         * preIndex == 0
         * follower是以空数据状态启动。
         */
        x72.preIndex(0);
        x72.preIndexTerm(0);
      }
      for (long end = _SelfMachine.index(), payloadSize = 0;
          next <= end && payloadSize < _SnapshotFragmentMaxSize;
          next++) {
        if (limit > 0 && entryList.size() >= limit) {
          break;
        }
        LogEntry nextLog = _RaftMapper.getEntry(next);
        _Logger.debug("leader → acceptor:%s", nextLog);
        entryList.add(nextLog);
        payloadSize += nextLog.sizeOf();
      }
      x72.withSub(entryList);
      _Logger.debug(
          "leader → acceptor[ %#x ] %d@%d with %d",
          x72.peer(), x72.index(), x72.indexTerm(), entryList.size());
    }
    return x72;
  }

  private Triple<IProtocol, ISession, IPipeEncoder> map(IProtocol source) {
    // source 一定持有 session 在上一步完成了这个操作。
    return Triple.of(source, source.session(), source.session().encoder());
  }

  @Override
  public boolean isInCongress() {
    return RaftState.isInCongress(_SelfMachine.state());
  }

  @Override
  public long peerId() {
    return _SelfMachine.peer();
  }

  @Override
  public long generateId() {
    return _ZUid.getId();
  }

  @Override
  public long generateIdWithType(ISort.Type type) {
    return _ZUid.getId(type.prefix());
  }

  @Override
  public RaftNode getLeader() {
    if (_SelfMachine.leader() != INVALID_PEER_ID) {
      return _RaftConfig.findById(_SelfMachine.leader());
    }
    return null;
  }

  private <T> void trigger(T delta, OperateType type) {
    final RingBuffer<QEvent> _Publisher = mClusterNode.selectPublisher(type);
    final ReentrantLock _Lock = mClusterNode.selectLock(type);
    _Lock.lock();
    try {
      long sequence = _Publisher.next();
      try {
        QEvent event = _Publisher.get(sequence);
        event.produce(type, Pair.of(this, delta), null);
      } finally {
        _Publisher.publish(sequence);
      }
    } finally {
      _Lock.unlock();
    }
  }

  @Override
  public void topology(RaftNode delta) {
    trigger(delta, OperateType.CLUSTER_TOPOLOGY);
  }

  @Override
  public <T extends IStorage> void timeTrigger(T content) {
    trigger(content, OperateType.CLUSTER_TIMER);
  }

  @Override
  public Collection<RaftNode> topology() {
    return _RaftConfig.getPeers().values();
  }

  private X77_RaftNotify createNotify(LogEntry raftLog) {
    if (raftLog != null) {
      X77_RaftNotify x77 = new X77_RaftNotify(_ZUid.getId());
      x77.withSub(raftLog);
      x77.client(raftLog.client());
      x77.origin(raftLog.origin());
      return x77;
    }
    return null;
  }

  public IRaftMapper mapper() {
    return _RaftMapper;
  }

  @Override
  public RaftState raftState() {
    return RaftState.valueOf(_SelfMachine.state());
  }

  // ==================== Pre-vote 机制 ====================

  /** Pre-vote 超时处理 */
  private void preVoteTimeout() {
    if (_PreVoteInProgress) {
      _Logger.debug("pre-vote timeout, cancel and retry");
      _PreVoteInProgress = false;
      _PreVoteReceived.clear();
      // 重新触发选举流程
      mTickTask = _TimeWheel.acquire(this, _TickSchedule);
    }
  }

  /** 发起 Pre-vote 探测 在实际增加 term 之前，先探测是否可能赢得选举 */
  private void startPreVote() {
    if (_PreVoteInProgress) {
      return;
    }
    _PreVoteInProgress = true;
    _PreVoteReceived.clear();

    _Logger.debug("start pre-vote, current term=%d", _SelfMachine.term());

    // 设置 pre-vote 超时
    if (mPreVoteTask != null) {
      mPreVoteTask.cancel();
    }
    mPreVoteTask = _TimeWheel.acquire(this, _PreVoteSchedule);

    // 触发 pre-vote 发送 (通过 timer 机制)
    timeTrigger(createPreVoteMachine());
  }

  private IRaftMachine createPreVoteMachine() {
    RaftMachine update = RaftMachine.createBy(_SelfMachine.peer(), OP_MODIFY);
    update.from(_SelfMachine);
    update.approve(CANDIDATE);
    // Pre-vote 期间不增加 term
    return update;
  }

  /** 处理 Pre-vote 请求 */
  public ITriple onPreVote(X6F_RaftPreVote x6f, IManager manager, ISession session) {
    // 检查任期
    if (x6f.term() < _SelfMachine.term()) {
      _Logger.debug("pre-vote rejected: lower term %d < %d", x6f.term(), _SelfMachine.term());
      return Triple.of(preVoteReject(x6f.msgId()), null, SINGLE);
    }

    // 检查日志完整性
    boolean logIsComplete =
        x6f.index() >= _SelfMachine.index() && x6f.indexTerm() >= _SelfMachine.indexTerm();

    if (!logIsComplete) {
      _Logger.debug(
          "pre-vote rejected: incomplete log [%d@%d] vs mine [%d@%d]",
          x6f.index(), x6f.indexTerm(), _SelfMachine.index(), _SelfMachine.indexTerm());
      return Triple.of(preVoteReject(x6f.msgId()), null, SINGLE);
    }

    // 同意 pre-vote
    _Logger.debug("pre-vote granted to %#x", x6f.peer());
    X6E_RaftPreVoteResp resp = new X6E_RaftPreVoteResp();
    resp.peer(_SelfMachine.peer());
    resp.term(_SelfMachine.term());
    resp.candidate(x6f.peer());
    resp.granted(true);
    resp.with(session);

    return Triple.of(resp, null, SINGLE);
  }

  /** 处理 Pre-vote 响应 */
  public ITriple onPreVoteResp(X6E_RaftPreVoteResp x6e, IManager manager) {
    if (!_PreVoteInProgress) {
      _Logger.debug("pre-vote response ignored: not in pre-vote phase");
      return null;
    }

    if (x6e.term() > _SelfMachine.term()) {
      _Logger.debug("pre-vote resp: higher term %d, step down", x6e.term());
      stepDown(x6e.term());
      _PreVoteInProgress = false;
      return null;
    }

    if (x6e.granted()) {
      _PreVoteReceived.put(x6e.peer(), true);
      _Logger.debug("pre-vote granted by %#x, total=%d", x6e.peer(), _PreVoteReceived.size());

      // 检查是否获得多数派支持
      if (_SelfGraph.isMajorAccept(_SelfMachine.peer(), _SelfMachine.term())
          || _PreVoteReceived.size() >= (_SelfGraph.getPeers().size() / 2)) {
        _Logger.debug("pre-vote majority granted, start real election");
        _PreVoteInProgress = false;
        if (mPreVoteTask != null) {
          mPreVoteTask.cancel();
          mPreVoteTask = null;
        }
        // 开始正式选举
        startVote();
      }
    }
    return null;
  }

  private X6E_RaftPreVoteResp preVoteReject(long msgId) {
    X6E_RaftPreVoteResp resp = new X6E_RaftPreVoteResp();
    resp.peer(_SelfMachine.peer());
    resp.term(_SelfMachine.term());
    resp.granted(false);
    return resp;
  }

  private List<ITriple> createPreVotes(Map<Long, IRaftMachine> peers, IManager manager) {
    return peers != null && !peers.isEmpty()
        ? peers.keySet().stream()
            .filter(peer -> peer != _SelfMachine.peer())
            .map(
                peer -> {
                  ISession session = manager.fairLoadSessionByPrefix(peer);
                  if (session == null) {
                    _Logger.warning("pre-vote: peer %#x session not found", peer);
                    return null;
                  }
                  X6F_RaftPreVote x6f = new X6F_RaftPreVote();
                  x6f.with(session);
                  x6f.peer(peer);
                  x6f.term(_SelfMachine.term()); // 不增加 term
                  x6f.index(_SelfMachine.index());
                  x6f.indexTerm(_SelfMachine.indexTerm());
                  x6f.commit(_SelfMachine.commit());
                  return x6f;
                })
            .filter(Objects::nonNull)
            .map(x6f -> Triple.of((IProtocol) x6f, x6f.session(), x6f.session().encoder()))
            .collect(Collectors.toList())
        : new LinkedList<>();
  }

  public List<ITriple> preVote(IRaftMachine update, IManager manager) {
    if (_PreVoteInProgress && update.isInState(CANDIDATE)) {
      return createPreVotes(RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph), manager);
    }
    return null;
  }

  // ==================== Snapshot 安装 ====================

  /** 处理 Leader 发来的 Snapshot 分片 */
  public ITriple onSnapshot(X7D_RaftSnapshot x7d, ISession session) {
    // 检查任期
    if (x7d.term() < _SelfMachine.term()) {
      _Logger.debug("snapshot rejected: lower term %d < %d", x7d.term(), _SelfMachine.term());
      return Triple.of(snapshotAck(x7d, false, "lower term"), null, SINGLE);
    }

    if (x7d.term() > _SelfMachine.term()) {
      stepDown(x7d.term());
    }

    // 检查是否正在安装 snapshot
    if (!_InstallingSnapshot) {
      // 开始新的 snapshot 安装
      _InstallingSnapshot = true;
      _SnapshotLeader = x7d.leader();
      _Logger.info(
          "start installing snapshot from leader %#x, lastIndex=%d@%d",
          x7d.leader(), x7d.lastIncludeIndex(), x7d.lastIncludeTerm());
    } else if (_SnapshotLeader != x7d.leader()) {
      // 正在从其他 leader 安装，拒绝
      return Triple.of(snapshotAck(x7d, false, "installing from another leader"), null, SINGLE);
    }

    _SnapshotLock.lock();
    try {
      // 应用 snapshot 数据到状态机
      boolean success = installSnapshotChunk(x7d);

      if (success && x7d.done()) {
        // 最后一个分片，完成安装
        _Logger.info(
            "snapshot installation completed, lastIndex=%d@%d",
            x7d.lastIncludeIndex(), x7d.lastIncludeTerm());

        // 更新本地状态
        _SelfMachine.term(x7d.term());
        _SelfMachine.index(x7d.lastIncludeIndex());
        _SelfMachine.indexTerm(x7d.lastIncludeTerm());
        _SelfMachine.commit(x7d.lastIncludeIndex());
        _SelfMachine.accept(x7d.lastIncludeIndex(), x7d.lastIncludeTerm());

        // 更新 mapper
        _RaftMapper.updateSnapshotMeta(x7d.lastIncludeIndex(), x7d.lastIncludeTerm());
        _RaftMapper.truncatePrefix(x7d.lastIncludeIndex() + 1);

        _InstallingSnapshot = false;
        _SnapshotLeader = INVALID_PEER_ID;
      }

      return Triple.of(snapshotAck(x7d, success, null), null, SINGLE);
    } finally {
      _SnapshotLock.unlock();
    }
  }

  /** 安装 snapshot 分片到本地存储 */
  private boolean installSnapshotChunk(X7D_RaftSnapshot chunk) {
    try {
      byte[] data = chunk.data();
      if (data == null || data.length == 0) {
        return true; // 空数据也算成功
      }

      // 将数据写入 snapshot 存储
      _RaftMapper.getSnapshot().withSub(data);

      _Logger.debug("snapshot chunk installed: offset=%d, size=%d", chunk.offset(), data.length);
      return true;
    } catch (Exception e) {
      _Logger.warning("failed to install snapshot chunk: %s", e);
      return false;
    }
  }

  private X7E_RaftSnapshotAck snapshotAck(
      X7D_RaftSnapshot snapshot, boolean success, String errorMsg) {
    X7E_RaftSnapshotAck ack = new X7E_RaftSnapshotAck();
    ack.peer(_SelfMachine.peer());
    ack.term(_SelfMachine.term());
    ack.offset(snapshot.offset());
    ack.lastIncludeIndex(snapshot.lastIncludeIndex());
    ack.success(success);
    if (errorMsg != null) {
      ack.errorMsg(errorMsg);
    }
    return ack;
  }

  /** 处理 Snapshot 安装响应 (Leader 端) */
  public ITriple onSnapshotAck(X7E_RaftSnapshotAck x7e) {
    if (!_SelfMachine.isInState(LEADER)) {
      return null;
    }

    if (x7e.term() > _SelfMachine.term()) {
      stepDown(x7e.term());
      return null;
    }

    if (x7e.success()) {
      _Logger.debug("snapshot ack from %#x: offset=%d", x7e.peer(), x7e.offset());
      // 更新 follower 的匹配索引
      IRaftMachine machine = getMachine(_SelfGraph, x7e.peer());
      if (machine != null) {
        machine.matchIndex(x7e.lastIncludeIndex());
        machine.index(x7e.lastIncludeIndex());
      }
    } else {
      _Logger.warning("snapshot failed on %#x: %s", x7e.peer(), x7e.errorMsg());
    }
    return null;
  }

  // ==================== ReadIndex 机制 ====================

  /** 读请求内部类 */
  private static class ReadIndexRequest {
    final long readId;
    final long origin;
    final long client;
    final long createTime;

    ReadIndexRequest(long readId, long origin, long client) {
      this.readId = readId;
      this.origin = origin;
      this.client = client;
      this.createTime = System.currentTimeMillis();
    }
  }

  /** 提交 ReadIndex 请求 (客户端或 Follower 调用) */
  public List<ITriple> readIndex(IManager manager, long origin, long client) {
    long readId = ++_LastReadId;

    switch (RaftState.valueOf(_SelfMachine.state())) {
      case LEADER -> {
        // Leader 直接处理
        return handleLeaderReadIndex(readId, origin, client, manager);
      }
      case FOLLOWER, CLIENT -> {
        // 转发给 Leader
        ISession session = manager.fairLoadSessionByPrefix(_SelfMachine.leader());
        if (session == null) {
          _Logger.warning("readIndex: leader session not found");
          return null;
        }
        X7F_RaftReadIndex x7f = new X7F_RaftReadIndex(generateId());
        x7f.with(session);
        x7f.client(client);
        x7f.readId(readId);
        x7f.origin(origin);
        return Collections.singletonList(Triple.of(x7f, session, session.encoder()));
      }
      default -> {
        _Logger.warning(
            "readIndex: cannot process in state %s", RaftState.roleOf(_SelfMachine.state()));
        return null;
      }
    }
  }

  /** Leader 处理 ReadIndex 请求 */
  private List<ITriple> handleLeaderReadIndex(
      long readId, long origin, long client, IManager manager) {
    long commitIndex = _SelfMachine.commit();

    // 检查当前 commit index 是否已经被应用
    if (_SelfMachine.accept() >= commitIndex) {
      // 可以直接返回
      _Logger.debug("readIndex %d: commit %d already applied", readId, commitIndex);
      return Collections.singletonList(
          Triple.of(createReadIndexResp(readId, commitIndex, true, null, null), null, NULL));
    }

    // 需要等待 commit index 被应用
    _Logger.debug(
        "readIndex %d: waiting for commit %d to be applied (current accept=%d)",
        readId, commitIndex, _SelfMachine.accept());

    ReadIndexRequest req = new ReadIndexRequest(readId, origin, client);
    _ReadIndexWaiters.computeIfAbsent(commitIndex, k -> new ArrayList<>()).add(req);

    return null; // 等待后续通知
  }

  /** 处理 ReadIndex 请求 (Leader 接收) */
  public ITriple onReadIndex(X7F_RaftReadIndex x7f, IManager manager) {
    if (!_SelfMachine.isInState(LEADER)) {
      return Triple.of(createReadIndexResp(x7f.readId(), 0, false, "not leader", null), null, NULL);
    }

    List<ITriple> result = handleLeaderReadIndex(x7f.readId(), x7f.origin(), x7f.client(), manager);
    if (result != null) {
      return Triple.of(result, null, BATCH);
    }
    return null;
  }

  /** 处理 ReadIndex 响应 */
  public ITriple onReadIndexResp(X80_RaftReadIndexResp x80) {
    _Logger.debug("readIndex response: readId=%#x, success=%s", x80.readId(), x80.success());
    return Triple.of(null, x80, NULL);
  }

  /** 检查并处理等待中的 ReadIndex 请求 在每次 accept 增加时调用 */
  private void checkReadIndexWaiters() {
    long currentAccept = _SelfMachine.accept();

    Iterator<Map.Entry<Long, List<ReadIndexRequest>>> it = _ReadIndexWaiters.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Long, List<ReadIndexRequest>> entry = it.next();
      long commitIndex = entry.getKey();

      if (currentAccept >= commitIndex) {
        // 这个 commit index 已经被应用，可以响应所有等待的读请求
        List<ReadIndexRequest> waiters = entry.getValue();
        _Logger.debug(
            "readIndex: commit %d applied, responding %d waiters", commitIndex, waiters.size());

        for (ReadIndexRequest req : waiters) {
          // 触发响应
          X80_RaftReadIndexResp resp =
              createReadIndexResp(req.readId, commitIndex, true, null, null);
          // 需要通过合适的方式发送给客户端
        }

        it.remove();
      }
    }
  }

  private X80_RaftReadIndexResp createReadIndexResp(
      long readId, long commitIndex, boolean success, String errorMsg, byte[] result) {
    X80_RaftReadIndexResp resp = new X80_RaftReadIndexResp();
    resp.peer(_SelfMachine.peer());
    resp.readId(readId);
    resp.commitIndex(commitIndex);
    resp.success(success);
    if (errorMsg != null) {
      resp.errorMsg(errorMsg);
    }
    if (result != null) {
      resp.result(result);
    }
    return resp;
  }

  /** 检查 ReadIndex 超时请求 */
  public void checkReadIndexTimeout() {
    long now = System.currentTimeMillis();
    long timeout = 5000; // 5秒超时

    _ReadIndexWaiters.forEach(
        (commitIndex, waiters) -> {
          waiters.removeIf(
              req -> {
                if (now - req.createTime > timeout) {
                  _Logger.warning("readIndex %d timeout", req.readId);
                  // 发送超时响应
                  return true;
                }
                return false;
              });
        });

    _ReadIndexWaiters.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  // ==================== Lease Read 机制 ====================

  /** 提交 Lease Read 请求 在 Leader 租约期内直接读取，无需等待 commit */
  public List<ITriple> leaseRead(IManager manager, long origin, long client) {
    long readId = ++_LastReadId;

    if (!mLeaseReadEnabled) {
      _Logger.warning("leaseRead: lease read is disabled, falling back to readIndex");
      return readIndex(manager, origin, client);
    }

    switch (RaftState.valueOf(_SelfMachine.state())) {
      case LEADER -> {
        // Leader 检查租约
        if (_LeaseManager.canLeaseRead(_SelfMachine.term())) {
          // 租约有效，直接读取
          return handleLeaderLeaseRead(readId, origin, client, manager);
        } else {
          // 租约无效，降级为 ReadIndex
          _Logger.debug("leaseRead: lease invalid, falling back to readIndex");
          return readIndex(manager, origin, client);
        }
      }
      case FOLLOWER, CLIENT -> {
        // 转发给 Leader
        ISession session = manager.fairLoadSessionByPrefix(_SelfMachine.leader());
        if (session == null) {
          _Logger.warning("leaseRead: leader session not found");
          return null;
        }
        X83_RaftLeaseRead x83 = new X83_RaftLeaseRead(generateId());
        x83.with(session);
        x83.client(client);
        x83.readId(readId);
        return Collections.singletonList(Triple.of(x83, session, session.encoder()));
      }
      default -> {
        _Logger.warning(
            "leaseRead: cannot process in state %s", RaftState.roleOf(_SelfMachine.state()));
        return null;
      }
    }
  }

  /** Leader 处理 Lease Read 请求 */
  private List<ITriple> handleLeaderLeaseRead(
      long readId, long origin, long client, IManager manager) {
    long commitIndex = _SelfMachine.commit();
    LeaseManager.Lease lease = _LeaseManager.getCurrentLease();
    long remainingMs = lease != null ? lease.getRemainingMs() : 0;

    _Logger.debug(
        "leaseRead %d: serving with lease, commit=%d, remaining=%dms",
        readId, commitIndex, remainingMs);

    // 创建响应
    X84_RaftLeaseReadResp resp = new X84_RaftLeaseReadResp();
    resp.peer(_SelfMachine.peer());
    resp.readId(readId);
    resp.setCode(0); // 成功
    resp.commitIndex(commitIndex);
    resp.leaseRemainingMs(remainingMs);

    // 如果是本地请求，直接返回
    if (client == _SelfMachine.peer()) {
      return Collections.singletonList(Triple.of(resp, null, NULL));
    }

    // 发送给客户端
    ISession session = manager.fairLoadSessionByPrefix(client);
    if (session != null) {
      resp.with(session);
      return Collections.singletonList(Triple.of(resp, null, SINGLE));
    }

    return null;
  }

  /** Leader 处理 Lease Read 请求（接收） */
  public ITriple onLeaseRead(X83_RaftLeaseRead x83, IManager manager) {
    if (!_SelfMachine.isInState(LEADER)) {
      return Triple.of(
          createLeaseReadResp(x83.readId(), 0, false, "not leader", 0, null), null, NULL);
    }

    if (!mLeaseReadEnabled || !_LeaseManager.canLeaseRead(_SelfMachine.term())) {
      // 租约无效，返回错误，客户端应该降级到 ReadIndex
      return Triple.of(
          createLeaseReadResp(x83.readId(), 0, false, "lease expired", 0, null), null, NULL);
    }

    List<ITriple> result = handleLeaderLeaseRead(x83.readId(), x83.origin(), x83.client(), manager);
    if (result != null) {
      return Triple.of(result, null, BATCH);
    }
    return null;
  }

  /** 处理 Lease Read 响应 */
  public void onLeaseReadResp(X84_RaftLeaseReadResp x84) {
    _Logger.debug(
        "leaseRead response: readId=%d, success=%s, leaseRemaining=%dms",
        x84.readId(), x84.isSuccess(), x84.leaseRemainingMs());
    // 响应会传递给上层处理
  }

  /** 创建 Lease Read 响应 */
  private X84_RaftLeaseReadResp createLeaseReadResp(
      long readId,
      long commitIndex,
      boolean success,
      String errorMsg,
      long leaseRemainingMs,
      ISession session) {
    X84_RaftLeaseReadResp resp = new X84_RaftLeaseReadResp();
    resp.peer(_SelfMachine.peer());
    resp.readId(readId);
    resp.setCode(success ? 0 : RaftCode.ILLEGAL_STATE.getCode());
    resp.commitIndex(commitIndex);
    resp.leaseRemainingMs(leaseRemainingMs);
    resp.setMessage(errorMsg);
    if (session != null) {
      resp.with(session);
    }
    return resp;
  }

  /** 获取 Lease 管理器 */
  public LeaseManager getLeaseManager() {
    return _LeaseManager;
  }

  /** 启用/禁用 Lease Read */
  public void setLeaseReadEnabled(boolean enabled) {
    mLeaseReadEnabled = enabled;
    _LeaseManager.setEnabled(enabled);
    _Logger.info("Lease read %s", enabled ? "enabled" : "disabled");
  }

  /** 检查 Lease Read 是否启用 */
  public boolean isLeaseReadEnabled() {
    return mLeaseReadEnabled;
  }

  // ==================== Learner 节点管理 ====================

  /** 获取 Learner 管理器 */
  public LearnerManager getLearnerManager() {
    return _LearnerManager;
  }

  /** 添加 Learner 节点 */
  public void addLearner(long peerId) {
    IRaftMachine learner = RaftMachine.createBy(peerId, IStorage.Operation.OP_MODIFY);
    learner.approve(RaftState.LEARNER);
    learner.leader(_SelfMachine.leader());
    _LearnerManager.addLearner(learner);

    _Logger.info("Added learner node: %#x", peerId);
  }

  /** 移除 Learner 节点 */
  public void removeLearner(long peerId) {
    _LearnerManager.removeLearner(peerId);
    _Logger.info("Removed learner node: %#x", peerId);
  }

  /** 启用/禁用 Learner 复制 */
  public void setLearnerReplicationEnabled(boolean enabled) {
    mLearnerReplicationEnabled = enabled;
    _Logger.info("Learner replication %s", enabled ? "enabled" : "disabled");
  }

  /** 检查 Learner 复制是否启用 */
  public boolean isLearnerReplicationEnabled() {
    return mLearnerReplicationEnabled;
  }

  /** 检查当前节点是否是 Learner */
  public boolean isSelfLearner() {
    return _SelfMachine.isInState(RaftState.LEARNER);
  }

  // ==================== CheckQuorum 机制 ====================

  /** Leader 定期检查是否仍有多数派连接 如果失去多数派连接，主动 step down 防止脑裂 */
  private void checkQuorum() {
    if (!_SelfMachine.isInState(LEADER)) {
      return;
    }

    Map<Long, IRaftMachine> peers = RaftGraph.join(_SelfMachine.peer(), _SelfGraph, _JointGraph);
    if (peers == null || peers.isEmpty()) {
      return;
    }

    // 统计最近活跃节点数
    int activeCount = 0;
    int totalPeers = peers.size();

    for (IRaftMachine peer : peers.values()) {
      // 如果节点索引接近 Leader，认为它是活跃的
      if (peer.index() >= _SelfMachine.commit() - _SyncBatchMaxSize) {
        activeCount++;
      }
    }

    _ActivePeers.set(activeCount);

    // 检查是否失去多数派
    boolean hasQuorum = _SelfGraph.isMajorAccept(_SelfMachine.peer(), _SelfMachine.term());

    if (!hasQuorum) {
      _Logger.warning("checkQuorum: lost quorum, active=%d/%d, step down", activeCount, totalPeers);
      stepDown(_SelfMachine.term());
    } else {
      _Logger.debug("checkQuorum: quorum maintained, active=%d/%d", activeCount, totalPeers);
    }

    _LastQuorumCheckTime = System.currentTimeMillis();
  }

  /** 更新节点活跃状态（在收到 Append/Accept 时调用） */
  public void updatePeerActive(long peerId) {
    // 更新最后活跃时间，用于 CheckQuorum
    IRaftMachine machine = getMachine(_SelfGraph, peerId);
    if (machine != null) {
      // 实际实现可以记录每个节点的最后活跃时间
      _Logger.debug("peer[%#x] active updated", peerId);
    }
  }
}
