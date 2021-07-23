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
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.ZCommand;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.control.X106_Identity;
import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.*;
import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.RaftPeer;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.queen.db.inf.IStorage;
import com.isahl.chess.queen.event.handler.cluster.IClusterCustom;
import com.isahl.chess.queen.event.handler.cluster.IConsistencyCustom;
import com.isahl.chess.queen.io.core.inf.*;

import java.util.Collections;
import java.util.List;

import static com.isahl.chess.knight.raft.model.RaftCode.WAL_FAILED;
import static com.isahl.chess.knight.raft.model.RaftState.LEADER;

public class RaftCustom<T extends IClusterPeer & IClusterTimer>
        implements IClusterCustom<RaftMachine>,
                   IConsistencyCustom
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<>() {};
    private final RaftPeer<T>                   _RaftPeer;

    public RaftCustom(RaftPeer<T> raftPeer)
    {
        _RaftPeer = raftPeer;
    }

    /**
     * @param manager cluster 管理器 注意与 device 管理器的区分
     * @param session 来源 session
     * @param content 需要 raft custom 处理的内容
     * @return IPair
     * first : list of command implements 'IControl',broadcast to all
     * cluster peers
     * second : command implements 'IConsistentNotify',
     */
    @Override
    public IPair handle(ISessionManager manager, ISession session, IControl content)
    {
        /*
         * leader -> follow, self::follow
         * 由于x76.origin == request.session.sessionIndex
         * LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
         */
        /* 作为 client 收到 notify */
        switch(content.serial()) {
            // follower → elector
            case X70_RaftVote.COMMAND -> {
                X70_RaftVote x70 = (X70_RaftVote) content;
                return _RaftPeer.elect(x70.getTerm(),
                                       x70.getIndex(),
                                       x70.getIndexTerm(),
                                       x70.getCandidateId(),
                                       x70.getCommit(),
                                       manager);
            }
            // elector → candidate
            case X71_RaftBallot.COMMAND -> {
                X71_RaftBallot x71 = (X71_RaftBallot) content;
                return _RaftPeer.ballot(x71.getTerm(),
                                        x71.getIndex(),
                                        x71.getElectorId(),
                                        x71.getCandidateId(),
                                        manager);
            }
            // leader → follower
            case X72_RaftAppend.COMMAND -> {
                X72_RaftAppend x72 = (X72_RaftAppend) content;
                if(x72.getPayload() != null) {
                    _RaftPeer.appendLogs(JsonUtil.readValue(x72.getPayload(), _TypeReferenceOfLogEntryList));
                }
                return _RaftPeer.append(x72.getTerm(),
                                        x72.getPreIndex(),
                                        x72.getPreIndexTerm(),
                                        x72.getLeaderId(),
                                        x72.getCommit());
            }
            // follower → leader
            case X73_RaftAccept.COMMAND -> {
                X73_RaftAccept x73 = (X73_RaftAccept) content;
                return _RaftPeer.onAccept(x73.getFollowerId(),
                                          x73.getTerm(),
                                          x73.getCatchUp(),
                                          x73.getLeaderId(),
                                          manager);
            }
            // * → candidate
            case X74_RaftReject.COMMAND -> {
                X74_RaftReject x74 = (X74_RaftReject) content;
                return _RaftPeer.onReject(x74.getPeerId(),
                                          x74.getTerm(),
                                          x74.getIndex(),
                                          x74.getIndexTerm(),
                                          x74.getCode(),
                                          x74.getState());
            }
            // client → leader
            case X75_RaftReq.COMMAND -> {
                if(_RaftPeer.getMachine()
                            .getState() == LEADER)
                {
                    X75_RaftReq x75 = (X75_RaftReq) content;
                    return _RaftPeer.newLeaderLogEntry(x75.getSubSerial(),
                                                       x75.getPayload(),
                                                       x75.getClientId(),
                                                       x75.getOrigin(),
                                                       manager,
                                                       x75.getSession());
                }
                else {
                    _Logger.warning("state error,expect:'LEADER',real:%s",
                                    _RaftPeer.getMachine()
                                             .getState());
                }
            }
            // leader → client 
            case X76_RaftResp.COMMAND -> {
                X76_RaftResp x76 = (X76_RaftResp) content;
                return new Pair<>(null, x76);
            }
            // leader → client
            case X77_RaftNotify.COMMAND -> {
                X77_RaftNotify x77 = (X77_RaftNotify) content;
                return new Pair<>(null, x77);
            }
            // peer *, behind in config → previous in config
            case X106_Identity.COMMAND -> {
                X106_Identity x106 = (X106_Identity) content;
                long peerId = x106.getIdentity();
                long newIdx = x106.getSessionIdx();
                _Logger.debug("=========> map peerId:%#x @ %#x", peerId, newIdx);
                manager.mapSession(newIdx, session, peerId);
            }
            default -> throw new IllegalStateException("Unexpected value: " + content.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(ISessionManager manager, RaftMachine machine)
    {
        if(machine == null) {return null;}
        if(machine.operation() == IStorage.Operation.OP_MODIFY) {
            // step down → follower
            _RaftPeer.turnToFollower(machine);
        }
        return switch(machine.operation()) {
            // heartbeat
            case OP_APPEND -> _RaftPeer.checkLogAppend(machine,
                                                       manager,
                                                       cmd->new Triple<>(cmd,
                                                                         cmd.getSession(),
                                                                         cmd.getSession()
                                                                            .getEncoder()));
            // vote
            case OP_INSERT -> _RaftPeer.checkVoteState(machine,
                                                       manager,
                                                       cmd->new Triple<>(cmd,
                                                                         cmd.getSession(),
                                                                         cmd.getSession()
                                                                            .getEncoder()));
            default -> null;
        };
    }

    private Triple<ZCommand, ISession, IPipeEncoder> tMapper(ZCommand in)
    {
        return new Triple<>(in, in.getSession(),//此处已执行完毕 manager.find
                            in.getSession()
                              .getEncoder());
    }

    @Override
    public <E extends IConsistent> List<ITriple> consensus(ISessionManager manager, E request)
    {
        _Logger.debug("cluster consensus %s", request);
        if(_RaftPeer.getMachine()
                    .getState() == LEADER)
        {
            return _RaftPeer.newLocalLogEntry(request,
                                              _RaftPeer.getMachine()
                                                       .getPeerId(),
                                              manager,
                                              this::tMapper);
        }
        else if(_RaftPeer.getMachine()
                         .getLeader() != ZUID.INVALID_PEER_ID)
        {
            ISession leaderSession = manager.findSessionByPrefix(_RaftPeer.getMachine()
                                                                          .getLeader());
            if(leaderSession != null) {
                _Logger.info("client → leader x75");
                X75_RaftReq x75 = new X75_RaftReq(_RaftPeer.generateId());
                x75.setSubSerial(request.serial());
                x75.setPayload(request.encode());
                x75.setOrigin(request.getOrigin());
                x75.setClientId(_RaftPeer.getMachine()
                                         .getPeerId());
                return Collections.singletonList(new Triple<>(x75, leaderSession, leaderSession.getEncoder()));
            }
            else {
                _Logger.fetal("Leader connection miss,wait for reconnecting");
            }
        }
        _Logger.fetal("cluster is electing");
        return null;
    }

    @Override
    public <E extends IConsistent> List<ITriple> changeTopology(ISessionManager manager, E topology)
    {
        _Logger.debug("cluster new topology %s", topology);
        if(_RaftPeer.getMachine()
                    .getState() == LEADER)
        {
            //Accept Machine State
            return _RaftPeer.newLocalLogEntry(topology,
                                              _RaftPeer.getMachine()
                                                       .getPeerId(),
                                              manager,
                                              this::tMapper);
        }
        else if(_RaftPeer.getMachine()
                         .getLeader() != ZUID.INVALID_PEER_ID)
        {
            ISession leaderSession = manager.findSessionByPrefix(_RaftPeer.getMachine()
                                                                          .getLeader());
            if(leaderSession != null) {
                return Collections.singletonList(new Triple<>(topology, leaderSession, leaderSession.getEncoder()));
            }
        }
        return null;
    }

    @Override
    public boolean waitForCommit()
    {
        //TODO learner 的情景需要处理
        return _RaftPeer.isInCongress();
    }

    /*
    device → LINK → ClusterEvent → ClusterProcessor (MixMapping.CLUSTER) →
    {

    }

    1: throwable
    2: ISessionError
    3: List<ITriple> 集群广播数据
     */

    @Override
    public IPair resolve(IConsistent request, ISession session)
    {
        X76_RaftResp x76 = _RaftPeer.raftResp(WAL_FAILED,
                                              _RaftPeer.getPeerId(),
                                              request.getOrigin(),
                                              request.serial(),
                                              request.encode());
        return new Pair<>(x76, session);
    }
}
