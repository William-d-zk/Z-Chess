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

package com.isahl.chess.pawn.endpoint.device.service;

import com.isahl.chess.king.base.disruptor.event.inf.IHealth;
import com.isahl.chess.king.base.disruptor.processor.Health;
import com.isahl.chess.king.base.exception.ZException;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.cluster.IClusterNode;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.pawn.endpoint.device.spi.IHandleHook;
import com.isahl.chess.queen.event.handler.mix.ILogicHandler;
import com.isahl.chess.queen.io.core.inf.IActivity;
import com.isahl.chess.queen.io.core.inf.IControl;
import com.isahl.chess.queen.io.core.inf.ISession;
import com.isahl.chess.queen.io.core.inf.ISessionManager;

import java.util.List;

/**
 * @author william.d.zk
 */
public class LogicHandler<T extends IActivity & ISessionManager & IClusterNode>
        implements ILogicHandler
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final T                    _Manager;
    private final IHealth              _Health;
    private final List<IAccessService> _AccessService;
    private final List<IHandleHook>    _HandleHooks;

    public LogicHandler(T manager, int slot, List<IAccessService> accessAdapters, List<IHandleHook> hooks)
    {
        _Manager = manager;
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
    public ISessionManager getISessionManager()
    {
        return _Manager;
    }

    @Override
    public IControl[] handle(ISessionManager manager, ISession session, IControl content) throws ZException
    {
        List<? extends IControl> pushList = null;
        for(IAccessService service : _AccessService) {
            if(service.isHandleProtocol(content)) {
                pushList = service.handle(manager, session, content);
                service.clusterHandle(manager, content, _Manager, pushList);
            }
        }
        for(IHandleHook hook : _HandleHooks) {
            hook.handle(content, pushList);
        }
        return (pushList == null || pushList.isEmpty()) ? null : pushList.toArray(new IControl[0]);
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }
}
