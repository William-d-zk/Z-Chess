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

import com.isahl.chess.bishop.protocol.zchat.model.command.X1F_Exchange;
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
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeFailed;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeService;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.LinkedList;
import java.util.List;

/**
 * @author william.d.zk
 */
public class LogicHandler<T extends IActivity & IExchanger & IClusterNode>
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
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public IExchanger getExchanger()
    {
        return _ClusterNode;
    }

    @Override
    public IPipeTransfer logicTransfer()
    {
        return this::logicHandle;
    }

    public IPipeService serviceTransfer()
    {
        return this::serviceHandle;
    }

    private List<ITriple> logicHandle(IProtocol content, ISession session)
    {
        List<ITriple> results = new LinkedList<>();
        for(IAccessService service : _AccessService) {
            if(service.isSupported(content)) {
                try {
                    service.onLogic(getExchanger(), session, content, results);
                }
                catch(Exception e) {
                    _Logger.warning("logic handle:%s",
                                    e,
                                    service.getClass()
                                           .getSimpleName());
                }
            }
            else if(content.serial() == 0x1F) {
                try {
                    X1F_Exchange x1F = (X1F_Exchange) content;
                    IManager manager = getExchanger();
                    IProtocol body = x1F.deserializeSub(manager.findIoFactoryBySerial(x1F.factory()));
                    ISession ts = manager.findSessionByIndex(x1F.target());
                    if(ts != null) {
                        body.with(ts);
                        service.onExchange(body, results);
                    }
                }
                catch(Exception e) {
                    _Logger.warning("on exchange,%s <- %s",
                                    e,
                                    content,
                                    service.getClass()
                                           .getSimpleName());
                }
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            if(hook.isExpect(content)) {
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
        }
        return results.isEmpty() ? null : results;
    }

    private List<ITriple> serviceHandle(IoSerial request)
    {
        List<ITriple> results = new LinkedList<>();
        for(IAccessService service : _AccessService) {
            if(service.isSupported(request)) {
                try {
                    service.consume(getExchanger(), request, results);
                }
                catch(Exception e) {
                    _Logger.warning("service handle:%s",
                                    e,
                                    service.getClass()
                                           .getSimpleName());
                }
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            if(hook.isExpect(request)) {
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
        return results;
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }


}
