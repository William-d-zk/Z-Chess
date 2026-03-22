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

import com.isahl.chess.bishop.protocol.mqtt.model.CodeMqtt;

/**
 * MQTT v5.0 服务器重定向原因码
 *
 * <p>当服务器需要客户端连接到另一个服务器时，使用以下原因码：
 *
 * <ul>
 *   <li>0x9C (156) - Use Another Server: 客户端应临时连接到指定的其他服务器
 *   <li>0x9D (157) - Server Moved: 客户端应永久连接到指定的其他服务器
 * </ul>
 *
 * @author william.d.zk
 * @see <a href="https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html#_Toc3901207">MQTT v5.0
 *     Reason Codes</a>
 */
public enum RedirectReason {

  /**
   * 使用其他服务器 (0x9C)
   *
   * <p>客户端应临时连接到 Server Reference 指定的其他服务器。 适用于负载均衡场景，当前服务器负载过高时将客户端引导至其他服务器。
   */
  USE_ANOTHER_SERVER((byte) 0x9C, "Use Another Server", false),

  /**
   * 服务器已迁移 (0x9D)
   *
   * <p>客户端应永久连接到 Server Reference 指定的其他服务器。 适用于服务器下线或迁移场景，客户端应更新配置使用新服务器。
   */
  SERVER_MOVED((byte) 0x9D, "Server Moved", true);

  private final byte code;
  private final String description;
  private final boolean permanent;

  RedirectReason(byte code, String description, boolean permanent) {
    this.code = code;
    this.description = description;
    this.permanent = permanent;
  }

  /** 获取原因码值 */
  public byte getCode() {
    return code;
  }

  /** 获取原因描述 */
  public String getDescription() {
    return description;
  }

  /**
   * 是否为永久重定向
   *
   * <p>true 表示客户端应永久使用新服务器地址， false 表示客户端仅在本次连接中使用新服务器地址。
   */
  public boolean isPermanent() {
    return permanent;
  }

  /**
   * 根据原因码查找枚举
   *
   * @param code 原因码
   * @return 对应的 RedirectReason，未找到返回 null
   */
  public static RedirectReason fromCode(byte code) {
    for (RedirectReason reason : values()) {
      if (reason.code == code) {
        return reason;
      }
    }
    return null;
  }

  /**
   * 根据原因码查找枚举
   *
   * @param code 原因码（int 类型）
   * @return 对应的 RedirectReason，未找到返回 null
   */
  public static RedirectReason fromCode(int code) {
    return fromCode((byte) code);
  }

  /** 转换为 MQTT 错误码 */
  public CodeMqtt toCodeMqtt() {
    return switch (this) {
      case USE_ANOTHER_SERVER -> CodeMqtt.USE_ANOTHER_SERVER;
      case SERVER_MOVED -> CodeMqtt.SERVER_MOVED;
    };
  }

  /** 从 MQTT 错误码创建 */
  public static RedirectReason fromCodeMqtt(CodeMqtt codeMqtt) {
    return switch (codeMqtt) {
      case USE_ANOTHER_SERVER -> USE_ANOTHER_SERVER;
      case SERVER_MOVED -> SERVER_MOVED;
      default -> null;
    };
  }

  @Override
  public String toString() {
    return String.format("%s(0x%02X, %s, permanent=%s)", name(), code, description, permanent);
  }
}
