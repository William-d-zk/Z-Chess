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
package com.tgx.chess.knight.cluster.spring.service;

import org.springframework.stereotype.Component;

import com.tgx.chess.bishop.io.zprotocol.raft.X76_RaftResult;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.knight.cluster.spring.model.ConsistentProtocol;
import com.tgx.chess.knight.json.JsonUtil;
import com.tgx.chess.queen.event.handler.cluster.INotifyCustom;
import com.tgx.chess.queen.io.core.inf.IProtocol;

@Component
public class ConsistentCustom
        implements
        INotifyCustom
{
    private final Logger _Logger = Logger.getLogger("cluster.knight" + getClass().getSimpleName());

    @Override
    public Void handle(IProtocol protocol, Void aVoid)
    {
        _Logger.debug("notify---consistent");
        if (protocol.serial() == X76_RaftResult.COMMAND) {
            X76_RaftResult x76 = (X76_RaftResult) protocol;
            byte[] data = x76.getPayload();
            switch (x76.getPayloadSerial())
            {
                case ConsistentProtocol._SERIAL:
                    ConsistentProtocol consistentProtocol = JsonUtil.readValue(data, ConsistentProtocol.class);
                    consistentProtocol.decode(data);
                    break;
                default:
                    _Logger.fetal("consistent notify failed");
                    break;
            }
            _Logger.debug("notify ok");
        }
        return null;
    }

    @Override
    public String getName()
    {
        return "operator.consistent";
    }
}
