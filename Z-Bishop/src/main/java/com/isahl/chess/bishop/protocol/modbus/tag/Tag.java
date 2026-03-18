/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

/**
 * Modbus 标签
 */
public class Tag {
    
    private final String name;
    private final String description;
    private final int unitId;
    private final int address;
    private final DataType dataType;
    private final DataArea dataArea;
    private final double scale;
    private final double offset;
    private final String unit;
    private final int precision;
    private final ByteOrder byteOrder;
    
    private Tag(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.unitId = builder.unitId;
        this.address = builder.address;
        this.dataType = builder.dataType;
        this.dataArea = builder.dataArea;
        this.scale = builder.scale;
        this.offset = builder.offset;
        this.unit = builder.unit;
        this.precision = builder.precision;
        this.byteOrder = builder.byteOrder;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getUnitId() { return unitId; }
    public int getAddress() { return address; }
    public DataType getDataType() { return dataType; }
    public DataArea getDataArea() { return dataArea; }
    public double getScale() { return scale; }
    public double getOffset() { return offset; }
    public String getUnit() { return unit; }
    public int getPrecision() { return precision; }
    public ByteOrder getByteOrder() { return byteOrder; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String name;
        private String description;
        private int unitId = 1;
        private int address;
        private DataType dataType = DataType.INT16;
        private DataArea dataArea = DataArea.HOLDING_REGISTER;
        private double scale = 1.0;
        private double offset = 0.0;
        private String unit = "";
        private int precision = 2;
        private ByteOrder byteOrder = ByteOrder.ABCD;
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder unitId(int unitId) { this.unitId = unitId; return this; }
        public Builder address(int address) { this.address = address; return this; }
        public Builder dataType(DataType dataType) { this.dataType = dataType; return this; }
        public Builder dataArea(DataArea dataArea) { this.dataArea = dataArea; return this; }
        public Builder scale(double scale) { this.scale = scale; return this; }
        public Builder offset(double offset) { this.offset = offset; return this; }
        public Builder unit(String unit) { this.unit = unit; return this; }
        public Builder precision(int precision) { this.precision = precision; return this; }
        public Builder byteOrder(ByteOrder byteOrder) { this.byteOrder = byteOrder; return this; }
        
        public Tag build() {
            return new Tag(this);
        }
    }
}
