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

package com.isahl.chess.bishop.protocol.zchat.custom;

import com.isahl.chess.king.base.features.model.ITriple;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.Triple;
import com.isahl.chess.queen.events.routes.IMappingCustom;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IManager;
import com.isahl.chess.queen.io.core.features.model.session.ISession;

import static com.isahl.chess.king.base.disruptor.features.functions.IOperator.Type.WRITE;

/**
 * @author william.d.zk
 * @date 2020/5/7
 */
abstract class ZBaseMappingCustom<E extends IMappingCustom>
        implements IMappingCustom
{

    private final   Logger _Logger = Logger.getLogger("protocol.bishop." + getClass().getSimpleName());
    protected final E      _Then;

    protected ZBaseMappingCustom(E then)
    {
        _Then = then;
    }

    @Override
    public ITriple inject(IManager manager, ISession session, IProtocol content)
    {
        _Logger.debug("mapping receive %s", content);
        switch(content.serial()) {
            case 0x01:
            case 0x02:
            case 0x03:
            case 0x04:
            case 0x05:
            case 0x06:
                /*
                 * 内嵌逻辑，在ZCommandFilter中已经处理结束
                 * 此处仅执行转发逻辑
                 */
                return new Triple<>(content, null, WRITE);
            default:
                if(_Then == null) {return null;}
                return _Then.inject(manager, session, content);
        }
    }
}
