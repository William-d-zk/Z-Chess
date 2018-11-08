package com.tgx.chess.bishop.biz.db.dao;

import com.tgx.chess.queen.db.inf.IStorage;

public class DeviceEntry
        implements
        IStorage
{
    private final static int DEVICE_ENTRY_SERIAL = DB_SERIAL + 1;

    @Override
    public int dataLength()
    {
        return 0;
    }

    @Override
    public int getSerial()
    {
        return DEVICE_ENTRY_SERIAL;
    }

}
