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

package com.isahl.chess.bishop.protocol.spi;

import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议处理上下文
 *
 * <p>在处理链中传递上下文信息，包括会话、属性等。
 *
 * @author william.d.zk
 * @since 1.1.2
 */
public class ProtocolContext {

  /** 关联的会话 */
  private final ISession _session;

  /** 原始消息 */
  private final IProtocol _message;

  /** 上下文属性 */
  private final Map<String, Object> _attributes;

  /** 处理结果 */
  private volatile ProtocolResult _result = ProtocolResult.CONTINUE;

  /** 处理开始时间 */
  private final long _startTime;

  public ProtocolContext(ISession session, IProtocol message) {
    this._session = session;
    this._message = message;
    this._attributes = new ConcurrentHashMap<>();
    this._startTime = System.currentTimeMillis();
  }

  // ==================== 会话访问 ====================

  /** 获取关联会话 */
  public ISession getSession() {
    return _session;
  }

  /** 获取会话 ID */
  public String getSessionId() {
    return _session != null ? _session.summary() : null;
  }

  // ==================== 消息访问 ====================

  /** 获取原始消息 */
  public IProtocol getMessage() {
    return _message;
  }

  /** 获取消息类型 */
  public String getMessageType() {
    return _message != null ? _message.getClass().getSimpleName() : null;
  }

  // ==================== 属性管理 ====================

  /** 设置属性 */
  public void setAttribute(String key, Object value) {
    _attributes.put(key, value);
  }

  /** 获取属性 */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String key) {
    return (T) _attributes.get(key);
  }

  /** 获取属性（带默认值） */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String key, T defaultValue) {
    T value = (T) _attributes.get(key);
    return value != null ? value : defaultValue;
  }

  /** 检查是否有属性 */
  public boolean hasAttribute(String key) {
    return _attributes.containsKey(key);
  }

  /** 移除属性 */
  public void removeAttribute(String key) {
    _attributes.remove(key);
  }

  /** 获取所有属性 */
  public Map<String, Object> getAttributes() {
    return new HashMap<>(_attributes);
  }

  // ==================== 处理结果 ====================

  /** 获取处理结果 */
  public ProtocolResult getResult() {
    return _result;
  }

  /** 设置处理结果 */
  public void setResult(ProtocolResult result) {
    this._result = result;
  }

  /** 标记处理完成 */
  public void markCompleted() {
    this._result = ProtocolResult.COMPLETED;
  }

  /** 标记处理失败 */
  public void markFailed(String reason) {
    this._result = ProtocolResult.failed(reason);
  }

  /** 标记跳过后续处理 */
  public void markSkip() {
    this._result = ProtocolResult.SKIP;
  }

  /** 是否继续处理 */
  public boolean shouldContinue() {
    return _result == ProtocolResult.CONTINUE;
  }

  /** 是否已完成 */
  public boolean isCompleted() {
    return _result == ProtocolResult.COMPLETED;
  }

  // ==================== 统计信息 ====================

  /** 获取处理开始时间 */
  public long getStartTime() {
    return _startTime;
  }

  /** 获取处理耗时 */
  public long getElapsedTime() {
    return System.currentTimeMillis() - _startTime;
  }

  @Override
  public String toString() {
    return String.format(
        "ProtocolContext{session=%s, message=%s, result=%s, attrs=%d}",
        getSessionId(), getMessageType(), _result, _attributes.size());
  }
}
