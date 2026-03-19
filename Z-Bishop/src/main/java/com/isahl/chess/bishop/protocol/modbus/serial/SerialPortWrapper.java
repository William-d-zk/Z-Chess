/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modbus RTU over Serial 通信接口
 *
 * <p>注意：此接口需要 jSerialComm 库支持 添加依赖：com.fazecast:jSerialComm:[2.0,3.0.0)
 */
public interface SerialPortWrapper {

  /** 打开串口 */
  boolean open() throws IOException;

  /** 关闭串口 */
  void close();

  /** 是否已打开 */
  boolean isOpen();

  /** 读取数据 */
  int read(byte[] buffer, int timeoutMs) throws IOException;

  /** 写入数据 */
  int write(byte[] buffer) throws IOException;

  /** 获取 3.5 字符静默间隔 (毫秒) */
  int getSilentIntervalMs();
}

/** 基于 jSerialComm 的串口实现 */
class JSerialPortWrapper implements SerialPortWrapper {

  private static final Logger LOG = LoggerFactory.getLogger(JSerialPortWrapper.class);

  private final SerialPortConfig config;
  private Object serialPort; // com.fazecast.jSerialComm.SerialPort
  private InputStream inputStream;
  private OutputStream outputStream;

  public JSerialPortWrapper(SerialPortConfig config) {
    this.config = config;
  }

  @Override
  public boolean open() throws IOException {
    try {
      // 使用反射加载 jSerialComm，避免硬依赖
      Class<?> serialPortClass = Class.forName("com.fazecast.jSerialComm.SerialPort");

      // 获取串口设备
      java.lang.reflect.Method getCommPort = serialPortClass.getMethod("getCommPort", String.class);
      serialPort = getCommPort.invoke(null, config.getPortName());

      if (serialPort == null) {
        throw new IOException("Serial port not found: " + config.getPortName());
      }

      // 配置串口参数
      java.lang.reflect.Method setBaudRate = serialPortClass.getMethod("setBaudRate", int.class);
      java.lang.reflect.Method setDataBits = serialPortClass.getMethod("setDataBits", int.class);
      java.lang.reflect.Method setStopBits = serialPortClass.getMethod("setStopBits", int.class);
      java.lang.reflect.Method setParity = serialPortClass.getMethod("setParity", int.class);
      java.lang.reflect.Method setFlowControl =
          serialPortClass.getMethod("setFlowControl", int.class);

      setBaudRate.invoke(serialPort, config.getBaudRate());
      setDataBits.invoke(serialPort, config.getDataBits());
      setStopBits.invoke(serialPort, config.getStopBits());
      setParity.invoke(serialPort, config.getParity());
      setFlowControl.invoke(serialPort, config.getFlowControl());

      // 打开串口
      java.lang.reflect.Method openPort = serialPortClass.getMethod("openPort");
      boolean opened = (Boolean) openPort.invoke(serialPort);

      if (!opened) {
        throw new IOException("Failed to open serial port: " + config.getPortName());
      }

      // 获取输入输出流
      java.lang.reflect.Method getInputStream = serialPortClass.getMethod("getInputStream");
      java.lang.reflect.Method getOutputStream = serialPortClass.getMethod("getOutputStream");

      inputStream = (InputStream) getInputStream.invoke(serialPort);
      outputStream = (OutputStream) getOutputStream.invoke(serialPort);

      LOG.info("Serial port opened: {} @ {}bps", config.getPortName(), config.getBaudRate());
      return true;

    } catch (ClassNotFoundException e) {
      throw new IOException(
          "jSerialComm library not found. Add dependency: com.fazecast:jSerialComm:[2.0,3.0.0)", e);
    } catch (Exception e) {
      throw new IOException("Failed to open serial port: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    if (serialPort != null) {
      try {
        java.lang.reflect.Method closePort = serialPort.getClass().getMethod("closePort");
        closePort.invoke(serialPort);
        LOG.info("Serial port closed: {}", config.getPortName());
      } catch (Exception e) {
        LOG.warn("Error closing serial port", e);
      }
      serialPort = null;
    }
  }

  @Override
  public boolean isOpen() {
    if (serialPort == null) return false;
    try {
      java.lang.reflect.Method isOpen = serialPort.getClass().getMethod("isOpen");
      return (Boolean) isOpen.invoke(serialPort);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public int read(byte[] buffer, int timeoutMs) throws IOException {
    if (inputStream == null) {
      throw new IOException("Serial port not open");
    }

    try {
      inputStream.mark(buffer.length);
      int bytesRead = inputStream.read(buffer, 0, buffer.length);
      return bytesRead > 0 ? bytesRead : 0;
    } catch (IOException e) {
      throw new IOException("Serial read error: " + e.getMessage(), e);
    }
  }

  @Override
  public int write(byte[] buffer) throws IOException {
    if (outputStream == null) {
      throw new IOException("Serial port not open");
    }

    try {
      outputStream.write(buffer);
      outputStream.flush();
      return buffer.length;
    } catch (IOException e) {
      throw new IOException("Serial write error: " + e.getMessage(), e);
    }
  }

  @Override
  public int getSilentIntervalMs() {
    return config.getSilentIntervalMs();
  }
}
