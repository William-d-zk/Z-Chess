package com.isahl.chess.knight.raft.component;

import com.isahl.chess.knight.raft.model.RaftMachine;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.replicate.LogEntry;
import com.isahl.chess.knight.raft.model.replicate.LogMeta;
import com.isahl.chess.knight.raft.model.replicate.SnapshotEntry;
import com.isahl.chess.knight.raft.model.replicate.SnapshotMeta;
import com.isahl.chess.queen.message.InnerProtocol;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class ClusterFactory
{

    @SuppressWarnings("ubchecked")
    public static <T extends InnerProtocol> T create(int serial, ByteBuffer input)
    {
        InnerProtocol protocol = switch(serial) {
            case 0xE01 -> new LogMeta();
            case 0xE02 -> new LogEntry();
            case 0xE03 -> new SnapshotMeta();
            case 0xE04 -> new SnapshotEntry();
            case 0xE05 -> new RaftNode();
            case 0xE06 -> new RaftMachine();
            default -> null;
        };
        if(protocol != null) {
            protocol.decode(input);
        }
        return (T) protocol;
    }

    public static <T extends InnerProtocol> List<T> listOf(int serial, ByteBuffer input)
    {
        List<T> list = new LinkedList<>();
        while(input.hasRemaining()) {
            T t = create(serial, input);
            if(t != null) {
                t.finish(input);
            }
            list.add(t);
        }
        return list.isEmpty() ? null : list;
    }

}
