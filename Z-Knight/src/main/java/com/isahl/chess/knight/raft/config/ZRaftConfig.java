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
import com.isahl.chess.knight.raft.model.RaftNode;
import com.isahl.chess.knight.raft.model.RaftState;
import com.isahl.chess.queen.db.inf.IStorage;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "z.chess.raft")
@PropertySource("classpath:raft.properties")
public class ZRaftConfig
        implements IRaftConfig,
                   IReset
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    public Uid getUid()
    {
        return mUid;
    }

    @PostConstruct
    public void loadLocal() throws IOException
    {
        String hostname = InetAddress.getLocalHost().getHostName();
        if(peers != null && !peers.isEmpty()) {
            for(Iterator<Map.Entry<Integer, String>> pIt = peers.entrySet().iterator(); pIt.hasNext(); ) {
                Map.Entry<Integer, String> entry = pIt.next();
                RaftNode node = convert1(entry.getValue(), RaftState.FOLLOWER);
                String host = node.getHost();
                if(hostname.equalsIgnoreCase(host)) {
                    if(mUid.getNodeId() < 0 && !isInCongress() && mPeerBind == null) {
                        //RaftNode 需要对比 host:port 当配置中出现相同当host/port 不同时需要排除
                        mUid.setNodeId(entry.getKey());
                        node.setId(createZUID().getPeerId());
                        mPeerBind = node;
                        mInCongress = true;
                    }
                    else {
                        _Logger.warning("duplicate:%s", node);
                        pIt.remove();
                        continue;
                    }
                    mRaftNodeMap.put(node.getId(), node);
                }
            }
        }
        mClusterMode = mInCongress || mClusterMode;
        if(isClusterMode()) {
            if(gates != null && !gates.isEmpty()) {
                for(String str : gates) {
                    if(hostname.equalsIgnoreCase(str)) {
                        RaftNode gate = convert1(str, RaftState.GATE);
                        if(!isGateNode() && mGateBind == null) {
                            gate.setId(createZUID().getPeerId());
                            mGateBind = gate;
                            mBeGate = true;
                        }
                        else {
                            _Logger.warning("duplicate gate:%s", str);
                        }
                    }

                }
            }
            if(!isGateNode()) {
                _Logger.info("the node %s isn't gate", hostname);
            }
        }
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
    public void update(IRaftConfig source) throws IOException
    {
        reset();
        mUid.from(source.getUid());
        loadLocal();
    }

    @Override
    public void reset()
    {
        mUid.reset();
        mZUid = null;
        mInCongress = false;
        mBeGate = false;
        mClusterMode = false;
        mGateBind = null;
        mPeerBind = null;
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
                for(Iterator<RaftNode> it = mRaftNodeMap.values().iterator(); it.hasNext(); ) {
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
            mRaftNodeMap.put(delta.getId(), delta);
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

    public void setUid(Uid uid)
    {
        this.mUid = uid;
    }

    @Override
    public List<RaftNode> getPeers()
    {
        return new ArrayList<>(mRaftNodeMap.values());
    }

    public void setPeers(Map<Integer, String> peers)
    {
        this.peers = peers;
    }

    private Uid mUid;

    private Map<Integer, String> peers;
    private List<String>         gates;
    private Duration             electInSecond;
    private Duration             snapshotInSecond;
    private Duration             heartbeatInSecond;
    private Duration             clientSubmitInSecond;
    private DataSize             snapshotMinSize;
    private DataSize             snapshotFragmentMaxSize;
    private int                  maxSegmentSize;

    private ZUID                mZUid;
    private boolean             mInCongress;
    private boolean             mBeGate;
    private boolean             mClusterMode;
    private Map<Long, RaftNode> mRaftNodeMap;
    private RaftNode            mPeerBind;
    private RaftNode            mGateBind;
    private String              mBaseDir;

    public int getMaxSegmentSize()
    {
        return maxSegmentSize;
    }

    public void setMaxSegmentSize(int maxSegmentSize)
    {
        this.maxSegmentSize = maxSegmentSize;
    }

    public String getBaseDir()
    {
        return mBaseDir;
    }

    public void setBaseDir(String dir)
    {
        mBaseDir = dir;
    }

    @Override
    public List<RaftNode> getGates()
    {
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

    public void setGates(List<String> gates)
    {
        this.gates = gates;
    }

    private List<RaftNode> convert(List<String> content, RaftState state)
    {
        return content.stream().map(str->convert1(str, state)).distinct().sorted().collect(Collectors.toList());
    }

    private RaftNode convert1(String content, RaftState state)
    {
        String[] split = content.split(":", 2);
        return new RaftNode(split[0], Integer.parseInt(split[1]), state);
    }

    public Duration getElectInSecond()
    {
        return electInSecond;
    }

    public void setElectInSecond(Duration electInSecond)
    {
        this.electInSecond = electInSecond;
    }

    @Override
    public Duration getSnapshotInSecond()
    {
        return snapshotInSecond;
    }

    public void setSnapshotInSecond(Duration snapshotInSecond)
    {
        this.snapshotInSecond = snapshotInSecond;
    }

    @Override
    public long getSnapshotMinSize()
    {
        return snapshotMinSize.toBytes();
    }

    public void setSnapshotMinSize(DataSize snapshotMinSize)
    {
        this.snapshotMinSize = snapshotMinSize;
    }

    @Override
    public long getSnapshotFragmentMaxSize()
    {
        return snapshotFragmentMaxSize.toBytes();
    }

    public void setSnapshotFragmentMaxSize(DataSize snapshotFragmentMaxSize)
    {
        this.snapshotFragmentMaxSize = snapshotFragmentMaxSize;
    }

    @Override
    public Duration getHeartbeatInSecond()
    {
        return heartbeatInSecond;
    }

    public void setHeartbeatInSecond(Duration heartbeatInSecond)
    {
        this.heartbeatInSecond = heartbeatInSecond;
    }

    @Override
    public Duration getClientSubmitInSecond()
    {
        return clientSubmitInSecond;
    }

    public void setClientSubmitInSecond(Duration clientSubmitInSecond)
    {
        this.clientSubmitInSecond = clientSubmitInSecond;
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
