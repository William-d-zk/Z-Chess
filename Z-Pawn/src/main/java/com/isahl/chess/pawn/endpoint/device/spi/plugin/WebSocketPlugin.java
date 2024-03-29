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

package com.isahl.chess.pawn.endpoint.device.spi.plugin;

import com.isahl.chess.bishop.protocol.ws.WsContext;
import com.isahl.chess.bishop.protocol.ws.command.X105_Text;
import com.isahl.chess.bishop.protocol.ws.ctrl.X101_HandShake;
import com.isahl.chess.bishop.protocol.ws.ctrl.X104_Pong;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.io.core.features.cluster.IConsistency;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author william.d.zk
 */
@Component
public class WebSocketPlugin
        implements IAccessService
{

    private final Logger _Logger = Logger.getLogger("pawn.endpoint." + getClass().getSimpleName());

    @Override
    public boolean isSupported(IoSerial input)
    {
        return input.serial() >= 0x101 && input.serial() <= 0x105;
    }

    @Override
    public boolean isSupported(ISession session)
    {
        return session.getContext(WsContext.class) != null;
    }

    @Override
    public void onLogic(IExchanger exchanger, ISession session, IProtocol content, List<ITriple> load)
    {
        _Logger.info("web socket recv:[ %s ]", content);

        switch(content.serial()) {
            case 0x101 -> {
                X101_HandShake<?> request = (X101_HandShake<?>) content;
                load.add(Triple.of(request.context()
                                          .getHandshake()
                                          .with(session), session, session.encoder()));
            }
            case 0x102 -> {
                load.add(Triple.of(content, session, session.encoder()));
            }
            case 0x103 -> {
                load.add(Triple.of(new X104_Pong<>().with(session), session, session.encoder()));
            }
            case 0x105 -> {
                load.add(Triple.of(new X105_Text<>(((X105_Text<?>) content).getText() + "OK\n").with(session), session, session.encoder()));
            }
        }
    }

    @Override
    public ITriple onLink(IManager manager, ISession session, IProtocol input)
    {
        return null;
    }

    public List<ITriple> onConsistency(IManager manager, IConsistency backload, IoSerial consensusBody)
    {
        return null;
    }
}
