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

package com.tgx.chess.queen.event.handler.cluster;

import java.util.List;

import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.IMappingCustom;
import com.tgx.chess.queen.io.core.inf.IConsistent;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IProtocol;
import com.tgx.chess.queen.io.core.inf.ISessionManager;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public interface IClusterCustom<C extends IContext<C>,
                                T extends IStorage>
        extends
        IMappingCustom<C>
{
    /*
     *
     * @param manager
     * @param session
     * @param content
     * @return pair
     *         first: to_send_array 1:N
     *         second: to_transfer
     *         Cluster->Link,consensus_result
     * @throws Exception
     * @see IPair handle(ISessionManager<C> manager, ISession<C> session,
     *      IControl<C> content) throws Exception;
     */

    /**
     * Cluster.Leader heartbeat timeout event
     * Cluster.Follower check leader available,timeout -> start vote
     * Cluster.Candidate check elector response,timeout -> restart vote
     *
     * @param manager
     * @param content
     * @return
     */
    List<ITriple> onTimer(ISessionManager<C> manager, T content);

    /**
     * Link -> Cluster.consensus(Link.consensus_data,consensus_data.origin)
     *
     * @param manager
     *            session 管理器
     * @param request
     *            需要进行强一致的指令
     * @return triples
     *         [托管给集群IoSwitch.write(triples) 或 Transfer → Link.notify(triples)]
     */
    <E extends IConsistent & IProtocol> List<ITriple> consensus(ISessionManager<C> manager, E request);

    /**
     * 用于验证是否需要执行集群commit
     * 
     * @return true 等待集群确认，false 接续执行
     */
    boolean waitForCommit();
}
