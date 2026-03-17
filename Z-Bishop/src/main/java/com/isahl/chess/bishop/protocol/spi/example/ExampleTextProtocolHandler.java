/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.spi.example;

import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.king.base.content.ByteBuf;

/**
 * 示例协议处理器
 */
public class ExampleTextProtocolHandler implements ProtocolHandler
{
    private static final byte[] SIGNATURE = {'E', 'X', 'M', 'P'};
    
    @Override
    public String getProtocolName()
    {
        return "ExampleText";
    }
    
    @Override
    public String getProtocolVersion()
    {
        return "1.0.0";
    }
    
    @Override
    public byte[] getProtocolSignature()
    {
        return SIGNATURE;
    }
    
    @Override
    public Object decode(ByteBuf buffer)
    {
        if (buffer.readableBytes() < 2) {
            return null;
        }
        
        byte[] lengthBytes = new byte[2];
        buffer.get(lengthBytes);
        int length = ((lengthBytes[0] & 0xFF) << 8) | (lengthBytes[1] & 0xFF);
        
        if (buffer.readableBytes() < length) {
            return null;
        }
        
        byte[] data = new byte[length];
        buffer.get(data);
        
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }
    
    @Override
    public byte[] encode(Object message)
    {
        if (!(message instanceof String)) {
            throw new IllegalArgumentException("Message must be a String");
        }
        
        String text = (String) message;
        byte[] data = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        
        byte[] result = new byte[2 + data.length];
        result[0] = (byte) ((data.length >> 8) & 0xFF);
        result[1] = (byte) (data.length & 0xFF);
        System.arraycopy(data, 0, result, 2, data.length);
        
        return result;
    }
    
    @Override
    public int getPriority()
    {
        return 1000;
    }
}
