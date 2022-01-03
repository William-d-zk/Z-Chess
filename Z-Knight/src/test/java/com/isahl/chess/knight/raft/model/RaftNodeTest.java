/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
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

package com.isahl.chess.knight.raft.model;

import com.isahl.chess.knight.raft.model.replicate.LogMeta;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class RaftNodeTest
{
    @Test
    public void testSerialization()
    {
        RaftNode node = new RaftNode("raft0", 5443, RaftState.LEADER);
        RaftNode gate = new RaftNode("raft0", -1, RaftState.GATE);
        gate.setGateHost("gate0");
        gate.setGatePort(6553);

        LogMeta meta = new LogMeta(1, 1, 1, 1, 1, 1, null, null);
        LogMeta meta2 = new LogMeta(1, 1, 1, 1, 1, 1, null, null);

        meta2.setPeerSet(Collections.singletonList(node));

        meta2.setGateSet(Collections.singletonList(gate));

        meta2.encode();
    }
}