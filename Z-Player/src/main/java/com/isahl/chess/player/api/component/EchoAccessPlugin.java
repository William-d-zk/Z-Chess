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

package com.isahl.chess.player.api.component;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.player.api.model.EchoDo;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.ISessionManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EchoAccessPlugin
        implements IAccessService
{
    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    @Override
    public boolean isHandleProtocol(IProtocol protocol)
    {
        return protocol.serial() == EchoDo._SERIAL;
    }

    @Override
    public List<? extends IControl> handle(ISessionManager manager, ISession session, IControl content)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ITriple onLink(ISessionManager manager, ISession session, IControl input)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onOffline(ISession session)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ITriple> onConsistencyResult(ISessionManager manager,
                                             long origin,
                                             IProtocol consensusBody,
                                             boolean isConsistency)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consume(IProtocol request)
    {
        EchoDo echo = (EchoDo) request;
        String content = echo.getContent();
        _Logger.debug("echo: [ %s ]", content);
    }
}