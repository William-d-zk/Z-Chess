/*
 * MIT License
 *
 * Copyright (c) 2016~2020 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.cluster.raft;

import java.util.List;

import com.tgx.chess.bishop.io.zprotocol.raft.X72_RaftVote;
import com.tgx.chess.bishop.io.zprotocol.raft.X7E_RaftBroadcast;
import com.tgx.chess.bishop.io.zprotocol.raft.X7F_RaftResponse;
import com.tgx.chess.cluster.raft.model.log.LogEntry;

/**
 * @author william.d.zk
 * @date 2019/12/10
 */
public interface IRaftNode
{

    /**
     * @param code
     * @return
     */
    X7F_RaftResponse reject(int code);

    /**
     * @return
     */
    X7F_RaftResponse stepDown();

    /**
     * 
     * @param peerId
     * @return
     */
    X7F_RaftResponse follow(long peerId, long commit);

    /**
     * 
     * @param peerId
     * @return
     */
    X7F_RaftResponse stepUp(long peerId);

    /**
     * 
     * @param peerId
     * @return
     */
    X7F_RaftResponse reTick(long peerId);

    /**
     * @param code
     * @return
     */
    X7F_RaftResponse rejectAndStepDown(int code);

    /**
     * 接收另一个machine的状态更新
     *
     * @param update
     * @return success or reject
     */
    X7F_RaftResponse merge(IRaftMachine update);

    boolean checkVoteState(X72_RaftVote x72);

    boolean checkLogAppend(X7E_RaftBroadcast x7e);

    void apply();

    void load(List<IRaftMessage> snapshot);

    void takeSnapshot(IRaftDao writer);

    IRaftMachine getMachine();

    List<LogEntry> diff();

    void appendLogs(List<LogEntry> entryList);

    enum RaftState
    {
        LEARNER(0),
        FOLLOWER(1),
        ELECTOR(2),
        CANDIDATE(3),
        LEADER(4);

        private final int _Code;

        RaftState(int code)
        {
            _Code = code;
        }

        public int getCode()
        {
            return _Code;
        }

        public static RaftState valueOf(int code)
        {
            switch (code)
            {
                case 0:
                    return LEARNER;
                case 1:
                    return FOLLOWER;
                case 2:
                    return ELECTOR;
                case 3:
                    return CANDIDATE;
                case 4:
                    return LEADER;
                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}
