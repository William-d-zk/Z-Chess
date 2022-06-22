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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isahl.chess.knight.raft.features.IRaftMachine;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static com.isahl.chess.knight.raft.model.RaftState.CLIENT;
import static com.isahl.chess.knight.raft.model.RaftState.JOINT;
import static java.lang.String.format;

/**
 * @author william.d.zk
 * @date 2020/2/21
 * Raft 集群的拓扑关系
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RaftGraph
{
    private final NavigableMap<Long, IRaftMachine> _Peers;
    private final NavigableMap<Long, IRaftMachine> _Members;

    private RaftGraph()
    {
        _Peers = new ConcurrentSkipListMap<>();
        _Members = new ConcurrentSkipListMap<>();
    }

    public IRaftMachine get(long peerId)
    {
        return _Members.get(peerId);
    }

    public NavigableMap<Long, IRaftMachine> getPeers()
    {
        return _Peers;
    }

    public void append(IRaftMachine machine)
    {
        _Peers.putIfAbsent(machine.peer(), machine);
        if(!machine.isInState(CLIENT)) {
            _Members.putIfAbsent(machine.peer(), machine);
        }
    }

    public void resetTo(RaftGraph other)
    {
        _Members.clear();
        _Peers.clear();
        _Members.putAll(other._Members);
        _Peers.putAll(other._Peers);
        other._Members.clear();
        other._Peers.clear();
    }

    public boolean isMajorAccept(long candidate, long term)
    {
        return _Members.values()
                       .stream()
                       .filter(machine->machine.term() <= term && machine.candidate() == candidate)
                       .count() > _Members.size() / 2;
    }

    public boolean isMajorReject(long candidate, long term)
    {
        return _Members.values()
                       .stream()
                       .filter(machine->machine.term() >= term && machine.candidate() != candidate)
                       .count() > _Members.size() / 2;
    }

    public boolean isMajorAccept(long accept)
    {
        return _Members.values()
                       .stream()
                       .filter(machine->machine.accept() >= accept)
                       .count() > _Members.size() / 2;
    }

    public boolean isMajorConfirm()
    {
        return _Members.values()
                       .stream()
                       .filter(machine->machine.isInState(JOINT))
                       .count() > _Members.size() / 2;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("{");
        _Members.forEach((k, v)->sb.append(format("#%x->%s\\n", k, v.toPrimary())));
        return sb.append('}')
                 .toString();
    }

    public static RaftGraph create()
    {
        return new RaftGraph();
    }

    /**
     * 联合一致发生时old_leader与new_leader结果上是相同的
     * old_leader 必然包含在 joint_graph 中, 由raft的变更流程保障
     * old_leader 发布转换状态指令,  joint_graph 开始执行选举流程。
     * <p>
     * self ∈ left && self ∈ right 返回并集
     * self ∈ left && self ∉ right 返回 left
     * self ∉ left && self ∈ right 返回 right
     * self ∉ left && self ∉ right 返回 null
     */
    public static Map<Long, IRaftMachine> join(long self, RaftGraph left, RaftGraph right)
    {
        if(!left._Members.isEmpty() && !right._Members.isEmpty()) {
            if(left._Members.containsKey(self) && right._Members.containsKey(self)) {
                Map<Long, IRaftMachine> union = new TreeMap<>();
                left._Members.forEach(union::putIfAbsent);
                right._Members.forEach(union::putIfAbsent);
                return union;
            }
        }
        else if(left._Members.isEmpty() && !right._Members.isEmpty() && right._Members.containsKey(self)) {
            return right._Members;
        }
        else if(right._Members.isEmpty() && !left._Members.isEmpty() && left._Members.containsKey(self)) {
            return left._Members;
        }
        return null;
    }

}
