/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.bishop.protocol.zchat.model.command.raft;

import com.isahl.chess.bishop.protocol.zchat.model.command.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

/**
 * Lease Read 请求
 *
 * <p>在 Leader 租约期内直接读取，无需等待 commit
 *
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL, serial = 0x83)
public class X83_RaftLeaseRead extends ZCommand implements IConsistent {

  public X83_RaftLeaseRead() {
    super();
  }

  public X83_RaftLeaseRead(long msgId) {
    super(msgId);
  }

  /** 读请求ID */
  private long mReadId;

  /** 客户端ID */
  private long mClient;

  /** 读操作类型（可选，用于区分不同类型的读） */
  private int mReadType;

  /** 原始请求来源 */
  private long mOrigin;

  @Override
  public int priority() {
    return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
  }

  @Override
  public Level level() {
    return Level.AT_LEAST_ONCE;
  }

  @Override
  public int length() {
    return super.length() + 8 + 8 + 4 + 8;
  }

  @Override
  public ByteBuf suffix(ByteBuf output) {
    return super.suffix(output)
        .putLong(mReadId)
        .putLong(mClient)
        .putInt(mReadType)
        .putLong(mOrigin);
  }

  @Override
  public int prefix(ByteBuf input) {
    int remain = super.prefix(input);
    mReadId = input.getLong();
    remain -= 8;
    mClient = input.getLong();
    remain -= 8;
    mReadType = input.getInt();
    remain -= 4;
    mOrigin = input.getLong();
    remain -= 8;
    return remain;
  }

  @Override
  public String toString() {
    return String.format(
        "X83_RaftLeaseRead { readId=%d, client=%#x, type=%d }", mReadId, mClient, mReadType);
  }

  @Override
  public int code() {
    return 0;
  }

  public long readId() {
    return mReadId;
  }

  public void readId(long readId) {
    mReadId = readId;
  }

  public long client() {
    return mClient;
  }

  public void client(long client) {
    mClient = client;
  }

  public int readType() {
    return mReadType;
  }

  public void readType(int readType) {
    mReadType = readType;
  }

  public long origin() {
    return mOrigin;
  }

  public void origin(long origin) {
    mOrigin = origin;
  }
}
