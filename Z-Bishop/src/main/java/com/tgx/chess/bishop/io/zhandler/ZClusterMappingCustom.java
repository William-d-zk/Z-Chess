package com.tgx.chess.bishop.io.zhandler;

import java.util.List;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.zprotocol.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.zprotocol.ztls.X06_EncryptComp;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.handler.IClusterCustom;
import com.tgx.chess.queen.io.core.async.AioSessionManager;
import com.tgx.chess.queen.io.core.inf.IControl;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 * @date 2020/4/20
 */
public class ZClusterMappingCustom<T extends IStorage>
        implements
        IClusterCustom<ZContext,
                       T>
{
    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    private final IClusterCustom<ZContext,
                                 T> _Then;

    public ZClusterMappingCustom(IClusterCustom<ZContext,
                                                T> then)
    {
        _Then = then;
    }

    @Override
    public IPair handle(AioSessionManager<ZContext> manager,
                        ISession<ZContext> session,
                        IControl<ZContext> content) throws Exception
    {
        _Logger.info("cluster mapping receive %s", content);
        switch (content.serial())
        {
            case X01_EncryptRequest.COMMAND:
            case X02_AsymmetricPub.COMMAND:
            case X03_Cipher.COMMAND:
            case X04_EncryptConfirm.COMMAND:
            case X05_EncryptStart.COMMAND:
            case X06_EncryptComp.COMMAND:
                /*
                 *  内嵌逻辑，在ZCommandFilter中已经处理结束
                 *  此处仅执行转发逻辑
                 */
                return new Pair<>(new IControl[] { content }, null);
            default:
                if (_Then == null) return null;
                return _Then.handle(manager, session, content);
        }
    }

    @Override
    public List<ITriple> onTimer(AioSessionManager<ZContext> manager, T content)
    {
        return _Then != null ? _Then.onTimer(manager, content)
                             : null;
    }

    @Override
    public List<ITriple> consensus(AioSessionManager<ZContext> manager, IControl<ZContext> request, long origin)
    {
        return _Then != null ? _Then.consensus(manager, request, origin)
                             : null;
    }

    @Override
    public boolean waitForCommit()
    {
        return _Then != null && _Then.waitForCommit();
    }
}
