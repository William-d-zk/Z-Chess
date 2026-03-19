/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.audience.bishop.protocol.modbus.serial;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.modbus.serial.SerialPortConfig;
import org.junit.jupiter.api.Test;

class SerialPortConfigTest {

  @Test
  void testBuilder_defaults() {
    SerialPortConfig config = SerialPortConfig.builder().build();

    assertEquals("/dev/ttyUSB0", config.getPortName());
    assertEquals(9600, config.getBaudRate());
    assertEquals(8, config.getDataBits());
    assertEquals(1, config.getStopBits());
    assertEquals(0, config.getParity());
    assertEquals(0, config.getFlowControl());
    assertEquals(1000, config.getReadTimeout());
  }

  @Test
  void testBuilder_custom() {
    SerialPortConfig config =
        SerialPortConfig.builder()
            .portName("/dev/ttyS0")
            .baudRate(19200)
            .dataBits(8)
            .stopBits(2)
            .parity(2) // Even
            .flowControl(1) // XON/XOFF
            .readTimeout(2000)
            .build();

    assertEquals("/dev/ttyS0", config.getPortName());
    assertEquals(19200, config.getBaudRate());
    assertEquals(2, config.getStopBits());
    assertEquals(2, config.getParity());
  }

  @Test
  void testSilentIntervalMs_9600bps() {
    SerialPortConfig config = SerialPortConfig.builder().baudRate(9600).build();

    // 3.5 * 11 * 1000 / 9600 ≈ 4.01ms → 5ms (ceiling)
    assertEquals(5, config.getSilentIntervalMs());
  }

  @Test
  void testSilentIntervalMs_19200bps() {
    SerialPortConfig config = SerialPortConfig.builder().baudRate(19200).build();

    // 3.5 * 11 * 1000 / 19200 ≈ 2.005ms → 3ms (ceiling)
    assertEquals(3, config.getSilentIntervalMs());
  }

  @Test
  void testSilentIntervalMs_115200bps() {
    SerialPortConfig config = SerialPortConfig.builder().baudRate(115200).build();

    // 3.5 * 11 * 1000 / 115200 ≈ 0.33ms → 1ms (ceiling)
    assertEquals(1, config.getSilentIntervalMs());
  }
}
