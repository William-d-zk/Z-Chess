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

import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.model.RaftConfig;
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.model.IStorage.Operation;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import static com.isahl.chess.knight.raft.model.RaftState.CLIENT;
import static com.isahl.chess.knight.raft.model.RaftState.FOLLOWER;

@Configuration
@ConfigurationProperties(prefix = "z.chess.raft")
@PropertySource("classpath:raft.properties")
public class ZRaftConfig
        implements IRaftConfig
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    private final Map<Long, RaftNode> _PeerMap = new TreeMap<>();
    private final Map<Long, RaftNode> _GateMap = new TreeMap<>();
    private final Map<Long, RaftNode> _NodeMap = new TreeMap<>();

    private final static String FILE_NAME = "raft.json";

    private ZUID       mZUid;
    private RaftNode   mPeerBind;
    private String     mBaseDir;
    private RaftConfig mConfig;

    public ZRaftConfig()
    {
    }

    private boolean storage(boolean overwrite)
    {
        try {
            File file = new File(mBaseDir + File.separator + FILE_NAME);
            boolean exists = file.exists();
            if(exists && overwrite || !exists && file.createNewFile()) {
                JsonUtil.writeValueWithFile(mConfig, file);
            }
            return exists;
        }
        catch(IOException | ZException e) {
            _Logger.warning("config file create & write ", e);
        }
        return false;
    }

    @PostConstruct
    private void load() throws IOException
    {
        String hostname = InetAddress.getLocalHost()
                                     .getHostName();
        if(!storage(false)) {
            //no exists → load default from package's inner-properties
            getUid().setNodeId(0);
            _Logger.info("load default → local properties");
        }
        else {
            //load custom define
            File file = new File(mBaseDir + File.separator + FILE_NAME);
            mConfig = JsonUtil.readValue(file, RaftConfig.class);
            _Logger.info("load exists config file");
        }
        // define self-node-id
        Map<Integer, String> nodes = mConfig.getNodes();
        if(nodes == null || nodes.isEmpty()) {
            throw new ZException("cluster's nodes is empty");
        }
        else {
            int nodeId = -1;
            for(Iterator<Map.Entry<Integer, String>> nIt = nodes.entrySet()
                                                                .iterator(); nIt.hasNext(); ) {
                Map.Entry<Integer, String> entry = nIt.next();
                RaftNode node = toNode(entry.getValue());
                if(hostname.equalsIgnoreCase(node.getHost())) {
                    if(nodeId < 0) {
                        getUid().setNodeId(nodeId = entry.getKey());
                    }
                    else {
                        _Logger.warning("duplicate node: %s", node.getHost());
                        nIt.remove();
                    }
                }
                else {
                    _Logger.debug("node: %s", node.getHost());
                }
            }
            if(nodeId < 0) {
                _Logger.warning("hostname [ %s ] isn't one of nodes", hostname);
                getUid().setNodeId(127);
            }
            else {
                getUid().setNodeId(nodeId);
            }
            ZUID zuid = getZUID();
            nodes.forEach((k, v)->_NodeMap.put(zuid.getPeerIdByNode(k), toNode(v)));
        }
        // define self - peer bind & load peers graph
        Map<Integer, String> peersIn = mConfig.getPeers();
        if(peersIn != null && !peersIn.isEmpty()) {
            peersIn.forEach((i, s)->{
                RaftNode peer = toNode(s);
                peer.setState(FOLLOWER.getCode() | CLIENT.getCode());
                if(hostname.equalsIgnoreCase(peer.getHost())) {
                    if(mPeerBind == null) {
                        //RaftNode 需要对比 host:port 当配置中出现相同当host/port 不同时需要排除
                        peer.setId(getZUID().getPeerId());
                        mPeerBind = peer;
                        _Logger.info("peer: %s ", mPeerBind);
                    }
                    else {
                        _Logger.warning("duplicate peer: %s", peer);
                    }
                }
                else {
                    peer.setId(getZUID().getPeerIdByNode(i));
                }
                _PeerMap.put(peer.getId(), peer);
            });
        }
        // define self gate bind & load gates graph
        Map<Long, String> gatesIn = mConfig.getGates();
        if(gatesIn != null && !gatesIn.isEmpty()) {
            gatesIn.forEach((l, s)->{
                RaftNode gate = toGateNode(s);
                gate.setId(l);
                if(hostname.equalsIgnoreCase(gate.getHost())) {
                    if(!isGateNode()) {
                        mPeerBind.setGateHost(gate.getGateHost());
                        mPeerBind.setGatePort(gate.getGatePort());
                    }
                    else {
                        _Logger.warning("duplicate gate:%s", gate);
                    }
                }
                _GateMap.put(gate.getId(), gate);
            });
        }
        _Logger.info("the local bind [ %s ]  \npeers:\n %s \ngates:\n %s ", mPeerBind, _PeerMap.values(), _GateMap.values());
    }

    public void setConfig(RaftConfig config)
    {
        mConfig = config;
        if(mConfig != null) {
            if(mConfig.getGates() != null) {
                mConfig.getGates()
                       .forEach((k, v)->_Logger.info("gate-key:%#x → %s", k, v));
            }
            if(mConfig.getPeers() != null) {
                mConfig.getPeers()
                       .forEach((k, v)->_Logger.info("peer-key:%d → %s", k, v));
            }
        }
    }

    private Uid getUid()
    {
        return mConfig.getUid();
    }

    @Override
    public ZUID getZUID()
    {
        return mZUid == null ? mZUid = new ZUID(getUid().getIdcId(), getUid().getClusterId(), getUid().getNodeId(), getUid().getType()) : mZUid;
    }

    @Override
    public void commit()
    {
        storage(true);
    }

    @Override
    public void change(RaftNode delta)
    {
        Objects.requireNonNull(delta);
        if(delta.getId() == -1) {
            throw new IllegalArgumentException(String.format("change topology : delta's id is wrong %#x", delta.getId()));
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
                            if(delta.isInState(RaftState.GATE) && senator.getGateHost() == null) {
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
            _GateMap.remove(delta.getId());
        }
        else if(!present) {
            _PeerMap.put(delta.getId(), delta);
        }
        if(mConfig.getPeers() != null) {
            mConfig.getPeers()
                   .clear();
            _PeerMap.values()
                    .forEach(n->mConfig.getPeers()
                                       .put(ZUID.getNodeId(n.getId()), String.format("%s:%d", n.getHost(), n.getPort())));
        }
        if(mConfig.getGates() != null) {
            mConfig.getGates()
                   .clear();
            _GateMap.values()
                    .forEach(g->mConfig.getGates()
                                       .put(g.getId(), String.format("%s/%s:%d", g.getHost(), g.getGateHost(), g.getGatePort())));
        }
    }

    @Override
    public Map<Long, RaftNode> getPeers()
    {
        return _PeerMap;
    }

    public Map<Long, RaftNode> getNodes()
    {
        return _NodeMap;
    }

    @Override
    public Map<Long, RaftNode> getGates()
    {
        return _GateMap;
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

    private RaftNode toNode(String content)
    {
        String[] split = content.split(":", 2);
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new RaftNode(host, port);
    }

    private RaftNode toGateNode(String content)
    {
        String[] split0 = content.split("/", 2);
        String host = split0[0];
        String gate = split0[1];
        String[] split1 = gate.split(":", 2);
        String gateHost = split1[0];
        int gatePort = Integer.parseInt(split1[1]);
        return new RaftNode(host, gateHost, gatePort);
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
        return isInCongress() || _NodeMap.containsKey(getZUID().getPeerId());
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

    @Override
    public int getSyncBatchMaxSize()
    {
        return mConfig.getSyncBatchMaxSize();
    }
}
