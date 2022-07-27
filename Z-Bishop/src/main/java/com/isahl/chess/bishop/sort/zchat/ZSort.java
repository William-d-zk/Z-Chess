package com.isahl.chess.bishop.sort.zchat;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.factory.*;
import com.isahl.chess.bishop.protocol.zchat.filter.ZCommandFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZControlFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZFrameFilter;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;
import com.isahl.chess.queen.io.core.model.BaseSort;

public class ZSort
        extends BaseSort<ZContext>
{
    public ZSort(Mode mode, Type type)
    {
        super(mode, type, "z-chat");
        _Head = new ZFrameFilter();
        _Head.linkFront(new ZControlFilter((ZChatFactory) getFactory()))
             .linkFront(new ZCommandFilter((ZChatFactory) getFactory()));
    }

    private final IFilterChain _Head;

    @Override
    public IFilterChain getFilterChain()
    {
        return _Head;
    }

    @Override
    public ZContext newContext(INetworkOption option)
    {
        return new ZContext(option, getMode(), getType());
    }

    @Override
    public IoFactory<IProtocol> _SelectFactory()
    {
        return switch(getMode()) {
            case CLUSTER -> ZClusterFactory._Instance;
            case LINK -> switch(getType()) {
                case SERVER -> ZServerFactory._Instance;
                case SYMMETRY -> ZSymmetryFactory._Instance;
                case CLIENT -> ZConsumerFactory._Instance;
                case INNER -> ZInnerFactory._Instance;
            };
        };
    }
}
