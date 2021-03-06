/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * @author william.d.zk
 * 
 * @date 2020/2/21
 *       Raft 集群的拓扑关系
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RaftGraph
{
    private final NavigableMap<Long,
                               RaftMachine> _NodeMap = new ConcurrentSkipListMap<>();

    public boolean contains(long peerId)
    {
        return _NodeMap.containsKey(peerId);
    }

    @JsonIgnore
    public RaftMachine getMachine(long peerId)
    {
        return _NodeMap.get(peerId);
    }

    /**
     * 删除过程只能forEach
     *
     * @param peers
     */
    public void remove(long... peers)
    {
        if (peers != null) {
            for (long peer : peers) {
                _NodeMap.remove(peer);
            }
        }
    }

    public NavigableMap<Long,
                        RaftMachine> getNodeMap()
    {
        return _NodeMap;
    }

    public void setNodeMap(Map<Long,
                               RaftMachine> map)
    {
        _NodeMap.putAll(map);
    }

    public void append(RaftMachine machine)
    {
        _NodeMap.put(machine.getPeerId(), machine);
    }

    public void merge(RaftGraph other)
    {
        Objects.requireNonNull(other);
        _NodeMap.putAll(other._NodeMap);
    }

    @JsonIgnore
    public boolean isMajorAcceptCandidate(long candidate, long term)
    {
        return _NodeMap.values()
                       .stream()
                       .filter(machine -> machine.getTerm() == term && machine.getCandidate() == candidate)
                       .count() > _NodeMap.size() / 2;
    }

    @JsonIgnore
    public boolean isMajorAcceptLeader(long leader, long term, long index)
    {
        return _NodeMap.values()
                       .stream()
                       .filter(machine -> machine.getTerm() == term
                                          && machine.getMatchIndex() >= index
                                          && machine.getLeader() == leader)
                       .count() > _NodeMap.size() / 2;
    }

    public boolean isMinorReject(long candidate, long term)
    {
        return _NodeMap.values()
                       .stream()
                       .filter(machine -> machine.getTerm() >= term && machine.getCandidate() != candidate)
                       .count() <= _NodeMap.size() / 2;
    }

}
