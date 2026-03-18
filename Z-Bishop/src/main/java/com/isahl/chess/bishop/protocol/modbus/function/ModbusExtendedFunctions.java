/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.function;

/**
 * Modbus 扩展功能码处理器
 * 支持 Phase 2 功能码：0x07, 0x08, 0x11, 0x16, 0x17
 */
public class ModbusExtendedFunctions {
    
    private ModbusExtendedFunctions() {}
    
    /**
     * 0x07 Read Exception Status
     * 读取从站的异常状态
     * 
     * @param unitId 从站地址
     * @return 异常状态字节
     */
    public static byte readExceptionStatus(int unitId) {
        // 返回 0x00 表示无异常
        // Bit 0-7: 设备特定状态
        return 0x00;
    }
    
    /**
     * 0x08 Diagnostics 子功能
     */
    public enum DiagnosticsSubfunction {
        QUERY_DATA(0x0000),
        RESTART_COMMUNICATIONS(0x0001),
        RETURN_DIAGNOSTIC_REGISTER(0x0002),
        CHANGE_ASCII_INPUT_DELIMITER(0x0003),
        LISTEN_ONLY_MODE(0x0004),
        CLEAR_COUNTERS(0x000A),
        RETURN_BUS_MESSAGE_COUNT(0x000B),
        RETURN_BUS_COMM_ERROR_COUNT(0x000C),
        RETURN_BUS_EXCEPTION_ERROR_COUNT(0x000D),
        RETURN_SLAVE_NO_RESPONSE_COUNT(0x000E),
        RETURN_SLAVE_NAK_COUNT(0x000F),
        RETURN_SLAVE_BUSY_COUNT(0x0010),
        RETURN_BUS_PROGRAM_CHECKSUM_ERROR_COUNT(0x0011),
        RETURN_IOP_OVERERRUN_COUNT(0x0012),
        CLEAR_OVERRUN_AND_COUNTER(0x0013),
        GET_COMM_EVENT_COUNTER(0x0014),
        GET_COMM_EVENT_LOG(0x0015);
        
        private final int code;
        
        DiagnosticsSubfunction(int code) {
            this.code = code;
        }
        
        public int getCode() {
            return code;
        }
        
        public static DiagnosticsSubfunction fromCode(int code) {
            for (DiagnosticsSubfunction f : values()) {
                if (f.code == code) {
                    return f;
                }
            }
            throw new IllegalArgumentException("Unknown diagnostics subfunction: 0x" + 
                Integer.toHexString(code));
        }
    }
    
    /**
     * 0x08 Diagnostics 处理
     * 
     * @param subfunction 子功能码
     * @param data 数据
     * @return 响应数据
     */
    public static byte[] diagnostics(DiagnosticsSubfunction subfunction, byte[] data) {
        switch (subfunction) {
            case QUERY_DATA:
                // 回显接收到的数据
                return data;
                
            case RESTART_COMMUNICATIONS:
                // 重启通信，初始化设备
                return new byte[]{0x00, 0x00};
                
            case RETURN_DIAGNOSTIC_REGISTER:
                // 返回诊断寄存器值
                return new byte[]{0x00, 0x00};
                
            case LISTEN_ONLY_MODE:
                // 设置为只听模式
                return new byte[]{0x00, 0x00};
                
            case CLEAR_COUNTERS:
                // 清除计数器
                return new byte[]{0x00, 0x00};
                
            case RETURN_BUS_MESSAGE_COUNT:
                // 返回总线消息计数
                return new byte[]{0x00, 0x00};
                
            case RETURN_BUS_COMM_ERROR_COUNT:
                // 返回通信错误计数
                return new byte[]{0x00, 0x00};
                
            case RETURN_BUS_EXCEPTION_ERROR_COUNT:
                // 返回异常错误计数
                return new byte[]{0x00, 0x00};
                
            case RETURN_SLAVE_NO_RESPONSE_COUNT:
                // 返回无响应计数
                return new byte[]{0x00, 0x00};
                
            case RETURN_SLAVE_NAK_COUNT:
                // 返回 NAK 计数
                return new byte[]{0x00, 0x00};
                
            case RETURN_SLAVE_BUSY_COUNT:
                // 返回忙计数
                return new byte[]{0x00, 0x00};
                
            default:
                // 不支持的子功能，返回非法数据值
                return null;
        }
    }
    
    /**
     * 0x11 Report Server ID
     * 报告服务器 ID
     * 
     * @param unitId 从站地址
     * @return 服务器 ID 字节数组
     */
    public static byte[] reportServerId(int unitId) {
        // 返回服务器 ID 和状态
        // Byte 0: Server ID (可配置)
        // Byte 1: Run/Stop status (0xFF = Run, 0x00 = Stop)
        // Byte 2+: Additional data (可选)
        return new byte[]{
            (byte) unitId,  // Server ID
            (byte) 0xFF,    // Running status
            'Z', 'C'        // Additional: "ZC" for Z-Chess
        };
    }
    
    /**
     * 0x16 Mask Write Register
     * 掩码写寄存器
     * 
     * AND 掩码和 OR 掩码的计算公式：
     * 新值 = (当前值 AND AND 掩码) OR (OR 掩码 AND (NOT AND 掩码))
     * 
     * @param currentValue 当前寄存器值
     * @param andMask AND 掩码
     * @param orMask OR 掩码
     * @return 新寄存器值
     */
    public static int maskWriteRegister(int currentValue, int andMask, int orMask) {
        return (currentValue & andMask) | (orMask & ~andMask);
    }
    
    /**
     * 0x17 Read/Write Multiple Registers
     * 读/写多个寄存器
     * 
     * @param registers 寄存器数组
     * @param readStartAddress 读起始地址
     * @param readQuantity 读数量
     * @param writeStartAddress 写起始地址
     * @param writeValues 写值
     * @return 读取的寄存器值
     */
    public static int[] readWriteMultipleRegisters(
            int[] registers,
            int readStartAddress, int readQuantity,
            int writeStartAddress, int[] writeValues) {
        
        // 先写
        if (writeValues != null && writeValues.length > 0) {
            for (int i = 0; i < writeValues.length; i++) {
                if (writeStartAddress + i < registers.length) {
                    registers[writeStartAddress + i] = writeValues[i] & 0xFFFF;
                }
            }
        }
        
        // 后读
        int[] result = new int[readQuantity];
        for (int i = 0; i < readQuantity; i++) {
            if (readStartAddress + i < registers.length) {
                result[i] = registers[readStartAddress + i];
            } else {
                result[i] = 0;
            }
        }
        
        return result;
    }
}
