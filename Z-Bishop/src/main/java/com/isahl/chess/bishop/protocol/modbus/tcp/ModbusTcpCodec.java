/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tcp;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;
import com.isahl.chess.king.base.content.ByteBuf;

/**
 * Modbus TCP 协议编解码器
 */
public class ModbusTcpCodec
{
    
    private static final int MBAP_HEADER_LENGTH = 7;
    private static final int PROTOCOL_ID = 0;
    
    public static ByteBuf encode(ModbusMessage message, int transactionId)
    {
        byte[] data = message.getData() != null ? message.getData() : new byte[0];
        int length = 1 + data.length;
        
        byte[] buffer = new byte[MBAP_HEADER_LENGTH + length];
        
        buffer[0] = (byte) ((transactionId >> 8) & 0xFF);
        buffer[1] = (byte) (transactionId & 0xFF);
        buffer[2] = 0;
        buffer[3] = 0;
        buffer[4] = (byte) ((length >> 8) & 0xFF);
        buffer[5] = (byte) (length & 0xFF);
        buffer[6] = (byte) message.getUnitId();
        buffer[7] = (byte) message.getFunction().getCode();
        if (data.length > 0) {
            System.arraycopy(data, 0, buffer, 8, data.length);
        }
        
        return ByteBuf.wrap(buffer);
    }
    
    public static ByteBuf encodeException(int unitId, int functionCode, ModbusExceptionCode exceptionCode, int transactionId)
    {
        byte[] data = new byte[]{(byte) exceptionCode.getCode()};
        int length = 1 + data.length;
        
        byte[] buffer = new byte[MBAP_HEADER_LENGTH + length];
        
        buffer[0] = (byte) ((transactionId >> 8) & 0xFF);
        buffer[1] = (byte) (transactionId & 0xFF);
        buffer[2] = 0;
        buffer[3] = 0;
        buffer[4] = (byte) ((length >> 8) & 0xFF);
        buffer[5] = (byte) (length & 0xFF);
        buffer[6] = (byte) unitId;
        buffer[7] = (byte) (functionCode | 0x80); // Exception function code
        buffer[8] = (byte) exceptionCode.getCode();
        
        return ByteBuf.wrap(buffer);
    }
    
    public static ModbusTcpMessage decode(ByteBuf buffer)
    {
        if (buffer.readableBytes() < MBAP_HEADER_LENGTH) {
            return null;
        }
        
        int transactionId = ((buffer.peek(0) & 0xFF) << 8) | (buffer.peek(1) & 0xFF);
        int protocolId = ((buffer.peek(2) & 0xFF) << 8) | (buffer.peek(3) & 0xFF);
        int length = ((buffer.peek(4) & 0xFF) << 8) | (buffer.peek(5) & 0xFF);
        int unitId = buffer.peek(6) & 0xFF;
        
        if (protocolId != PROTOCOL_ID) {
            throw new IllegalArgumentException("Invalid Modbus TCP protocol ID: " + protocolId);
        }
        
        int pduLength = length - 1;
        int frameLength = MBAP_HEADER_LENGTH + length;
        
        if (buffer.readableBytes() < frameLength) {
            return null;
        }
        
        int functionCode = buffer.peek(7) & 0xFF;
        
        byte[] data = null;
        if (pduLength > 0) {
            data = new byte[pduLength];
            for (int i = 0; i < pduLength; i++) {
                data[i] = buffer.peek(8 + i);
            }
        }
        
        ModbusFunction function = ModbusFunction.fromCode(functionCode);
        return new ModbusTcpMessage(transactionId, unitId, function, data);
    }
    
    public static int getFrameLength(ByteBuf buffer)
    {
        if (buffer.readableBytes() < 6) {
            return -1;
        }
        
        return MBAP_HEADER_LENGTH + (((buffer.peek(4) & 0xFF) << 8) | (buffer.peek(5) & 0xFF));
    }
    
    public static class ModbusTcpMessage extends ModbusMessage
    {
        private int transactionId;
        
        public ModbusTcpMessage() { }
        
        public ModbusTcpMessage(int transactionId, int unitId, ModbusFunction function, byte[] data)
        {
            super(unitId, function, data);
            this.transactionId = transactionId;
        }
        
        public int getTransactionId() { return transactionId; }
        public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    }
}
