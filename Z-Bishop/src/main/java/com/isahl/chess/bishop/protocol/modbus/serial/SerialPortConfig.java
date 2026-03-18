/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.serial;

/**
 * Modbus 串口配置
 * 支持 RTU over Serial 通信
 */
public class SerialPortConfig {
    
    private final String portName;
    private final int baudRate;
    private final int dataBits;
    private final int stopBits;
    private final int parity;
    private final int flowControl;
    private final int readTimeout;
    
    private SerialPortConfig(Builder builder) {
        this.portName = builder.portName;
        this.baudRate = builder.baudRate;
        this.dataBits = builder.dataBits;
        this.stopBits = builder.stopBits;
        this.parity = builder.parity;
        this.flowControl = builder.flowControl;
        this.readTimeout = builder.readTimeout;
    }
    
    public String getPortName() {
        return portName;
    }
    
    public int getBaudRate() {
        return baudRate;
    }
    
    public int getDataBits() {
        return dataBits;
    }
    
    public int getStopBits() {
        return stopBits;
    }
    
    public int getParity() {
        return parity;
    }
    
    public int getFlowControl() {
        return flowControl;
    }
    
    public int getReadTimeout() {
        return readTimeout;
    }
    
    /**
     * 计算 3.5 字符静默间隔 (毫秒)
     * 公式：3.5 * 11 * 1000 / baudRate
     * 11 = 1 起始位 + 8 数据位 + 1 校验位 + 1 停止位
     */
    public int getSilentIntervalMs() {
        return (int) Math.ceil(3.5 * 11 * 1000.0 / baudRate);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String portName = "/dev/ttyUSB0";
        private int baudRate = 9600;
        private int dataBits = 8;
        private int stopBits = 1;
        private int parity = 0; // 0=None, 1=Odd, 2=Even, 3=Mark, 4=Space
        private int flowControl = 0; // 0=None, 1=XON/XOFF, 2=RTS/CTS
        private int readTimeout = 1000;
        
        public Builder portName(String portName) {
            this.portName = portName;
            return this;
        }
        
        public Builder baudRate(int baudRate) {
            this.baudRate = baudRate;
            return this;
        }
        
        public Builder dataBits(int dataBits) {
            this.dataBits = dataBits;
            return this;
        }
        
        public Builder stopBits(int stopBits) {
            this.stopBits = stopBits;
            return this;
        }
        
        public Builder parity(int parity) {
            this.parity = parity;
            return this;
        }
        
        public Builder flowControl(int flowControl) {
            this.flowControl = flowControl;
            return this;
        }
        
        public Builder readTimeout(int timeoutMs) {
            this.readTimeout = timeoutMs;
            return this;
        }
        
        public SerialPortConfig build() {
            return new SerialPortConfig(this);
        }
    }
}
