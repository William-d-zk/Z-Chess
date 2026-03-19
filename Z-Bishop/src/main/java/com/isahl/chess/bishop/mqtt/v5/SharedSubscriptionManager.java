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

import com.isahl.chess.queen.io.core.net.socket.AioSession;
import java.util.List;

/** MQTT 5.0 共享订阅管理器 负责管理共享订阅组和消息分发 */
public interface SharedSubscriptionManager {

  /** 添加共享订阅 */
  void addSubscription(String shareName, SharedSubscription subscription, AioSession<?> session);

  /** 移除共享订阅 */
  void removeSubscription(String shareName, String topic, AioSession<?> session);

  /** 为消息选择消费者（轮询策略） */
  AioSession<?> selectConsumer(String shareName);

  /** 获取共享订阅组的所有消费者 */
  List<AioSession<?>> getConsumers(String shareName);

  /** 清理会话相关的所有共享订阅 */
  void cleanupSession(AioSession<?> session);
}
