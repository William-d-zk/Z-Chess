/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.model;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;

/**
 * Modbus 请求/响应基类
 * 
 * @author william.d.zk
 */
public class ModbusMessage
{
    protected int unitId;         // 从站地址 (1 字节)
    protected ModbusFunction function;  // 功能码 (1 字节)
    protected byte[] data;        // 数据
    
    public ModbusMessage()
    {
    }
    
    public ModbusMessage(int unitId, ModbusFunction function)
    {
        this.unitId = unitId;
        this.function = function;
    }
    
    public ModbusMessage(int unitId, ModbusFunction function, byte[] data)
    {
        this.unitId = unitId;
        this.function = function;
        this.data = data;
    }
    
    public int getUnitId()
    {
        return unitId;
    }
    
    public void setUnitId(int unitId)
    {
        this.unitId = unitId;
    }
    
    public ModbusFunction getFunction()
    {
        return function;
    }
    
    public void setFunction(ModbusFunction function)
    {
        this.function = function;
    }
    
    public byte[] getData()
    {
        return data;
    }
    
    public void setData(byte[] data)
    {
        this.data = data;
    }
    
    /**
     * 是否是异常响应
     */
    public boolean isException()
    {
        return ModbusFunction.isException(function.getCode());
    }
}
