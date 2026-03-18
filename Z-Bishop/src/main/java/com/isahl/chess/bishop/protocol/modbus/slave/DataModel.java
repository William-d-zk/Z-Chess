/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.slave;

import java.util.HashMap;
import java.util.Map;

/**
 * Modbus 从站数据模型
 * 管理线圈、离散输入、保持寄存器、输入寄存器
 */
public class DataModel {
    
    private final int unitId;
    private final boolean[] coils;
    private final boolean[] discreteInputs;
    private final int[] holdingRegisters;
    private final int[] inputRegisters;
    
    public DataModel(int unitId, int coilCount, int discreteInputCount, 
                     int holdingRegisterCount, int inputRegisterCount) {
        this.unitId = unitId;
        this.coils = new boolean[coilCount];
        this.discreteInputs = new boolean[discreteInputCount];
        this.holdingRegisters = new int[holdingRegisterCount];
        this.inputRegisters = new int[inputRegisterCount];
    }
    
    public int getUnitId() {
        return unitId;
    }
    
    public boolean getCoil(int address) {
        if (address < 0 || address >= coils.length) {
            throw new IllegalArgumentException("Invalid coil address: " + address);
        }
        return coils[address];
    }
    
    public void setCoil(int address, boolean value) {
        if (address < 0 || address >= coils.length) {
            throw new IllegalArgumentException("Invalid coil address: " + address);
        }
        coils[address] = value;
    }
    
    public boolean getDiscreteInput(int address) {
        if (address < 0 || address >= discreteInputs.length) {
            throw new IllegalArgumentException("Invalid discrete input address: " + address);
        }
        return discreteInputs[address];
    }
    
    public int getHoldingRegister(int address) {
        if (address < 0 || address >= holdingRegisters.length) {
            throw new IllegalArgumentException("Invalid holding register address: " + address);
        }
        return holdingRegisters[address];
    }
    
    public void setHoldingRegister(int address, int value) {
        if (address < 0 || address >= holdingRegisters.length) {
            throw new IllegalArgumentException("Invalid holding register address: " + address);
        }
        holdingRegisters[address] = value & 0xFFFF;
    }
    
    public int getInputRegister(int address) {
        if (address < 0 || address >= inputRegisters.length) {
            throw new IllegalArgumentException("Invalid input register address: " + address);
        }
        return inputRegisters[address];
    }
    
    public void setInputRegister(int address, int value) {
        if (address < 0 || address >= inputRegisters.length) {
            throw new IllegalArgumentException("Invalid input register address: " + address);
        }
        inputRegisters[address] = value & 0xFFFF;
    }
    
    public boolean[] getCoils(int startAddress, int quantity) {
        if (startAddress < 0 || startAddress + quantity > coils.length) {
            throw new IllegalArgumentException("Invalid coil range");
        }
        boolean[] result = new boolean[quantity];
        System.arraycopy(coils, startAddress, result, 0, quantity);
        return result;
    }
    
    public int[] getHoldingRegisters(int startAddress, int quantity) {
        if (startAddress < 0 || startAddress + quantity > holdingRegisters.length) {
            throw new IllegalArgumentException("Invalid holding register range");
        }
        int[] result = new int[quantity];
        for (int i = 0; i < quantity; i++) {
            result[i] = holdingRegisters[startAddress + i];
        }
        return result;
    }
}
