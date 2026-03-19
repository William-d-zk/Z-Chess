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

package com.isahl.chess.bishop.protocol.zchat.model.command.raft;

import static com.isahl.chess.queen.io.core.features.model.session.IQoS.Level.AT_LEAST_ONCE;

import com.isahl.chess.bishop.protocol.zchat.model.command.ZCommand;
import com.isahl.chess.board.annotation.ISerialGenerator;
import com.isahl.chess.board.base.ISerial;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;

/**
 * Raft Pre-vote 响应
 *
 * @author william.d.zk
 * @date 2024
 */
@ISerialGenerator(parent = ISerial.PROTOCOL_BISHOP_COMMAND_SERIAL, serial = 0x6E)
public class X6E_RaftPreVoteResp extends ZCommand implements IConsistent {

  private long mPeer;
  private long mTerm;
  private long mCandidate;
  private boolean mGranted;
  private int mCode;

  public X6E_RaftPreVoteResp() {
    super();
  }

  public X6E_RaftPreVoteResp(long msgId) {
    super(msgId);
  }

  @Override
  public int priority() {
    return QOS_PRIORITY_03_CLUSTER_EXCHANGE;
  }

  @Override
  public Level level() {
    return AT_LEAST_ONCE;
  }

  public long peer() {
    return mPeer;
  }

  public long term() {
    return mTerm;
  }

  public long candidate() {
    return mCandidate;
  }

  public boolean granted() {
    return mGranted;
  }

  public void peer(long peer) {
    mPeer = peer;
  }

  public void term(long term) {
    mTerm = term;
  }

  public void candidate(long candidate) {
    mCandidate = candidate;
  }

  public void granted(boolean granted) {
    mGranted = granted;
  }

  public void setCode(int code) {
    mCode = code;
  }

  @Override
  public int code() {
    return mCode;
  }

  @Override
  public int length() {
    return super.length() + 25; // 8+8+8+1
  }

  @Override
  public int prefix(ByteBuf input) {
    int remain = super.prefix(input);
    mPeer = input.getLong();
    mTerm = input.getLong();
    mCandidate = input.getLong();
    mGranted = input.get() == 1;
    return remain - 25;
  }

  @Override
  public ByteBuf suffix(ByteBuf output) {
    return super.suffix(output)
        .putLong(mPeer)
        .putLong(mTerm)
        .putLong(mCandidate)
        .put((byte) (mGranted ? 1 : 0));
  }

  @Override
  public String toString() {
    return String.format(
        "X6E_RaftPreVoteResp{msgId=%#x, peer=%#x, term=%d, candidate=%#x, granted=%s}",
        msgId(), mPeer, mTerm, mCandidate, mGranted);
  }
}
