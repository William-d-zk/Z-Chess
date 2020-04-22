package com.tgx.chess.queen.event.handler;

import java.util.List;

import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public interface IClusterCustom<C extends IContext<C>,
                                T extends IStorage>
{
    /**
     *
     * @param manager
     * @param session
     * @param content
     * @return pair
     *         first: to_send_array 1:N
     *         second: to_transfer
     *         Cluster->Link,consensus_result
     * @throws Exception
     */
    IPair handle(AioSessionManager<C> manager, ISession<C> session, IControl<C> content) throws Exception;

    /**
     * Cluster.Leader heartbeat timeout event
     * Cluster.Follower check leader available,timeout -> start vote
     * Cluster.Candidate check elector response,timeout -> restart vote
     *
     * @param manager
     * @param content
     * @return
     */
    List<ITriple> onTimer(AioSessionManager<C> manager, T content);

    /**
     * Link -> Cluster.consensus(Link.consensus_data,consensus_data.origin)
     *
     * @param manager
     *            session 管理器
     * @param request
     *            需要进行强一致的指令
     * @param origin
     *            request 的源ID
     * @return triples
     *         [托管给集群IoSwitch.write(triples) 或 Transfer → Link.notify(triples)]
     */
    List<ITriple> consensus(AioSessionManager<C> manager, IControl<C> request, long origin);

    /**
     * 用于验证是否需要执行集群commit
     * 
     * @return true 等待集群确认，false 接续执行
     */
    boolean waitForCommit();
}
