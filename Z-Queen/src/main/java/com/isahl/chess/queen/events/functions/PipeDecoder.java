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
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.pipe.IPipeDecoder;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

/**
 * @author william.d.zk
 * @date 2019-05-08
 */
public class PipeDecoder
        implements IPipeDecoder
{

    @Override
    public ITriple handle(IPacket input, ISession session)
    {
        IControl<?>[] received = filterRead(input, session);
        /*
            一旦read出现异常将抛出到event-handler进行处理，
            无异常时才继续session.readNext()操作;
            不在 read 事件触发时直接进行readNext()，还将保护io-ring-buffer容量问题，
            防止过量事件挤占io-event-queue;
            当然这对大数据量高带宽传输支持不良，对session-context-read-buffer容量存在压力。
        */
        session.readNext();
        return received != null ? new Triple<>(received, session, session.getTransfer()) : null;
    }

    @Override
    public String getName()
    {
        return "operator.pipe.decoder";
    }
}
