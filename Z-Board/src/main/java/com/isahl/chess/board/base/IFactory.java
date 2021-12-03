package com.isahl.chess.board.base;

public interface IFactory
{
    default ISerial build(int serial)
    {
        return null;
    }
}
