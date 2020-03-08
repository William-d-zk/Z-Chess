/*
 * MIT License                                                                    
 *                                                                                
 * Copyright (c) 2016~2020 Z-Chess                                                
 *                                                                                
 * Permission is hereby granted, free of charge, to any person obtaining a copy   
 * of this software and associated documentation files (the "Software"), to deal  
 * in the Software without restriction, including without limitation the rights   
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell      
 * copies of the Software, and to permit persons to whom the Software is          
 * furnished to do so, subject to the following conditions:                       
 *                                                                                
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.                                
 *                                                                                
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR     
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,       
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE    
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER         
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,  
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE  
 * SOFTWARE.                                                                      
 */

package com.tgx.chess.cluster.raft.model;

import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author william.d.zk
 * @date 2020/2/21
 *       Raft 集群的拓扑关系
 */
public class RaftGraph
{
    private final NavigableMap<Long,
                               RaftMachine> _NodeMap = new ConcurrentSkipListMap<>();

    public RaftGraph()
    {
    }

    public boolean contains(long peerId)
    {
        return _NodeMap.containsKey(peerId);
    }

    @JsonIgnore
    public RaftMachine getNode(long peerId)
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

    public void setNodeMap(NavigableMap<Long,
                                        RaftMachine> map)
    {
        _NodeMap.putAll(map);
    }

    public void append(long peerId, long term, long index, long candidate, long leader)
    {
        if (_NodeMap.computeIfPresent(peerId, (key, old) -> new RaftMachine(term, index, candidate, leader)) == null) {
            _NodeMap.put(peerId, new RaftMachine(term, index, candidate, leader));
        }
    }

    public void merge(RaftGraph other)
    {
        if (other != null) _NodeMap.putAll(other._NodeMap);
    }

    public void apply(long selfId, long logIndex, RaftGraph other)
    {
        other._NodeMap.entrySet()
                      .stream()
                      // 仅接受自己没有的peer 状态机更新。  
                      .filter(entry -> entry.getKey() != selfId && !_NodeMap.containsKey(entry.getKey()))
                      .forEach(entry ->
                      {
                          RaftMachine old = entry.getValue();
                          _NodeMap.put(entry.getKey(),
                                       new RaftMachine(old.getTerm(),
                                                       logIndex + 1,
                                                       old.getCandidate(),
                                                       old.getLeader()));
                      });
    }
}
