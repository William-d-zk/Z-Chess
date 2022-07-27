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
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.features.model.channels.IActivity;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

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
    public IManager getManager()
    {
        return _ClusterNode;
    }

    @Override
    public IPipeTransfer defaultTransfer()
    {
        return this::logicHandle;
    }

    private List<ITriple> logicHandle(IProtocol content, ISession session)
    {
        List<ITriple> results = null;
        for(IAccessService service : _AccessService) {
            if(!service.isSupported(content)) continue;
            try {
                if(results == null) {
                    results = service.onLogic(getManager(), session, content);
                }
                else {
                    List<ITriple> appends = service.onLogic(getManager(), session, content);
                    if(appends != null && !appends.isEmpty()) results.addAll(appends);
                }
            }
            catch(Exception e) {
                _Logger.warning("logic handle:%s",
                                e,
                                service.getClass()
                                       .getSimpleName());
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            try {
                hook.afterLogic(content, results);
            }
            catch(Exception e) {
                _Logger.warning("hook:%s",
                                e,
                                hook.getClass()
                                    .getSimpleName());
            }
        }
        return results;
    }

    @Override
    public void serviceHandle(IoSerial request)
    {
        for(IAccessService service : _AccessService) {
            if(!service.isSupported(request)) continue;
            try {
                service.consume(request);
            }
            catch(Exception e) {
                _Logger.warning("service handle:%s",
                                e,
                                service.getClass()
                                       .getSimpleName());
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            try {
                hook.afterConsume(request);
            }
            catch(Exception e) {
                _Logger.warning("hook:%s",
                                e,
                                hook.getClass()
                                    .getSimpleName());
            }
        }
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
