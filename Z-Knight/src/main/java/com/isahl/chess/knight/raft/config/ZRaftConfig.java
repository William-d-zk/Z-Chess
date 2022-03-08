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

package com.isahl.chess.knight.raft.config;

import com.isahl.chess.king.base.features.IReset;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.model.RaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.model.IStorage.Operation;
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

    private final Map<Long, RaftNode> _PeerMap = new TreeMap<>();
    private final Map<Long, RaftNode> _GateMap = new TreeMap<>();

    private ZUID       mZUid;
    private RaftNode   mPeerBind;
    private String     mBaseDir;
    private RaftConfig mConfig;

    public ZRaftConfig()
    {
    }

    @PostConstruct
    public void load() throws IOException
    {
        if(mConfig.getNodes() == null || mConfig.getNodes()
                                                .isEmpty())
        {return;}
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
        Map<Integer, String> peersIn = mConfig.getPeers();
        if(peersIn != null && !peersIn.isEmpty()) {
            peersIn.forEach((i, s)->{
                RaftNode peer = toPeerNode(s, RaftState.FOLLOWER);
                if(hostname.equalsIgnoreCase(peer.getHost())) {
                    if(mPeerBind == null) {
                        //RaftNode 需要对比 host:port 当配置中出现相同当host/port 不同时需要排除
                        peer.setId(getZUID().getPeerId());
                        mPeerBind = peer;
                        _Logger.info("peer: %s ", mPeerBind);
                    }
                    else {
                        _Logger.warning("duplicate peer: %s", peer);
                        return;
                    }
                }
                else {
                    peer.setId(getZUID().getPeerIdByNode(i));
                }
                _PeerMap.put(peer.getId(), peer);
            });
        }
        Map<Long, String> gatesIn = mConfig.getGates();
        if(gatesIn != null && !gatesIn.isEmpty()) {
            gatesIn.forEach((l, s)->{
                RaftNode gate = toGateNode(s, RaftState.GATE);
                gate.setId(l);
                if(hostname.equalsIgnoreCase(gate.getHost())) {
                    if(!isGateNode()) {
                        mPeerBind.setGateHost(gate.getGateHost());
                        mPeerBind.setGatePort(gate.getGatePort());
                    }
                    else {
                        _Logger.warning("duplicate gate:%s", gate);
                        return;
                    }
                }
                _GateMap.put(gate.getId(), gate);
            });
        }
        if(!isGateNode()) {
            _Logger.info("the node [ %s ] isn't gate", hostname);
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
                   .forEach((k, v)->_Logger.info("gate-key:%#x → %s", k, v));
        }
    }

    @Override
    public Uid getUid()
    {
        return mConfig.getUid();
    }

    @Override
    public ZUID getZUID()
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
        _PeerMap.clear();
        _GateMap.clear();
    }

    public void changeTopology(RaftNode delta)
    {
        Objects.requireNonNull(delta);
        if(delta.getId() == -1) {
            throw new IllegalArgumentException(String.format("change topology : delta's id is wrong %#x",
                                                             delta.getId()));
        }
        Operation operation = delta.operation();
        /*
          此处不使用map.computeIf* 的结构是因为判断过于复杂还是for的表达容易理解
         */
        boolean present = false, remove = false;
        LOOP:
        {
            for(RaftNode senator : _PeerMap.values()) {
                present = present || delta.compareTo(senator) == 0;
                switch(operation) {
                    case OP_APPEND -> {
                        if(present) {
                            if(delta.getState() == RaftState.GATE && senator.getGateHost() == null) {
                                senator.setGateHost(delta.getGateHost());
                                senator.setGatePort(delta.getGatePort());
                            }
                            return;
                        }
                    }
                    case OP_REMOVE, OP_MODIFY -> {
                        if(present) {
                            remove = true;
                            break LOOP;//map.put:update delta→present
                        }
                    }
                }
            }
        }
        if(remove) {
            _PeerMap.remove(delta.getId());
        }
        else if(!present) {
            _PeerMap.put(delta.getId(), delta);
        }
    }

    @Override
    public List<RaftNode> getPeers()
    {
        if(_PeerMap.isEmpty()) {return null;}
        return new ArrayList<>(_PeerMap.values());
    }

    @Override
    public List<RaftNode> getGates()
    {
        if(_GateMap.isEmpty()) {return null;}
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

    private RaftNode toPeerNode(String content, RaftState state)
    {
        String[] split = content.split(":", 2);
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new RaftNode(host, port, state);
    }

    private RaftNode toGateNode(String content, RaftState state)
    {
        String[] split0 = content.split("/", 2);
        String host = split0[0];
        String gate = split0[1];
        String[] split1 = gate.split(":", 2);
        String gateHost = split1[0];
        int gatePort = Integer.parseInt(split1[1]);
        RaftNode node = new RaftNode(host, -1, state);
        node.setGateHost(gateHost);
        node.setGatePort(gatePort);
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
        return _PeerMap.get(peerId);
    }
}
