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

package com.tgx.chess.cluster.raft.service;

import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.CANDIDATE;
import static com.tgx.chess.cluster.raft.IRaftNode.RaftState.LEADER;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.control.X109_Consensus;
import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.cluster.raft.model.RaftMachine;
import com.tgx.chess.cluster.raft.model.RaftNode;
import com.tgx.chess.cluster.raft.model.log.LogEntry;
import com.tgx.chess.json.JsonUtil;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.db.inf.IRepository;
import com.tgx.chess.queen.event.inf.ICustomLogic;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionManager;
import com.tgx.chess.queen.io.core.manager.QueenManager;

@Component
public class ClusterCustom<T extends ISessionManager<ZContext> & IActivity<ZContext> & IClusterPeer>
        implements
        ICustomLogic<ZContext>
{
    private final Logger                        _Logger                      = Logger.getLogger(getClass().getSimpleName());
    private final IRepository<RaftNode<T>>      _ClusterRepository;
    private final TypeReference<List<LogEntry>> _TypeReferenceOfLogEntryList = new TypeReference<List<LogEntry>>()
                                                                             {
                                                                             };
    private RaftNode<T>                         mRaftNode;

    @Autowired
    public ClusterCustom(IRepository<RaftNode<T>> clusterRepository)
    {
        _ClusterRepository = clusterRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] handle(QueenManager<ZContext> manager,
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
                return x7f != null ? new IControl[] { x7f }
                                   : null;
            case X7E_RaftBroadcast.COMMAND:
                X7E_RaftBroadcast x7e = (X7E_RaftBroadcast) content;
                List<LogEntry> entryList = JsonUtil.readValue(x7e.getPayload(), _TypeReferenceOfLogEntryList);
                if (entryList != null && !entryList.isEmpty()) {
                    mRaftNode.appendLogs(entryList);
                }
                machine = new RaftMachine(x7e.getPeerId());
                machine.setState(LEADER);
                machine.setTerm(x7e.getTerm());
                machine.setCommit(x7e.getCommit());
                machine.setIndexTerm(x7e.getPreIndexTerm());
                machine.setIndex(x7e.getPreIndex());
                x7f = mRaftNode.merge(machine);
                return x7f != null ? new IControl[] { x7f }
                                   : null;
            case X106_Identity.COMMAND:
                X106_Identity x106 = (X106_Identity) content;
                long peerId = x106.getIdentity();
                manager.mapSession(session.getIndex(), session, peerId);
                break;

        }
        return null;
    }

    @Override
    public IControl<ZContext> consensus(QueenManager<ZContext> manager,
                                        ISession<ZContext> session,
                                        IControl<ZContext> content) throws Exception
    {

        switch (content.serial())
        {
            case X7E_RaftBroadcast.COMMAND:
                //状态机已完成merge 但并未执行apply，此处执行apply 操作，并将diff发送给LINK 完成业务端的一致化
                List<LogEntry> diff = mRaftNode.diff();
                mRaftNode.apply();
                if (diff != null) {
                    byte[] payload = JsonUtil.writeValueAsBytes(diff);
                    X109_Consensus x109 = new X109_Consensus();
                    x109.setPayload(payload);
                    return x109;
                }
                break;
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IControl<ZContext>[] onTransfer(IControl<ZContext>[] content)
    {
        if (content == null || content.length == 0) { return null; }
        if (content.length > 1) {
            List<IControl<ZContext>> resultList = new LinkedList<>();
            for (IControl<ZContext> control : content) {
                switch (control.serial())
                {
                    case X7E_RaftBroadcast.COMMAND:
                        if (mRaftNode.checkLogAppend((X7E_RaftBroadcast) control)) {
                            resultList.add(control);
                        }
                        break;
                }
            }
            if (!resultList.isEmpty()) { return resultList.toArray(new IControl[0]); }
        }
        else {
            IControl<ZContext> control = content[0];
            switch (control.serial())
            {
                case X72_RaftVote.COMMAND:
                    if (mRaftNode.checkVoteState((X72_RaftVote) control)) { return content; }
                    break;
            }
        }
        return null;
    }

    public void setRaftNode(RaftNode<T> raftNode)
    {
        mRaftNode = raftNode;
    }
}
