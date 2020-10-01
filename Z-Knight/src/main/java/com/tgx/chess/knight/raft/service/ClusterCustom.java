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
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.knight.raft.service;

import static com.tgx.chess.knight.raft.RaftState.LEADER;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.raft.X70_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X71_RaftBallot;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftAppend;
import com.tgx.chess.bishop.io.zprotocol.raft.X73_RaftAccept;
import com.tgx.chess.bishop.io.zprotocol.raft.X74_RaftReject;
import com.tgx.chess.bishop.io.zprotocol.raft.X75_RaftRequest;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.king.topology.ZUID;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.knight.raft.model.RaftMachine;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.model.log.LogEntry;
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
            case X70_RaftVote.COMMAND://follower → elector
                X70_RaftVote x70 = (X70_RaftVote) content;
                return _RaftNode.elect(x70.getTerm(),
                                       x70.getIndex(),
                                       x70.getIndexTerm(),
                                       x70.getCandidateId(),
                                       x70.getCommit(),
                                       manager);
            case X71_RaftBallot.COMMAND://elector → candidate
                X71_RaftBallot x71 = (X71_RaftBallot) content;
                return _RaftNode.ballot(x71.getTerm(),
                                        x71.getIndex(),
                                        x71.getElectorId(),
                                        x71.getCandidateId(),
                                        manager);
            case X72_RaftAppend.COMMAND://leader → follower 
                X72_RaftAppend x72 = (X72_RaftAppend) content;
                if (x72.getPayload() != null) {
                    _RaftNode.appendLogs(JsonUtil.readValue(x72.getPayload(), _TypeReferenceOfLogEntryList));
                }
                return _RaftNode.append(x72.getTerm(),
                                        x72.getPreIndex(),
                                        x72.getPreIndexTerm(),
                                        x72.getLeaderId(),
                                        x72.getCommit());
            case X73_RaftAccept.COMMAND: // follower → leader
                X73_RaftAccept x73 = (X73_RaftAccept) content;
                return _RaftNode.onAccept(x73.getFollowerId(),
                                          x73.getTerm(),
                                          x73.getCatchUp(),
                                          x73.getLeaderId(),
                                          manager);
            case X74_RaftReject.COMMAND:// * → candidate
                X74_RaftReject x74 = (X74_RaftReject) content;
                return _RaftNode.onReject(x74.getPeerId(),
                                          x74.getTerm(),
                                          x74.getIndex(),
                                          x74.getIndexTerm(),
                                          x74.getCode(),
                                          x74.getState());
            case X75_RaftRequest.COMMAND:// client → leader
                if (_RaftNode.getMachine()
                             .getState() != LEADER)
                {
                    _Logger.warning("state error,expect:'LEADER',real:%s",
                                    _RaftNode.getMachine()
                                             .getState());
                    break;
                }
                X75_RaftRequest x75 = (X75_RaftRequest) content;
                return _RaftNode.newLogEntry(x75.getSerial(),
                                             x75.getPayload(),
                                             x75.getClientId(),
                                             x75.getOrigin(),
                                             x75.isPublic(),
                                             manager);
            case X76_RaftNotify.COMMAND:// leader → client
                /*
                leader -> follow, self::follow
                由于x76.origin == request.session.sessionIndex
                LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
                */
                X76_RaftNotify x76 = (X76_RaftNotify) content;
                /*作为 client 收到 notify */
                x76.setNotify();
                return new Pair<>(null, x76);
            case X106_Identity.COMMAND:// peer *, behind → previous
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
            case OP_MODIFY://step down → follower
                _RaftNode.turnToFollower(machine);
                break;
            case OP_APPEND://heartbeat
                Stream<X72_RaftAppend> x72Stream = _RaftNode.checkLogAppend(machine, manager);
                if (x72Stream != null) {
                    return x72Stream.map(x72 -> new Triple<>(x72,
                                                             x72.getSession(),
                                                             x72.getSession()
                                                                .getContext()
                                                                .getSort()
                                                                .getEncoder()))
                                    .collect(Collectors.toList());
                }
                break;
            case OP_INSERT://vote
                Stream<X70_RaftVote> x70Stream = _RaftNode.checkVoteState(machine, manager);
                if (x70Stream != null) {
                    return x70Stream.map(x70 -> new Triple<>(x70,
                                                             x70.getSession(),
                                                             x70.getSession()
                                                                .getContext()
                                                                .getSort()
                                                                .getEncoder()))
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
                                         manager)
                            .map(x72 -> new Triple<>(x72,
                                                     x72.getSession(),
                                                     x72.getSession()
                                                        .getContext()
                                                        .getSort()
                                                        .getEncoder()))
                            .collect(Collectors.toList());
        }
        else if (_RaftNode.getMachine()
                          .getLeader() != ZUID.INVALID_PEER_ID)
        {
            X75_RaftRequest x75 = new X75_RaftRequest(_RaftNode.getRaftZuid());
            x75.setSerial(request.serial());
            x75.setPayload(request.encode());
            x75.setOrigin(request.getOrigin());
            x75.setClientId(_RaftNode.getMachine()
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
