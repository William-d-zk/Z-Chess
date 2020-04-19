/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X75_RaftRequest;
import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftResult;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.knight.raft.RaftState;
import com.tgx.chess.knight.raft.model.RaftCode;
import com.tgx.chess.knight.raft.model.RaftMachine;
import com.tgx.chess.knight.raft.model.RaftNode;
import com.tgx.chess.knight.raft.model.log.LogEntry;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IConsensus;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.manager.QueenManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tgx.chess.knight.raft.RaftState.CANDIDATE;
import static com.tgx.chess.knight.raft.RaftState.LEADER;

@Component
public class ClusterCustom<T extends IActivity<ZContext> & IClusterPeer & IConsensus>
        implements
        ICustomLogic<ZContext,
                     RaftMachine>
{
    private final Logger                        _Logger                      = Logger.getLogger(getClass().getSimpleName());
    private final IRepository<RaftNode<T>>      _ClusterRepository;
    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<List<LogEntry>>()
                                                                             {
                                                                             };

    private RaftNode<T> mRaftNode;

    @Autowired
    public ClusterCustom(IRepository<RaftNode<T>> clusterRepository)
    {
        _ClusterRepository = clusterRepository;
    }

    @Override
    public IPair handle(QueenManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> content) throws Exception
    {
        switch (content.serial())
        {
            case X72_RaftVote.COMMAND:
                X72_RaftVote x72 = (X72_RaftVote) content;
                RaftMachine machine = new RaftMachine(x72.getPeerId());
                machine.setIndex(x72.getLogIndex());
                machine.setIndexTerm(x72.getLogTerm());
                machine.setTerm(x72.getTerm());
                machine.setCandidate(x72.getPeerId());
                machine.setState(CANDIDATE);
                X7F_RaftResponse x7f = mRaftNode.merge(machine);
                return x7f != null ? new Pair<>(new X7F_RaftResponse[] { x7f }, null)
                                   : null;
            case X75_RaftRequest.COMMAND:
                if (mRaftNode.getMachine()
                             .getState() != LEADER)
                {
                    _Logger.warning("state error,expect:'LEADER',real:%s",
                                    mRaftNode.getMachine()
                                             .getState()
                                             .name());
                    break;
                }
                X75_RaftRequest x75 = (X75_RaftRequest) content;
                return new Pair<>(mRaftNode.newLogEntry(x75.getPayloadSerial(),
                                                        x75.getPayload(),
                                                        x75.getPeerId(),
                                                        x75.getOrigin())
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
                                           .toArray(X7E_RaftBroadcast[]::new),
                                  null);
            case X76_RaftResult.COMMAND:
                return new Pair<>(null, content);
            case X7E_RaftBroadcast.COMMAND:
                X7E_RaftBroadcast x7e = (X7E_RaftBroadcast) content;
                if (x7e.getPayload() != null) {
                    List<LogEntry> entryList = JsonUtil.readValue(x7e.getPayload(), _TypeReferenceOfLogEntryList);
                    if (entryList != null && !entryList.isEmpty()) {
                        mRaftNode.appendLogs(entryList);
                    }
                }
                machine = new RaftMachine(x7e.getPeerId());
                machine.setState(LEADER);
                machine.setTerm(x7e.getTerm());
                machine.setCommit(x7e.getCommit());
                machine.setIndexTerm(x7e.getPreIndexTerm());
                machine.setIndex(x7e.getPreIndex());
                x7f = mRaftNode.merge(machine);
                return x7f != null ? new Pair<>(new X7F_RaftResponse[] { x7f }, null)
                                   : null;
            case X7F_RaftResponse.COMMAND:
                x7f = (X7F_RaftResponse) content;
                return mRaftNode.onResponse(x7f.getPeerId(),
                                            x7f.getTerm(),
                                            x7f.getCatchUp(),
                                            x7f.getCandidate(),
                                            RaftState.valueOf(x7f.getState()),
                                            RaftCode.valueOf(x7f.getCode()),
                                            manager);
            case X106_Identity.COMMAND:
                X106_Identity x106 = (X106_Identity) content;
                long peerId = x106.getIdentity();
                _Logger.info("recv peerId:%#x", peerId);
                manager.mapSession(session.getIndex(), session, peerId);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + content.serial());
        }
        return null;
    }

    @Override
    public List<ITriple> onTimer(QueenManager<ZContext> manager, RaftMachine machine)
    {
        if (machine == null) { return null; }
        switch (machine.getOperation())
        {
            case OP_APPEND://heartbeat
                List<X7E_RaftBroadcast> x7EList = mRaftNode.checkLogAppend(machine);
                if (x7EList != null) {
                    return x7EList.stream()
                                  .map(x7E ->
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
                List<X72_RaftVote> x72List = mRaftNode.checkVoteState(machine);
                if (x72List != null) {
                    return x72List.stream()
                                  .map(x72 ->
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
    public List<ITriple> consensus(QueenManager<ZContext> manager,
                                   IControl<ZContext> request,
                                   ISession<ZContext> session)
    {
        if (mRaftNode.getMachine()
                     .getState() == LEADER)
        {
            /*
             * session belong to Link
             */
            return mRaftNode.newLogEntry(request,
                                         mRaftNode.getMachine()
                                                  .getPeerId(),
                                         session.getIndex())
                            .stream()
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
        else if (mRaftNode.getMachine()
                          .getLeader() != ZUID.INVALID_PEER_ID)
        {
            X75_RaftRequest x75 = new X75_RaftRequest(_ClusterRepository.getZid());
            x75.setPayloadSerial(request.serial());
            x75.setPayload(request.encode());
            x75.setOrigin(session.getIndex());
            x75.setPeerId(mRaftNode.getMachine()
                                   .getPeerId());
            ISession<ZContext> targetSession = manager.findSessionByPrefix(mRaftNode.getMachine()
                                                                                    .getLeader());
            if (targetSession != null) {
                return Collections.singletonList(new Triple<>(x75,
                                                              targetSession,
                                                              targetSession.getContext()
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

    public void setRaftNode(RaftNode<T> raftNode)
    {
        mRaftNode = raftNode;
    }
}
