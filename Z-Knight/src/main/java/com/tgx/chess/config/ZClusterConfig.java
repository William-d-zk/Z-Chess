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

package com.tgx.chess.config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.unit.DataSize;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tgx.chess.bishop.ZUID;
import com.tgx.chess.bishop.biz.config.IClusterConfig;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;

@Configuration
@ConfigurationProperties(prefix = "z.chess.cluster")
@PropertySource("classpath:cluster.properties")
public class ZClusterConfig
        implements
        IClusterConfig
{
    private final Logger _Logger = Logger.getLogger(ZClusterConfig.class.getSimpleName());

    public Uid getUid()
    {
        return uid;
    }

    @PostConstruct
    void initUid() throws IOException
    {
        if (peerTest != null) {
            String peerTestHost = peerTest.getFirst();
            InetSocketAddress peerTestAddr = new InetSocketAddress(peerTestHost, peerTest.getSecond());
            try (Socket socket = new Socket()) {
                socket.connect(peerTestAddr, 3000);
                if (!socket.isConnected()) { throw new RuntimeException("peer test connect failed"); }
                InetSocketAddress localAddr = (InetSocketAddress) socket.getLocalSocketAddress();
                String localHostStr = localAddr.getHostString();
                _Logger.info("local host:%s", localHostStr);
                bind.setFirst(localHostStr);
                setNodeId(localHostStr);
            }
        }
    }

    @Override
    public ZUID createZUID(boolean withType)
    {
        return zuid == null ? zuid = new ZUID(getUid().getIdcId(),
                                              getUid().getClusterId(),
                                              getUid().getNodeId(),
                                              (withType ? getUid().getType()
                                                        : 0))
                            : zuid;
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

    private Uid                   uid;
    private List<IPair>           peers;
    private List<IPair>           gates;
    private Pair<String,
                 Integer>         bind;
    private ZUID                  zuid;
    private Duration              electInSecond;
    private Duration              snapshotInSecond;
    private Duration              heartbeatInSecond;
    private DataSize              snapshotMinSize;
    private IPair                 peerTest;
    private IPair                 gateTest;

    @Override
    public List<IPair> getGates()
    {
        return gates;
    }

    @Override
    public IPair getBind()
    {
        return bind;
    }

    @Override
    public IPair getPeerTest()
    {
        return peerTest;
    }

    @Override
    public IPair getGateTest()
    {
        return gateTest;
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
                      .collect(Collectors.toList());
    }

    public void setBind(String bind)
    {
        String[] split = bind.split(":", 2);
        this.bind = new Pair<>(split[0], Integer.parseInt(split[1]));
    }

    public void setPeerTest(String test)
    {
        String[] split = test.split(":", 2);
        peerTest = new Pair<>(split[0], Integer.parseInt(split[1]));
    }

    public void setGateTest(String test)
    {
        String[] split = test.split(":", 2);
        gateTest = new Pair<>(split[0], Integer.parseInt(split[1]));
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
    public Duration getHeartbeatInSecond()
    {
        return heartbeatInSecond;
    }

    public void setHeartbeatInSecond(Duration heartbeatInSecond)
    {
        this.heartbeatInSecond = heartbeatInSecond;
    }

    @JsonIgnore
    private void setNodeId(String localHostStr)
    {
        boolean set = false;
        for (int i = 0, size = peers.size(); i < size; i++) {
            if (peers.get(i)
                     .getFirst()
                     .equals(localHostStr))
            {
                uid.setNodeId(i);
                set = true;
                _Logger.info("node-id:%d", i);
                break;
            }
        }
        if (!set) {
            _Logger.warning("no set node-id,Learner?");
        }
    }
}
