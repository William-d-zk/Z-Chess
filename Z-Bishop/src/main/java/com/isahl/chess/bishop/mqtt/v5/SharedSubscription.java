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

import java.util.Objects;

/** MQTT 5.0 共享订阅表示 主题格式：$share/{shareName}/{topic} */
public class SharedSubscription {
  private final String shareName;
  private final String topic;
  private final int qos;

  public SharedSubscription(String shareName, String topic, int qos) {
    if (shareName == null || shareName.isEmpty()) {
      throw new IllegalArgumentException("shareName cannot be empty");
    }
    if (topic == null || topic.isEmpty()) {
      throw new IllegalArgumentException("topic cannot be empty");
    }
    this.shareName = shareName;
    this.topic = topic;
    this.qos = qos;
  }

  public String getShareName() {
    return shareName;
  }

  public String getTopic() {
    return topic;
  }

  public int getQos() {
    return qos;
  }

  public String getFullTopic() {
    return "$share/" + shareName + "/" + topic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SharedSubscription that = (SharedSubscription) o;
    return Objects.equals(shareName, that.shareName) && Objects.equals(topic, that.topic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(shareName, topic);
  }
}
