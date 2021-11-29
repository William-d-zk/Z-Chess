package com.isahl.chess.bishop.protocol.zchat.model.ctrl;

import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.base.ZProtocol;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.nio.ByteBuffer;

public abstract class ZControl
        extends ZProtocol
        implements IControl
{

    public ZControl()
    {
        super();
        withId(false);
    }

    private ISession mSession;

    @Override
    public void reset()
    {
        mSession = null;
    }

    @Override
    public void putSession(ISession session)
    {
        mSession = session;
    }

    @Override
    public ISession session()
    {
        return mSession;
    }

    @Override
    public void put(byte ctrl)
    {
        setHeader(ctrl);
    }

    @Override
    public byte ctrl()
    {
        return getHeader();
    }

    @Override
    public boolean isCtrl()
    {
        return true;
    }

    @Override
    public boolean isMapping()
    {
        return true;
    }

    @Override
    public void decodec(ByteBuffer input)
    {

    }

    @Override
    public void encodec(ByteBuffer output)
    {

    }

    @Override
    @SuppressWarnings("unchecked")
    public ZContext context()
    {
        return super.context();
    }
}
