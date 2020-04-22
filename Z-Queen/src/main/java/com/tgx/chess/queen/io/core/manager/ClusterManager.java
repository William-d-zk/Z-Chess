package com.tgx.chess.queen.io.core.manager;

import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.executor.ClusterCore;
import com.tgx.chess.queen.io.core.inf.IActivity;
import com.tgx.chess.queen.io.core.inf.IContext;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

public class ClusterManager<C extends IContext<C>>
        extends
        AioSessionManager<C>
        implements
        IActivity<C>
{
    protected final ClusterCore<C> _ClusterCore;

    public ClusterManager(IBizIoConfig config,
                          ClusterCore<C> clusterCore)
    {
        super(config);
        _ClusterCore = clusterCore;
    }

    @Override
    public void close(ISession<C> session, IOperator.Type type)
    {
        _ClusterCore.close(session, type);
    }

    @Override
    @SafeVarargs
    public final boolean send(ISession<C> session, IOperator.Type type, IControl<C>... commands)
    {
        return _ClusterCore.send(session, type, commands);
    }

    protected ClusterCore<C> getClusterCore()
    {
        return _ClusterCore;
    }

}
