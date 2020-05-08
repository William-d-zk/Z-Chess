/*
 * MIT License
 *
 * Copyright (c) 2016~2020. Z-Chess
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
package com.tgx.chess.knight.cluster.spring.api;

import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftNotify;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.knight.cluster.spring.model.ConsistentProtocol;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.event.handler.cluster.INotifyCustom;
import com.tgx.chess.queen.io.core.inf.IConsistentProtocol;

@Component
public class ConsistentCustom
        implements
        INotifyCustom
{
    private final Logger _Logger = Logger.getLogger("cluster.knight." + getClass().getSimpleName());

    @Override
    public Void handle(IConsistentProtocol protocol, Throwable throwable)
    {
        if (throwable == null) {
            _Logger.debug("notify---consistent");
            if (protocol.serial() == X76_RaftNotify.COMMAND) {
                _Logger.debug("cluster mode");

                X76_RaftNotify x76 = (X76_RaftNotify) protocol;
                byte[] data = x76.getPayload();
                if (x76.getPayloadSerial() == ConsistentProtocol._SERIAL) {
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
    public String getName()
    {
        return "operator.consistent";
    }
}
