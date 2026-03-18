/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus;

/**
 * Modbus 协议常量
 */
public final class ModbusConstants {
    
    private ModbusConstants() {}
    
    // 默认端口
    public static final int DEFAULT_TCP_PORT = 502;
    public static final int DEFAULT_TLS_PORT = 802;
    
    // MBAP 报文头长度
    public static final int MBAP_HEADER_LENGTH = 7;
    
    // RTU 帧最小长度
    public static final int RTU_MIN_FRAME_LENGTH = 4;
    
    // 最大 PDU 长度
    public static final int MAX_PDU_LENGTH = 253;
    
    // 广播地址
    public static final int BROADCAST_UNIT_ID = 0;
    
    // 最大从站地址
    public static final int MAX_UNIT_ID = 247;
    
    // 事务 ID 最大值
    public static final int MAX_TRANSACTION_ID = 65535;
    
    // 3.5 字符静默间隔 (毫秒，9600 波特率)
    public static final int RTU_SILENT_INTERVAL_MS = 4; // 3.5 * 11 * 1000 / 9600 ≈ 4ms
}
