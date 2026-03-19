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

package com.isahl.chess.audience.knight.cluster.management;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.knight.cluster.management.ClusterManagementService;
import com.isahl.chess.knight.cluster.management.ClusterManagementServiceImpl;
import com.isahl.chess.knight.cluster.management.ClusterNode;
import com.isahl.chess.knight.cluster.management.ClusterStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClusterManagementServiceTest {

  private ClusterManagementService manager;

  @BeforeEach
  void setUp() {
    manager = new ClusterManagementServiceImpl();
  }

  @Test
  void testAddNode() throws Exception {
    ClusterNode node = new ClusterNode("node1", "192.168.1.10", 9000);

    Boolean result = manager.addNode(node).join();

    assertTrue(result);
    assertEquals(1, manager.getMembers().size());
  }

  @Test
  void testRemoveNode() throws Exception {
    ClusterNode node = new ClusterNode("node1", "192.168.1.10", 9000);
    manager.addNode(node).join();

    Boolean result = manager.removeNode("node1").join();

    assertTrue(result);
    assertEquals(0, manager.getMembers().size());
  }

  @Test
  void testGetStatus() {
    ClusterNode node1 = new ClusterNode("node1", "192.168.1.10", 9000);
    ClusterNode node2 = new ClusterNode("node2", "192.168.1.11", 9000);
    manager.addNode(node1).join();
    manager.addNode(node2).join();

    ClusterStatus status = manager.getStatus();

    assertEquals(2, status.getClusterSize());
    assertEquals(2, status.getQuorumSize());
  }

  @Test
  void testGetMembers() {
    ClusterNode node1 = new ClusterNode("node1", "192.168.1.10", 9000);
    ClusterNode node2 = new ClusterNode("node2", "192.168.1.11", 9000);

    manager.addNode(node1).join();
    manager.addNode(node2).join();

    List<ClusterNode> members = manager.getMembers();

    assertEquals(2, members.size());
    assertTrue(members.contains(node1));
    assertTrue(members.contains(node2));
  }

  @Test
  void testDuplicateNode() {
    ClusterNode node = new ClusterNode("node1", "192.168.1.10", 9000);

    manager.addNode(node).join();
    Boolean result = manager.addNode(node).join();

    assertFalse(result);
    assertEquals(1, manager.getMembers().size());
  }

  @Test
  void testRemoveNonExistentNode() {
    Boolean result = manager.removeNode("nonexistent").join();

    assertFalse(result);
  }
}
