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

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.env.ZUID;
import com.isahl.chess.knight.raft.component.ClusterFactory;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.events.server.ILinkCustom;
import com.isahl.chess.queen.io.core.features.cluster.IConsistent;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LinkCustom
        implements ILinkCustom
{
    private final Logger _Logger = Logger.getLogger("endpoint.pawn." + getClass().getSimpleName());

    private final List<IAccessService> _AccessServices;

    @Autowired
    public LinkCustom(List<IAccessService> accessAdapters)
    {
        _AccessServices = accessAdapters;
    }

    /**
     * @param manager session 管理器f
     * @param session 当前处理的 session
     * @param input   收到的消息
     * @return first | 当前Link链路上需要返回的结果，second | 需要进行一致性处理的结果
     */
    @Override
    public ITriple handle(IManager manager, ISession session, IControl input)
    {
        for(IAccessService service : _AccessServices) {
            if(service.isHandleProtocol(input)) {
                return service.onLink(manager, session, input);
            }
        }
        return null;
    }

    @Override
    public List<ITriple> notify(IManager manager, IControl request, long origin, boolean isConsistency)
    {
        for(IAccessService service : _AccessServices) {
            if(service.isHandleProtocol(request)) {
                return service.onConsistencyResult(manager, origin, request, isConsistency);
            }
        }
        return null;
    }

    @Override
    public void close(ISession session)
    {
        if((ZUID.TYPE_MASK & session.getIndex()) == ZUID.TYPE_CONSUMER) {
            for(IAccessService service : _AccessServices) {
                service.onOffline(session);
            }
        }
        try {
            session.close();
        }
        catch(Throwable e) {
            _Logger.warning("session[ %#x ] close", session.getIndex(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IProtocol> T adjudge(IConsistent consistency, ISession session)
    {
        _Logger.debug("link custom by leader %s", consistency);
        switch(consistency.serial()) {
            case 0x76, 0x79 -> {
                IProtocol consensusBody = ClusterFactory.oneOf(consistency._sub(), consistency.payload());
                return (T) consensusBody;
            }
            default -> {
                return null;
            }
        }
    }
}
