/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 */

package com.isahl.chess.bishop.protocol.spi.example;

import com.isahl.chess.bishop.protocol.spi.ProtocolContext;
import com.isahl.chess.bishop.protocol.spi.ProtocolHandler;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;

/**
 * 示例文本协议处理器
 *
 * <p>演示如何创建自定义协议处理器。
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class ExampleTextProtocolHandler implements ProtocolHandler {
  private static final byte[] SIGNATURE = {'E', 'X', 'M', 'P'};

  @Override
  public boolean supports(IProtocol message) {
    // 检查消息是否以 EXMP 签名开头
    byte[] payload = message.payload();
    if (payload == null || payload.length < 4) {
      return false;
    }
    for (int i = 0; i < 4; i++) {
      if (payload[i] != SIGNATURE[i]) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void handle(IProtocol message, ProtocolContext context) {
    // 处理消息
    Object decoded = decode(message.payload());
    if (decoded != null) {
      context.setAttribute("decodedMessage", decoded);
    }
  }

  @Override
  public String getName() {
    return "ExampleText";
  }

  @Override
  public String getDescription() {
    return "Example text protocol handler for demonstration";
  }

  @Override
  public int getPriority() {
    return 1000;
  }

  // ==================== 编码解码方法 ====================

  public byte[] getProtocolSignature() {
    return SIGNATURE;
  }

  /** 解码消息 */
  public Object decode(byte[] payload) {
    if (payload == null || payload.length < 6) {
      return null;
    }

    // 跳过 4 字节签名
    int length = ((payload[4] & 0xFF) << 8) | (payload[5] & 0xFF);

    if (payload.length < 6 + length) {
      return null;
    }

    byte[] data = new byte[length];
    System.arraycopy(payload, 6, data, 0, length);

    return new String(data, java.nio.charset.StandardCharsets.UTF_8);
  }

  /** 编码消息 */
  public byte[] encode(String message) {
    byte[] data = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);

    byte[] result = new byte[6 + data.length];
    System.arraycopy(SIGNATURE, 0, result, 0, 4);
    result[4] = (byte) ((data.length >> 8) & 0xFF);
    result[5] = (byte) (data.length & 0xFF);
    System.arraycopy(data, 0, result, 6, data.length);

    return result;
  }
}
