/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.slave;

/** Modbus 从站配置 */
public class ModbusSlaveConfig {

  private final int port;
  private final int unitId;
  private final int timeoutMs;

  private ModbusSlaveConfig(Builder builder) {
    this.port = builder.port;
    this.unitId = builder.unitId;
    this.timeoutMs = builder.timeoutMs;
  }

  public int getPort() {
    return port;
  }

  public int getUnitId() {
    return unitId;
  }

  public int getTimeoutMs() {
    return timeoutMs;
  }

  public static Builder builder() {
    return new Builder();
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

    public ModbusSlaveConfig build() {
      return new ModbusSlaveConfig(this);
    }
  }
}
