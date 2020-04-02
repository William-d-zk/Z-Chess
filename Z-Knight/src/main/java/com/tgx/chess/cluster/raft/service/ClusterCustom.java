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

import java.util.List;
import java.util.stream.Stream;

import com.tgx.chess.cluster.raft.model.RaftResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.control.X106_Identity;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
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
        ICustomLogic<ZContext,
                     RaftResponse>
{
    private final Logger                   _Logger = Logger.getLogger(getClass().getSimpleName());
    private final IRepository<RaftNode<T>> _ClusterRepository;
    private RaftNode<T>                    mRaftNode;

    @Autowired
    public ClusterCustom(IRepository<RaftNode<T>> clusterRepository)
    {
        _ClusterRepository = clusterRepository;
    }

    @Override
    public IControl<ZContext>[] handle(QueenManager<ZContext> manager,
                                       ISession<ZContext> session,
                                       IControl<ZContext> content) throws Exception
    {
        switch (content.serial())
        {

            case X7E_RaftBroadcast.COMMAND:
                X7E_RaftBroadcast x7e = (X7E_RaftBroadcast) content;
                List<LogEntry> entryList = JsonUtil.readValue(x7e.getPayload(), new TypeReference<List<LogEntry>>()
                {
                });
                break;
            case X106_Identity.COMMAND:
                X106_Identity x106 = (X106_Identity) content;
                long peerId = x106.getIdentity();
                manager.mapSession(session.getIndex(), session, peerId);
                break;

        }

        return null;
    }

    @Override
    public IControl<ZContext>[] onTransfer(IControl<ZContext>[] content)
    {
        if (content == null || content.length == 0) { return null; }

    }

    public void setRaftNode(RaftNode<T> raftNode)
    {
        mRaftNode = raftNode;
    }
}
