package com.isahl.chess.bishop.protocol.zchat.model.base;

import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.queen.io.core.features.model.content.IFrame;

import java.nio.ByteBuffer;

@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_FRAME_SERIAL)
public class ZFrame
        implements IFrame,
                   IReset
{
    @Override
    public void reset()
    {

    }

    @Override
    public void put(byte ctrl)
    {

    }

    @Override
    public byte ctrl()
    {
        return 0;
    }

    @Override
    public boolean isCtrl()
    {
        return false;
    }

    @Override
    public void decodec(ByteBuffer input)
    {

    }

    @Override
    public void encodec(ByteBuffer output)
    {

    }
}
