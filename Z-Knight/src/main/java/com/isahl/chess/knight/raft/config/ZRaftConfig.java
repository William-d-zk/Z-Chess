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
import com.isahl.chess.queen.db.inf.IStorage;
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

    private ZUID       mZUid;
    private boolean    mInCongress;
    private boolean    mBeGate;
    private boolean    mClusterMode;
    private RaftNode   mPeerBind;
    private RaftNode   mGateBind;
    private String     mBaseDir;
    private RaftConfig mConfig;

    @PostConstruct
    public void load() throws IOException
    {
        mClusterMode = mConfig.getNodes() != null && !mConfig.getNodes()
                                                             .isEmpty();
        if(!mClusterMode) { return; }
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
            mClusterMode = false;
            return;
        }
        if(mConfig.getPeers() != null && !mConfig.getPeers()
                                                 .isEmpty())
        {
            for(String peerAddr : mConfig.getPeers()
                                         .values()) {
                RaftNode peer = toRaftNode(peerAddr, RaftState.FOLLOWER);
                String peerHost = peer.getHost();
                if(hostname.equalsIgnoreCase(peerHost)) {
                    if(!isInCongress() && mPeerBind == null) {
                        //RaftNode 需要对比 host:port 当配置中出现相同当host/port 不同时需要排除
                        peer.setId(createZUID().getPeerId());
                        mPeerBind = peer;
                        mInCongress = true;
                    }
                    else {
                        _Logger.warning("duplicate peer: %s", peer);
                        continue;
                    }
                    _RaftNodeMap.put(peer.getId(), peer);
                }
            }
        }
        mClusterMode = mInCongress || mClusterMode;
        if(isClusterMode()) {
            if(mConfig.getGates() != null && !mConfig.getGates()
                                                     .isEmpty())
            {
                for(String gateAddr : mConfig.getGates()) {
                    if(hostname.equalsIgnoreCase(gateAddr)) {
                        RaftNode gate = toRaftNode(gateAddr, RaftState.GATE);
                        if(!isGateNode() && mGateBind == null) {
                            gate.setId(createZUID().getPeerId());
                            mGateBind = gate;
                            mBeGate = true;
                        }
                        else {
                            _Logger.warning("duplicate gate:%s", gateAddr);
                        }
                    }
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
        mInCongress = false;
        mBeGate = false;
        mClusterMode = false;
        mGateBind = null;
        mPeerBind = null;
        _RaftNodeMap.clear();
    }

    @Override
    public void changeTopology(RaftNode delta, IStorage.Operation operation)
    {
        Objects.requireNonNull(delta);
        Objects.requireNonNull(operation);
        if(delta.getId() == -1) {
            throw new IllegalArgumentException(String.format("change topology : delta's id is wrong %#x",
                                                             delta.getId()));
        }
        /*
         * 总量有限
         */
        boolean present = false;
        CHECK:
        {
            LOOP:
            {
                for(Iterator<RaftNode> it = _RaftNodeMap.values()
                                                        .iterator(); it.hasNext(); ) {
                    RaftNode peer = it.next();
                    present = present || delta.compareTo(peer) == 0;
                    switch(operation) {
                        case OP_APPEND -> {
                            if(present) {
                                _Logger.warning("duplicate peer:%s", peer);
                                break CHECK;
                            }
                            break LOOP;
                        }
                        case OP_REMOVE -> {
                            if(present) {
                                it.remove();
                                break CHECK;
                            }
                        }
                        case OP_MODIFY -> {
                            if(present) {
                                it.remove();
                                break LOOP;
                            }
                        }
                    }
                }
            }
            _RaftNodeMap.put(delta.getId(), delta);
        }
    }

    @Override
    public void changeGate(RaftNode delta, IStorage.Operation operation)
    {
        Objects.requireNonNull(delta);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(mPeerBind);
        String hostname = mPeerBind.getHost();
        if(hostname.equalsIgnoreCase(delta.getHost())) {
            mBeGate = true;
            delta.setId(mPeerBind.getId());
            mGateBind = delta;
        }
    }

    @Override
    public List<RaftNode> getPeers()
    {
        if(_RaftNodeMap.isEmpty()) { return null; }
        return new ArrayList<>(_RaftNodeMap.values());
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
    public List<RaftNode> getGates()
    {
        //TODO
        return null;
    }

    @Override
    public RaftNode getPeerBind()
    {
        return mPeerBind;
    }

    @Override
    public RaftNode getGateBind()
    {
        return mGateBind;
    }

    private RaftNode toRaftNode(String content, RaftState state)
    {
        String[] split = content.split(":", 2);
        return new RaftNode(split[0], Integer.parseInt(split[1]), state);
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
        return mInCongress;
    }

    @Override
    public boolean isClusterMode()
    {
        return mClusterMode;
    }

    @Override
    public boolean isGateNode()
    {
        return mBeGate;
    }

}
