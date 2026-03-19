/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusMessage;

/** Modbus 请求 */
public class ModbusRequest extends ModbusMessage {

  private final int transactionId;

  public ModbusRequest(int unitId, ModbusFunction function, byte[] data) {
    super(unitId, function, data);
    this.transactionId = -1;
  }

  public ModbusRequest(int transactionId, int unitId, ModbusFunction function, byte[] data) {
    super(unitId, function, data);
    this.transactionId = transactionId;
  }

  public int getTransactionId() {
    return transactionId;
  }
}
