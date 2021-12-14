package com.isahl.chess.bishop.sort.zchat;

import com.isahl.chess.bishop.io.BaseSort;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.factory.*;
import com.isahl.chess.bishop.protocol.zchat.filter.ZCommandFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZControlFilter;
import com.isahl.chess.bishop.protocol.zchat.filter.ZFrameFilter;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;

public class ZSort
        extends BaseSort<ZContext>
{
    public ZSort(Mode mode, Type type)
    {
        super(mode, type, "z-chat");
        ZChatFactory factory = switch(mode) {
            case CLUSTER -> new ZClusterFactory();
            case LINK -> switch(type) {
                case SERVER -> ZServerFactory._Instance;
                case SYMMETRY -> ZSymmetryFactory._Instance;
                case CLIENT -> ZConsumerFactory._Instance;
                case INNER -> ZInnerFactory._Instance;
            };
        };
        _Head.linkFront(new ZControlFilter())
             .linkFront(new ZCommandFilter(factory));
    }

    private final ZFrameFilter _Head = new ZFrameFilter();

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
}
