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

import org.springframework.stereotype.Service;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.model.RaftNode;
import com.tgx.chess.cluster.raft.service.api.IConsensusService;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 */
@Service
public class ConsensusService<T extends ISessionManager<ZContext> & IActivity<ZContext> & IClusterPeer>
        implements
        IConsensusService<T>
{

    public ConsensusService()
    {
    }

    @Override
    public List<IRaftMessage> prepare()
    {
        return null;
    }

    @Override
    public List<IRaftMessage> elect(RaftNode<T> candidate)
    {
        return null;
    }

    @Override
    public void appendLog(IRaftMessage log)
    {

    }

    @Override
    public void installSnapshot(List<IRaftMessage> snapshot)
    {

    }
}
