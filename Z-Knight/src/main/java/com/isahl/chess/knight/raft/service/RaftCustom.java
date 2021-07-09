/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X106_Identity;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.*;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.knight.raft.ClusterPeer;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.io.core.inf.*;

import java.util.Collections;
import java.util.List;

import static com.isahl.chess.knight.raft.model.RaftState.LEADER;

public class RaftCustom<T extends IClusterPeer & IClusterTimer>
        implements
        IClusterCustom<RaftMachine>
{
    private final Logger                        _Logger                      = Logger.getLogger("cluster.knight."
                                                                                                + getClass().getSimpleName());
    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<>()
                                                                             {
                                                                             };
    private final ClusterPeer<T>                _ClusterPeer;

    public RaftCustom(ClusterPeer<T> clusterPeer)
    {
        _ClusterPeer = clusterPeer;
    }

    /**
     *
     * @param manager
     *            cluster 管理器 注意与 device 管理器的区分
     * @param session
     *            来源 session
     * @param content
     *            需要 raft custom 处理的内容
     *
     * @return IPair
     *         first : list of command implements 'IControl',broadcast to all
     *         cluster peers
     *         second : command implements 'IConsistentNotify',
     * @throws Exception
     */
    @Override
    public IPair handle(ISessionManager manager, ISession session, IControl content) throws Exception
    {
        /*
         * leader -> follow, self::follow
         * 由于x76.origin == request.session.sessionIndex
         * LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
         */
        /* 作为 client 收到 notify */
        switch (content.serial())
        {
            // follower → elector
            case X70_RaftVote.COMMAND ->
                {
                    X70_RaftVote x70 = (X70_RaftVote) content;
                    return _ClusterPeer.elect(x70.getTerm(),
                                              x70.getIndex(),
                                              x70.getIndexTerm(),
                                              x70.getCandidateId(),
                                              x70.getCommit(),
                                              manager);
                }
            // elector → candidate
            case X71_RaftBallot.COMMAND ->
                {
                    X71_RaftBallot x71 = (X71_RaftBallot) content;
                    return _ClusterPeer.ballot(x71.getTerm(),
                                               x71.getIndex(),
                                               x71.getElectorId(),
                                               x71.getCandidateId(),
                                               manager);
                }
            // leader → follower
            case X72_RaftAppend.COMMAND ->
                {
                    X72_RaftAppend x72 = (X72_RaftAppend) content;
                    if (x72.getPayload() != null) {
                        _ClusterPeer.appendLogs(JsonUtil.readValue(x72.getPayload(), _TypeReferenceOfLogEntryList));
                    }
                    return _ClusterPeer.append(x72.getTerm(),
                                               x72.getPreIndex(),
                                               x72.getPreIndexTerm(),
                                               x72.getLeaderId(),
                                               x72.getCommit());
                }
            // follower → leader
            case X73_RaftAccept.COMMAND ->
                {
                    X73_RaftAccept x73 = (X73_RaftAccept) content;
                    return _ClusterPeer.onAccept(x73.getFollowerId(),
                                                 x73.getTerm(),
                                                 x73.getCatchUp(),
                                                 x73.getLeaderId(),
                                                 manager);
                }
            // * → candidate
            case X74_RaftReject.COMMAND ->
                {
                    X74_RaftReject x74 = (X74_RaftReject) content;
                    return _ClusterPeer.onReject(x74.getPeerId(),
                                                 x74.getTerm(),
                                                 x74.getIndex(),
                                                 x74.getIndexTerm(),
                                                 x74.getCode(),
                                                 x74.getState());
                }
            // client → leader
            case X75_RaftReq.COMMAND ->
                {
                    if (_ClusterPeer.getMachine()
                                    .getState() != LEADER)
                    {
                        _Logger.warning("state error,expect:'LEADER',real:%s",
                                        _ClusterPeer.getMachine()
                                                    .getState());
                        break;
                    }
                    X75_RaftReq x75 = (X75_RaftReq) content;
                    return _ClusterPeer.newLeaderLogEntry(x75.getPayloadSerial(),
                                                          x75.getPayload(),
                                                          x75.getClientId(),
                                                          x75.getOrigin(),
                                                          x75.isPublic(),
                                                          manager,
                                                          x75.getSession());
                }
            // leader → client 
            case X76_RaftResp.COMMAND ->
                {
                    X76_RaftResp x76 = (X76_RaftResp) content;
                    return new Pair<>(null, x76);
                }
            // leader → client
            case X77_RaftNotify.COMMAND ->
                {
                    X77_RaftNotify x77 = (X77_RaftNotify) content;
                    x77.setNotify();
                    return new Pair<>(null, x77);
                }
            // peer *, behind → previous
            case X106_Identity.COMMAND ->
                {
                    X106_Identity x106 = (X106_Identity) content;
                    long peerId = x106.getIdentity();
                    long index = x106.getIndex();
                    _Logger.debug("=========> map peerId:%#x @ %#x", peerId, index);
                    manager.mapSession(index, session, peerId);
                }
            default -> throw new IllegalStateException("Unexpected value: " + content.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(ISessionManager manager, RaftMachine machine)
    {
        if (machine.operation() == IStorage.Operation.OP_MODIFY) {
            // step down → follower
            _ClusterPeer.turnToFollower(machine);
        }
        return switch (machine.operation())
        {
            // heartbeat
            case OP_APPEND -> _ClusterPeer.checkLogAppend(machine,
                                                          manager,
                                                          cmd -> new Triple<>(cmd,
                                                                              cmd.getSession(),
                                                                              cmd.getSession()
                                                                                 .getEncoder()));
            // vote
            case OP_INSERT -> _ClusterPeer.checkVoteState(machine,
                                                          manager,
                                                          cmd -> new Triple<>(cmd,
                                                                              cmd.getSession(),
                                                                              cmd.getSession()
                                                                                 .getEncoder()));
            default -> null;
        };
    }

    @Override
    public <E extends IConsistent & IControl> List<ITriple> consensus(ISessionManager manager, E request)
    {
        _Logger.debug("cluster consensus %s", request);
        if (_ClusterPeer.getMachine()
                        .getState() == LEADER)
        {
            /*
             * session belong to Link
             */
            return _ClusterPeer.newLocalLogEntry(request,
                                                 _ClusterPeer.getMachine()
                                                             .getPeerId(),
                                                 manager,
                                                 cmd -> new Triple<>(cmd,
                                                                     cmd.getSession(),
                                                                     cmd.getSession()
                                                                        .getEncoder()),
                                                 request.getSession());
        }
        else if (_ClusterPeer.getMachine()
                             .getLeader() != ZUID.INVALID_PEER_ID)
        {

            ISession leaderSession = manager.findSessionByPrefix(_ClusterPeer.getMachine()
                                                                             .getLeader());
            if (leaderSession != null) {
                X75_RaftReq x75 = new X75_RaftReq(_ClusterPeer.getRaftZUid());
                x75.setPayloadSerial(request.serial());
                x75.setPayload(request.encode());
                x75.setOrigin(request.getOrigin());
                x75.setClientId(_ClusterPeer.getMachine()
                                            .getPeerId());
                x75.setPublic(request.isPublic());
                return Collections.singletonList(new Triple<>(x75, leaderSession, leaderSession.getEncoder()));
            }
            else {
                _Logger.fetal("Leader connection miss,wait for reconnecting");
            }
        }
        _Logger.fetal("cluster is electing");
        // TODO 返回一个x76.failed 
        return null;
    }

    @Override
    public <E extends IConsistent & IControl> List<ITriple> change(ISessionManager manager, E topology)
    {
        _Logger.debug("cluster new topology %s", topology);
        if (_ClusterPeer.getMachine()
                        .getState() == LEADER)
        {
            List<ITriple> result = _ClusterPeer.newLocalLogEntry(topology,
                                                                 _ClusterPeer.getMachine()
                                                                             .getPeerId(),
                                                                 manager,
                                                                 cmd -> new Triple<>(cmd,
                                                                                     cmd.getSession(),
                                                                                     cmd.getSession()
                                                                                        .getEncoder()),
                                                                 //从follower来的时候有session,leader
                                                                 topology.getSession());
            //Accept Machine State
            return result;
        }
        else if (_ClusterPeer.getMachine()
                             .getLeader() != ZUID.INVALID_PEER_ID)
        {
            ISession leaderSession = manager.findSessionByPrefix(_ClusterPeer.getMachine()
                                                                             .getLeader());
            if (leaderSession != null) {
                return Collections.singletonList(new Triple<>(topology, leaderSession, leaderSession.getEncoder()));
            }
        }
        return null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _ClusterPeer.isClusterMode();
    }
}
