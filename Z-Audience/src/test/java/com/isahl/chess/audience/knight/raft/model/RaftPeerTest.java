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

package com.isahl.chess.knight.raft.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaftPeerTest
{

    @Test
    void raftGraphAdd()
    {
        long a = 0x8892301123L;
        String formatted = String.format("%#x", a);
        
        // 验证格式化结果
        assertNotNull(formatted, "格式化结果不应该为 null");
        assertEquals("0x8892301123", formatted, "十六进制格式化应该正确");
        
        // 验证长整型值
        assertEquals(0x8892301123L, a, "长整型值应该匹配");
        assertTrue(a > 0, "值应该为正数");
    }
    
    @Test
    void testHexFormatting()
    {
        // 测试不同的十六进制格式化
        long value1 = 0xFF;
        assertEquals("0xff", String.format("%#x", value1), "小写十六进制格式化");
        assertEquals("0xFF", String.format("%#X", value1), "大写十六进制格式化");
        
        long value2 = 0x123456789ABCDEFL;
        assertEquals("0x123456789abcdef", String.format("%#x", value2));
        
        // 测试无前缀的十六进制 (返回大写)
        assertEquals("FF", String.format("%x", value1));
    }
    
    @Test
    void testRaftStateCodes()
    {
        // 验证 RaftState 枚举值
        assertEquals(0x00, RaftState.OUTSIDE.getCode(), "OUTSIDE 代码应该是 0x00");
        assertEquals(0x01, RaftState.CLIENT.getCode(), "CLIENT 代码应该是 0x01");
        assertEquals(0x02, RaftState.FOLLOWER.getCode(), "FOLLOWER 代码应该是 0x02");
        assertEquals(0x04, RaftState.ELECTOR.getCode(), "ELECTOR 代码应该是 0x04");
        assertEquals(0x08, RaftState.CANDIDATE.getCode(), "CANDIDATE 代码应该是 0x08");
        assertEquals(0x10, RaftState.LEADER.getCode(), "LEADER 代码应该是 0x10");
        assertEquals(0x20, RaftState.GATE.getCode(), "GATE 代码应该是 0x20");
        assertEquals(0x40, RaftState.LEARNER.getCode(), "LEARNER 代码应该是 0x40");
        assertEquals(0x80, RaftState.JOINT.getCode(), "JOINT 代码应该是 0x80");
    }
    
    @Test
    void testRaftStateValueOf()
    {
        // 验证根据代码获取状态
        assertEquals(RaftState.FOLLOWER, RaftState.valueOf(0x02), "根据代码获取 FOLLOWER");
        assertEquals(RaftState.LEADER, RaftState.valueOf(0x10), "根据代码获取 LEADER");
        assertEquals(RaftState.CANDIDATE, RaftState.valueOf(0x08), "根据代码获取 CANDIDATE");
    }
    
    @Test
    void testRaftStateRoleOf()
    {
        // 验证状态角色名称 (带 R: 前缀)
        assertEquals("R:FOLLOWER", RaftState.roleOf(0x02), "FOLLOWER 角色名称");
        assertEquals("R:LEADER", RaftState.roleOf(0x10), "LEADER 角色名称");
        assertEquals("R:CANDIDATE", RaftState.roleOf(0x08), "CANDIDATE 角色名称");
    }
    
    @Test
    void testRaftCode()
    {
        // 验证 RaftCode 枚举
        assertEquals(0, RaftCode.SUCCESS.getCode(), "SUCCESS 代码应该是 0");
        assertEquals(1, RaftCode.LOWER_TERM.getCode(), "LOWER_TERM 代码应该是 1");
        assertEquals(2, RaftCode.CONFLICT.getCode(), "CONFLICT 代码应该是 2");
        assertEquals(3, RaftCode.ILLEGAL_STATE.getCode(), "ILLEGAL_STATE 代码应该是 3");
        assertEquals(4, RaftCode.SPLIT_CLUSTER.getCode(), "SPLIT_CLUSTER 代码应该是 4");
        assertEquals(5, RaftCode.ALREADY_VOTE.getCode(), "ALREADY_VOTE 代码应该是 5");
        assertEquals(6, RaftCode.OBSOLETE.getCode(), "OBSOLETE 代码应该是 6");
    }
}
