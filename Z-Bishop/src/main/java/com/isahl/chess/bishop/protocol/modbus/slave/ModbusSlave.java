/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.slave;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusRequest;
import com.isahl.chess.bishop.protocol.modbus.master.ModbusResponse;
import com.isahl.chess.bishop.protocol.modbus.model.ModbusExceptionCode;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec;
import com.isahl.chess.king.base.content.ByteBuf;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Modbus 从站 (服务端) Phase 2: 完整实现服务端监听和请求处理 */
public class ModbusSlave {

  private static final Logger LOG = LoggerFactory.getLogger(ModbusSlave.class);

  private final ModbusSlaveConfig config;
  private final DataModel dataModel;
  private final Map<ModbusFunction, FunctionHandler> handlers;
  private AsynchronousServerSocketChannel serverChannel;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final CountDownLatch shutdownLatch = new CountDownLatch(1);

  private ModbusSlave(ModbusSlaveConfig config, DataModel dataModel) {
    this.config = config;
    this.dataModel = dataModel;
    this.handlers = new ConcurrentHashMap<>();
    registerDefaultHandlers();
  }

  public static ModbusSlave create(ModbusSlaveConfig config) {
    DataModel dataModel =
        new DataModel(
            config.getUnitId(),
            1000, // coils
            1000, // discrete inputs
            1000, // holding registers
            1000 // input registers
            );
    return new ModbusSlave(config, dataModel);
  }

  public static Builder builder() {
    return new Builder();
  }

  private void registerDefaultHandlers() {
    // Phase 1 功能码
    registerHandler(ModbusFunction.READ_COILS, this::handleReadCoils);
    registerHandler(ModbusFunction.READ_DISCRETE_INPUTS, this::handleReadDiscreteInputs);
    registerHandler(ModbusFunction.READ_HOLDING_REGISTERS, this::handleReadHoldingRegisters);
    registerHandler(ModbusFunction.READ_INPUT_REGISTERS, this::handleReadInputRegisters);
    registerHandler(ModbusFunction.WRITE_SINGLE_COIL, this::handleWriteSingleCoil);
    registerHandler(ModbusFunction.WRITE_SINGLE_REGISTER, this::handleWriteSingleRegister);
    registerHandler(ModbusFunction.WRITE_MULTIPLE_COILS, this::handleWriteMultipleCoils);
    registerHandler(ModbusFunction.WRITE_MULTIPLE_REGISTERS, this::handleWriteMultipleRegisters);

    // Phase 2 扩展功能码
    registerHandler(ModbusFunction.READ_EXCEPTION_STATUS, this::handleReadExceptionStatus);
    registerHandler(ModbusFunction.DIAGNOSTICS, this::handleDiagnostics);
    registerHandler(ModbusFunction.REPORT_SERVER_ID, this::handleReportServerId);
  }

  public void registerHandler(ModbusFunction function, FunctionHandler handler) {
    handlers.put(function, handler);
    LOG.info(
        "Registered handler for function: {} (0x{:02X})",
        function.getDescription(),
        function.getCode());
  }

  public void start() throws IOException {
    if (running.compareAndSet(false, true)) {
      serverChannel = AsynchronousServerSocketChannel.open();
      serverChannel.bind(new InetSocketAddress(config.getPort()));
      LOG.info("Modbus Slave started on port {}", config.getPort());
      acceptNextClient();
    }
  }

  private void acceptNextClient() {
    if (!running.get()) {
      return;
    }

    serverChannel.accept(
        null,
        new CompletionHandler<AsynchronousSocketChannel, Void>() {
          @Override
          public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
            acceptNextClient(); // Accept next client
            handleClient(clientChannel);
          }

          @Override
          public void failed(Throwable exc, Void attachment) {
            if (running.get()) {
              LOG.error("Failed to accept client", exc);
              acceptNextClient();
            }
          }
        });
  }

  private void handleClient(AsynchronousSocketChannel clientChannel) {
    try {
      LOG.info("Client connected: {}", clientChannel.getRemoteAddress());

      ByteBuf buffer = ByteBuf.allocate(256);
      int bytesRead =
          clientChannel
              .read(java.nio.ByteBuffer.wrap(buffer.array(), 0, 256))
              .get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);

      if (bytesRead > 0) {
        ModbusResponse response = processRequest(buffer);

        if (response != null) {
          byte[] responseData = ModbusTcpCodec.encode(response, 1).array();
          clientChannel
              .write(java.nio.ByteBuffer.wrap(responseData))
              .get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
        }
      }
    } catch (Exception e) {
      LOG.error("Error handling client", e);
    } finally {
      try {
        clientChannel.close();
        LOG.info("Client disconnected");
      } catch (IOException e) {
        LOG.warn("Error closing client channel", e);
      }
    }
  }

  private ModbusResponse processRequest(ByteBuf buffer) {
    try {
      ModbusTcpCodec.ModbusTcpMessage request = ModbusTcpCodec.decode(buffer);
      if (request == null) {
        return null;
      }

      ModbusFunction function = request.getFunction();
      FunctionHandler handler = handlers.get(function);

      if (handler == null) {
        LOG.warn("No handler for function: {}", function);
        return ModbusResponse.exception(
            request.getUnitId(), function, ModbusExceptionCode.ILLEGAL_FUNCTION.getCode());
      }

      ModbusRequest modbusRequest =
          new ModbusRequest(
              request.getTransactionId(), request.getUnitId(), function, request.getData());

      return handler.handle(modbusRequest);

    } catch (Exception e) {
      LOG.error("Error processing request", e);
      return ModbusResponse.exception(
          0,
          ModbusFunction.READ_HOLDING_REGISTERS,
          ModbusExceptionCode.SERVER_DEVICE_FAILURE.getCode());
    }
  }

  // ========== 功能码处理器实现 ==========

  private ModbusResponse handleReadCoils(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      boolean[] coils = dataModel.getCoils(startAddress, quantity);
      byte[] data = packCoils(coils);
      return new ModbusResponse(request.getUnitId(), ModbusFunction.READ_COILS, data);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.READ_COILS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleReadDiscreteInputs(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      boolean[] inputs = new boolean[quantity];
      for (int i = 0; i < quantity; i++) {
        inputs[i] = dataModel.getDiscreteInput(startAddress + i);
      }
      byte[] data = packCoils(inputs);
      return new ModbusResponse(request.getUnitId(), ModbusFunction.READ_DISCRETE_INPUTS, data);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.READ_DISCRETE_INPUTS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleReadHoldingRegisters(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      int[] registers = dataModel.getHoldingRegisters(startAddress, quantity);
      byte[] data = new byte[1 + quantity * 2];
      data[0] = (byte) (quantity * 2);
      for (int i = 0; i < quantity; i++) {
        data[1 + i * 2] = (byte) ((registers[i] >> 8) & 0xFF);
        data[1 + i * 2 + 1] = (byte) (registers[i] & 0xFF);
      }
      return new ModbusResponse(request.getUnitId(), ModbusFunction.READ_HOLDING_REGISTERS, data);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.READ_HOLDING_REGISTERS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleReadInputRegisters(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      byte[] data = new byte[1 + quantity * 2];
      data[0] = (byte) (quantity * 2);
      for (int i = 0; i < quantity; i++) {
        int value = dataModel.getInputRegister(startAddress + i);
        data[1 + i * 2] = (byte) ((value >> 8) & 0xFF);
        data[1 + i * 2 + 1] = (byte) (value & 0xFF);
      }
      return new ModbusResponse(request.getUnitId(), ModbusFunction.READ_INPUT_REGISTERS, data);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.READ_INPUT_REGISTERS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleWriteSingleCoil(ModbusRequest request) {
    int address = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int value = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      dataModel.setCoil(address, value == 0xFF00);
      return new ModbusResponse(
          request.getUnitId(), ModbusFunction.WRITE_SINGLE_COIL, request.getData());
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.WRITE_SINGLE_COIL,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleWriteSingleRegister(ModbusRequest request) {
    int address = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int value = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      dataModel.setHoldingRegister(address, value);
      return new ModbusResponse(
          request.getUnitId(), ModbusFunction.WRITE_SINGLE_REGISTER, request.getData());
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.WRITE_SINGLE_REGISTER,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleWriteMultipleCoils(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      byte[] coilData = new byte[request.getData().length - 4];
      System.arraycopy(request.getData(), 5, coilData, 0, coilData.length);
      boolean[] coils = unpackCoils(coilData, quantity);

      for (int i = 0; i < quantity; i++) {
        dataModel.setCoil(startAddress + i, coils[i]);
      }

      byte[] response = new byte[4];
      response[0] = request.getData()[0];
      response[1] = request.getData()[1];
      response[2] = request.getData()[2];
      response[3] = request.getData()[3];
      return new ModbusResponse(request.getUnitId(), ModbusFunction.WRITE_MULTIPLE_COILS, response);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.WRITE_MULTIPLE_COILS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  private ModbusResponse handleWriteMultipleRegisters(ModbusRequest request) {
    int startAddress = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);
    int quantity = ((request.getData()[2] & 0xFF) << 8) | (request.getData()[3] & 0xFF);

    try {
      for (int i = 0; i < quantity; i++) {
        int value =
            ((request.getData()[5 + i * 2] & 0xFF) << 8)
                | (request.getData()[5 + i * 2 + 1] & 0xFF);
        dataModel.setHoldingRegister(startAddress + i, value);
      }

      byte[] response = new byte[4];
      response[0] = request.getData()[0];
      response[1] = request.getData()[1];
      response[2] = request.getData()[2];
      response[3] = request.getData()[3];
      return new ModbusResponse(
          request.getUnitId(), ModbusFunction.WRITE_MULTIPLE_REGISTERS, response);
    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.WRITE_MULTIPLE_REGISTERS,
          ModbusExceptionCode.ILLEGAL_DATA_ADDRESS.getCode());
    }
  }

  // ========== 扩展功能码处理器 ==========

  private ModbusResponse handleReadExceptionStatus(ModbusRequest request) {
    byte status =
        com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions.readExceptionStatus(
            request.getUnitId());
    byte[] data = new byte[] {status};
    return new ModbusResponse(request.getUnitId(), ModbusFunction.READ_EXCEPTION_STATUS, data);
  }

  private ModbusResponse handleDiagnostics(ModbusRequest request) {
    int subfunction = ((request.getData()[0] & 0xFF) << 8) | (request.getData()[1] & 0xFF);

    try {
      com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions.DiagnosticsSubfunction
          sf =
              com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions
                  .DiagnosticsSubfunction.fromCode(subfunction);

      byte[] data =
          com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions.diagnostics(
              sf, request.getData());

      if (data == null) {
        return ModbusResponse.exception(
            request.getUnitId(),
            ModbusFunction.DIAGNOSTICS,
            ModbusExceptionCode.ILLEGAL_DATA_VALUE.getCode());
      }

      byte[] response = new byte[2 + data.length];
      response[0] = request.getData()[0];
      response[1] = request.getData()[1];
      System.arraycopy(data, 0, response, 2, data.length);

      return new ModbusResponse(request.getUnitId(), ModbusFunction.DIAGNOSTICS, response);

    } catch (IllegalArgumentException e) {
      return ModbusResponse.exception(
          request.getUnitId(),
          ModbusFunction.DIAGNOSTICS,
          ModbusExceptionCode.ILLEGAL_DATA_VALUE.getCode());
    }
  }

  private ModbusResponse handleReportServerId(ModbusRequest request) {
    byte[] serverId =
        com.isahl.chess.bishop.protocol.modbus.function.ModbusExtendedFunctions.reportServerId(
            request.getUnitId());

    byte[] data = new byte[1 + serverId.length];
    data[0] = (byte) serverId.length;
    System.arraycopy(serverId, 0, data, 1, serverId.length);

    return new ModbusResponse(request.getUnitId(), ModbusFunction.REPORT_SERVER_ID, data);
  }

  // ========== 辅助方法 ==========

  private byte[] packCoils(boolean[] coils) {
    int byteCount = (coils.length + 7) / 8;
    byte[] data = new byte[1 + byteCount];
    data[0] = (byte) byteCount;

    for (int i = 0; i < coils.length; i++) {
      if (coils[i]) {
        data[i / 8 + 1] |= (byte) (1 << (i % 8));
      }
    }
    return data;
  }

  private boolean[] unpackCoils(byte[] data, int quantity) {
    boolean[] coils = new boolean[quantity];
    for (int i = 0; i < quantity; i++) {
      coils[i] = (data[i / 8] & (1 << (i % 8))) != 0;
    }
    return coils;
  }

  public void stop() {
    if (running.compareAndSet(true, false)) {
      try {
        if (serverChannel != null) {
          serverChannel.close();
        }
        LOG.info("Modbus Slave stopped");
      } catch (IOException e) {
        LOG.error("Error stopping Modbus Slave", e);
      }
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public ModbusSlaveConfig getConfig() {
    return config;
  }

  public DataModel getDataModel() {
    return dataModel;
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
      ModbusSlaveConfig config =
          ModbusSlaveConfig.builder().port(port).unitId(unitId).timeout(timeoutMs).build();
      return ModbusSlave.create(config);
    }
  }
}
