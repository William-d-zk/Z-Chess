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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;

import com.isahl.chess.king.base.inf.IReset;
import com.isahl.chess.king.base.inf.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.king.topology.ZUID;
import com.isahl.chess.queen.db.inf.IStorage;

@Configuration
@ConfigurationProperties(prefix = "z.chess.raft")
@PropertySource("classpath:raft.properties")
public class ZRaftConfig
        implements
        IRaftConfig,
        IReset
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    public Uid getUid()
    {
        return uid;
    }

    @PostConstruct
    public void loadLocal() throws IOException
    {
        String hostname = InetAddress.getLocalHost()
                                     .getHostName();
        final Set<Triple<Long,
                         String,
                         Integer>> _NodeSet = new TreeSet<>();
        if (peers != null && !peers.isEmpty()) {
            _NodeSet.addAll(convert(peers));
        }
        if (learners != null && !learners.isEmpty()) {
            _NodeSet.addAll(convert(learners));
        }
        if (!_NodeSet.isEmpty()) {
            for (Iterator<Triple<Long,
                                 String,
                                 Integer>> nIt = _NodeSet.iterator(); nIt.hasNext();)
            {
                Triple<Long,
                       String,
                       Integer> triple = nIt.next();
                String host = triple.getSecond();
                if (hostname.equalsIgnoreCase(host)) {
                    //no support multi boot on same host;
                    if (!isInCongress() && mPeerBind == null) {
                        mPeerBind = triple;
                        long peerId = triple.getFirst();
                        if (peerId < 0) {
                            uid.setNodeId(seq);
                            triple.setFirst(createZUID().getPeerId());
                        }
                    }
                    else {
                        _Logger.warning("duplicate peer:%#x,%s:%d",
                                        triple.getFirst(),
                                        triple.getSecond(),
                                        triple.getThird());
                        pIt.remove();
                        continue;
                    }
                }
                seq++;
            }
        }
        if (learners != null && !learners.isEmpty())

        {
            int seq = 0;
            for (Iterator<Triple<Long,
                                 String,
                                 Integer>> lIt = learners.listIterator(); lIt.hasNext();)
            {
                Triple<Long,
                       String,
                       Integer> triple = lIt.next();
                String host = triple.getSecond();
                if (hostname.equalsIgnoreCase(host)) {
                    if (!isInCongress() && mLearnerBind == null) {
                        mLearnerBind = triple;
                        long learnerId = triple.getFirst();
                        if (learnerId < 0) {
                            uid.setNodeId(seq);
                            triple.setFirst(createZUID().getPeerId());
                        }
                        clusterMode = true;
                    }
                    else {
                        _Logger.warning("duplicate learner %#x,%s:%d",
                                        triple.getFirst(),
                                        triple.getSecond(),
                                        triple.getThird());
                        lIt.remove();
                        continue;
                    }
                    seq++;
                }
            }
        }
        clusterMode = inCongress || clusterMode;
        if (isClusterMode()) {
            if (gates != null && !gates.isEmpty()) {
                for (Iterator<Triple<Long,
                                     String,
                                     Integer>> gIt = gates.listIterator(); gIt.hasNext();)
                {
                    Triple<Long,
                           String,
                           Integer> triple = gIt.next();
                    String host = triple.getSecond();
                    if (hostname.equalsIgnoreCase(host)) {
                        if (!isGateNode() && mGateBind == null) {
                            mGateBind = triple;
                            beGate = true;
                            triple.setFirst(createZUID().getPeerId());
                        }
                        else {
                            _Logger.warning("duplicate gate:%#x,%s:%d",
                                            triple.getFirst(),
                                            triple.getSecond(),
                                            triple.getThird());
                            gIt.remove();
                        }
                    }
                }
            }
            if (!isGateNode()) {
                _Logger.info("the node %s isn't gate", hostname);
            }
        }
    }

    @Override
    public ZUID createZUID()
    {
        return zuid == null ? zuid = new ZUID(getUid().getIdcId(),
                                              getUid().getClusterId(),
                                              getUid().getNodeId(),
                                              getUid().getType())
                            : zuid;
    }

    @Override
    public void update(IRaftConfig source) throws IOException
    {
        uid = source.getUid();
        peers.clear();
        peers.addAll(source.getPeers()
                           .stream()
                           .map(t ->
                           {
                               long pId = t.getFirst();
                               String host = t.getSecond();
                               int port = t.getThird();
                               return new Triple<>(pId, host, port);
                           })
                           .collect(Collectors.toList()));
        gates.clear();
        gates.addAll(source.getGates()
                           .stream()
                           .map(t ->
                           {
                               long pId = t.getFirst();
                               String host = t.getSecond();
                               int port = t.getThird();
                               return new Triple<>(pId, host, port);
                           })
                           .collect(Collectors.toList()));
        reset();
        loadLocal();
    }

    @Override
    public void reset()
    {
        mInCongress = false;
        mBeGate = false;
        mClusterMode = false;
        mGateBind = null;
        mPeerBind = null;
        mLearnerBind = null;
    }

    @Override
    public void changeTopology(ITriple peer, IStorage.Operation operation)
    {
        /*
         * 总量有限
         */
        boolean present = false;
        loop:
        {
            for (Iterator<Triple<Long,
                                 String,
                                 Integer>> it = peers.listIterator(); it.hasNext();)
            {
                Triple<Long,
                       String,
                       Integer> p = it.next();
                present = present
                          || p.getFirst()
                              .equals(peer.getFirst());
                switch (operation)
                {
                    case OP_APPEND ->
                        {
                            if (present) {
                                break loop;
                            }
                        }
                    case OP_REMOVE ->
                        {
                            if (present) {
                                it.remove();
                                learners.add();
                            }
                        }
                    case OP_MODIFY ->
                        {
                            if (present) {
                                it.remove();
                            }
                        }

                }
            }
            if (!present && operation == IStorage.Operation.OP_APPEND) {
                peers.add(peer);
            }
            if (present && operation == IStorage.Operation.OP_MODIFY) {
                peers.add(peer);
            }
        }
    }

    @Override
    public void changeGate(ITriple gate, IStorage.Operation operation)
    {

    }

    public void setUid(Uid uid)
    {
        this.uid = uid;
    }

    @Override
    public List<ITriple> getPeers()
    {
        return new ArrayList<>(peers);
    }

    public void setPeers(List<String> peers)
    {
        this.peers = peers;
    }

    private Uid uid;

    private List<String> peers;
    private List<String> gates;
    private List<String> learners;
    private ZUID         zuid;
    private Duration     electInSecond;
    private Duration     snapshotInSecond;
    private Duration     heartbeatInSecond;
    private Duration     clientSubmitInSecond;
    private DataSize     snapshotMinSize;
    private DataSize     snapshotFragmentMaxSize;
    private int          maxSegmentSize;

    private boolean            mInCongress;
    private boolean            mBeGate;
    private boolean            mClusterMode;
    private Map<Long,
                ITriple>       mClusterNodeMap;
    private ITriple            mPeerBind;
    private ITriple            mLearnerBind;
    private ITriple            mGateBind;
    private String             mBaseDir;

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
    public List<ITriple> getGates()
    {
        return new ArrayList<>(gates);
    }

    @Override
    public ITriple getPeerBind()
    {
        return mPeerBind;
    }

    @Override
    public ITriple getGateBind()
    {
        return mGateBind;
    }

    public void setGates(List<String> gates)
    {
        this.gates = gates;
    }

    private List<Triple<Long,
                        String,
                        Integer>> convert(List<String> content)
    {
        return content.stream()
                      .map(this::convert1)
                      .distinct()
                      .sorted()
                      .collect(Collectors.toList());
    }

    private Triple<Long,
                   String,
                   Integer> convert1(String content)
    {
        String[] split = content.split(":", 2);
        return new Triple<>(-1L, split[0], Integer.parseInt(split[1]));
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

    @Override
    public ITriple getLearnerBind()
    {
        return mLearnerBind;
    }

    public void setLearners(List<String> learners)
    {
        this.learners = learners;
    }
}
