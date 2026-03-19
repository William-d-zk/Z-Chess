/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.tag;

/** Modbus 数据区 */
public enum DataArea {
  COIL(0),
  DISCRETE_INPUT(1),
  INPUT_REGISTER(3),
  HOLDING_REGISTER(4);

  private final int addressOffset;

  DataArea(int addressOffset) {
    this.addressOffset = addressOffset;
  }

  public int getAddressOffset() {
    return addressOffset;
  }
}
