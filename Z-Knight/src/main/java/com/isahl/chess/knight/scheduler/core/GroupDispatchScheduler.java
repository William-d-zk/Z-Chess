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

package com.isahl.chess.knight.scheduler.core;

import com.isahl.chess.knight.scheduler.domain.*;
import com.isahl.chess.knight.scheduler.repository.NodeGroupRepository;
import com.isahl.chess.knight.scheduler.repository.SubTaskRepository;
import com.isahl.chess.knight.scheduler.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GroupDispatchScheduler implements GroupScheduler {
  private static final Logger _Logger = LoggerFactory.getLogger(GroupDispatchScheduler.class);

  private final TaskRepository _TaskRepository;
  private final SubTaskRepository _SubTaskRepository;
  private final NodeGroupRepository _NodeGroupRepository;
  private final TaskScheduler _BaseScheduler;

  public GroupDispatchScheduler(
      TaskRepository taskRepository,
      SubTaskRepository subTaskRepository,
      NodeGroupRepository nodeGroupRepository,
      TaskScheduler baseScheduler) {
    _TaskRepository = taskRepository;
    _SubTaskRepository = subTaskRepository;
    _NodeGroupRepository = nodeGroupRepository;
    _BaseScheduler = baseScheduler;
  }

  @Override
  @Transactional
  public Task dispatchToGroup(
      String taskId, String payload, String groupId, List<String> targetNodes, int timeoutSeconds) {
    Optional<NodeGroup> groupOpt = _NodeGroupRepository.findById(groupId);
    if (groupOpt.isEmpty()) {
      _Logger.warn("Group {} not found, using provided nodes", groupId);
      return _BaseScheduler.dispatchTask(taskId, payload, targetNodes, timeoutSeconds);
    }

    NodeGroup group = groupOpt.get();
    _Logger.info(
        "Dispatching task {} to group {} with {} nodes", taskId, groupId, targetNodes.size());
    return _BaseScheduler.dispatchTask(taskId, payload, targetNodes, timeoutSeconds);
  }

  @Override
  @Transactional
  public Task broadcastToGroup(String taskId, String payload, String groupId, int timeoutSeconds) {
    Optional<NodeGroup> groupOpt = _NodeGroupRepository.findById(groupId);
    if (groupOpt.isEmpty()) {
      _Logger.warn("Group {} not found, cannot broadcast", groupId);
      throw new IllegalArgumentException("Group not found: " + groupId);
    }

    NodeGroup group = groupOpt.get();
    List<String> allNodes = group.getNodeIds();
    _Logger.info(
        "Broadcasting task {} to group {} with {} nodes", taskId, groupId, allNodes.size());
    return _BaseScheduler.dispatchTask(taskId, payload, allNodes, timeoutSeconds);
  }

  @Override
  public int getGroupPendingCount(String groupId) {
    Optional<NodeGroup> groupOpt = _NodeGroupRepository.findById(groupId);
    if (groupOpt.isEmpty()) {
      return 0;
    }

    NodeGroup group = groupOpt.get();
    List<SubTask> pendingTasks = _SubTaskRepository.findByStatus(SubTaskStatus.PENDING);
    return (int)
        pendingTasks.stream().filter(st -> group.getNodeIds().contains(st.getTargetNode())).count();
  }
}
