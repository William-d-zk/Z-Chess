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

package com.isahl.chess.knight.scheduler.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scheduler_node_group")
public class NodeGroup {
  @Id private String groupId;

  @Column(nullable = false)
  private String groupName;

  @ElementCollection
  @CollectionTable(name = "node_group_members", joinColumns = @JoinColumn(name = "group_id"))
  @Column(name = "node_id")
  private List<String> nodeIds = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NodeGroupType type;

  @Column(nullable = false)
  private Instant createdAt;

  public NodeGroup() {}

  public NodeGroup(String groupId, String groupName, NodeGroupType type) {
    this.groupId = groupId;
    this.groupName = groupName;
    this.type = type;
    this.createdAt = Instant.now();
  }

  public void addNode(String nodeId) {
    if (!nodeIds.contains(nodeId)) {
      nodeIds.add(nodeId);
    }
  }

  public void removeNode(String nodeId) {
    nodeIds.remove(nodeId);
  }

  public int getNodeCount() {
    return nodeIds.size();
  }

  public String getGroupId() {
    return groupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public List<String> getNodeIds() {
    return nodeIds;
  }

  public NodeGroupType getType() {
    return type;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
