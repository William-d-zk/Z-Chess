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
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.player.api.model.EchoDo;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EchoPlugin
        implements IAccessService
{
    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    @Override
    public boolean isSupported(IoSerial input)
    {
        return input.serial() == 0x0D;
    }

    @Override
    public List<ITriple> logicHandle(IManager manager, ISession session, IProtocol content)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ITriple onLink(IManager manager, ISession session, IProtocol input)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onOffline(ISession session)
    {
    }

    @Override
    public List<ITriple> onConsistency(IManager manager, long origin, IoSerial consensusBody)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void consume(IoSerial request)
    {
        EchoDo echo = (EchoDo) request;
    }
}
