package com.isahl.chess.board.base;

public interface IFactory
{
    default int serial() {return -1;}

    default boolean isSupport(int serial)
    {
        return false;
    }
}
