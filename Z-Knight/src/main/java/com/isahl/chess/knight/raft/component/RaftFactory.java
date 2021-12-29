package com.isahl.chess.knight.raft.component;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.isahl.chess.board.base.IFactory;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.model.ListSerial;
import com.isahl.chess.queen.message.InnerProtocol;

import java.util.List;

@ISerialFactory(parent = ISerial.CLUSTER_KNIGHT_RAFT_SERIAL)
public class RaftFactory
        implements IFactory,
                   IoFactory
{

    public static final RaftFactory _Instance = new RaftFactory();

    public static <T extends IoSerial> List<T> listOf(ByteBuf input)
    {
        ListSerial<T> list = new ListSerial<>(_Instance);
        list.decode(input);
        return list.isEmpty() ? null : list;
    }

    public static <T extends InnerProtocol> T oneOf(int serial, ByteBuf input)
    {
        return _Instance.one(serial, input);
    }

    @SuppressWarnings("unchecked")
    public <T extends InnerProtocol> T one(int serial, ByteBuf input)
    {
        T one = (T) build(serial);
        if(one != null) {
            one.decode(input);
            return one;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends IoSerial> T create(ByteBuf input)
    {
        int serial = input.peekUnsignedShort(0);
        T content = (T) build(serial);
        content.decode(input);
        return content;
    }

}
