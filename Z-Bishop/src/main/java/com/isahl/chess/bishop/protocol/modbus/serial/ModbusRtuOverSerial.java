/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.serial;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.rtu.ModbusRtuCodec;
import com.isahl.chess.king.base.content.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Modbus RTU over Serial 主站
 * 
 * 注意：需要 jSerialComm 库支持
 * 添加依赖：com.fazecast:jSerialComm:[2.0,3.0.0)
 * 
 * 使用示例:
 * <pre>
 * SerialPortConfig config = SerialPortConfig.builder()
 *     .portName("/dev/ttyUSB0")
 *     .baudRate(9600)
 *     .build();
 * 
 * ModbusRtuOverSerial master = new ModbusRtuOverSerial(config);
 * master.open();
 * 
 * ModbusRequest request = master.createReadHoldingRegisters(1, 0, 10);
 * ModbusResponse response = master.sendRequest(request);
 * 
 * master.close();
 * </pre>
 */
public class ModbusRtuOverSerial {
    
    private static final Logger LOG = LoggerFactory.getLogger(ModbusRtuOverSerial.class);
    
    private final SerialPortConfig config;
    private SerialPortWrapper serialPort;
    private final int transactionTimeoutMs;
    
    public ModbusRtuOverSerial(SerialPortConfig config) {
        this(config, 3000);
    }
    
    public ModbusRtuOverSerial(SerialPortConfig config, int transactionTimeoutMs) {
        this.config = config;
        this.transactionTimeoutMs = transactionTimeoutMs;
    }
    
    /**
     * 打开串口
     */
    public void open() throws IOException {
        serialPort = new JSerialPortWrapper(config);
        serialPort.open();
        LOG.info("Modbus RTU over Serial opened: {} @ {}bps", 
            config.getPortName(), config.getBaudRate());
    }
    
    /**
     * 关闭串口
     */
    public void close() {
        if (serialPort != null) {
            serialPort.close();
            LOG.info("Modbus RTU over Serial closed");
        }
    }
    
    /**
     * 发送 Modbus RTU 请求并接收响应
     */
    public ModbusResponse sendRequest(ModbusRequest request) throws IOException {
        if (serialPort == null || !serialPort.isOpen()) {
            throw new IOException("Serial port not open");
        }
        
        // 编码 RTU 帧
        ByteBuf encoded = ModbusRtuCodec.encode(request);
        byte[] txData = new byte[encoded.readableBytes()];
        encoded.get(txData);
        
        // 等待 3.5 字符静默间隔
        waitForSilentInterval();
        
        // 发送数据
        serialPort.write(txData);
        LOG.debug("TX RTU: {} bytes", txData.length);
        
        // 接收响应
        byte[] rxBuffer = new byte[256];
        long startTime = System.currentTimeMillis();
        int totalBytesRead = 0;
        
        while (totalBytesRead < 8 && 
               (System.currentTimeMillis() - startTime) < transactionTimeoutMs) {
            int bytesRead = serialPort.read(rxBuffer, config.getReadTimeout());
            if (bytesRead > 0) {
                totalBytesRead += bytesRead;
                
                // 检查是否已接收完整帧
                if (totalBytesRead >= 8) {
                    int frameLength = estimateRtuFrameLength(rxBuffer, totalBytesRead);
                    if (totalBytesRead >= frameLength) {
                        break;
                    }
                }
            }
            
            // 等待 3.5 字符静默间隔
            try {
                TimeUnit.MILLISECONDS.sleep(serialPort.getSilentIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (totalBytesRead == 0) {
            throw new IOException("Modbus RTU timeout: no response received");
        }
        
        // 解码响应
        byte[] rxData = new byte[totalBytesRead];
        System.arraycopy(rxBuffer, 0, rxData, 0, totalBytesRead);
        ByteBuf rxBufferWrap = ByteBuf.wrap(rxData);
        try {
            com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage message = 
                ModbusRtuCodec.decode(rxBufferWrap);
            
            if (message == null) {
                throw new IOException("Failed to decode Modbus RTU response");
            }
            
            return new ModbusResponse(message.getUnitId(), message.getFunction(), message.getData());
            
        } catch (IllegalArgumentException e) {
            throw new IOException("Modbus RTU CRC error: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建读保持寄存器请求
     */
    public ModbusRequest createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
        byte[] data = new byte[4];
        data[0] = (byte) ((startAddress >> 8) & 0xFF);
        data[1] = (byte) (startAddress & 0xFF);
        data[2] = (byte) ((quantity >> 8) & 0xFF);
        data[3] = (byte) (quantity & 0xFF);
        
        return new ModbusRequest(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
    }
    
    /**
     * 等待 3.5 字符静默间隔
     */
    private void waitForSilentInterval() {
        try {
            TimeUnit.MILLISECONDS.sleep(serialPort.getSilentIntervalMs());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 估算 RTU 帧长度
     */
    private int estimateRtuFrameLength(byte[] buffer, int bytesRead) {
        if (bytesRead < 3) return 8; // 最小帧长度
        
        int functionCode = buffer[1] & 0xFF;
        
        switch (functionCode) {
            case 0x01: // Read Coils
            case 0x02: // Read Discrete Inputs
            case 0x03: // Read Holding Registers
            case 0x04: // Read Input Registers
                if (bytesRead >= 4) {
                    return 3 + buffer[2] + 2; // header + byte count + data + CRC
                }
                return 8;
            case 0x05: // Write Single Coil
            case 0x06: // Write Single Register
                return 8;
            case 0x0F: // Write Multiple Coils
            case 0x10: // Write Multiple Registers
                return 8;
            default:
                return 8;
        }
    }
    
    public boolean isOpen() {
        return serialPort != null && serialPort.isOpen();
    }
    
    public SerialPortConfig getConfig() {
        return config;
    }
}
