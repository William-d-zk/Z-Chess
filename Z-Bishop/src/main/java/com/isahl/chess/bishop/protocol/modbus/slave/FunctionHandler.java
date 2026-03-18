/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.slave;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;

/**
 * Modbus 功能码处理器接口
 */
@FunctionalInterface
public interface FunctionHandler {
    
    /**
     * 处理 Modbus 功能码请求
     * 
     * @param request 请求
     * @return 响应
     */
    ModbusResponse handle(ModbusRequest request);
    
    /**
     * 获取支持的功能码
     */
    default ModbusFunction getFunctionCode() {
        throw new UnsupportedOperationException("Must implement getFunctionCode");
    }
}
