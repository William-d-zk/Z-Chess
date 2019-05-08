/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tgx.chess.bishop.io;

import com.tgx.chess.bishop.io.mqtt.bean.QttContext;
import com.tgx.chess.bishop.io.mqtt.filter.QttFrameFilter;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.AioWriter;
import com.tgx.chess.queen.event.operator.CloseOperator;
import com.tgx.chess.queen.event.operator.ErrorOperator;
import com.tgx.chess.queen.event.operator.PipeDecoder;
import com.tgx.chess.queen.event.operator.PipeEncoder;
import com.tgx.chess.queen.event.operator.TransferOperator;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public enum QttZSort
        implements
        ISort
{
    SYMMETRY
    {
        @Override
        IFilterChain<QttContext> getFilterChain()
        {
            return _QttFrameFilter;
        }

        @Override
        public Mode getMode()
        {
            return Mode.LINK;
        }

        @Override
        public Type getType()
        {
            return Type.SYMMETRY;
        }

    };
    final QttFrameFilter<QttContext>        _QttFrameFilter = new QttFrameFilter<>();
    private final CloseOperator<QttContext> _CloseOperator  = new CloseOperator<>();
    private final ErrorOperator<QttContext> _ErrorOperator  = new ErrorOperator<>(_CloseOperator);
    private final AioWriter<QttContext>     _AioWriter      = new AioWriter<>();
    private final IPipeEncoder<QttContext>  _Encoder        = new PipeEncoder<>(getFilterChain(),
                                                                                _ErrorOperator,
                                                                                _AioWriter);
    private TransferOperator<QttContext>    _Transfer       = new TransferOperator<>(_Encoder);
    private final IPipeDecoder<QttContext>  _Decoder        = new PipeDecoder<>(getFilterChain(), _Transfer);

    public IPipeEncoder<QttContext> getEncoder()
    {
        return _Encoder;
    }

    public IPipeDecoder<QttContext> getDecoder()
    {
        return _Decoder;
    }

    public CloseOperator<QttContext> getCloseOperator()
    {
        return _CloseOperator;
    }

    public TransferOperator<QttContext> getTransfer()
    {
        return _Transfer;
    }

    abstract IFilterChain<QttContext> getFilterChain();
}
