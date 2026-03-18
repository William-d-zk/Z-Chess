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

import com.isahl.chess.knight.scheduler.domain.SubTask;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TaskPool
{
    private final ConcurrentHashMap<String, SubTask> _PendingTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> _GroupTasks = new ConcurrentHashMap<>();

    public void addTask(SubTask task)
    {
        _PendingTasks.put(task.getSubTaskId(), task);
        String groupId = task.getTargetNode();
        if(groupId != null) {
            _GroupTasks.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(task.getSubTaskId());
        }
    }

    public Optional<SubTask> claim(String nodeId, int maxCount)
    {
        List<String> keys = new ArrayList<>(_PendingTasks.keySet());
        if(keys.isEmpty()) {
            return Optional.empty();
        }

        String firstKey = keys.get(0);
        SubTask task = _PendingTasks.remove(firstKey);
        if(task != null) {
            task.setTargetNode(nodeId);
            return Optional.of(task);
        }
        return Optional.empty();
    }

    public Optional<SubTask> claimFromGroup(String groupId, String nodeId, int maxCount)
    {
        Set<String> groupTaskIds = _GroupTasks.get(groupId);
        if(groupTaskIds == null || groupTaskIds.isEmpty()) {
            return Optional.empty();
        }

        String firstId = groupTaskIds.iterator().next();
        SubTask task = _PendingTasks.remove(firstId);
        if(task != null) {
            groupTaskIds.remove(task.getSubTaskId());
            task.setTargetNode(nodeId);
            return Optional.of(task);
        }
        return Optional.empty();
    }

    public List<SubTask> claimBatch(String nodeId, int maxCount)
    {
        List<SubTask> claimed = new ArrayList<>();
        for(int i = 0; i < maxCount; i++) {
            Optional<SubTask> task = claim(nodeId, 1);
            if(task.isEmpty()) {
                break;
            }
            claimed.add(task.get());
        }
        return claimed;
    }

    public void release(SubTask task)
    {
        task.setTargetNode(null);
        _PendingTasks.put(task.getSubTaskId(), task);
    }

    public int getPendingCount()
    {
        return _PendingTasks.size();
    }

    public int getPendingCountByGroup(String groupId)
    {
        Set<String> groupTaskIds = _GroupTasks.get(groupId);
        if(groupTaskIds == null) {
            return 0;
        }
        return groupTaskIds.size();
    }

    public Set<String> getAllGroups()
    {
        return new HashSet<>(_GroupTasks.keySet());
    }
}
