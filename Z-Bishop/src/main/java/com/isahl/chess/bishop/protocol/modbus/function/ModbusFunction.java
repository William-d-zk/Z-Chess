/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.function;

/**
 * Modbus 功能码
 * 
 * @author william.d.zk
 */
public enum ModbusFunction
{
    // 公共功能码
    READ_COILS(0x01, "Read Coils"),
    READ_DISCRETE_INPUTS(0x02, "Read Discrete Inputs"),
    READ_HOLDING_REGISTERS(0x03, "Read Holding Registers"),
    READ_INPUT_REGISTERS(0x04, "Read Input Registers"),
    WRITE_SINGLE_COIL(0x05, "Write Single Coil"),
    WRITE_SINGLE_REGISTER(0x06, "Write Single Register"),
    READ_EXCEPTION_STATUS(0x07, "Read Exception Status"),
    DIAGNOSTICS(0x08, "Diagnostics"),
    WRITE_MULTIPLE_COILS(0x0F, "Write Multiple Coils"),
    WRITE_MULTIPLE_REGISTERS(0x10, "Write Multiple Registers"),
    REPORT_SERVER_ID(0x11, "Report Server ID"),
    READ_FILE_RECORD(0x14, "Read File Record"),
    WRITE_FILE_RECORD(0x15, "Write File Record"),
    MASK_WRITE_REGISTER(0x16, "Mask Write Register"),
    READ_WRITE_MULTIPLE_REGISTERS(0x17, "Read/Write Multiple Registers"),
    FIFO_QUEUE_COMMAND(0x18, "FIFO Queue Command"),
    
    // 异常响应（功能码 + 0x80）
    EXCEPTION(0x80, "Exception Response");
    
    private final int code;
    private final String description;
    
    ModbusFunction(int code, String description)
    {
        this.code = code;
        this.description = description;
    }
    
    public int getCode()
    {
        return code;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    /**
     * 根据功能码获取枚举
     */
    public static ModbusFunction fromCode(int code)
    {
        for (ModbusFunction f : values()) {
            if (f.code == code) {
                return f;
            }
        }
        // 检查是否是异常响应
        if ((code & 0x80) != 0) {
            return EXCEPTION;
        }
        throw new IllegalArgumentException("Unknown Modbus function code: 0x" + Integer.toHexString(code));
    }
    
    /**
     * 判断是否是异常响应
     */
    public static boolean isException(int code)
    {
        return (code & 0x80) != 0;
    }
}
