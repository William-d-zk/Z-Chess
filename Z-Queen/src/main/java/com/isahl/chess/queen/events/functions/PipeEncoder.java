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

package com.isahl.chess.queen.events.functions;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeEncoder;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class PipeEncoder
        implements IPipeEncoder
{
    private final Logger    _Logger = Logger.getLogger("io.queen.operator." + getClass().getSimpleName());
    private final AioWriter _AioWriter;

    public PipeEncoder(AioWriter aioWriter)
    {
        _AioWriter = aioWriter;
    }

    @Override
    public ITriple handle(IProtocol output, ISession session)
    {
        IPacket send = protocolWrite(output, session);
        //write 错误将向event handler 抛出异常，并终止向session 执行写操作。
        if(send != null) {
            _Logger.debug("%s ", output);
            session.write(send, _AioWriter);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return "operator.pipe.encoder";
    }
}
