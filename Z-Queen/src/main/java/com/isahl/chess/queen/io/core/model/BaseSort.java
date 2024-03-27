/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.queen.io.core.model;

import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.events.functions.*;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeDecoder;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.session.ICloser;
import com.isahl.chess.queen.io.core.features.model.session.ISessionFailed;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.net.socket.features.IAioSort;

public abstract class BaseSort<C extends IPContext>
        implements IAioSort<C>
{
    private final AioWriter            _AioWriter     = new AioWriter();
    private final ICloser        _CloseOperator = new Closer();
    private final ISessionFailed _ErrorOperator = new SessionFailed();
    private final IPipeEncoder   _Encoder       = new PipeEncoder(_AioWriter);
    private final IPipeDecoder         _Decoder       = new PipeDecoder();
    private final SessionIgnore        _Ignore        = new SessionIgnore();
    private final Mode                 _Mode;
    private final Type                 _Type;
    private final String               _Protocol;
    private final IoFactory<IProtocol> _Factory;

    protected BaseSort(Mode mode, Type type, String protocol)
    {
        _Mode = mode;
        _Type = type;
        _Protocol = protocol;
        _Factory = _SelectFactory();
    }

    protected BaseSort(Mode mode, Type type, String protocol, IoFactory<IProtocol> factory)
    {
        _Mode = mode;
        _Type = type;
        _Protocol = protocol;
        _Factory = factory;
    }

    @Override
    public Mode getMode()
    {
        return _Mode;
    }

    @Override
    public Type getType()
    {
        return _Type;
    }

    @Override
    public IPipeEncoder getEncoder()
    {
        return _Encoder;
    }

    @Override
    public IPipeDecoder getDecoder()
    {
        return _Decoder;
    }

    @Override
    public ICloser getCloser()
    {
        return _CloseOperator;
    }

    @Override
    public ISessionFailed getError()
    {
        return _ErrorOperator;
    }

    @Override
    public SessionIgnore getIgnore()
    {
        return _Ignore;
    }

    @Override
    public String getProtocol()
    {
        return _Protocol;
    }

    @Override
    public IoFactory<IProtocol> getFactory()
    {
        return _Factory;
    }

}
