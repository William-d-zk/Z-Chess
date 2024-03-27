/*
 * MIT License
 *
 * Copyright (c) 2021. Z-Chess
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

package com.isahl.chess.audience.client.component;

import com.isahl.chess.king.base.disruptor.features.debug.IHealth;
import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.events.server.ILogicHandler;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeFailed;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeService;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeTransfer;
import com.isahl.chess.queen.io.core.features.model.session.IExchanger;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import java.util.List;

public class ClientHandler
        implements ILogicHandler
{
    private final Logger     _Logger = Logger.getLogger("test.audience." + getClass().getSimpleName());
    private final IHealth    _Health;
    private final IExchanger _Exchanger;

    public ClientHandler(IHealth health, IExchanger exchanger)
    {
        _Health = health;
        _Exchanger = exchanger;
    }

    @Override
    public IHealth _Health()
    {
        return _Health;
    }

    @Override
    public Logger getLogger()
    {
        return _Logger;
    }

    @Override
    public IExchanger getExchanger()
    {
        return _Exchanger;
    }

    @Override
    public IPipeTransfer logicTransfer()
    {
        return this::logicHandle;
    }

    @Override
    public IPipeService serviceTransfer()
    {
        return this::clientHandle;
    }

    private List<ITriple> logicHandle(IProtocol content, ISession session)
    {
        List<ITriple> results = null;

        _Logger.info("logic recv: %s", content);
        return results;
    }

    private List<ITriple> clientHandle(IoSerial request)
    {
        return null;
    }
}
