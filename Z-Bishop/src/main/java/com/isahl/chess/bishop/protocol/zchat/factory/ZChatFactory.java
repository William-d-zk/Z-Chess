package com.isahl.chess.bishop.protocol.zchat.factory;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZFrame;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0D_PlainText;
import com.isahl.chess.bishop.protocol.zchat.model.command.X0E_Consensus;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.*;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls.*;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.queen.io.core.features.model.content.IProtocolFactory;

public class ZChatFactory
        implements IProtocolFactory<ZFrame, ZContext>
{
    public final static ZChatFactory _Instance = new ZChatFactory();

    @Override
    public ZControl create(ZFrame frame, ZContext context)
    {
        ByteBuf input = frame.subEncoded();
        ZControl instance = build(ZFrame.peekSubSerial(input)).wrap(context);
        instance.decode(input);
        return instance;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ZControl create(ByteBuf input)
    {
        ZControl instance = build(ZFrame.peekSubSerial(input));
        instance.decode(input);
        return instance;
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
            case 0x0B -> new X0B_Ping();
            case 0x0C -> new X0C_Pong();
            case 0x0D -> new X0D_PlainText();
            case 0x0E -> new X0E_Consensus();
            default -> throw new ZException("unsupported serial %d", serial);
        };
    }
}
