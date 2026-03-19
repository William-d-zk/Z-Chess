/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.modbus.master;

import com.isahl.chess.bishop.protocol.modbus.function.ModbusFunction;
import com.isahl.chess.bishop.protocol.modbus.retry.RetryPolicy;
import com.isahl.chess.bishop.protocol.modbus.tcp.ModbusTcpCodec;
import com.isahl.chess.king.base.content.ByteBuf;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Modbus 主站 (客户端) */
public class ModbusMaster {

  private final String host;
  private final int port;
  private final int timeoutMs;
  private final RetryPolicy retryPolicy;
  private AsynchronousSocketChannel channel;
  private final AtomicInteger transactionIdGenerator = new AtomicInteger(0);

  private ModbusMaster(Builder builder) {
    this.host = builder.host;
    this.port = builder.port;
    this.timeoutMs = builder.timeoutMs;
    this.retryPolicy = builder.retryPolicy;
  }

  public static Builder builder() {
    return new Builder();
  }

  public void connect()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    channel = AsynchronousSocketChannel.open();
    channel.connect(new InetSocketAddress(host, port)).get(timeoutMs, TimeUnit.MILLISECONDS);
  }

  public void close() throws IOException {
    if (channel != null && channel.isOpen()) {
      channel.close();
    }
  }

  public boolean isConnected() {
    return channel != null && channel.isOpen();
  }

  public ModbusResponse sendRequest(ModbusRequest request)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    int transactionId = nextTransactionId();

    // 编码
    ByteBuf buffer = ModbusTcpCodec.encode(request, transactionId);

    // 发送
    channel.write(java.nio.ByteBuffer.wrap(buffer.array())).get(timeoutMs, TimeUnit.MILLISECONDS);

    // 接收响应
    byte[] responseBuffer = new byte[256];
    int bytesRead =
        channel
            .read(java.nio.ByteBuffer.wrap(responseBuffer))
            .get(timeoutMs, TimeUnit.MILLISECONDS);

    // 解码
    byte[] data = new byte[bytesRead];
    System.arraycopy(responseBuffer, 0, data, 0, bytesRead);
    ByteBuf responseByteBuf = ByteBuf.wrap(data);
    ModbusTcpCodec.ModbusTcpMessage response = ModbusTcpCodec.decode(responseByteBuf);

    return new ModbusResponse(response.getUnitId(), response.getFunction(), response.getData());
  }

  public ModbusRequest createReadHoldingRegisters(int unitId, int startAddress, int quantity) {
    byte[] data = new byte[4];
    data[0] = (byte) ((startAddress >> 8) & 0xFF);
    data[1] = (byte) (startAddress & 0xFF);
    data[2] = (byte) ((quantity >> 8) & 0xFF);
    data[3] = (byte) (quantity & 0xFF);

    return new ModbusRequest(unitId, ModbusFunction.READ_HOLDING_REGISTERS, data);
  }

  private int nextTransactionId() {
    return transactionIdGenerator.incrementAndGet() & 0xFFFF;
  }

  public static class Builder {
    private String host;
    private int port = 502;
    private int timeoutMs = 3000;
    private RetryPolicy retryPolicy;

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder timeout(int timeoutMs) {
      this.timeoutMs = timeoutMs;
      return this;
    }

    public Builder retryPolicy(RetryPolicy retryPolicy) {
      this.retryPolicy = retryPolicy;
      return this;
    }

    public ModbusMaster build() {
      return new ModbusMaster(this);
    }
  }
}
