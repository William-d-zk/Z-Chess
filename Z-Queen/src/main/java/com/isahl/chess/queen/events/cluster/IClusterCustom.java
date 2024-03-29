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

package com.isahl.chess.queen.events.cluster;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.queen.db.model.IStorage;
import com.isahl.chess.queen.events.routes.IMappingCustom;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.model.session.IManager;

import java.util.List;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public interface IClusterCustom<T extends IStorage>
        extends IMappingCustom
{
    /**
     * Cluster.Leader heartbeat timeout event
     * Cluster.Follower check leader available,timeout -> start vote
     * Cluster.Candidate check elector response,timeout -> restart vote
     *
     * @param manager session-manager
     * @param machine cluster-state-machine
     * @return cluster result list of {fst:content,snd:session,trd:encoder}
     */
    List<ITriple> onTimer(IManager manager, T machine);

    /**
     * Link → Cluster.consistent(Link.consensus_data,consensus_data.origin)
     *
     * @param manager session 管理器
     * @param request 需要进行强一致的指令
     * @param origin  指令的发起者编号；一致性完成时需要回溯
     * @param factory request 的构建器编号
     * @return triples
     * fst:request
     * snd:session[cluster|single::linker]
     * trd:operator[session.encoder]
     */
    List<ITriple> consistent(IManager manager, IoSerial request, long origin, int factory);

    /**
     * consensus-api-publisher → cluster.change(new-topology)
     *
     * @param manager  session-manager
     * @param topology 节点信息
     * @return list of triple → publish (write,list)
     */
    List<ITriple> change(IManager manager, IoSerial topology);

    /**
     * 用于验证是否需要执行集群commit
     *
     * @return true 等待集群确认，false 接续执行
     */
    boolean waitForCommit();

    IConsistency skipConsistency(IoSerial request, long origin);
}
