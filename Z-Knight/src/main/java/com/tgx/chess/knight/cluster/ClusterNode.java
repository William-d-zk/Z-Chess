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

package com.tgx.chess.knight.cluster;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.zfilter.ZContext;
import com.tgx.chess.king.base.inf.IPair;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.queen.config.IBizIoConfig;
import com.tgx.chess.queen.config.IServerConfig;
import com.tgx.chess.queen.db.inf.IStorage;
import com.tgx.chess.queen.event.inf.IOperator;
import com.tgx.chess.queen.io.core.executor.ClusterCore;
import com.tgx.chess.queen.io.core.inf.IClusterPeer;
import com.tgx.chess.queen.io.core.inf.IConsensus;
import com.tgx.chess.queen.io.core.inf.ISession;
import com.tgx.chess.queen.io.core.inf.ISessionDismiss;
import com.tgx.chess.queen.io.core.manager.MixManager;

@Component
public class ClusterNode
        extends
        MixManager<ZContext>
        implements
        ISessionDismiss<ZContext>,
        IClusterPeer,
        IConsensus
{
    private final Logger _Logger = Logger.getLogger(getClass().getSimpleName());

    @Override
    public void onDismiss(ISession<ZContext> session)
    {
        _Logger.info("dismiss %s", session);
        rmSession(session);
    }

    public ClusterNode(IBizIoConfig config,
                       IServerConfig serverConfig)
    {
        super(config, new ClusterCore<ZContext>(serverConfig));
    }

    @PostConstruct
    void init()
    {
        _Logger.info("load cluster node");
    }

    @Override
    public void close(ISession<ZContext> session, IOperator.Type eventType)
    {

    }

    @Override
    public void addPeer(IPair remote) throws IOException
    {

    }

    @Override
    public void addGate(IPair remote) throws IOException
    {

    }

    @Override
    public <T extends IStorage> void publishConsensus(IOperator.Type type, T content)
    {

    }

}
