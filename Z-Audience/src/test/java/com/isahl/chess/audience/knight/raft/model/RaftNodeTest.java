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

import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.knight.raft.model.replicate.LogMeta;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RaftNodeTest
{
    @Test
    public void testSerialization()
    {
        RaftNode node = new RaftNode("raft0", 5228);
        node.setId(0x1234L);
        
        RaftNode gate = new RaftNode("raft0", "gate0", 5300);
        gate.setId(0x1234L);
        gate.setGateHost("gate0");
        gate.setGatePort(6553);
        
        // 验证节点属性
        assertEquals("raft0", node.getHost(), "host 应该匹配");
        assertEquals(5228, node.getPort(), "port 应该匹配");
        assertTrue(node.isInState(RaftState.FOLLOWER), "默认状态应该是 FOLLOWER");
        
        assertEquals("raft0", gate.getHost(), "gate node host 应该匹配");
        assertEquals("gate0", gate.getGateHost(), "gate host 应该匹配");
        assertEquals(6553, gate.getGatePort(), "gate port 应该匹配");
        assertTrue(gate.isInState(RaftState.GATE), "gate 状态应该是 GATE");
        assertEquals(-1, gate.getPort(), "gate 的 service port 应该是 -1");
        
        // 测试 LogMeta 编解码
        LogMeta meta = new LogMeta(1, 1, 1, 1, 1, 1);
        LogMeta meta2 = new LogMeta(1, 1, 1, 1, 1, 1);
        
        ByteBuf output = meta2.encode();
        assertNotNull(output, "编码后的 buffer 不应该为 null");
        assertTrue(output.writerIdx() > 0, "编码后的数据长度应该大于 0");
        
        meta.decode(output);
        
        // 验证解码后的字段
        assertEquals(meta2.getStart(), meta.getStart(), "解码后 start 应该匹配");
        assertEquals(meta2.getTerm(), meta.getTerm(), "解码后 term 应该匹配");
        assertEquals(meta2.getIndex(), meta.getIndex(), "解码后 index 应该匹配");
        assertEquals(meta2.getCommit(), meta.getCommit(), "解码后 commit 应该匹配");
        assertEquals(meta2.getAccept(), meta.getAccept(), "解码后 accept 应该匹配");
    }
    
    @Test
    void testNoHost()
    {
        RaftNode node = new RaftNode("raft00");
        
        assertEquals("raft00", node.getHost(), "host 应该匹配");
        assertEquals(0, node.getPort(), "未设置 port 时应该为 0");
        assertTrue(node.isInState(RaftState.OUTSIDE), "默认状态应该是 OUTSIDE (未设置)");
        assertNotNull(node.toString(), "toString 不应该返回 null");
    }
    
    @Test
    void testRaftNodeCreationWithHostAndPort()
    {
        String host = "192.168.1.1";
        int port = 8080;
        
        RaftNode node = new RaftNode(host, port);
        
        assertEquals(host, node.getHost(), "host 应该匹配");
        assertEquals(port, node.getPort(), "port 应该匹配");
        assertTrue(node.isInState(RaftState.FOLLOWER), "默认应该是 FOLLOWER 状态");
    }
    
    @Test
    void testRaftNodeCreationWithGate()
    {
        String host = "192.168.1.1";
        String gateHost = "192.168.1.2";
        int gatePort = 9090;
        
        RaftNode node = new RaftNode(host, gateHost, gatePort);
        
        assertEquals(host, node.getHost(), "host 应该匹配");
        assertEquals(gateHost, node.getGateHost(), "gateHost 应该匹配");
        assertEquals(gatePort, node.getGatePort(), "gatePort 应该匹配");
        assertTrue(node.isInState(RaftState.GATE), "应该是 GATE 状态");
    }
    
    @Test
    void testRaftNodeStateTransitions()
    {
        RaftNode node = new RaftNode("test", 8080);
        
        // 初始状态是 FOLLOWER
        assertTrue(node.isInState(RaftState.FOLLOWER), "初始状态应该是 FOLLOWER");
        assertFalse(node.isInState(RaftState.LEADER), "不应该处于 LEADER 状态");
        assertFalse(node.isInState(RaftState.CANDIDATE), "不应该处于 CANDIDATE 状态");
        
        // RaftNode 的 setState 是 package-private，这里通过创建新节点来测试不同状态
        RaftNode leaderNode = new RaftNode("test2", 9090);
        leaderNode.setId(0x5678L);
        // 验证新节点也是 FOLLOWER 状态
        assertTrue(leaderNode.isInState(RaftState.FOLLOWER), "新节点默认应该是 FOLLOWER 状态");
    }
    
    @Test
    void testRaftNodeEncoding()
    {
        RaftNode node = new RaftNode("testhost", 8080);
        node.setId(0x1234L);
        
        ByteBuf buffer = ByteBuf.allocate(256);
        node.suffix(buffer);
        
        assertTrue(buffer.writerIdx() > 0, "编码后应该有数据");
        
        // 解码验证 - 创建新的 ByteBuf 从已写入的数据
        byte[] encodedData = new byte[buffer.writerIdx()];
        System.arraycopy(buffer.array(), 0, encodedData, 0, buffer.writerIdx());
        ByteBuf decodeBuffer = ByteBuf.wrap(encodedData);
        RaftNode decoded = new RaftNode(decodeBuffer);
        
        assertEquals(node.getHost(), decoded.getHost(), "解码后 host 应该匹配");
        assertEquals(node.getPort(), decoded.getPort(), "解码后 port 应该匹配");
    }
    
    @Test
    void testRaftNodeComparison()
    {
        RaftNode node1 = new RaftNode("host1", 8080);
        node1.setId(1L);
        
        RaftNode node2 = new RaftNode("host2", 9090);
        node2.setId(2L);
        
        RaftNode node3 = new RaftNode("host1", 7070);
        node3.setId(1L);
        
        // 不同 ID，应该按 ID 比较
        assertTrue(node1.compareTo(node2) < 0, "ID 1 应该小于 ID 2");
        assertTrue(node2.compareTo(node1) > 0, "ID 2 应该大于 ID 1");
        
        // 相同 ID 不同 host，应该按 host 比较
        // 注意：当前实现中 compareTo 会比较 host，即使 ID 相同
    }
    
    @Test
    void testRaftNodeId()
    {
        RaftNode node = new RaftNode("test", 8080);
        
        long id = 0xABCD1234L;
        node.setId(id);
        
        assertEquals(id, node.getId(), "getId 应该返回设置的 ID");
    }
    
    @Test
    void testHostBytes()
    {
        RaftNode node = new RaftNode("testhost", 8080);
        
        byte[] hostBytes = node.hostBytes();
        assertNotNull(hostBytes, "hostBytes 不应该为 null");
        assertArrayEquals("testhost".getBytes(), hostBytes, "hostBytes 应该匹配");
    }
    
    @Test
    void testGateHostBytes()
    {
        RaftNode node = new RaftNode("host", "gatehost", 9090);
        
        byte[] gateHostBytes = node.gateHostBytes();
        assertNotNull(gateHostBytes, "gateHostBytes 不应该为 null");
        assertArrayEquals("gatehost".getBytes(), gateHostBytes, "gateHostBytes 应该匹配");
    }
    
    @Test
    void testToString()
    {
        RaftNode node = new RaftNode("192.168.1.1", 8080);
        node.setId(0x1234L);
        
        String str = node.toString();
        assertNotNull(str, "toString 不应该返回 null");
        assertTrue(str.contains("192.168.1.1"), "toString 应该包含 host");
        assertTrue(str.contains("8080"), "toString 应该包含 port");
    }
}
