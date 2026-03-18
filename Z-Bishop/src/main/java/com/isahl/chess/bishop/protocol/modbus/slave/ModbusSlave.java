/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.slave;

/**
 * Modbus 从站 (服务端)
 * Phase 1: 简化实现，仅支持配置
 */
public class ModbusSlave {
    
    private final ModbusSlaveConfig config;
    private boolean running;
    
    private ModbusSlave(ModbusSlaveConfig config) {
        this.config = config;
    }
    
    public static ModbusSlave create(ModbusSlaveConfig config) {
        return new ModbusSlave(config);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public void start() {
        this.running = true;
        // Phase 2: 实现完整的服务端监听和请求处理
    }
    
    public void stop() {
        this.running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public ModbusSlaveConfig getConfig() {
        return config;
    }
    
    public static class Builder {
        private int port = 502;
        private int unitId = 1;
        private int timeoutMs = 3000;
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder unitId(int unitId) {
            this.unitId = unitId;
            return this;
        }
        
        public Builder timeout(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }
        
        public ModbusSlave build() {
            ModbusSlaveConfig config = ModbusSlaveConfig.builder()
                .port(port)
                .unitId(unitId)
                .timeout(timeoutMs)
                .build();
            return new ModbusSlave(config);
        }
    }
}
