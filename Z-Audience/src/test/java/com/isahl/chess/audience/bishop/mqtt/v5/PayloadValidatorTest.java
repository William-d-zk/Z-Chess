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

package com.isahl.chess.audience.bishop.mqtt.v5;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.mqtt.v5.PayloadValidator;
import com.isahl.chess.bishop.mqtt.v5.PayloadValidator.PayloadValidationException;
import com.isahl.chess.bishop.mqtt.v5.PayloadValidator.ValidationResult;
import com.isahl.chess.bishop.protocol.mqtt.command.X113_QttPublish;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Payload 格式验证器测试
 *
 * @author william.d.zk
 * @since 1.2.1
 */
class PayloadValidatorTest {

  private PayloadValidator validator;

  @BeforeEach
  void setUp() {
    validator = new PayloadValidator();
  }

  @Test
  void testValidateNullMessage() {
    ValidationResult result = validator.validate(null);
    assertFalse(result.isValid());
    assertEquals("Message is null", result.getErrorMessage());
  }

  @Test
  void testValidateDisabled() {
    validator.setEnabled(false);

    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(1); // UTF-8
    message.setPayload(new byte[] {(byte) 0xFF, (byte) 0xFE}); // 无效 UTF-8

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid()); // 禁用验证，应该通过
  }

  @Test
  void testValidateBinaryPayload() {
    // Payload Format Indicator = 0 (二进制)，不验证
    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(0);
    message.setPayload(new byte[] {(byte) 0xFF, (byte) 0xFE, (byte) 0xFD});

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid());
  }

  @Test
  void testValidateValidUtf8() {
    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(1); // UTF-8
    message.setPayload("Hello, World! 你好世界 🌍".getBytes(StandardCharsets.UTF_8));

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid());
  }

  @Test
  void testValidateInvalidUtf8() {
    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(1); // UTF-8
    // 无效的 UTF-8 序列
    message.setPayload(new byte[] {(byte) 0xC0, (byte) 0x80});

    ValidationResult result = validator.validate(message);
    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("UTF-8"));
  }

  @Test
  void testValidateInvalidUtf8StrictMode() {
    validator.setStrictMode(true);

    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(1); // UTF-8
    message.setPayload(new byte[] {(byte) 0xC0, (byte) 0x80}); // 无效 UTF-8

    assertThrows(PayloadValidationException.class, () -> validator.validate(message));
  }

  @Test
  void testValidateValidMimeType() {
    X113_QttPublish message = new X113_QttPublish();
    message.setContentType("application/json");

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid());
  }

  @Test
  void testValidateInvalidMimeType() {
    X113_QttPublish message = new X113_QttPublish();
    message.setContentType("invalid mime type"); // 包含空格

    ValidationResult result = validator.validate(message);
    assertFalse(result.isValid());
    assertTrue(result.getErrorMessage().contains("MIME"));
  }

  @Test
  void testValidateComplexMimeType() {
    X113_QttPublish message = new X113_QttPublish();
    message.setContentType("application/vnd.api+json");

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid());
  }

  @Test
  void testIsValidUtf8() {
    assertTrue(validator.isValidUtf8("Hello".getBytes(StandardCharsets.UTF_8)));
    assertTrue(validator.isValidUtf8("你好".getBytes(StandardCharsets.UTF_8)));
    assertTrue(validator.isValidUtf8("🌍".getBytes(StandardCharsets.UTF_8)));

    // 无效的 UTF-8 序列
    assertFalse(validator.isValidUtf8(new byte[] {(byte) 0xC0, (byte) 0x80}));
    assertFalse(validator.isValidUtf8(new byte[] {(byte) 0xFF}));
  }

  @Test
  void testQuickValidateUtf8() {
    assertTrue(PayloadValidator.quickValidateUtf8("Hello".getBytes(StandardCharsets.UTF_8)));
    assertTrue(PayloadValidator.quickValidateUtf8("你好".getBytes(StandardCharsets.UTF_8)));

    // 截断的多字节序列
    assertFalse(PayloadValidator.quickValidateUtf8(new byte[] {(byte) 0xE4, (byte) 0xBD}));
  }

  @Test
  void testIsValidMimeType() {
    // 有效的 MIME 类型
    assertTrue(validator.isValidMimeType("text/plain"));
    assertTrue(validator.isValidMimeType("application/json"));
    assertTrue(validator.isValidMimeType("image/png"));
    assertTrue(validator.isValidMimeType("application/vnd.api+json"));
    // 注意：包含参数的 MIME 类型（如 text/html; charset=utf-8）需要更复杂的验证

    // 无效的 MIME 类型
    assertFalse(validator.isValidMimeType(""));
    assertFalse(validator.isValidMimeType(null));
    assertFalse(validator.isValidMimeType("invalid"));
    assertFalse(validator.isValidMimeType("type/sub/type"));
    assertFalse(validator.isValidMimeType("type sub"));
  }

  @Test
  void testEmptyPayload() {
    X113_QttPublish message = new X113_QttPublish();
    message.setPayloadFormatIndicator(1); // UTF-8
    message.setPayload(new byte[0]);

    ValidationResult result = validator.validate(message);
    assertTrue(result.isValid());
  }

  @Test
  void testStrictMode() {
    assertFalse(validator.isStrictMode());

    validator.setStrictMode(true);
    assertTrue(validator.isStrictMode());

    validator.setStrictMode(false);
    assertFalse(validator.isStrictMode());
  }
}
