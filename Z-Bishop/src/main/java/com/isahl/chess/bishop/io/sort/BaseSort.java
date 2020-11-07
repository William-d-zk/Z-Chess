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

package com.isahl.chess.bishop.io.sort;

import com.isahl.chess.queen.event.inf.ISort;
import com.isahl.chess.queen.event.operator.AioWriter;
import com.isahl.chess.queen.event.operator.CloseOperator;
import com.isahl.chess.queen.event.operator.ErrorOperator;
import com.isahl.chess.queen.event.operator.IgnoreOperator;
import com.isahl.chess.queen.event.operator.PipeDecoder;
import com.isahl.chess.queen.event.operator.PipeEncoder;
import com.isahl.chess.queen.event.operator.TransferOperator;
import com.isahl.chess.queen.io.core.inf.IPContext;
import com.isahl.chess.queen.io.core.inf.IPipeDecoder;
import com.isahl.chess.queen.io.core.inf.IPipeEncoder;
import com.isahl.chess.queen.io.core.inf.IPipeTransfer;
import com.isahl.chess.queen.io.core.inf.ISessionCloser;
import com.isahl.chess.queen.io.core.inf.ISessionError;

public abstract class BaseSort<C extends IPContext<C>>
        implements
        ISort<C>
{
    private final AioWriter      _AioWriter     = new AioWriter();
    private final ISessionCloser _CloseOperator = new CloseOperator();
    private final ISessionError  _ErrorOperator = new ErrorOperator();
    private final IPipeEncoder   _Encoder       = new PipeEncoder(_AioWriter);
    private final IPipeTransfer  _Transfer      = new TransferOperator();
    private final IPipeDecoder   _Decoder       = new PipeDecoder();
    private final IgnoreOperator _Ignore        = new IgnoreOperator();
    private final Mode           _Mode;
    private final Type           _Type;
    private final String         _Protocol;

    protected BaseSort(Mode mode,
                       Type type,
                       String protocol)
    {
        _Mode = mode;
        _Type = type;
        _Protocol = protocol;
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
    public IPipeTransfer getTransfer()
    {
        return _Transfer;
    }

    @Override
    public ISessionCloser getCloser()
    {
        return _CloseOperator;
    }

    @Override
    public ISessionError getError()
    {
        return _ErrorOperator;
    }

    @Override
    public IgnoreOperator getIgnore()
    {
        return _Ignore;
    }

    @Override
    public String getProtocol()
    {
        return _Protocol;
    }

}
