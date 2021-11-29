package com.isahl.chess.bishop.protocol.zchat.factory;

import com.isahl.chess.bishop.protocol.zchat.model.base.ZProtocol;
import com.isahl.chess.queen.io.core.features.model.content.IControl;

public class ZInnerFactory
        extends ZChatFactory
{
    private final static ZInnerFactory _Instance = new ZInnerFactory();

    @Override
    protected <T extends ZProtocol & IControl> T build(int serial)
    {
        return super.build(serial);
    }
}
