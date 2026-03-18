/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.rtu;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.CryptoUtil;

/**
 * Modbus RTU 协议编解码器
 */
public class ModbusRtuCodec {
    
    private static final int RTU_HEADER_LENGTH = 2; // Address + Function
    private static final int RTU_CRC_LENGTH = 2;
    
    /**
     * 编码 RTU 帧
     */
    public static ByteBuf encode(ModbusMessage message) {
        byte[] data = message.getData() != null ? message.getData() : new byte[0];
        int length = RTU_HEADER_LENGTH + data.length + RTU_CRC_LENGTH;
        
        byte[] buffer = new byte[length];
        buffer[0] = (byte) message.getUnitId();
        buffer[1] = (byte) message.getFunction().getCode();
        if (data.length > 0) {
            System.arraycopy(data, 0, buffer, 2, data.length);
        }
        
        // 计算 CRC (little endian)
        int crc = CryptoUtil.crc16_modbus(buffer, 0, length - 2);
        buffer[length - 2] = (byte) (crc & 0xFF);
        buffer[length - 1] = (byte) ((crc >> 8) & 0xFF);
        
        return ByteBuf.wrap(buffer);
    }
    
    /**
     * 解码 RTU 帧
     */
    public static ModbusMessage decode(ByteBuf buffer) {
        if (buffer.readableBytes() < RTU_HEADER_LENGTH + RTU_CRC_LENGTH) {
            return null;
        }
        
        int unitId = buffer.peek(0) & 0xFF;
        int functionCode = buffer.peek(1) & 0xFF;
        
        // 估算帧长度 (基于功能码)
        int estimatedLength = estimateFrameLength(functionCode, buffer);
        if (estimatedLength < 0 || buffer.readableBytes() < estimatedLength) {
            return null;
        }
        
        // 验证 CRC
        int crcLength = estimatedLength - RTU_CRC_LENGTH;
        int receivedCrc = (buffer.peek(estimatedLength - 2) & 0xFF) | 
                         ((buffer.peek(estimatedLength - 1) & 0xFF) << 8);
        int calculatedCrc = CryptoUtil.crc16_modbus(buffer.array(), 0, crcLength);
        
        if (receivedCrc != calculatedCrc) {
            throw new IllegalArgumentException("Modbus RTU CRC error");
        }
        
        // 解析数据
        byte[] data = new byte[crcLength - RTU_HEADER_LENGTH];
        for (int i = 0; i < data.length; i++) {
            data[i] = buffer.peek(2 + i);
        }
        
        ModbusFunction function = ModbusFunction.fromCode(functionCode);
        return new ModbusMessage(unitId, function, data);
    }
    
    /**
     * 估算帧长度
     */
    private static int estimateFrameLength(int functionCode, ByteBuf buffer) {
        switch (functionCode) {
            case 0x01: // Read Coils
            case 0x02: // Read Discrete Inputs
            case 0x03: // Read Holding Registers
            case 0x04: // Read Input Registers
                return 8; // 2 header + 4 data + 2 CRC
            case 0x05: // Write Single Coil
            case 0x06: // Write Single Register
                return 8; // 2 header + 4 data + 2 CRC
            case 0x0F: // Write Multiple Coils
            case 0x10: // Write Multiple Registers
                if (buffer.readableBytes() >= 7) {
                    int byteCount = buffer.peek(6) & 0xFF;
                    return 2 + 5 + byteCount + 2;
                }
                return -1;
            default:
                return -1;
        }
    }
}
