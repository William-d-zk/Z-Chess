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

import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.cluster.raft.IMachine;
import com.tgx.chess.cluster.raft.IRaftMessage;
import com.tgx.chess.cluster.raft.IRaftNode;
import com.tgx.chess.king.base.log.Logger;

/**
 * @author william.d.zk
 * @date 2020/1/4
 */
public class RaftNode
        implements
        IRaftNode,
        IMachine
{
    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());
    /**
     * 状态
     */
    private RaftState    state   = RaftState.FOLLOWER;
    /**
     * 当前选举的轮次
     * 有效值 > 0
     */
    private long         term    = 0;
    /**
     * 候选人 有效值 非0，很可能是负值
     * 
     * @see ZUID
     */
    private long         candidate;
    /**
     * 已知的leader
     * 有效值 非0，很可能是负值
     * 
     * @see ZUID
     */
    private long         leader;
    /**
     * 已被提交的日志ID
     * 有效值 非0，很可能是负值
     * 48bit
     * 
     * @see ZUID
     */
    private long         commit;
    /**
     * 已被状态机采纳的纪录ID
     * 必须 <= commit
     * 48bit
     * 
     * @see ZUID
     */
    private long         apply;

    private void reset()
    {
        leader = 0;
        candidate = 0;
        term = 0;
        commit = 0;
        apply = 0;
    }

    @Override
    public RaftState getState()
    {
        return state;
    }

    @Override
    public long getTerm()
    {
        return term;
    }

    @Override
    public long getElector()
    {
        return 0;
    }

    @Override
    public long getLeader()
    {
        return 0;
    }

    @Override
    public long getCommitIndex()
    {
        return 0;
    }

    @Override
    public long getLastAppliedIndex()
    {
        return 0;
    }

    @Override
    public void apply(IRaftMessage msg)
    {

    }

    @Override
    public void load(List<IRaftMessage> snapshot)
    {

    }

    @Override
    public void save(List<IRaftMessage> snapshot)
    {

    }
}
