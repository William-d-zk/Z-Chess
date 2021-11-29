package com.isahl.chess.bishop.protocol.zchat.factory;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZProtocol;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.*;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IFactory;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.nio.ByteBuffer;

public class ZChatFactory
        implements IFactory<IFrame, ZContext>
{
    @Override
    public <T extends IControl> T create(IFrame frame, ZContext context)
    {
        return build(frame, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IControl> T create(int serial, ByteBuffer input)
    {
        IControl control = build(serial);
        if(control != null) {
            control.decode(input);
        }
        return (T) control;
    }

    @SuppressWarnings("unchecked")
    protected <T extends IControl, E extends ZProtocol & IControl> T build(IFrame frame, ZContext context)
    {
        E control = build(frame._sub());
        if(control != null) {
            control.put(frame.ctrl());
            control.putContext(context);
            control.decode(frame.payload(), context);
        }
        return (T) control;
    }

    @SuppressWarnings("unchecked")
    protected <T extends ZProtocol & IControl> T build(int serial)
    {
        return (T) switch(serial) {
            case 0x10A -> new X10A_PlainText();
            case 0x105 -> new X105_SslHandShake();
            case 0x106 -> new X106_Identity();
            case 0x107 -> new X107_Redirect();
            case 0x108 -> new X108_Shutdown();
            case 0x109 -> new X109_Consensus();
            default -> null;
        };
    }
}
