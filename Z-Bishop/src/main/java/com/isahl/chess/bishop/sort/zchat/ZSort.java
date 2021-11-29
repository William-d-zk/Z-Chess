package com.isahl.chess.bishop.sort.zchat;

import com.isahl.chess.bishop.io.BaseSort;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.pipe.IFilterChain;

public class ZSort
        extends BaseSort<ZContext>
{

    protected ZSort(Mode mode, Type type)
    {
        super(mode, type, "z-chat");
    }

    @Override
    public IFilterChain getFilterChain()
    {
        return null;
    }

    @Override
    public ZContext newContext(INetworkOption option)
    {
        return null;
    }
}
