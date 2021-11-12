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

import com.fasterxml.jackson.core.type.TypeReference;
import com.isahl.chess.bishop.protocol.ws.zchat.model.command.raft.*;
import com.isahl.chess.bishop.protocol.ws.zchat.model.ctrl.X106_Identity;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.model.RaftCode;
import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.queen.events.cluster.IClusterCustom;
import com.isahl.chess.queen.events.cluster.IConsistencyHandler;
import com.isahl.chess.queen.events.cluster.IConsistencyReject;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.WRITE;
import static com.isahl.chess.knight.raft.model.RaftState.LEADER;

public class RaftCustom
        implements IClusterCustom<RaftMachine>
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<>() {};
    private final RaftPeer                      _RaftPeer;
    private final List<IConsistencyHandler>     _HandlerList                 = new LinkedList<>();
    private final IConsistencyReject            _Reject;

    public RaftCustom(RaftPeer raftPeer, IConsistencyReject reject)
    {
        _RaftPeer = raftPeer;
        _Reject = reject;
    }

    /**
     * @param manager  cluster 管理器 注意与 device 管理器的区分
     * @param session  来源 session
     * @param received 需要 raft custom 处理的内容
     * @return ITriple
     * first :  command implements 'IControl', BATCH:List<IControl> ; SINGLE: IControl
     * second : command implements 'IConsistent', for transfer → LINK
     * third : operator-type [SINGLE|BATCH]
     */
    @Override
    public ITriple handle(IManager manager, ISession session, IControl received)
    {
        /*
         * leader -> follow, self::follow
         * 由于x76.origin == request.session.sessionIndex
         * LinkCustom中专门有对应findSession的操作，所以此处不再执行此操作，且在LinkCustom中执行更为安全
         */
        /* 作为 client 收到 notify */
        switch(received.serial()) {
            // follower → elector
            case 0x70 -> {
                X70_RaftVote x70 = (X70_RaftVote) received;
                return _RaftPeer.elect(x70.getTerm(),
                                       x70.getIndex(),
                                       x70.getIndexTerm(),
                                       x70.getCandidateId(),
                                       x70.getCommit(),
                                       manager,
                                       session);
            }
            // elector → candidate
            case 0x71 -> {
                X71_RaftBallot x71 = (X71_RaftBallot) received;
                return _RaftPeer.ballot(x71.getTerm(),
                                        x71.getElectorId(),
                                        x71.getIndex(),
                                        x71.getCandidateId(),
                                        x71.getCommit(),
                                        manager,
                                        session);
            }
            // leader → follower
            case 0x72 -> {
                X72_RaftAppend x72 = (X72_RaftAppend) received;
                if(x72.payload() != null) {
                    _RaftPeer.receiveLogs(JsonUtil.readValue(x72.payload(), _TypeReferenceOfLogEntryList));
                }
                return _RaftPeer.onResponse(x72.getTerm(),
                                            x72.getPreIndex(),
                                            x72.getPreIndexTerm(),
                                            x72.getLeaderId(),
                                            x72.getCommit(),
                                            session);
            }
            // follower → leader
            case 0x73 -> {
                X73_RaftAccept x73 = (X73_RaftAccept) received;
                return _RaftPeer.onAccept(x73.getTerm(),
                                          x73.getCatchUp(),
                                          x73.getCatchUpTerm(),
                                          x73.getFollowerId(),
                                          manager);
            }
            // * → candidate
            case 0x74 -> {
                X74_RaftReject x74 = (X74_RaftReject) received;
                return _RaftPeer.onReject(x74.getTerm(),
                                          x74.getIndex(),
                                          x74.getIndexTerm(),
                                          x74.getPeerId(),
                                          x74.getCode(),
                                          x74.getState(),
                                          session);
            }
            // client → leader
            case 0x75 -> {
                if(_RaftPeer.getMachine()
                            .getState() == LEADER)
                {
                    X75_RaftReq x75 = (X75_RaftReq) received;
                    return _RaftPeer.onRequest(x75._sub(),
                                               x75.payload(),
                                               x75.getClientId(),
                                               x75.getOrigin(),
                                               manager,
                                               session);
                }
                else {
                    _Logger.warning("state error,expect:'LEADER',real:%s",
                                    _RaftPeer.getMachine()
                                             .getState());
                }
            }
            // client.received:x76
            case 0x76 -> {
                X76_RaftResp x76 = (X76_RaftResp) received;
                _Logger.debug("received: %s, %s", x76, RaftCode.valueOf(x76.getCode()));
                return new Triple<>(null, x76, WRITE);
            }
            // leader → client
            case 0x77 -> {
                X77_RaftNotify x77 = (X77_RaftNotify) received;
                return _RaftPeer.onNotify(x77.getIndex());
            }
            // peer *, behind in config → previous in config
            case 0x106 -> {
                X106_Identity x106 = (X106_Identity) received;
                long peerId = x106.getIdentity();
                long newIdx = x106.getSessionIdx();
                _Logger.debug("===> map peerId:%#x @ %#x", peerId, newIdx);
                manager.mapSession(newIdx, session, peerId);
            }
            default -> throw new IllegalStateException("Unexpected value: " + received.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(IManager manager, RaftMachine machine)
    {
        if(machine == null) {return null;}
        return switch(machine.operation()) {
            // step down → follower
            case OP_MODIFY -> _RaftPeer.turnToFollower(machine);
            // vote
            case OP_INSERT -> _RaftPeer.checkVoteState(machine, manager);
            // heartbeat
            case OP_APPEND -> _RaftPeer.checkLogAppend(machine, manager);
            default -> null;
        };
    }

    @Override
    public <E extends IProtocol> List<ITriple> consistent(IManager manager, E request, long origin)
    {
        _Logger.debug("cluster consensus %s", request);
        if(_RaftPeer.getMachine()
                    .getState() == LEADER)
        {
            return _RaftPeer.onImmediate(request, manager, origin);
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
                x75.put(request.encode());
                x75.setOrigin(origin);
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
    public <E extends IConsistent> List<ITriple> changeTopology(IManager manager, E topology)
    {
        _Logger.debug("cluster new topology %s", topology);
        if(_RaftPeer.getMachine()
                    .getState() == LEADER)
        {
            //Accept Machine State
            return _RaftPeer.onImmediate(topology, manager, _RaftPeer.getPeerId());
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
        return _RaftPeer.isInCongress();
    }

    @Override
    public boolean onConsistentCall(IConsistent result)
    {
        return !_HandlerList.isEmpty() && _HandlerList.stream()
                                                      .anyMatch(handler->handler.onConsistencyCall(result));
    }

    @Override
    public IConsistencyReject getReject()
    {
        return _Reject;
    }

    @Override
    public void register(IConsistencyHandler handler)
    {
        _HandlerList.add(handler);
    }
}
