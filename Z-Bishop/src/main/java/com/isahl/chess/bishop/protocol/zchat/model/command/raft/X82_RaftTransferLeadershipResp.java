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

import static com.isahl.chess.king.config.KingCode.SUCCESS;

import com.isahl.chess.bishop.protocol.zchat.model.command.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

/**
 * Leader 转让响应
 *
 * @author william.d.zk
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL, serial = 0x82)
public class X82_RaftTransferLeadershipResp extends ZCommand implements IConsistent {

  public X82_RaftTransferLeadershipResp() {
    super();
  }

  public X82_RaftTransferLeadershipResp(long msgId) {
    super(msgId);
  }

  /** 响应节点ID */
  private long mPeer;

  /** 状态码: 0=成功, 其他=失败原因 */
  private int mCode;

  /** 当前节点的 log index */
  private long mIndex;

  /** 当前节点的 commit index */
  private long mCommit;

  /** 消息 */
  private String mMessage;

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
    int messageLen = mMessage != null ? mMessage.getBytes().length : 0;
    return super.length() + 8 + 4 + 8 + 8 + 4 + messageLen;
  }

  @Override
  public ByteBuf suffix(ByteBuf output) {
    super.suffix(output);
    output.putLong(mPeer);
    output.putInt(mCode);
    output.putLong(mIndex);
    output.putLong(mCommit);

    byte[] messageBytes = mMessage != null ? mMessage.getBytes() : new byte[0];
    output.putInt(messageBytes.length);
    output.put(messageBytes);

    return output;
  }

  @Override
  public int prefix(ByteBuf input) {
    int remain = super.prefix(input);
    mPeer = input.getLong();
    remain -= 8;
    mCode = input.getInt();
    remain -= 4;
    mIndex = input.getLong();
    remain -= 8;
    mCommit = input.getLong();
    remain -= 8;

    int messageLen = input.getInt();
    remain -= 4;
    if (messageLen > 0) {
      byte[] messageBytes = new byte[messageLen];
      input.get(messageBytes);
      mMessage = new String(messageBytes);
      remain -= messageLen;
    } else {
      mMessage = "";
    }

    return remain;
  }

  @Override
  public String toString() {
    return String.format(
        "X82_RaftTransferLeadershipResp { peer:%#x, code:%d, index:%d, commit:%d, message:%s }",
        mPeer, mCode, mIndex, mCommit, mMessage);
  }

  public int getCode() {
    return mCode;
  }

  public void setCode(int code) {
    mCode = code;
  }

  @Override
  public int code() {
    return mCode;
  }

  public long getPeer() {
    return mPeer;
  }

  public void setPeer(long peer) {
    mPeer = peer;
  }

  public long getIndex() {
    return mIndex;
  }

  public void setIndex(long index) {
    mIndex = index;
  }

  public long getCommit() {
    return mCommit;
  }

  public void setCommit(long commit) {
    mCommit = commit;
  }

  public String getMessage() {
    return mMessage;
  }

  public void setMessage(String message) {
    mMessage = message;
  }

  public boolean isSuccess() {
    return mCode == SUCCESS;
  }
}
