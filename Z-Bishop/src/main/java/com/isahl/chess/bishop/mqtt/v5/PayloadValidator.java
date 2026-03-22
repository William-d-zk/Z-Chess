/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.bishop.mqtt.v5;

import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import com.isahl.chess.king.base.log.Logger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * MQTT v5.0 Payload 格式验证器
 *
 * <p>验证消息负载的格式是否符合 MQTT 5.0 规范：
 *
 * <ul>
 *   <li>Payload Format Indicator = 1 (UTF-8) 时，验证 payload 是否为有效 UTF-8
 *   <li>Content Type 不为空时，验证 MIME 类型格式
 * </ul>
 *
 * @author william.d.zk
 * @since 1.2.1
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc3901128">MQTT v5.0
 *     Payload Format Indicator</a>
 */
public class PayloadValidator {

  private static final Logger _Logger =
      Logger.getLogger("mqtt.broker." + PayloadValidator.class.getSimpleName());

  /** MIME 类型格式正则表达式 */
  private static final Pattern MIME_TYPE_PATTERN =
      Pattern.compile("^[a-zA-Z0-9][-a-zA-Z0-9._+]*/[a-zA-Z0-9][-a-zA-Z0-9._+]*$");

  /** 是否启用验证 */
  private volatile boolean _enabled = true;

  /** 严格模式 - 验证失败时抛出异常 */
  private volatile boolean _strictMode = false;

  // ==================== 配置 ====================

  /** 启用/禁用验证 */
  public void setEnabled(boolean enabled) {
    _enabled = enabled;
    _Logger.info("Payload validation {}", enabled ? "enabled" : "disabled");
  }

  /** 检查是否启用验证 */
  public boolean isEnabled() {
    return _enabled;
  }

  /**
   * 设置严格模式
   *
   * <p>严格模式下，验证失败会抛出异常；非严格模式下，仅记录警告日志。
   */
  public void setStrictMode(boolean strictMode) {
    _strictMode = strictMode;
    _Logger.info("Strict mode {}", strictMode ? "enabled" : "disabled");
  }

  /** 检查是否为严格模式 */
  public boolean isStrictMode() {
    return _strictMode;
  }

  // ==================== 验证方法 ====================

  /**
   * 验证消息
   *
   * @param message 待验证的消息
   * @return 验证结果
   */
  public ValidationResult validate(X113_QttPublish message) {
    if (!_enabled) {
      return ValidationResult.success();
    }

    if (message == null) {
      return ValidationResult.failure("Message is null");
    }

    // 验证 Payload Format Indicator
    ValidationResult formatResult = validatePayloadFormat(message);
    if (!formatResult.isValid()) {
      return formatResult;
    }

    // 验证 Content Type
    ValidationResult contentTypeResult = validateContentType(message);
    if (!contentTypeResult.isValid()) {
      return contentTypeResult;
    }

    return ValidationResult.success();
  }

  /** 验证 Payload 格式 */
  private ValidationResult validatePayloadFormat(X113_QttPublish message) {
    Integer formatIndicator = message.getPayloadFormatIndicator();

    // 未设置 Payload Format Indicator，视为二进制格式
    if (formatIndicator == null) {
      return ValidationResult.success();
    }

    // 0 = 未指定字节流，无需验证
    if (formatIndicator == 0) {
      return ValidationResult.success();
    }

    // 1 = UTF-8 编码文本
    if (formatIndicator == 1) {
      byte[] payload = message.payload();
      if (payload == null || payload.length == 0) {
        return ValidationResult.success();
      }

      if (!isValidUtf8(payload)) {
        String errorMsg = "Payload is not valid UTF-8";
        _Logger.warning("Validation failed: {}", errorMsg);
        if (_strictMode) {
          throw new PayloadValidationException(errorMsg);
        }
        return ValidationResult.failure(errorMsg);
      }
    }

    return ValidationResult.success();
  }

  /** 验证 Content Type */
  private ValidationResult validateContentType(X113_QttPublish message) {
    String contentType = message.getContentType();

    if (contentType == null || contentType.isEmpty()) {
      return ValidationResult.success();
    }

    if (!isValidMimeType(contentType)) {
      String errorMsg = "Invalid MIME type: " + contentType;
      _Logger.warning("Validation failed: {}", errorMsg);
      if (_strictMode) {
        throw new PayloadValidationException(errorMsg);
      }
      return ValidationResult.failure(errorMsg);
    }

    return ValidationResult.success();
  }

  // ==================== 格式检查 ====================

  /**
   * 检查是否为有效的 UTF-8 字节序列
   *
   * @param bytes 待检查的字节数组
   * @return true 如果是有效的 UTF-8
   */
  public boolean isValidUtf8(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return true;
    }

    try {
      // 尝试使用 UTF-8 解码
      Charset charset = StandardCharsets.UTF_8;
      String decoded = new String(bytes, charset);
      // 再编码回字节，检查是否一致
      byte[] reencoded = decoded.getBytes(charset);

      // 如果长度不一致，说明有无效字节
      if (reencoded.length != bytes.length) {
        return false;
      }

      // 逐字节比较
      for (int i = 0; i < bytes.length; i++) {
        if (reencoded[i] != bytes[i]) {
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 检查是否为有效的 MIME 类型
   *
   * @param mimeType MIME 类型字符串
   * @return true 如果是有效的 MIME 类型
   */
  public boolean isValidMimeType(String mimeType) {
    if (mimeType == null || mimeType.isEmpty()) {
      return false;
    }

    // 基本格式检查：type/subtype
    if (!MIME_TYPE_PATTERN.matcher(mimeType).matches()) {
      return false;
    }

    // 检查是否有且仅有一个斜杠
    int slashCount = 0;
    for (char c : mimeType.toCharArray()) {
      if (c == '/') {
        slashCount++;
      }
    }

    return slashCount == 1;
  }

  // ==================== 快速验证方法 ====================

  /** 快速验证 UTF-8（不创建验证器实例） */
  public static boolean quickValidateUtf8(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      return true;
    }

    int i = 0;
    while (i < bytes.length) {
      int b = bytes[i] & 0xFF;

      // 单字节 ASCII (0xxxxxxx)
      if (b < 0x80) {
        i++;
        continue;
      }

      // 双字节 (110xxxxx 10xxxxxx)
      if ((b & 0xE0) == 0xC0) {
        if (i + 1 >= bytes.length) return false;
        if ((bytes[i + 1] & 0xC0) != 0x80) return false;
        i += 2;
        continue;
      }

      // 三字节 (1110xxxx 10xxxxxx 10xxxxxx)
      if ((b & 0xF0) == 0xE0) {
        if (i + 2 >= bytes.length) return false;
        if ((bytes[i + 1] & 0xC0) != 0x80) return false;
        if ((bytes[i + 2] & 0xC0) != 0x80) return false;
        i += 3;
        continue;
      }

      // 四字节 (11110xxx 10xxxxxx 10xxxxxx 10xxxxxx)
      if ((b & 0xF8) == 0xF0) {
        if (i + 3 >= bytes.length) return false;
        if ((bytes[i + 1] & 0xC0) != 0x80) return false;
        if ((bytes[i + 2] & 0xC0) != 0x80) return false;
        if ((bytes[i + 3] & 0xC0) != 0x80) return false;
        i += 4;
        continue;
      }

      // 无效的 UTF-8 起始字节
      return false;
    }

    return true;
  }

  // ==================== 内部类 ====================

  /** 验证结果 */
  public static class ValidationResult {
    private final boolean _valid;
    private final String _errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
      _valid = valid;
      _errorMessage = errorMessage;
    }

    public static ValidationResult success() {
      return new ValidationResult(true, null);
    }

    public static ValidationResult failure(String errorMessage) {
      return new ValidationResult(false, errorMessage);
    }

    public boolean isValid() {
      return _valid;
    }

    public String getErrorMessage() {
      return _errorMessage;
    }

    @Override
    public String toString() {
      return _valid
          ? "ValidationResult{valid=true}"
          : "ValidationResult{valid=false, error=" + _errorMessage + "}";
    }
  }

  /** Payload 验证异常 */
  public static class PayloadValidationException extends RuntimeException {
    public PayloadValidationException(String message) {
      super(message);
    }
  }
}
