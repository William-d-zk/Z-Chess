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

package com.tgx.chess.knight.raft.service;

import static com.tgx.chess.knight.raft.RaftState.CANDIDATE;
import static com.tgx.chess.knight.raft.RaftState.LEADER;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.raft.X70_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X75_RaftRequest;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.bishop.io.zprotocol.raft.X70_RaftAppend;
import com.tgx.chess.bishop.io.zprotocol.raft.X71_RaftAccept;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.knight.raft.RaftState;
import com.tgx.chess.knight.raft.model.RaftCode;
import com.tgx.chess.knight.raft.model.RaftMachine;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.model.log.LogEntry;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.cluster.IClusterCustom;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IClusterTimer;
import com.tgx.chess.queen.io.core.inf.IConsistent;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

public class ClusterCustom<T extends IClusterPeer & IClusterTimer>
        implements
        IClusterCustom<ZContext,
                       RaftMachine>
{
    private final Logger                        _Logger                      = Logger.getLogger("cluster.knight."
                                                                                                + getClass().getSimpleName());
    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<List<LogEntry>>()
                                                                             {
                                                                             };

    private final RaftNode<T> _RaftNode;

    public ClusterCustom(RaftNode<T> raftNode)
    {
        _RaftNode = raftNode;
    }

    @Override
    public IPair handle(ISessionManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> content) throws Exception
    {
        switch (content.serial())
        {
            case X70_RaftVote.COMMAND:
                X70_RaftVote x72 = (X70_RaftVote) content;
                RaftMachine machine = new RaftMachine(x72.getPeerId());
                machine.setIndex(x72.getIndex());
                machine.setIndexTerm(x72.getIndexTerm());
                machine.setTerm(x72.getTerm());
                machine.setCandidate(x72.getPeerId());
                machine.setCommit(x72.getCommit());
                machine.setState(CANDIDATE);
                IControl<ZContext>[] response = _RaftNode.merge(machine);
                return response != null ? new Pair<>(response, null)
                                        : null;
            case X75_RaftRequest.COMMAND:
                if (_RaftNode.getMachine()
                             .getState() != LEADER)
                {
                    _Logger.warning("state error,expect:'LEADER',real:%s",
                                    _RaftNode.getMachine()
                                             .getState()
                                             .name());
                    break;
                }
                X75_RaftRequest x75 = (X75_RaftRequest) content;
                return new Pair<>(_RaftNode.newLogEntry(x75.getPayloadSerial(),
                                                        x75.getPayload(),
                                                        x75.getPeerId(),
                                                        x75.isPublic(),
                                                        x75.getOrigin(),
                                                        IStorage.Operation.OP_APPEND)
                                           .map(x7e ->
                                           {
                                               long follower = x7e.getFollower();
                                               ISession<ZContext> targetSession = manager.findSessionByPrefix(follower);
                                               if (targetSession != null) {
                                                   //此处不用判断是否管理，执行线程与 onDismiss在同一线程，只要存在此时的状态一定为valid
                                                   x7e.setSession(targetSession);
                                                   return x7e;
                                               }
                                               else return null;
                                           })
                                           .filter(Objects::nonNull)
                                           .toArray(X70_RaftAppend[]::new),
                                  null);
            case X76_RaftNotify.COMMAND:
                /*
                leader -> follow, self::follow
                由于x76.origin == request.session.sessionIndex
                LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
                */
                X76_RaftNotify x76 = (X76_RaftNotify) content;
                /*作为 client 收到 notify */
                x76.setNotify();
                return new Pair<>(null, x76);
            case X70_RaftAppend.COMMAND:
                X70_RaftAppend x7e = (X70_RaftAppend) content;
                if (x7e.getPayload() != null) {
                    List<LogEntry> entryList = JsonUtil.readValue(x7e.getPayload(), _TypeReferenceOfLogEntryList);
                    if (entryList != null && !entryList.isEmpty()) {
                        _RaftNode.appendLogs(entryList);
                    }
                }
                machine = new RaftMachine(x7e.getPeerId());
                machine.setState(LEADER);
                machine.setTerm(x7e.getTerm());
                machine.setCommit(x7e.getCommit());
                machine.setIndexTerm(x7e.getPreIndexTerm());
                machine.setIndex(x7e.getPreIndex());
                response = _RaftNode.merge(machine);
                return response != null ? new Pair<>(response, null)
                                        : null;
            case X71_RaftAccept.COMMAND:
                X71_RaftAccept x7f = (X71_RaftAccept) content;
                return _RaftNode.onResponse(x7f.getPeerId(),
                                            x7f.getTerm(),
                                            x7f.getCatchUp(),
                                            x7f.getCatchUpTerm(),
                                            x7f.getCandidate(),
                                            RaftState.valueOf(x7f.getState()),
                                            RaftCode.valueOf(x7f.getCode()),
                                            manager);
            case X106_Identity.COMMAND:
                X106_Identity x106 = (X106_Identity) content;
                long peerId = x106.getIdentity();
                _Logger.debug("=========> map peerId:%#x", peerId);
                manager.mapSession(session.getIndex(), session, peerId);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + content.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(ISessionManager<ZContext> manager, RaftMachine machine)
    {
        if (machine == null) { return null; }
        switch (machine.operation())
        {
            case OP_APPEND://heartbeat
                Stream<X70_RaftAppend> x7eStream = _RaftNode.checkLogAppend(machine);
                if (x7eStream != null) {
                    return x7eStream.map(x7E ->
                    {
                        ISession<ZContext> session = manager.findSessionByPrefix(x7E.getFollower());
                        if (session == null) {
                            _Logger.warning("not found peerId:%#x session", x7E.getFollower());
                            return null;
                        }
                        else {
                            x7E.setSession(session);
                            return new Triple<>(x7E,
                                                session,
                                                session.getContext()
                                                       .getSort()
                                                       .getEncoder());
                        }
                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                }
                break;
            case OP_INSERT://vote
                Stream<X70_RaftVote> x72Stream = _RaftNode.checkVoteState(machine);
                if (x72Stream != null) {
                    return x72Stream.map(x72 ->
                    {
                        ISession<ZContext> session = manager.findSessionByPrefix(x72.getElector());
                        if (session == null) {
                            _Logger.warning("not found peerId:%#x session", x72.getElector());
                            return null;
                        }
                        else {
                            x72.setSession(session);
                            return new Triple<>(x72,
                                                session,
                                                session.getContext()
                                                       .getSort()
                                                       .getEncoder());
                        }
                    })
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                }
                break;
        }
        return null;
    }

    @Override
    public <E extends IConsistent & IProtocol> List<ITriple> consensus(ISessionManager<ZContext> manager, E request)
    {
        _Logger.debug("cluster consensus %s", request);
        if (_RaftNode.getMachine()
                     .getState() == LEADER)
        {
            /*
             * session belong to Link
             */
            return _RaftNode.newLogEntry(request,
                                         _RaftNode.getMachine()
                                                  .getPeerId(),
                                         IStorage.Operation.OP_INSERT)
                            .map(x7e ->
                            {
                                long follower = x7e.getFollower();
                                ISession<ZContext> targetSession = manager.findSessionByPrefix(follower);
                                if (targetSession != null) {
                                    //此处不用判断是否管理，执行线程与 onDismiss在同一线程，只要存在此时的状态一定为valid
                                    x7e.setSession(targetSession);
                                    return new Triple<>(x7e,
                                                        targetSession,
                                                        targetSession.getContext()
                                                                     .getSort()
                                                                     .getEncoder());
                                }
                                else return null;
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
        }
        else if (_RaftNode.getMachine()
                          .getLeader() != ZUID.INVALID_PEER_ID)
        {
            X75_RaftRequest x75 = new X75_RaftRequest(_RaftNode.getRaftZuid());
            x75.setPayloadSerial(request.serial());
            x75.setPayload(request.encode());
            x75.setOrigin(request.getOrigin());
            x75.setPeerId(_RaftNode.getMachine()
                                   .getPeerId());
            x75.setPublic(request.isPublic());
            ISession<ZContext> leaderSession = manager.findSessionByPrefix(_RaftNode.getMachine()
                                                                                    .getLeader());
            if (leaderSession != null) {
                return Collections.singletonList(new Triple<>(x75,
                                                              leaderSession,
                                                              leaderSession.getContext()
                                                                           .getSort()
                                                                           .getEncoder()));

            }
            else {
                _Logger.fetal("Leader connection miss");
            }
        }
        _Logger.fetal("cluster is electing");
        return null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _RaftNode.isClusterMode();
    }
}
