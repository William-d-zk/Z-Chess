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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.king.base.disruptor.components.Health;
import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.features.model.channels.IActivity;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.IManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author william.d.zk
 */
public class LogicHandler<T extends IActivity & IManager & IClusterNode>
        implements ILogicHandler
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final T                    _ClusterNode;
    private final IHealth              _Health;
    private final List<IAccessService> _AccessService;
    private final List<IHandleHook>    _HandleHooks;

    public LogicHandler(T cluster, int slot, List<IAccessService> accessAdapters, List<IHandleHook> hooks)
    {
        _ClusterNode = cluster;
        _Health = new Health(slot);
        _AccessService = accessAdapters;
        _HandleHooks = hooks;
    }

    @Override
    public IHealth getHealth()
    {
        return _Health;
    }

    @Override
    public IManager getISessionManager()
    {
        return _ClusterNode;
    }

    @Override
    public List<ITriple> logicHandle(IManager manager, ISession session, IControl content) throws ZException
    {
        List<? extends IControl> pushList = null;
        for(IAccessService service : _AccessService) {
            if(service.isHandleProtocol(content)) {
                pushList = service.handle(manager, session, content);
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            hook.handle(content, pushList);
        }
        return (pushList == null || pushList.isEmpty()) ? null : pushList.stream()
                                                                         .map(cmd->new Triple<>(cmd,
                                                                                                session,
                                                                                                session.getEncoder()))
                                                                         .collect(Collectors.toList());
    }

    @Override
    public void serviceHandle(IProtocol request) throws Exception
    {
        for(IAccessService service : _AccessService) {
            if(service.isHandleProtocol(request)) {
                service.consume(request);
            }
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
