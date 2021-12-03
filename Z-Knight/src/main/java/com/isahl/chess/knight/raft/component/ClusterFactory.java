package com.isahl.chess.knight.raft.component;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.isahl.chess.board.base.IFactory;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.queen.message.InnerProtocol;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

@ISerialFactory(parent = ISerial.CLUSTER_KNIGHT_RAFT_SERIAL)
public class ClusterFactory
        implements IFactory
{

    public static final ClusterFactory _Instance = new ClusterFactory();

    public static <T extends InnerProtocol> List<T> listOf(int serial, ByteBuffer input)
    {
        List<T> list = new LinkedList<>();
        while(input.hasRemaining()) {
            T t = oneOf(serial, input);
            if(t != null) {
                t.finish(input);
            }
            list.add(t);
        }
        return list.isEmpty() ? null : list;
    }

    public static <T extends InnerProtocol> T oneOf(int serial, ByteBuffer input)
    {
        return _Instance.one(serial, input);
    }

    @SuppressWarnings("unchecked")
    public <T extends InnerProtocol> T one(int serial, ByteBuffer input)
    {
        ISerial one = build(serial);
        if(one != null) {
            one.decode(input);
            return (T) one;
        }
        return null;
    }

}
