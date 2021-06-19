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

import com.isahl.chess.king.base.inf.IPair;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.topology.ZUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "z.chess.raft")
@PropertySource("classpath:raft.properties")
public class ZRaftConfig
        implements
        IRaftConfig
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    public Uid getUid()
    {
        return uid;
    }

    @PostConstruct
    public void load() throws IOException
    {
        String hostname = InetAddress.getLocalHost()
                                     .getHostName();
        if (peers != null && !peers.isEmpty()) {
            int seq = 0;
            for (Iterator<IPair> pIt = peers.listIterator(); pIt.hasNext();) {
                IPair pair = pIt.next();
                String host = pair.getFirst();
                if (hostname.equalsIgnoreCase(host)) {
                    //no support multi boot on same host;
                    if (!isInCongress() && mPeerBind == null) {
                        mPeerBind = pair;
                        uid.setNodeId(seq);
                    }
                    else {
                        _Logger.warning("duplicate peer:%s", pair);
                        pIt.remove();
                        continue;
                    }
                }
                seq++;
            }
        }
        clusterMode = inCongress || clusterMode;
        if (isClusterMode()) {
            if (gates != null && !gates.isEmpty()) {
                for (Iterator<IPair> gIt = gates.listIterator(); gIt.hasNext();) {
                    IPair gate = gIt.next();
                    String host = gate.getFirst();
                    if (hostname.equalsIgnoreCase(host)) {
                        if (!isGateNode() && mGateBind == null) {
                            mGateBind = gate;
                            beGate = true;
                        }
                        else {
                            _Logger.warning("duplicate gate:%s", gate);
                            gIt.remove();
                        }
                    }
                }
            }
            if (!isGateNode()) {
                _Logger.info("the node %s isn't gate", hostname);
            }
        }
        createZUID();
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
        peers.addAll(source.getPeers());
        gates.clear();
        gates.addAll(source.getGates());
        load();
    }

    public void setUid(Uid uid)
    {
        this.uid = uid;
    }

    public List<IPair> getPeers()
    {
        return peers;
    }

    public void setPeers(List<String> peers)
    {
        this.peers = convert(peers);
    }

    private Uid         uid;
    private List<IPair> peers;
    private List<IPair> gates;
    private ZUID        zuid;
    private Duration    electInSecond;
    private Duration    snapshotInSecond;
    private Duration    heartbeatInSecond;
    private Duration    clientSubmitInSecond;
    private DataSize    snapshotMinSize;
    private DataSize    snapshotFragmentMaxSize;
    private boolean     inCongress;
    private boolean     beGate;
    private boolean     clusterMode;
    private int         maxSegmentSize;

    private IPair  mGateBind;
    private IPair  mPeerBind;
    private String mBaseDir;

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
    public List<IPair> getGates()
    {
        return gates;
    }

    @Override
    public IPair getPeerBind()
    {
        return mPeerBind;
    }

    @Override
    public IPair getGateBind()
    {
        return mGateBind;
    }

    public void setGates(List<String> gates)
    {
        this.gates = convert(gates);
    }

    private List<IPair> convert(List<String> content)
    {
        return content.stream()
                      .map(str ->
                      {
                          String[] split = str.split(":", 2);
                          return new Pair<>(split[0], Integer.parseInt(split[1]));
                      })
                      .distinct()
                      .sorted()
                      .collect(Collectors.toList());
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
        return inCongress;
    }

    public void setInCongress(boolean in)
    {
        inCongress = in;
    }

    @Override
    public boolean isClusterMode()
    {
        return clusterMode;
    }

    public void setClusterMode(boolean mode)
    {
        clusterMode = mode;
    }

    @Override
    public boolean isGateNode()
    {
        return beGate;
    }
}
