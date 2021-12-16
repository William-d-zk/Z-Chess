package com.isahl.chess.queen.io.core.net.socket;

import com.isahl.chess.king.base.content.ByteBuf;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

class AioPacketTest
{
    @Test
    public void serialize()
    {
        AioPacket packet = new AioPacket(10);
        packet.getBuffer()
              .put("test".getBytes(StandardCharsets.UTF_8));
        ByteBuf encoded = packet.encode();

    }
}