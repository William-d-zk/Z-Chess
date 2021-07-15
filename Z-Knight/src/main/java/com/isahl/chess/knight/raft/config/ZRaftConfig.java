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

package com.isahl.chess.knight.raft.config;

import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.knight.raft.model.RaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.inf.IStorage.Operation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "z.chess.raft")
@PropertySource("classpath:raft.properties")
public class ZRaftConfig
        implements IRaftConfig,
                   IReset
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final Map<Long, RaftNode> _RaftNodeMap = new TreeMap<>();
    private final Map<Long, RaftNode> _GateMap     = new TreeMap<>();

    private ZUID       mZUid;
    private RaftNode   mPeerBind;
    private String     mBaseDir;
    private RaftConfig mConfig;

    @PostConstruct
    public void load() throws IOException
    {
        if(mConfig.getNodes() == null || mConfig.getNodes()
                                                .isEmpty())
        { return; }
        getUid().setNodeId(-1);
        String hostname = InetAddress.getLocalHost()
                                     .getHostName();
        for(Iterator<Map.Entry<Integer, String>> nIt = mConfig.getNodes()
                                                              .entrySet()
                                                              .iterator(); nIt.hasNext(); ) {
            Map.Entry<Integer, String> entry = nIt.next();
            String nodeHost = entry.getValue();
            if(hostname.equalsIgnoreCase(nodeHost)) {
                if(getUid().getNodeId() < 0) {
                    getUid().setNodeId(entry.getKey());
                }
                else {
                    _Logger.warning("duplicate node: %s", nodeHost);
                    nIt.remove();
                }
            }
            else {
                _Logger.info("node: %s", nodeHost);
            }
        }
        if(getUid().getNodeId() < 0) {
            _Logger.warning("hostname [ %s ] isn't one of nodes", hostname);
            getUid().setNodeId(0);
            return;
        }
        if(mConfig.getPeers() != null && !mConfig.getPeers()
                                                 .isEmpty())
        {
            for(Map.Entry<Integer, String> peerEntry : mConfig.getPeers()
                                                              .entrySet()) {
                RaftNode peer = toRaftNode(peerEntry.getValue(), RaftState.FOLLOWER);
                String peerHost = peer.getHost();
                if(hostname.equalsIgnoreCase(peerHost)) {
                    if(mPeerBind == null) {
                        //RaftNode 需要对比 host:port 当配置中出现相同当host/port 不同时需要排除
                        peer.setId(createZUID().getPeerId());
                        mPeerBind = peer;
                        _Logger.info("peer: %s ", mPeerBind);
                    }
                    else {
                        _Logger.warning("duplicate peer: %s", peer);
                        continue;
                    }
                }
                else {
                    peer.setId(createZUID().getPeerIdByNode(peerEntry.getKey()));
                }
                _RaftNodeMap.put(peer.getId(), peer);
            }
        }
        if(isInCongress()) {
            if(mConfig.getGates() != null && !mConfig.getGates()
                                                     .isEmpty())
            {
                for(Map.Entry<Long, String> gateEntry : mConfig.getGates()
                                                               .entrySet()) {
                    RaftNode gate = toRaftNode(gateEntry.getValue(), RaftState.GATE);
                    gate.setId(gateEntry.getKey());
                    if(hostname.equalsIgnoreCase(gate.getHost())) {
                        if(!isGateNode()) {
                            mPeerBind.setGateHost(gate.getGateHost());
                            mPeerBind.setGatePort(gate.getGatePort());
                        }
                        else {
                            _Logger.warning("duplicate gate:%s", gateEntry);
                            continue;
                        }
                    }
                    _GateMap.put(gate.getId(), gate);
                }
            }
            if(!isGateNode()) {
                _Logger.info("the node [ %s ] isn't gate", hostname);
            }
        }
    }

    @Override
    public RaftConfig getConfig()
    {
        return mConfig;
    }

    public void setConfig(RaftConfig config)
    {
        mConfig = config;
        if(mConfig != null && mConfig.getGates() != null) {
            mConfig.getGates()
                   .forEach((k, v)->{
                       _Logger.info("gate-key:%#x → %s", k, v);
                   });
        }
    }

    @Override
    public Uid getUid()
    {
        return mConfig.getUid();
    }

    @Override
    public ZUID createZUID()
    {
        return mZUid == null ? mZUid = new ZUID(getUid().getIdcId(),
                                                getUid().getClusterId(),
                                                getUid().getNodeId(),
                                                getUid().getType()) : mZUid;
    }

    @Override
    public void update(RaftConfig source) throws IOException
    {
        reset();
        setConfig(source);
        load();
    }

    @Override
    public void reset()
    {
        mConfig = null;
        mZUid = null;
        mPeerBind = null;
        _RaftNodeMap.clear();
    }

    @Override
    public void changeTopology(RaftNode delta, Operation operation)
    {
        Objects.requireNonNull(delta);
        Objects.requireNonNull(operation);
        if(delta.getId() == -1) {
            throw new IllegalArgumentException(String.format("change topology : delta's id is wrong %#x",
                                                             delta.getId()));
        }

            /*
            此处不使用map.computeIf* 的结构是因为判断过于复杂
            还是for的表达容易理解
             */
        boolean present = false;
        CHECK:
        {
            LOOP:
            {
                for(RaftNode senator : _RaftNodeMap.values()) {
                    present = present || delta.compareTo(senator) == 0;
                    switch(operation) {
                        case OP_APPEND -> {
                            if(present) {
                                if(delta.getState() == RaftState.GATE && senator.getGateHost() == null) {
                                    senator.setGateHost(delta.getGateHost());
                                    senator.setGatePort(delta.getGatePort());
                                }
                                break CHECK;
                            }
                        }
                        case OP_REMOVE, OP_MODIFY -> {
                            if(present) {
                                break LOOP;//map.put:update delta→present
                            }
                        }
                    }
                }
            }
            if(!present && operation == Operation.OP_REMOVE || operation == Operation.OP_MODIFY) {
                break CHECK;
            }
            _RaftNodeMap.put(delta.getId(), delta);
        }

    }

    @Override
    public List<RaftNode> getPeers()
    {
        if(_RaftNodeMap.isEmpty()) { return null; }
        return new ArrayList<>(_RaftNodeMap.values());
    }

    @Override
    public List<RaftNode> getGates()
    {
        if(_GateMap.isEmpty()) { return null; }
        return new ArrayList<>(_GateMap.values());
    }

    @Override
    public long getMaxSegmentSize()
    {
        return mConfig.getMaxSegmentSize();
    }

    public void setBaseDir(String baseDir)
    {
        mBaseDir = baseDir;
    }

    public String getBaseDir()
    {
        return mBaseDir;
    }

    @Override
    public RaftNode getPeerBind()
    {
        return mPeerBind;
    }

    private RaftNode toRaftNode(String content, RaftState state)
    {
        String[] split = content.split(":", 2);
        String[] split1 = split[0].split("/", 2);
        RaftNode node = new RaftNode(split1[0], Integer.parseInt(split[1]), state);
        if(split1.length == 2) {
            node.setGateHost(split1[1]);
            node.setGatePort(Integer.parseInt(split[1]));
        }
        return node;
    }

    public Duration getElectInSecond()
    {
        return mConfig.getElectInSecond();
    }

    @Override
    public Duration getSnapshotInSecond()
    {
        return mConfig.getSnapshotInSecond();
    }

    @Override
    public long getSnapshotMinSize()
    {
        return mConfig.getSnapshotMinSize();
    }

    @Override
    public long getSnapshotFragmentMaxSize()
    {
        return mConfig.getSnapshotFragmentMaxSize();
    }

    @Override
    public Duration getHeartbeatInSecond()
    {
        return mConfig.getHeartbeatInSecond();
    }

    @Override
    public Duration getClientSubmitInSecond()
    {
        return mConfig.getClientSubmitInSecond();
    }

    @Override
    public boolean isInCongress()
    {
        return mPeerBind != null;
    }

    @Override
    public boolean isClusterMode()
    {
        return isInCongress() || mConfig.isBeLearner();
    }

    @Override
    public boolean isGateNode()
    {
        return mPeerBind != null && mPeerBind.getGateHost() != null;
    }

    @Override
    public RaftNode findById(long peerId)
    {
        return _RaftNodeMap.get(peerId);
    }
}
