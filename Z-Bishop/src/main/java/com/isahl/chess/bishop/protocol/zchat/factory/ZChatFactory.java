package com.isahl.chess.bishop.protocol.zchat.factory;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0D_PlainText;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0E_Consensus;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.*;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls.*;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.model.content.IProtocolFactory;

public class ZChatFactory
        implements IProtocolFactory<ZFrame, ZContext>
{
    public final static ZChatFactory _Instance = new ZChatFactory();

    @Override
    public ZControl create(ZFrame frame, ZContext context)
    {
        ByteBuf framePayload = frame.payload();
        ZControl control = create(framePayload).wrap(context);
        control.decode(framePayload);
        return control;
    }

    @Override
    public ZControl create(ByteBuf input)
    {
        return build(ZFrame.seekSubSerial(input));
    }

    protected ZControl build(int serial)
    {
        return switch(serial) {
            case 0x01 -> new X01_EncryptRequest();
            case 0x02 -> new X02_AsymmetricPub();
            case 0x03 -> new X03_Cipher();
            case 0x04 -> new X04_EncryptConfirm();
            case 0x05 -> new X05_EncryptStart();
            case 0x06 -> new X06_EncryptComp();
            case 0x07 -> new X07_SslHandShake();
            case 0x08 -> new X08_Identity();
            case 0x09 -> new X09_Redirect();
            case 0x0A -> new X0A_Shutdown();
            case 0x0B -> new X0E_Consensus();
            case 0x0C -> new X0D_PlainText();
            default -> null;
        };
    }
}
