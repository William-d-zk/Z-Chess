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

import com.isahl.chess.bishop.protocol.ws.ctrl.X101_HandShake;
import com.isahl.chess.bishop.protocol.ws.ctrl.X103_Ping;
import com.isahl.chess.bishop.protocol.ws.ctrl.X104_Pong;
import com.isahl.chess.bishop.protocol.ws.zchat.model.ctrl.X105_SslHandShake;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.pawn.endpoint.device.spi.IAccessService;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.ISession;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author william.d.zk
 */
@Component
public class WebSocketAccessPlugin
        implements IAccessService
{

    @Override
    public boolean isHandleProtocol(IProtocol protocol)
    {
        return protocol.serial() >= X101_HandShake.COMMAND && protocol.serial() <= X105_SslHandShake.COMMAND;
    }

    @Override
    public List<? extends IControl> handle(IManager manager, ISession session, IControl content)
    {

        switch(content.serial()) {
            case X103_Ping.COMMAND -> {
                return Collections.singletonList(new X104_Pong());
            }
        }
        return null;
    }

    @Override
    public ITriple onLink(IManager manager, ISession session, IControl input)
    {
        return null;
    }

    @Override
    public void onOffline(ISession session)
    {

    }

    @Override
    public List<ITriple> onConsistencyResult(IManager manager,
                                             long origin,
                                             IProtocol consensusBody,
                                             boolean isConsistency)
    {
        return null;
    }

}
