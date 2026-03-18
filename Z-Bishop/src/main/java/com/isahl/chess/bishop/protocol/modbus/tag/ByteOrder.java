/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * 字节序
 */
public enum ByteOrder {
    ABCD,  // Big Endian
    CDAB,  // Little Endian
    BADC,  // Byte Swap
    DCBA;  // Full Swap
    
    public static final ByteOrder BIG_ENDIAN = ABCD;
    public static final ByteOrder LITTLE_ENDIAN = CDAB;
}
