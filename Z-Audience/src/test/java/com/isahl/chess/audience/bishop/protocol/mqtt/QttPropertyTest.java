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

package com.isahl.chess.bishop.protocol.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.bishop.protocol.mqtt.model.QttProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** QttProperty 枚举测试类 */
class QttPropertyTest {

  @Test
  void testPropertyIds() {
    // 验证关键属性的 ID 值
    assertThat(QttProperty.PAYLOAD_FORMAT_INDICATOR.getId()).isEqualTo(0x01);
    assertThat(QttProperty.MESSAGE_EXPIRY_INTERVAL.getId()).isEqualTo(0x02);
    assertThat(QttProperty.CONTENT_TYPE.getId()).isEqualTo(0x03);
    assertThat(QttProperty.RESPONSE_TOPIC.getId()).isEqualTo(0x08);
    assertThat(QttProperty.SUBSCRIPTION_IDENTIFIER.getId()).isEqualTo(0x0B);
    assertThat(QttProperty.SESSION_EXPIRY_INTERVAL.getId()).isEqualTo(0x11);
    assertThat(QttProperty.RECEIVE_MAXIMUM.getId()).isEqualTo(0x21);
    assertThat(QttProperty.TOPIC_ALIAS_MAXIMUM.getId()).isEqualTo(0x22);
    assertThat(QttProperty.TOPIC_ALIAS.getId()).isEqualTo(0x23);
    assertThat(QttProperty.USER_PROPERTY.getId()).isEqualTo(0x26);
  }

  @ParameterizedTest
  @EnumSource(QttProperty.class)
  void testAllPropertiesHaveValidIds(QttProperty property) {
    assertThat(property.getId()).isBetween(0x01, 0x2A);
  }

  @ParameterizedTest
  @CsvSource({
    "0x01, PAYLOAD_FORMAT_INDICATOR",
    "0x02, MESSAGE_EXPIRY_INTERVAL",
    "0x03, CONTENT_TYPE",
    "0x08, RESPONSE_TOPIC",
    "0x0B, SUBSCRIPTION_IDENTIFIER",
    "0x11, SESSION_EXPIRY_INTERVAL",
    "0x21, RECEIVE_MAXIMUM",
    "0x22, TOPIC_ALIAS_MAXIMUM",
    "0x23, TOPIC_ALIAS",
    "0x26, USER_PROPERTY"
  })
  void testValueOfById(int id, String expectedName) {
    QttProperty property = QttProperty.valueOf(id);
    assertThat(property).isNotNull();
    assertThat(property.name()).isEqualTo(expectedName);
    assertThat(property.getId()).isEqualTo(id);
  }

  @ParameterizedTest
  @ValueSource(ints = {0x00, 0x50, 0xFF, -1})
  void testValueOfInvalidIdReturnsNull(int invalidId) {
    assertThat(QttProperty.valueOf(invalidId)).isNull();
  }

  @ParameterizedTest
  @ValueSource(ints = {0x01, 0x11, 0x21, 0x26, 0x2A})
  void testIsValidWithValidIds(int id) {
    assertThat(QttProperty.isValid(id)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(ints = {0x00, 0x50, 0xFF})
  void testIsValidWithInvalidIds(int id) {
    assertThat(QttProperty.isValid(id)).isFalse();
  }

  @Test
  void testPropertyTypes() {
    assertThat(QttProperty.PAYLOAD_FORMAT_INDICATOR.getType())
        .isEqualTo(QttProperty.PropertyType.BYTE);
    assertThat(QttProperty.RECEIVE_MAXIMUM.getType())
        .isEqualTo(QttProperty.PropertyType.TWO_BYTE_INTEGER);
    assertThat(QttProperty.MESSAGE_EXPIRY_INTERVAL.getType())
        .isEqualTo(QttProperty.PropertyType.FOUR_BYTE_INTEGER);
    assertThat(QttProperty.SUBSCRIPTION_IDENTIFIER.getType())
        .isEqualTo(QttProperty.PropertyType.VARIABLE_BYTE_INTEGER);
    assertThat(QttProperty.CONTENT_TYPE.getType())
        .isEqualTo(QttProperty.PropertyType.UTF_8_ENCODED_STRING);
    assertThat(QttProperty.USER_PROPERTY.getType())
        .isEqualTo(QttProperty.PropertyType.UTF_8_STRING_PAIR);
    assertThat(QttProperty.CORRELATION_DATA.getType())
        .isEqualTo(QttProperty.PropertyType.BINARY_DATA);
  }

  @Test
  void testRepeatableProperties() {
    assertThat(QttProperty.USER_PROPERTY.isRepeatable()).isTrue();
    assertThat(QttProperty.SUBSCRIPTION_IDENTIFIER.isRepeatable()).isTrue();

    // 不可重复的属性
    assertThat(QttProperty.SESSION_EXPIRY_INTERVAL.isRepeatable()).isFalse();
    assertThat(QttProperty.RECEIVE_MAXIMUM.isRepeatable()).isFalse();
    assertThat(QttProperty.TOPIC_ALIAS.isRepeatable()).isFalse();
  }

  @Test
  void testPropertyTypeFixedLength() {
    assertThat(QttProperty.PropertyType.BYTE.getFixedLength()).isEqualTo(1);
    assertThat(QttProperty.PropertyType.TWO_BYTE_INTEGER.getFixedLength()).isEqualTo(2);
    assertThat(QttProperty.PropertyType.FOUR_BYTE_INTEGER.getFixedLength()).isEqualTo(4);
    assertThat(QttProperty.PropertyType.VARIABLE_BYTE_INTEGER.getFixedLength()).isEqualTo(-1);
    assertThat(QttProperty.PropertyType.UTF_8_ENCODED_STRING.getFixedLength()).isEqualTo(-2);
  }

  @Test
  void testPropertyTypeIsVariableLength() {
    assertThat(QttProperty.PropertyType.BYTE.isVariableLength()).isFalse();
    assertThat(QttProperty.PropertyType.VARIABLE_BYTE_INTEGER.isVariableLength()).isTrue();
    assertThat(QttProperty.PropertyType.UTF_8_ENCODED_STRING.isVariableLength()).isTrue();
  }

  @Test
  void testToStringFormat() {
    QttProperty property = QttProperty.RECEIVE_MAXIMUM;
    String str = property.toString();
    assertThat(str).contains("RECEIVE_MAXIMUM");
    assertThat(str).contains("0x21");
  }
}
