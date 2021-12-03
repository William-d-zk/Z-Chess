package com.isahl.chess.queen.io.core.net.socket;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class AioPacketTest
{
    @Test
    public void serialize()
    {
        AioPacket packet = new AioPacket(10, false);
        packet.put("test".getBytes(StandardCharsets.UTF_8));
        ByteBuffer encoded = packet.encode();
        encoded.flip();

        AioPacket decoded = new AioPacket(ByteBuffer.allocate(encoded.capacity()));
        decoded.decode(encoded);

    }
}