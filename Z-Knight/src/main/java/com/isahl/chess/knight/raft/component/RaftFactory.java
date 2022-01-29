package com.isahl.chess.knight.raft.component;

import com.isahl.chess.board.annotation.ISerialFactory;
import com.isahl.chess.board.base.IFactory;
import com.isahl.chess.board.base.ISerial;

@ISerialFactory(parent = ISerial.CLUSTER_KNIGHT_RAFT_SERIAL)
public class RaftFactory
        implements IFactory
{
    public static final IFactory _Instance = new RaftFactory();
}
