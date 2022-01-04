/*
 * MIT License
 *
 * Copyright (c) 2021. Z-Chess
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

package com.isahl.chess.knight.raft.model.replicate;

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class LogMetaTest
{
    @Test
    void test()
    {
        LogMeta meta = new LogMeta();
        RaftNode node = new RaftNode("raft0", 5228, RaftState.FOLLOWER);
        RaftNode node1 = new RaftNode("raft1", 5228, RaftState.FOLLOWER);
        RaftNode gate = new RaftNode("gate0", 5666, RaftState.GATE);
        RaftNode gate1 = new RaftNode("gate1", 5666, RaftState.GATE);
        meta.setPeerSet(Arrays.asList(node, node1));
        meta.setGateSet(Arrays.asList(gate, gate1));
        ByteBuf buffer = meta.encode();

        LogMeta meta2 = new LogMeta();
        meta2.decode(buffer);

    }
}