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
package com.isahl.chess.knight.cluster.spring.api;

import org.springframework.stereotype.Component;

import com.isahl.chess.bishop.io.ws.zchat.zprotocol.raft.X76_RaftNotify;
import com.isahl.chess.king.base.disruptor.event.inf.IOperator;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.knight.cluster.spring.model.ConsistentProtocol;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.queen.event.handler.cluster.IConsistentCustom;
import com.isahl.chess.queen.io.core.inf.IProtocol;
import com.isahl.chess.queen.io.core.inf.ITraceable;

@Component
public class ConsistentCustom
        implements
        IConsistentCustom
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    @Override
    public <T extends ITraceable & IProtocol> Void consistentHandle(T protocol, Throwable throwable)
    {
        if (throwable == null) {
            _Logger.debug("notify---consistent");
            if (protocol.serial() == X76_RaftNotify.COMMAND) {
                _Logger.debug("cluster mode");
                X76_RaftNotify x76 = (X76_RaftNotify) protocol;
                byte[] data = x76.getPayload();
                if (x76.load() == ConsistentProtocol._SERIAL) {
                    ConsistentProtocol consistentProtocol = JsonUtil.readValue(data, ConsistentProtocol.class);
                    consistentProtocol.decode(data);
                    _Logger.debug("notify ok");
                }
                else {
                    _Logger.fetal("consistent notify failed");
                }
            }
            else {
                _Logger.debug("single mode");
                _Logger.debug("notify ok");
            }
        }
        else {
            _Logger.warning("request:%s", throwable, protocol);
        }
        return null;
    }

    @Override
    public void adjudge(IProtocol consensus)
    {

    }

    @Override
    public <T extends ITraceable & IProtocol> IOperator<T,
                                                        Throwable,
                                                        Void> getOperator()
    {
        return this::consistentHandle;
    }
}
